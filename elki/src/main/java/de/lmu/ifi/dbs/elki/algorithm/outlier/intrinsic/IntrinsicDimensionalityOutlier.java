package de.lmu.ifi.dbs.elki.algorithm.outlier.intrinsic;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.IntrinsicDimensionalityEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.MOMEstimator;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Use intrinsic dimensionality for outlier detection. This idea was first
 * explored by Michael Houle, Arthur Zimek, Jonathan von Brünken, et al., and is
 * provided for completeness and for visualization purposes. It turns out that
 * ID provides some insight into outlierness, but cannot be simply used as is.
 * Please see their upcoming publications for an improved solution.
 *
 * @author Erich Schubert
 * @since 0.3
 *
 * @param <O> Object type
 */
public class IntrinsicDimensionalityOutlier<O> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(IntrinsicDimensionalityOutlier.class);

  /**
   * Number of neighbors to use.
   */
  protected int k;

  /**
   * Estimator for intrinsic dimensionality.
   */
  protected IntrinsicDimensionalityEstimator estimator;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param k Neighborhood size
   * @param estimator Estimator for intrinsic dimensionality
   */
  public IntrinsicDimensionalityOutlier(DistanceFunction<? super O> distanceFunction, int k, IntrinsicDimensionalityEstimator estimator) {
    super(distanceFunction);
    this.k = k;
    this.estimator = estimator;
  }

  /**
   * Run the algorithm
   *
   * @param database Database
   * @param relation Data relation
   * @return Outlier result
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    final DistanceQuery<O> distanceQuery = database.getDistanceQuery(relation, getDistanceFunction());
    final KNNQuery<O> knnQuery = database.getKNNQuery(distanceQuery, k + 1);

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("kNN distance for objects", relation.size(), LOG) : null;

    double[] buf = new double[k + 10];
    DoubleMinMax minmax = new DoubleMinMax();
    WritableDoubleDataStore id_score = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    // compute distance to the k nearest neighbor.
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      // distance to the kth nearest neighbor
      // (assuming the query point is always included, with distance 0)
      KNNList knns = knnQuery.getKNNForDBID(iditer, k + 1);
      // Try to handle duplicates (TODO: incremental kNN API)
      while(knns.getKNNDistance() == 0.) {
        int k2 = knns.size() + k;
        if(k2 >= relation.size()) {
          throw new AbortException("Too many duplicates!");
        }
        knns = knnQuery.getKNNForDBID(iditer, k2);
        // Did not get the requested amount of neighbors (preprocessed?)
        if(knns.size() < k2) {
          break;
        }
      }

      // Ensure our buffer is large enough
      if(buf.length < knns.size()) {
        buf = new double[knns.size()];
      }
      // Copy data into buffer (to remove 0 distances)
      int p = 0;
      for(DoubleDBIDListIter it = knns.iter(); it.valid(); it.advance()) {
        if(it.doubleValue() == 0. || DBIDUtil.equal(iditer, it)) {
          continue;
        }
        buf[p++] = it.doubleValue();
      }
      double id = 0.;
      try {
        id = (p > 1) ? estimator.estimate(buf, p) : 0.;
      }
      catch(ArithmeticException e) {
        id = 0.;
      }

      id_score.putDouble(iditer, id);
      minmax.put(id);

      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    DoubleRelation scoreres = new MaterializedDoubleRelation("Intrinsic dimensionality", "id-score", id_score, relation.getDBIDs());
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0.0);
    return new OutlierResult(meta, scoreres);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
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
   * @apiviz.exclude
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Parameter for the number of neighbors.
     */
    public static final OptionID K_ID = new OptionID("id.k", "Number of nearest neighbors to use for ID estimation (usually 20-100).");

    /**
     * Class to use for estimating the ID.
     */
    public static final OptionID ESTIMATOR_ID = new OptionID("id.estimator", "Class to estimate ID from distance distribution.");

    /**
     * Number of neighbors to use for ID estimation.
     */
    protected int k;

    /**
     * Estimator for intrinsic dimensionality.
     */
    protected IntrinsicDimensionalityEstimator estimator;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(K_ID) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.intValue();
      }

      ObjectParameter<IntrinsicDimensionalityEstimator> estP = new ObjectParameter<>(ESTIMATOR_ID, IntrinsicDimensionalityEstimator.class, MOMEstimator.class);
      if(config.grab(estP)) {
        estimator = estP.instantiateClass(config);
      }
    }

    @Override
    protected IntrinsicDimensionalityOutlier<O> makeInstance() {
      return new IntrinsicDimensionalityOutlier<>(distanceFunction, k, estimator);
    }
  }
}
