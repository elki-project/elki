/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.outlier.lof;

import elki.outlier.OutlierAlgorithm;
import elki.AbstractDistanceBasedAlgorithm;
import elki.data.spatial.SpatialComparable;
import elki.data.type.CombinedTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.DatabaseUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.DoubleDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.database.ids.DoubleDBIDListIter;
import elki.database.ids.KNNList;
import elki.database.query.knn.KNNQuery;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.progress.StepProgress;
import elki.math.DoubleMinMax;
import elki.math.MathUtil;
import elki.math.statistics.distribution.GammaDistribution;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

import net.jafama.FastMath;

/**
 * Variance of Volume for outlier detection.
 * <p>
 * The volume is estimated by the distance to the k-nearest neighbor, then the
 * variance of volume is computed.
 * <p>
 * Unfortunately, this approach needs an enormous numerical precision, and may
 * not work for high-dimensional, non-normalized data. We therefore divide each
 * volume by the average across the data set. This means values are even less
 * comparable across data sets, but this avoids some of the numerical problems
 * of this method.
 * <p>
 * Reference:
 * <p>
 * T. Hu, S. Y. Sung<br>
 * Detecting pattern-based outliers<br>
 * Pattern Recognition Letters 24(16)
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - KNNQuery
 *
 * @param <O> the type of data objects handled by this algorithm
 */
@Reference(authors = "T. Hu, S. Y. Sung", //
    title = "Detecting pattern-based outliers", //
    booktitle = "Pattern Recognition Letters 24(16)", //
    url = "https://doi.org/10.1016/S0167-8655(03)00165-X", //
    bibkey = "DBLP:journals/prl/HuS03")
public class VarianceOfVolume<O extends SpatialComparable> extends AbstractDistanceBasedAlgorithm<Distance<? super O>, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(VarianceOfVolume.class);

  /**
   * The number of neighbors to query (including the query point!)
   */
  protected int k;

  /**
   * Constructor.
   *
   * @param k the number of neighbors to use for comparison (excluding the query
   *        point)
   * @param distanceFunction the neighborhood distance function
   */
  public VarianceOfVolume(int k, Distance<? super O> distanceFunction) {
    super(distanceFunction);
    this.k = k + 1; // + query point
  }

  /**
   * Runs the VOV algorithm on the given database.
   *
   * @param database Database to query
   * @param relation Data to process
   * @return VOV outlier result
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress("VOV", 3) : null;
    DBIDs ids = relation.getDBIDs();
    int dim = RelationUtil.dimensionality(relation);

    LOG.beginStep(stepprog, 1, "Materializing nearest-neighbor sets.");
    KNNQuery<O> knnq = DatabaseUtil.precomputedKNNQuery(database, relation, getDistance(), k);

    // Compute Volumes
    LOG.beginStep(stepprog, 2, "Computing Volumes.");
    WritableDoubleDataStore vols = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    computeVolumes(knnq, dim, ids, vols);

    // compute VOV of each object
    LOG.beginStep(stepprog, 3, "Computing Variance of Volumes (VOV).");
    WritableDoubleDataStore vovs = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_DB);
    // track the maximum value for normalization.
    DoubleMinMax vovminmax = new DoubleMinMax();
    computeVOVs(knnq, ids, vols, vovs, vovminmax);

    LOG.setCompleted(stepprog);

    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Variance of Volume", ids, vovs);
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(vovminmax.getMin(), vovminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0.0);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * Compute volumes
   *
   * @param knnq KNN query
   * @param dim Data dimensionality
   * @param ids IDs to process
   * @param vols Volume storage
   */
  private void computeVolumes(KNNQuery<O> knnq, int dim, DBIDs ids, WritableDoubleDataStore vols) {
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Volume", ids.size(), LOG) : null;
    double scaleconst = MathUtil.SQRTPI * FastMath.pow(GammaDistribution.gamma(1 + dim * .5), -1. / dim);
    boolean warned = false;
    double sum = 0.;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      double dk = knnq.getKNNForDBID(iter, k).getKNNDistance();
      double vol = dk > 0 ? MathUtil.powi(dk * scaleconst, dim) : 0.;
      if(vol == Double.POSITIVE_INFINITY && !warned) {
        LOG.warning("Variance of Volumes has hit double precision limits, results are not reliable.");
        warned = true;
      }
      vols.putDouble(iter, vol);
      sum += vol;
      LOG.incrementProcessed(prog);
    }
    double scaling = ids.size() / sum;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      vols.putDouble(iter, vols.doubleValue(iter) * scaling);
    }
    LOG.ensureCompleted(prog);
  }

  /**
   * Compute variance of volumes.
   *
   * @param knnq KNN query
   * @param ids IDs to process
   * @param vols Volumes
   * @param vovs Variance of Volume storage
   * @param vovminmax Score minimum/maximum tracker
   */
  private void computeVOVs(KNNQuery<O> knnq, DBIDs ids, DoubleDataStore vols, WritableDoubleDataStore vovs, DoubleMinMax vovminmax) {
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Variance of Volume", ids.size(), LOG) : null;
    boolean warned = false;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      KNNList knns = knnq.getKNNForDBID(iter, k);
      DoubleDBIDListIter it = knns.iter();
      double vbar = 0.;
      for(; it.valid(); it.advance()) {
        vbar += vols.doubleValue(it);
      }
      vbar /= knns.size(); // Average
      double vov = 0.;
      for(it.seek(0); it.valid(); it.advance()) {
        double v = vols.doubleValue(it) - vbar;
        vov += v * v;
      }
      if(!(vov < Double.POSITIVE_INFINITY) && !warned) {
        LOG.warning("Variance of Volumes has hit double precision limits, results are not reliable.");
        warned = true;
      }
      vov = (vov < Double.POSITIVE_INFINITY) ? vov / (knns.size() - 1) : Double.POSITIVE_INFINITY;
      vovs.putDouble(iter, vov);
      // update minimum and maximum
      vovminmax.put(vov);
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(new CombinedTypeInformation(getDistance().getInputTypeRestriction(), TypeUtil.SPATIAL_OBJECT));
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Parameterizer<O extends SpatialComparable> extends AbstractDistanceBasedAlgorithm.Parameterizer<Distance<? super O>> {
    /**
     * Parameter to specify the number of nearest neighbors of an object to be
     * considered for computing its VOV score, must be an integer greater than
     * or equal to 1.
     */
    public static final OptionID K_ID = new OptionID("vov.k", "The number of nearest neighbors (not including the query point) of an object to be considered for computing its VOV score.");

    /**
     * The neighborhood size to use.
     */
    protected int k = 2;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      final IntParameter pK = new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(pK)) {
        k = pK.intValue();
      }
    }

    @Override
    protected VarianceOfVolume<O> makeInstance() {
      return new VarianceOfVolume<>(k, distanceFunction);
    }
  }
}
