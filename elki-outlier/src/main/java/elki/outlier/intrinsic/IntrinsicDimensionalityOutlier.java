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
package elki.outlier.intrinsic;

import elki.algorithm.AbstractDistanceBasedAlgorithm;
import elki.outlier.OutlierAlgorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNQuery;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.DoubleMinMax;
import elki.math.statistics.intrinsicdimensionality.IntrinsicDimensionalityEstimator;
import elki.math.statistics.intrinsicdimensionality.MOMEstimator;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
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
@Reference(authors = "Michael E. Houle, Erich Schubert, Arthur Zimek", //
    title = "On the Correlation Between Local Intrinsic Dimensionality and Outlierness", //
    booktitle = "Proc. 11th Int. Conf. Similarity Search and Applications (SISAP'2018)", //
    // TODO: add url when DOI is assigned.
    bibkey = "DBLP:conf/sisap/HouleSZ18")
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
  public IntrinsicDimensionalityOutlier(Distance<? super O> distanceFunction, int k, IntrinsicDimensionalityEstimator estimator) {
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
    final DistanceQuery<O> distanceQuery = database.getDistanceQuery(relation, getDistance());
    final KNNQuery<O> knnQuery = database.getKNNQuery(distanceQuery, k + 1);

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("kNN distance for objects", relation.size(), LOG) : null;

    DoubleMinMax minmax = new DoubleMinMax();
    WritableDoubleDataStore id_score = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double id = 0.;
      try {
        id = estimator.estimate(knnQuery, iditer, k + 1);
      }
      catch(ArithmeticException e) {
        // pass
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
