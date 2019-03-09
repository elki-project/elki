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
import elki.database.datastore.WritableDBIDDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDVar;
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
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Nearest Neighbor Data Description.
 * <p>
 * A variation inbetween of KNN outlier and LOF, comparing the nearest neighbor
 * distance of a point to the nearest neighbor distance of the nearest neighbor.
 * <p>
 * The initial description used k=1, where this equation makes most sense.
 * For k &gt; 1, one may want to use averaging similar to LOF.
 * <p>
 * Reference ("1-Nearest-Neighbor method"):
 * <p>
 * D. de Ridder, D. M. J. Tax, R. P. W. Duin<br>
 * An experimental comparison of one-class classification methods<br>
 * Proc. 4th Ann. Conf. Advanced School for Computing and Imaging (ASCI'98)
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @has - - - KNNQuery
 *
 * @param <O> the type of objects processed by this algorithm
 */
@Title("KNNDD: k-Nearest Neighbor Data Description")
@Reference(authors = "D. de Ridder, D. M. J. Tax, R. P. W. Duin", //
    title = "An experimental comparison of one-class classification methods", //
    booktitle = "Proc. 4th Ann. Conf. Advanced School for Computing and Imaging (ASCI'98)", //
    url = "http://prlab.tudelft.nl/sites/default/files/asci_98.pdf", //
    bibkey = "conf/asci/deRidderTD98")
public class KNNDD<O> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KNNDD.class);

  /**
   * The parameter k (including query point!)
   */
  private int k;

  /**
   * Constructor for a single kNN query.
   *
   * @param distanceFunction distance function to use
   * @param k Value of k (excluding query point!)
   */
  public KNNDD(Distance<? super O> distanceFunction, int k) {
    super(distanceFunction);
    this.k = k + 1;
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   *
   * @param database Database (no longer used)
   * @param relation Data relation
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    return run(relation);
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   *
   * @param relation Data relation
   */
  public OutlierResult run(Relation<O> relation) {
    final DistanceQuery<O> distanceQuery = relation.getDistanceQuery(getDistance());
    final KNNQuery<O> knnQuery = relation.getKNNQuery(distanceQuery, k);

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("kNN distance for objects", relation.size(), LOG) : null;

    WritableDoubleDataStore knnDist = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    WritableDBIDDataStore neighbor = DataStoreUtil.makeDBIDStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    DBIDVar var = DBIDUtil.newVar();
    // Find nearest neighbors, and store the distances.
    for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
      final KNNList knn = knnQuery.getKNNForDBID(it, k);
      knnDist.putDouble(it, knn.getKNNDistance());
      neighbor.put(it, knn.assignVar(knn.size() - 1, var));
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);

    prog = LOG.isVerbose() ? new FiniteProgress("kNN distance descriptor", relation.size(), LOG) : null;
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_DB);
    DoubleMinMax minmax = new DoubleMinMax();
    for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
      // Distance
      double d = knnDist.doubleValue(it);
      // Distance of neighbor
      double nd = knnDist.doubleValue(neighbor.assignVar(it, var));
      double knndd = nd > 0 ? d / nd : d > 0 ? Double.POSITIVE_INFINITY : 1.;
      scores.put(it, knndd);
      minmax.put(knndd);
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    DoubleRelation scoreres = new MaterializedDoubleRelation("kNN Data Descriptor", relation.getDBIDs(), scores);
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0., Double.POSITIVE_INFINITY, 1.);
    return new OutlierResult(meta, scoreres);
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
     * Parameter to specify the k nearest neighbor
     */
    public static final OptionID K_ID = new OptionID("knndd.k", //
        "The k nearest neighbor, excluding the query point "//
            + "(i.e. query point is the 0-nearest-neighbor)");

    /**
     * k parameter
     */
    protected int k = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter kP = new IntParameter(K_ID, 1)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.getValue();
      }
    }

    @Override
    protected KNNDD<O> makeInstance() {
      return new KNNDD<>(distanceFunction, k);
    }
  }
}
