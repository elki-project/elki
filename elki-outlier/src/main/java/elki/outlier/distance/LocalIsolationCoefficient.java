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
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

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
 * @has - - - KNNSearcher
 *
 * @param <O> the type of objects handled by this algorithm
 */
@Reference(authors = "B. Yu, M. Song, L. Wang", //
    title = "Local Isolation Coefficient-Based Outlier Mining Algorithm", //
    booktitle = "Int. Conf. on Information Technology and Computer Science (ITCS) 2009", //
    url = "https://doi.org/10.1109/ITCS.2009.230", //
    bibkey = "doi:10.1109/ITCS.2009.230")
public class LocalIsolationCoefficient<O> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(LocalIsolationCoefficient.class);

  /**
   * Holds the number of nearest neighbors to query (plus the query point!)
   */
  private int kplus;

  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * Constructor with parameters.
   *
   * @param distance Distance function
   * @param k k Parameter (not including query point!)
   */
  public LocalIsolationCoefficient(Distance<? super O> distance, int k) {
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

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Compute Local Isolation Coefficients", relation.size(), LOG) : null;

    DoubleMinMax minmax = new DoubleMinMax();
    WritableDoubleDataStore lic_score = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
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
      double lic = knn.getKNNDistance() + (i > 0 ? skn / i : 0);
      lic_score.putDouble(iditer, lic);
      minmax.put(lic);
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);

    DoubleRelation res = new MaterializedDoubleRelation("Local Isolation Coefficient", relation.getDBIDs(), lic_score);
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
    public static final OptionID K_ID = new OptionID("lic.k", //
        "The k nearest neighbor, excluding the query point "//
            + "(i.e. query point is the 0-nearest-neighbor)");

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
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
    }

    @Override
    public LocalIsolationCoefficient<O> make() {
      return new LocalIsolationCoefficient<>(distance, k);
    }
  }
}
