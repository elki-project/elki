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
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
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
import elki.utilities.Alias;
import elki.utilities.Priority;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Outlier Detection based on the distance of an object to its k nearest
 * neighbor.
 * <p>
 * This implementation differs from the original pseudocode: the k nearest
 * neighbors do not exclude the point that is currently evaluated. I.e. for k=1
 * the resulting score is the distance to the 1-nearest neighbor that is not the
 * query point and therefore should match k=2 in the exact pseudocode - a value
 * of k=1 in the original code does not make sense, as the 1NN distance will be
 * 0 for every point in the database. If you for any reason want to use the
 * original algorithm, subtract 1 from the k parameter.
 * <p>
 * Reference:
 * <p>
 * S. Ramaswamy, R. Rastogi, K. Shim<br>
 * Efficient Algorithms for Mining Outliers from Large Data Sets.<br>
 * In: Proc. Int. Conf. on Management of Data (SIGMOD 2000)
 *
 * @author Lisa Reichert
 * @since 0.3
 *
 * @has - - - KNNSearcher
 *
 * @param <O> the type of objects handled by this algorithm
 */
@Title("KNN outlier: Efficient Algorithms for Mining Outliers from Large Data Sets")
@Description("Outlier Detection based on the distance of an object to its k nearest neighbor.")
@Reference(authors = "S. Ramaswamy, R. Rastogi, K. Shim", //
    title = "Efficient Algorithms for Mining Outliers from Large Data Sets", //
    booktitle = "Proc. Int. Conf. on Management of Data (SIGMOD 2000)", //
    url = "https://doi.org/10.1145/342009.335437", //
    bibkey = "DBLP:conf/sigmod/RamaswamyRS00")
@Alias({ "knno" })
@Priority(Priority.RECOMMENDED)
public class KNNOutlier<O> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KNNOutlier.class);

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
  public KNNOutlier(Distance<? super O> distance, int k) {
    super();
    this.distance = distance;
    this.kplus = k + 1; // INCLUDE the query point now
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
    DoubleMinMax minmax = new DoubleMinMax();
    WritableDoubleDataStore knno_score = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    // compute distance to the k nearest neighbor.
    for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
      // distance to the kth nearest neighbor
      // (assuming the query point is always included, with distance 0)
      final double dkn = knnQuery.getKNN(it, kplus).getKNNDistance();
      knno_score.putDouble(it, dkn);
      minmax.put(dkn);
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    DoubleRelation scoreres = new MaterializedDoubleRelation("kNN Outlier Score", relation.getDBIDs(), knno_score);
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0., Double.POSITIVE_INFINITY, 0.);
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
    public static final OptionID K_ID = new OptionID("knno.k", //
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
      new IntParameter(K_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
    }

    @Override
    public KNNOutlier<O> make() {
      return new KNNOutlier<>(distance, k);
    }
  }
}
