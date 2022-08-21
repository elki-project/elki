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
import elki.utilities.Alias;
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
 * Outlier Detection based on the accumulated distances of a point to its k
 * nearest neighbors.
 * <p>
 * As in the original publication (as far as we could tell from the pseudocode
 * included), the current point is not included in the nearest neighbors (see
 * figures in the publication). This matches the intuition common in nearest
 * neighbor classification, where the evaluated instances are not part of the
 * training set; but it contrasts to the pseudocode of the kNN outlier method
 * and the database interpretation (which returns all objects stored in the
 * database).
 * <p>
 * Furthermore, we report the sum of the k distances (called "weight" in the
 * original publication). Other implementations may return the average distance
 * instead, and therefore yield different results.
 * <p>
 * Reference:
 * <p>
 * F. Angiulli, C. Pizzuti<br>
 * Fast Outlier Detection in High Dimensional Spaces<br>
 * Proc. European Conf. Principles of Knowledge Discovery and Data Mining
 * (PKDD'02)
 *
 * @author Lisa Reichert
 * @since 0.3
 *
 * @has - - - KNNSearcher
 *
 * @param <O> the type of objects handled by this algorithm
 */
@Title("KNNWeight outlier detection")
@Description("Outlier detection based on the sum of distances of an object to its k nearest neighbors.")
@Reference(authors = "F. Angiulli, C. Pizzuti", //
    title = "Fast Outlier Detection in High Dimensional Spaces", //
    booktitle = "Proc. European Conf. Principles of Knowledge Discovery and Data Mining (PKDD'02)", //
    url = "https://doi.org/10.1007/3-540-45681-3_2", //
    bibkey = "DBLP:conf/pkdd/AngiulliP02")
@Alias({ "knnw" })
public class KNNWeightOutlier<O> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KNNWeightOutlier.class);

  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * Holds the number of nearest neighbors to query (plus the query point!)
   */
  protected int kplus;

  /**
   * Constructor with parameters.
   *
   * @param distance Distance function
   * @param k k parameter (not including query point!)
   */
  public KNNWeightOutlier(Distance<? super O> distance, int k) {
    super();
    this.distance = distance;
    this.kplus = k + 1; // Plus query point
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
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Compute kNN weights", relation.size(), LOG) : null;

    DoubleMinMax minmax = new DoubleMinMax();
    WritableDoubleDataStore knnw_score = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      final KNNList knn = knnQuery.getKNN(iditer, kplus);
      double skn = 0; // sum of the distances to the k nearest neighbors
      int i = 0; // number of neighbors so far
      for(DoubleDBIDListIter neighbor = knn.iter(); neighbor.getOffset() < kplus && neighbor.valid(); neighbor.advance()) {
        if(DBIDUtil.equal(iditer, neighbor)) {
          continue;
        }
        skn += neighbor.doubleValue();
        ++i;
      }
      if(i < kplus - 1) {
        // Less than k neighbors found
        // Approximative index, or k > data set size!
        skn = Double.POSITIVE_INFINITY;
      }
      knnw_score.putDouble(iditer, skn);
      minmax.put(skn);

      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);

    DoubleRelation res = new MaterializedDoubleRelation("kNN weight Outlier Score", relation.getDBIDs(), knnw_score);
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0., Double.POSITIVE_INFINITY, 0.);
    return new OutlierResult(meta, res);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Parameter to specify the k nearest neighbor.
     */
    public static final OptionID K_ID = new OptionID("knnwod.k", //
        "The k nearest neighbor, excluding the query point "//
            + "(i.e. query point is the 0-nearest-neighbor)");

    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    /**
     * k parameter
     */
    protected int k;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
    }

    @Override
    public KNNWeightOutlier<O> make() {
      return new KNNWeightOutlier<>(distance, k);
    }
  }
}
