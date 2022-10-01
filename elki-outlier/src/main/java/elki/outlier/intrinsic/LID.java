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
package elki.outlier.intrinsic;

import elki.Algorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.DoubleMinMax;
import elki.math.statistics.intrinsicdimensionality.IntrinsicDimensionalityEstimator;
import elki.math.statistics.intrinsicdimensionality.MOMEstimator;
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
 * Use intrinsic dimensionality for outlier detection.
 * <p>
 * Reference:
 * <p>
 * Michael E. Houle, Erich Schubert, Arthur Zimek<br>
 * On the Correlation Between Local Intrinsic Dimensionality and Outlierness<br>
 * Proc. 11th Int. Conf. Similarity Search and Applications (SISAP'2018)
 * <p>
 * This idea was also briefly explored before by Michael Houle, Arthur Zimek,
 * Jonathan von Brünken, et al.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <O> Object type
 */
@Title("LID: Intrinsic Dimensionality Outlier")
@Reference(authors = "Michael E. Houle, Erich Schubert, Arthur Zimek", //
    title = "On the Correlation Between Local Intrinsic Dimensionality and Outlierness", //
    booktitle = "Proc. 11th Int. Conf. Similarity Search and Applications (SISAP'2018)", //
    url = "https://doi.org/10.1007/978-3-030-02224-2_14", //
    bibkey = "DBLP:conf/sisap/HouleSZ18")
public class LID<O> implements OutlierAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(LID.class);

  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * Number of neighbors to use + query point.
   */
  protected int kplus;

  /**
   * Estimator for intrinsic dimensionality.
   */
  protected IntrinsicDimensionalityEstimator<? super O> estimator;

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param k Neighborhood size
   * @param estimator Estimator for intrinsic dimensionality
   */
  public LID(Distance<? super O> distance, int k, IntrinsicDimensionalityEstimator<? super O> estimator) {
    super();
    this.distance = distance;
    this.kplus = k + 1; // + query point
    this.estimator = estimator;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  /**
   * Run the algorithm
   *
   * @param relation Data relation
   * @return Outlier result
   */
  public OutlierResult run(Relation<O> relation) {
    QueryBuilder<O> qb = new QueryBuilder<>(relation, distance);
    DistanceQuery<O> distQ = qb.distanceQuery();
    KNNSearcher<DBIDRef> knnQ = qb.kNNByDBID(kplus);

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("kNN distance for objects", relation.size(), LOG) : null;

    DoubleMinMax minmax = new DoubleMinMax();
    WritableDoubleDataStore id_score = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double id = 0.;
      try {
        id = estimator.estimate(knnQ, distQ, iditer, kplus);
      }
      catch(ArithmeticException e) {
        // pass, use 0.
      }
      id_score.putDouble(iditer, id);
      minmax.put(id);
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    DoubleRelation scoreres = new MaterializedDoubleRelation("Intrinsic dimensionality", relation.getDBIDs(), id_score);
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0.0);
    return new OutlierResult(meta, scoreres);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Parameter for the number of neighbors.
     */
    public static final OptionID K_ID = new OptionID("id.k", "Number of nearest neighbors to use for ID estimation (usually 20-100).");

    /**
     * Class to use for estimating the ID.
     */
    public static final OptionID ESTIMATOR_ID = new OptionID("id.estimator", "Class to estimate ID from distance distribution.");

    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    /**
     * Number of neighbors to use for ID estimation.
     */
    protected int k;

    /**
     * Estimator for intrinsic dimensionality.
     */
    protected IntrinsicDimensionalityEstimator<? super O> estimator;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
      new ObjectParameter<IntrinsicDimensionalityEstimator<? super O>>(ESTIMATOR_ID, IntrinsicDimensionalityEstimator.class, MOMEstimator.class) //
          .grab(config, x -> estimator = x);
    }

    @Override
    public LID<O> make() {
      return new LID<>(distance, k, estimator);
    }
  }
}
