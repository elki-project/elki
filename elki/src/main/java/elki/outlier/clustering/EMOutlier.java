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
package elki.outlier.clustering;

import java.util.List;

import elki.clustering.em.EM;
import elki.clustering.em.models.EMClusterModel;
import elki.clustering.em.models.EMClusterModelFactory;
import elki.clustering.em.models.MultivariateGaussianModelFactory;
import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.LongStatistic;
import elki.math.DoubleMinMax;
import elki.outlier.OutlierAlgorithm;
import elki.result.outlier.InvertedOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Outlier detection algorithm using EM Clustering.
 * <p>
 * If an object does not belong to any cluster it is supposed to be an outlier.
 * We use the log likelihood sum that the object is explained by the clustering
 * as anomaly score in this approach. If you use this implementation as
 * reference, please cite the latest ELKI release.
 * 
 * @author Erich Schubert
 * @since 0.3
 *
 * @param <V> Vector type
 */
@Title("EM Outlier: Outlier Detection based on the generic EM clustering")
@Description("The outlier score assigned is based on the highest cluster probability obtained from EM clustering.")
public class EMOutlier<V extends NumberVector> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(EM.class);

  /**
   * Number of clusters
   */
  protected int k;

  /**
   * Delta parameter
   */
  protected double delta;

  /**
   * Factory for producing the initial cluster model.
   */
  protected EMClusterModelFactory<? super V, ?> mfactory;

  /**
   * Minimum number of iterations to do
   */
  protected int miniter;

  /**
   * Maximum number of iterations to allow
   */
  protected int maxiter;

  /**
   * Prior to enable MAP estimation (use 0 for MLE)
   */
  protected double prior = 0.;

  /**
   * Minimum loglikelihood to avoid -infinity.
   */
  protected static final double MIN_LOGLIKELIHOOD = -100000;

  /**
   * Constructor.
   *
   * @param k k parameter
   * @param delta delta parameter
   * @param mfactory EM cluster model factory
   * @param miniter Minimum number of iterations
   * @param maxiter Maximum number of iterations
   * @param prior MAP prior
   */
  public EMOutlier(int k, double delta, EMClusterModelFactory<? super V, ?> mfactory, int miniter, int maxiter, double prior) {
    super();
    this.k = k;
    this.delta = delta;
    this.mfactory = mfactory;
    this.miniter = miniter;
    this.maxiter = maxiter;
    this.prior = prior;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   * 
   * @param relation Relation to process
   * @return Outlier result
   */
  public OutlierResult run(Relation<V> relation) {
    if(relation.size() == 0) {
      throw new IllegalArgumentException("database empty: must contain elements");
    }
    // initial models
    List<? extends EMClusterModel<? super V, ?>> models = mfactory.buildInitialModels(relation, k);
    WritableDataStore<double[]> probClusterIGivenX = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_SORTED, double[].class);
    WritableDoubleDataStore loglikelihoods = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_SORTED, Double.NEGATIVE_INFINITY);
    double loglikelihood = EM.assignProbabilitiesToInstances(relation, models, probClusterIGivenX, loglikelihoods);
    DoubleStatistic likestat = new DoubleStatistic(this.getClass().getName() + ".loglikelihood");
    LOG.statistics(likestat.setDouble(loglikelihood));

    // iteration unless no change
    int it = 0, lastimprovement = 0;
    double bestloglikelihood = Double.NEGATIVE_INFINITY;
    for(++it; it < maxiter || maxiter < 0; it++) {
      final double oldloglikelihood = loglikelihood;
      EM.recomputeCovarianceMatrices(relation, probClusterIGivenX, models, prior);
      // reassign probabilities
      loglikelihood = EM.assignProbabilitiesToInstances(relation, models, probClusterIGivenX, loglikelihoods);

      LOG.statistics(likestat.setDouble(loglikelihood));
      if(loglikelihood - bestloglikelihood > delta) {
        lastimprovement = it;
        bestloglikelihood = loglikelihood;
      }
      if(it >= miniter && (Math.abs(loglikelihood - oldloglikelihood) <= delta || lastimprovement < it >> 1)) {
        break;
      }
    }
    LOG.statistics(new LongStatistic(EMOutlier.class.getSimpleName() + ".iterations", it));

    DoubleMinMax mm = new DoubleMinMax();
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      mm.put(loglikelihoods.doubleValue(iditer));
    }
    DoubleRelation scoreres = new MaterializedDoubleRelation("EM Loglikelihoods", relation.getDBIDs(), loglikelihoods);
    OutlierScoreMeta meta = new InvertedOutlierScoreMeta(mm.getMin(), mm.getMax(), Double.NEGATIVE_INFINITY, 1);
    OutlierResult result = new OutlierResult(meta, scoreres);
    // TODO: also retain the EM clustering we did?
    return result;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par<V extends NumberVector> implements Parameterizer {
    /**
     * Number of clusters.
     */
    protected int k;

    /**
     * Stopping threshold
     */
    protected double delta;

    /**
     * Cluster model factory.
     */
    protected EMClusterModelFactory<V, ?> mfactory;

    /**
     * Minimum number of iterations.
     */
    protected int miniter = 1;

    /**
     * Maximum number of iterations.
     */
    protected int maxiter = -1;

    /**
     * Prior to enable MAP estimation (use 0 for MLE)
     */
    double prior = 0.;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(EM.Par.K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
      new ObjectParameter<EMClusterModelFactory<V, ?>>(EM.Par.MODEL_ID, EMClusterModelFactory.class, MultivariateGaussianModelFactory.class) //
          .grab(config, x -> mfactory = x);
      new DoubleParameter(EM.Par.DELTA_ID, 1e-7)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .grab(config, x -> delta = x);
      new IntParameter(EM.Par.MINITER_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .setOptional(true) //
          .grab(config, x -> miniter = x);
      new IntParameter(EM.Par.MAXITER_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .setOptional(true) //
          .grab(config, x -> maxiter = x);
      new DoubleParameter(EM.Par.PRIOR_ID) //
          .setOptional(true) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> prior = x);
    }

    @Override
    public EMOutlier<V> make() {
      return new EMOutlier<>(k, delta, mfactory, miniter, maxiter, prior);
    }
  }
}
