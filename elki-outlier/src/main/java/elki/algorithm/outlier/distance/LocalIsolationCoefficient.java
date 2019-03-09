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
package elki.algorithm.outlier.distance;

import elki.algorithm.AbstractDistanceBasedAlgorithm;
import elki.algorithm.outlier.OutlierAlgorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DoubleDBIDListIter;
import elki.database.ids.KNNList;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNQuery;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.distancefunction.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.DoubleMinMax;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * The Local Isolation Coefficient is the sum of the kNN distance and the
 * average distance to its k nearest neighbors.
 * <p>
 * The algorithm originally used a normalized Manhattan distance on numerical
 * attributes, and Hamming distance on categorial attributes.
 * <p>
 * Reference:
 * <p>
 * B. Yu, M. Song, L. Wang<br>
 * Local Isolation Coefficient-Based Outlier Mining Algorithm<br>
 * Int. Conf. on Information Technology and Computer Science (ITCS) 2009
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - KNNQuery
 *
 * @param <O> the type of objects handled by this algorithm
 */
@Reference(authors = "B. Yu, M. Song, L. Wang", //
    title = "Local Isolation Coefficient-Based Outlier Mining Algorithm", //
    booktitle = "Int. Conf. on Information Technology and Computer Science (ITCS) 2009", //
    url = "https://doi.org/10.1109/ITCS.2009.230", //
    bibkey = "doi:10.1109/ITCS.2009.230")
public class LocalIsolationCoefficient<O> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(LocalIsolationCoefficient.class);

  /**
   * Holds the number of nearest neighbors to query (including query point!)
   */
  private int k;

  /**
   * Constructor with parameters.
   *
   * @param distanceFunction Distance function
   * @param k k Parameter (not including query point!)
   */
  public LocalIsolationCoefficient(Distance<? super O> distanceFunction, int k) {
    super(distanceFunction);
    this.k = k;
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   *
   * @param database Database context
   * @param relation Data relation
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    final DistanceQuery<O> distanceQuery = database.getDistanceQuery(relation, getDistance());
    KNNQuery<O> knnQuery = database.getKNNQuery(distanceQuery, k + 1); // +
                                                                       // query
                                                                       // point

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Compute Local Isolation Coefficients", relation.size(), LOG) : null;

    DoubleMinMax minmax = new DoubleMinMax();
    WritableDoubleDataStore lic_score = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      final KNNList knn = knnQuery.getKNNForDBID(iditer, k + 1); // + query
                                                                 // point
      double skn = 0; // sum of the distances to the k nearest neighbors
      int i = 0; // number of neighbors so far
      for(DoubleDBIDListIter neighbor = knn.iter(); i < k && neighbor.valid(); neighbor.advance()) {
        if(DBIDUtil.equal(iditer, neighbor)) {
          continue;
        }
        skn += neighbor.doubleValue();
        ++i;
      }
      double lic = knn.getKNNDistance() + (i > 0 ? skn / i : 0);
      lic_score.putDouble(iditer, lic);
      minmax.put(skn);

      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);

    DoubleRelation res = new MaterializedDoubleRelation("Local Isolation Coefficient", relation.getDBIDs(), lic_score);
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0., Double.POSITIVE_INFINITY, 0.);
    return new OutlierResult(meta, res);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistance().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Parameter to specify the k nearest neighbor.
     */
    public static final OptionID K_ID = new OptionID("lic.k", //
        "The k nearest neighbor, excluding the query point "//
            + "(i.e. query point is the 0-nearest-neighbor)");

    /**
     * k parameter
     */
    protected int k = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter kP = new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.getValue();
      }
    }

    @Override
    protected LocalIsolationCoefficient<O> makeInstance() {
      return new LocalIsolationCoefficient<>(distanceFunction, k);
    }
  }
}
