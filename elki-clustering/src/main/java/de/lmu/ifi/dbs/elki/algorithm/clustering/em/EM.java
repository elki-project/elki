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
package de.lmu.ifi.dbs.elki.algorithm.clustering.em;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.argmax;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeans;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

import net.jafama.FastMath;

/**
 * Clustering by expectation maximization (EM-Algorithm), also known as Gaussian
 * Mixture Modeling (GMM), with optional MAP regularization.
 * <p>
 * Reference:
 * <p>
 * A. P. Dempster, N. M. Laird, D. B. Rubin:<br>
 * Maximum Likelihood from Incomplete Data via the EM algorithm.<br>
 * Journal of the Royal Statistical Society, Series B, 39(1), 1977, pp. 1-31
 * <p>
 * The MAP estimation is derived from
 * <p>
 * C. Fraley and A. E. Raftery<br>
 * Bayesian Regularization for Normal Mixture Estimation and Model-Based
 * Clustering<br>
 * J. Classification 24(2)
 * 
 * @author Arthur Zimek
 * @author Erich Schubert
 * @since 0.1
 * 
 * @composed - - - EMClusterModelFactory
 * 
 * @param <V> vector type to analyze
 * @param <M> model type to produce
 */
@Title("EM-Clustering: Clustering by Expectation Maximization")
@Description("Cluster data via Gaussian mixture modeling and the EM algorithm")
@Reference(authors = "A. P. Dempster, N. M. Laird, D. B. Rubin", //
    title = "Maximum Likelihood from Incomplete Data via the EM algorithm", //
    booktitle = "Journal of the Royal Statistical Society, Series B, 39(1)", //
    url = "http://www.jstor.org/stable/2984875", //
    bibkey = "journals/jroyastatsocise2/DempsterLR77")
@Reference(title = "Bayesian Regularization for Normal Mixture Estimation and Model-Based Clustering", //
    authors = "C. Fraley, A. E. Raftery", //
    booktitle = "J. Classification 24(2)", //
    url = "https://doi.org/10.1007/s00357-007-0004-5", //
    bibkey = "DBLP:journals/classification/FraleyR07")
