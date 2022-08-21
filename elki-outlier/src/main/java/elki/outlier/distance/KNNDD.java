/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.outlier.distance;

import elki.Algorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDBIDDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.knn.KNNSearcher;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.DoubleMinMax;
import elki.outlier.OutlierAlgorithm;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

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
 * @has - - - KNNSearcher
 *
 * @param <O> the type of objects processed by this algorithm
 */
@Title("KNNDD: k-Nearest Neighbor Data Description")
@Reference(authors = "D. de Ridder, D. M. J. Tax, R. P. W. Duin", //
    title = "An experimental comparison of one-class classification methods", //
    booktitle = "Proc. 4th Ann. Conf. Advanced School for Computing and Imaging (ASCI'98)", //
    url = "http://prlab.tudelft.nl/sites/default/files/asci_98.pdf", //
    bibkey = "conf/asci/deRidderTD98")
public class KNNDD<O> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KNNDD.class);

  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * The parameter k (plus query point!)
   */
  protected int kplus;

  /**
   * Constructor for a single kNN query.
   *
   * @param distance distance function to use
   * @param k Value of k (excluding query point!)
   */
  public KNNDD(Distance<? super O> distance, int k) {
    super();
    this.distance = distance;
    this.kplus = k + 1;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   *
   * @param relation Data relation
   */
  public OutlierResult run(Relation<O> relation) {
    KNNSearcher<DBIDRef> knnQuery = new QueryBuilder<>(relation, distance).kNNByDBID(kplus);
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("kNN distance for objects", relation.size(), LOG) : null;

    WritableDoubleDataStore knnDist = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    WritableDBIDDataStore neighbor = DataStoreUtil.makeDBIDStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    DBIDVar var = DBIDUtil.newVar();
    // Find nearest neighbors, and store the distances.
    for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
      final KNNList knn = knnQuery.getKNN(it, kplus);
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

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Parameter to specify the k nearest neighbor
     */
    public static final OptionID K_ID = new OptionID("knndd.k", //
        "The k nearest neighbor, excluding the query point (i.e. query point is the 0-nearest-neighbor)");

    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    /**
     * k parameter
     */
    protected int k = 0;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new IntParameter(K_ID, 1)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
    }

    @Override
    public KNNDD<O> make() {
      return new KNNDD<>(distance, k);
    }
  }
}