@Alias("de.lmu.ifi.dbs.elki.algorithm.clustering.EM")
@Priority(Priority.RECOMMENDED)
public class EM<V extends NumberVector, M extends MeanModel> extends AbstractAlgorithm<Clustering<M>> implements ClusteringAlgorithm<Clustering<M>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(EM.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = EM.class.getName();

  /**
   * Number of clusters
   */
  private int k;

  /**
   * Delta parameter
   */
  private double delta;

  /**
   * Factory for producing the initial cluster model.
   */
  private EMClusterModelFactory<V, M> mfactory;

  /**
   * Maximum number of iterations to allow
   */
  private int maxiter;

  /**
   * Prior to enable MAP estimation (use 0 for MLE)
   */
  private double prior = 0.;

  /**
   * Retain soft assignments.
   */
  private boolean soft;

  /**
   * Minimum loglikelihood to avoid -infinity.
   */
  private static final double MIN_LOGLIKELIHOOD = -100000;

  /**
   * Soft assignment result type.
   */
  public static final SimpleTypeInformation<double[]> SOFT_TYPE = new SimpleTypeInformation<>(double[].class);

  /**
   * Constructor.
   *
   * @param k k parameter
   * @param delta delta parameter
   * @param mfactory EM cluster model factory
   */
  public EM(int k, double delta, EMClusterModelFactory<V, M> mfactory) {
    this(k, delta, mfactory, -1, 0., false);
  }

  /**
   * Constructor.
   *
   * @param k k parameter
   * @param delta delta parameter
   * @param mfactory EM cluster model factory
   * @param maxiter Maximum number of iterations
   * @param soft Include soft assignments
   */
  public EM(int k, double delta, EMClusterModelFactory<V, M> mfactory, int maxiter, boolean soft) {
    this(k, delta, mfactory, maxiter, 0., soft);
  }

  /**
   * Constructor.
   *
   * @param k k parameter
   * @param delta delta parameter
   * @param mfactory EM cluster model factory
   * @param maxiter Maximum number of iterations
   * @param prior MAP prior
   * @param soft Include soft assignments
   */
  public EM(int k, double delta, EMClusterModelFactory<V, M> mfactory, int maxiter, double prior, boolean soft) {
    super();
    this.k = k;
    this.delta = delta;
    this.mfactory = mfactory;
    this.maxiter = maxiter;
    this.prior = prior;
    this.soft = soft;
  }

  /**
   * Performs the EM clustering algorithm on the given database.
   *
   * Finally a hard clustering is provided where each clusters gets assigned the
   * points exhibiting the highest probability to belong to this cluster. But
   * still, the database objects hold associated the complete probability-vector
   * for all models.
   * 
   * @param database Database
   * @param relation Relation
   * @return Result
   */
  public Clustering<M> run(Database database, Relation<V> relation) {
    if(relation.size() == 0) {
      throw new IllegalArgumentException("database empty: must contain elements");
    }
    // initial models
    List<? extends EMClusterModel<M>> models = mfactory.buildInitialModels(database, relation, k, SquaredEuclideanDistanceFunction.STATIC);
    WritableDataStore<double[]> probClusterIGivenX = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_SORTED, double[].class);
    double loglikelihood = assignProbabilitiesToInstances(relation, models, probClusterIGivenX);
    DoubleStatistic likestat = LOG.isStatistics() ? new DoubleStatistic(this.getClass().getName() + ".loglikelihood") : null;
    if(LOG.isStatistics()) {
      LOG.statistics(likestat.setDouble(loglikelihood));
    }

    // iteration unless no change
    int it = 0, lastimprovement = 0;
    double bestloglikelihood = loglikelihood; // For detecting instabilities.
    for(++it; it < maxiter || maxiter < 0; it++) {
      final double oldloglikelihood = loglikelihood;
      recomputeCovarianceMatrices(relation, probClusterIGivenX, models, prior);
      // reassign probabilities
      loglikelihood = assignProbabilitiesToInstances(relation, models, probClusterIGivenX);

      if(LOG.isStatistics()) {
        LOG.statistics(likestat.setDouble(loglikelihood));
      }
      if(loglikelihood - bestloglikelihood > delta) {
        lastimprovement = it;
        bestloglikelihood = loglikelihood;
      }
      if(Math.abs(loglikelihood - oldloglikelihood) <= delta || lastimprovement < it >> 1) {
        break;
      }
    }
    if(LOG.isStatistics()) {
      LOG.statistics(new LongStatistic(KEY + ".iterations", it));
    }

    // fill result with clusters and models
    List<ModifiableDBIDs> hardClusters = new ArrayList<>(k);
    for(int i = 0; i < k; i++) {
      hardClusters.add(DBIDUtil.newArray());
    }

    // provide a hard clustering
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      hardClusters.get(argmax(probClusterIGivenX.get(iditer))).add(iditer);
    }
    Clustering<M> result = new Clustering<>("EM Clustering", "em-clustering");
    // provide models within the result
    for(int i = 0; i < k; i++) {
      result.addToplevelCluster(new Cluster<>(hardClusters.get(i), models.get(i).finalizeCluster()));
    }
    if(isSoft()) {
      result.addChildResult(new MaterializedRelation<>("cluster assignments", "em-soft-score", SOFT_TYPE, probClusterIGivenX, relation.getDBIDs()));
    }
    else {
      probClusterIGivenX.destroy();
    }
    return result;
  }

  /**
   * Recompute the covariance matrixes.
   * 
   * @param relation Vector data
   * @param probClusterIGivenX Object probabilities
   * @param models Cluster models to update
   * @param prior MAP prior (use 0 for MLE)
   */
  public static void recomputeCovarianceMatrices(Relation<? extends NumberVector> relation, WritableDataStore<double[]> probClusterIGivenX, List<? extends EMClusterModel<?>> models, double prior) {
    final int k = models.size();
    boolean needsTwoPass = false;
    for(EMClusterModel<?> m : models) {
      m.beginEStep();
      needsTwoPass |= m.needsTwoPass();
    }
    // First pass, only for two-pass models.
    if(needsTwoPass) {
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        double[] clusterProbabilities = probClusterIGivenX.get(iditer);
        NumberVector instance = relation.get(iditer);
        for(int i = 0; i < clusterProbabilities.length; i++) {
          final double prob = clusterProbabilities[i];
          if(prob > 1e-10) {
            models.get(i).firstPassE(instance, prob);
          }
        }
      }
      for(EMClusterModel<?> m : models) {
        m.finalizeFirstPassE();
      }
    }
    double[] wsum = new double[k];
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double[] clusterProbabilities = probClusterIGivenX.get(iditer);
      NumberVector instance = relation.get(iditer);
      for(int i = 0; i < clusterProbabilities.length; i++) {
        final double prob = clusterProbabilities[i];
        if(prob > 1e-10) {
          models.get(i).updateE(instance, prob);
        }
        wsum[i] += prob;
      }
    }
    for(int i = 0; i < models.size(); i++) {
      // MLE / MAP
      final double weight = prior <= 0. ? wsum[i] / relation.size() : (wsum[i] + prior - 1) / (relation.size() + prior * k - k);
      models.get(i).finalizeEStep(weight, prior);
    }
  }

  /**
   * Assigns the current probability values to the instances in the database and
   * compute the expectation value of the current mixture of distributions.
   * 
   * Computed as the sum of the logarithms of the prior probability of each
   * instance.
   * 
   * @param relation the database used for assignment to instances
   * @param models Cluster models
   * @param probClusterIGivenX Output storage for cluster probabilities
   * @return the expectation value of the current mixture of distributions
   */
  public static double assignProbabilitiesToInstances(Relation<? extends NumberVector> relation, List<? extends EMClusterModel<?>> models, WritableDataStore<double[]> probClusterIGivenX) {
    final int k = models.size();
    double emSum = 0.;

    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      NumberVector vec = relation.get(iditer);
      double[] probs = new double[k];
      for(int i = 0; i < k; i++) {
        double v = models.get(i).estimateLogDensity(vec);
        probs[i] = v > MIN_LOGLIKELIHOOD ? v : MIN_LOGLIKELIHOOD;
      }
      final double logP = logSumExp(probs);
      for(int i = 0; i < k; i++) {
        probs[i] = FastMath.exp(probs[i] - logP);
      }
      probClusterIGivenX.put(iditer, probs);
      emSum += logP;
    }
    return emSum / relation.size();
  }

  /**
   * Compute log(sum(exp(x_i)), with attention to numerical issues.
   * 
   * @param x Input
   * @return Result
   */
  private static double logSumExp(double[] x) {
    double max = x[0];
    for(int i = 1; i < x.length; i++) {
      final double v = x[i];
      max = v > max ? v : max;
    }
    final double cutoff = max - 35.350506209; // log_e(2**51)
    double acc = 0.;
    for(int i = 0; i < x.length; i++) {
      final double v = x[i];
      if(v > cutoff) {
        acc += v < max ? FastMath.exp(v - max) : 1.;
      }
    }
    return acc > 1. ? (max + FastMath.log(acc)) : max;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * @return the soft
   */
  public boolean isSoft() {
    return soft;
  }

  /**
   * @param soft the soft to set
   */
  public void setSoft(boolean soft) {
    this.soft = soft;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer<V extends NumberVector, M extends MeanModel> extends AbstractParameterizer {
    /**
     * Parameter to specify the number of clusters to find, must be an integer
     * greater than 0.
     */
    public static final OptionID K_ID = new OptionID("em.k", "The number of clusters to find.");

    /**
     * Parameter to specify the termination criterion for maximization of E(M):
     * E(M) - E(M') &lt; em.delta, must be a double equal to or greater than 0.
     */
    public static final OptionID DELTA_ID = new OptionID("em.delta", //
        "The termination criterion for maximization of E(M): " + //
            "E(M) - E(M') < em.delta");

    /**
     * Parameter to specify the EM cluster models to use.
     */
    public static final OptionID INIT_ID = new OptionID("em.model", //
        "Model factory.");

    /**
     * Parameter to specify the MAP prior
     */
    public static final OptionID PRIOR_ID = new OptionID("em.map.prior", //
        "Regularization factor for MAP estimation.");

    /**
     * Number of clusters.
     */
    protected int k;

    /**
     * Stopping threshold
     */
    protected double delta;

    /**
     * Initialization method
     */
    protected EMClusterModelFactory<V, M> initializer;

    /**
     * Maximum number of iterations.
     */
    protected int maxiter = -1;

    /**
     * Prior to enable MAP estimation (use 0 for MLE)
     */
    double prior = 0.;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.getValue();
      }

      ObjectParameter<EMClusterModelFactory<V, M>> initialP = new ObjectParameter<>(INIT_ID, EMClusterModelFactory.class, MultivariateGaussianModelFactory.class);
      if(config.grab(initialP)) {
        initializer = initialP.instantiateClass(config);
      }

      DoubleParameter deltaP = new DoubleParameter(DELTA_ID, 1e-7)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(deltaP)) {
        delta = deltaP.getValue();
      }

      IntParameter maxiterP = new IntParameter(KMeans.MAXITER_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .setOptional(true);
      if(config.grab(maxiterP)) {
        maxiter = maxiterP.getValue();
      }

      DoubleParameter priorP = new DoubleParameter(PRIOR_ID) //
          .setOptional(true) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(priorP)) {
        prior = priorP.doubleValue();
      }
    }

    @Override
    protected EM<V, M> makeInstance() {
      return new EM<>(k, delta, initializer, maxiter, prior, false);
    }
  }
}
