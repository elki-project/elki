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
package elki.clustering.em;

import static elki.math.linearalgebra.VMath.argmax;

import java.util.ArrayList;
import java.util.List;

import elki.clustering.ClusteringAlgorithm;
import elki.clustering.em.models.EMClusterModel;
import elki.clustering.em.models.EMClusterModelFactory;
import elki.clustering.em.models.MultivariateGaussianModelFactory;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.MeanModel;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.MaterializedRelation;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.LongStatistic;
import elki.math.linearalgebra.VMath;
import elki.result.Metadata;
import elki.utilities.Priority;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import net.jafama.FastMath;

/**
 * Clustering by expectation maximization (EM-Algorithm), also known as Gaussian
 * Mixture Modeling (GMM), with optional MAP regularization.
 * <p>
 * In <em>soft mode</em> (default), cluster responsibilities are distributed
 * according to
 * the probabilities. In <em>hard assignment</em> mode, every point will be
 * assigned to the most likely cluster during optimization.
 * Independent of this choice, the algorithm will always output a hard cluster
 * assignment at the end for API compatibility, and the cluster probabilities
 * are attached to the result for further analysis if the "soft" option is set.
 * <p>
 * In hard assignment, clusters can become empty, and it is advisable to enable
 * MAP regularization (which can also help prevent other result degradation).
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
 * @param <O> object type to analyze
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
@Priority(Priority.RECOMMENDED)
public class EM<O, M extends MeanModel> implements ClusteringAlgorithm<Clustering<M>> {
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
  protected int k;

  /**
   * Delta parameter
   */
  protected double delta;

  /**
   * Factory for producing the initial cluster model.
   */
  protected EMClusterModelFactory<? super O, M> mfactory;

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
   * Use hard assignments (hard) during optimization.
   */
  protected boolean hard;

  /**
   * Retain soft assignments.
   */
  protected boolean soft;

  /**
   * Minimum loglikelihood to avoid -infinity.
   */
  protected static final double MIN_LOGLIKELIHOOD = -100000;

  /**
   * Soft assignment result type.
   */
  public static final SimpleTypeInformation<double[]> SOFT_TYPE = new SimpleTypeInformation<>(double[].class);

  /**
   * Constructor.
   * 
   * @param mfactory EM cluster model factory
   * @param k k parameter
   * @param delta delta parameter
   * @param miniter Minimum number of iterations
   * @param maxiter Maximum number of iterations
   * @param hard Use hard assignments during optimization
   * @param soft Include soft assignments
   * @param prior MAP prior
   */
  public EM(EMClusterModelFactory<? super O, M> mfactory, int k, double delta, int miniter, int maxiter, boolean hard, boolean soft, double prior) {
    super();
    this.mfactory = mfactory;
    this.k = k;
    this.delta = delta;
    this.miniter = miniter;
    this.maxiter = maxiter;
    this.hard = hard;
    this.soft = soft;
    this.prior = prior;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(mfactory.getInputTypeRestriction());
  }

  /**
   * Performs the EM clustering algorithm on the given database.
   * <p>
   * Finally a hard clustering is provided where each clusters gets assigned the
   * points exhibiting the highest probability to belong to this cluster. But
   * still, the database objects hold associated the complete probability-vector
   * for all models.
   * 
   * @param relation Relation
   * @return Clustering result
   */
  public Clustering<M> run(Relation<? extends O> relation) {
    if(relation.size() == 0) {
      throw new IllegalArgumentException("database empty: must contain elements");
    }
    // initial models
    List<? extends EMClusterModel<? super O, M>> models = mfactory.buildInitialModels(relation, k);
    WritableDataStore<double[]> probClusterIGivenX = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_SORTED, double[].class);
    double loglikelihood = assignProbabilitiesToInstances(relation, models, probClusterIGivenX, null);
    DoubleStatistic likestat = new DoubleStatistic(this.getClass().getName() + ".loglikelihood");
    LOG.statistics(likestat.setDouble(loglikelihood));

    // iteration unless no change
    int it = 0, lastimprovement = 0;
    double bestloglikelihood = Double.NEGATIVE_INFINITY;
    for(++it; it < maxiter || maxiter < 0; it++) {
      final double oldloglikelihood = loglikelihood;
      if(hard) {
        recomputeModelsHard(relation, probClusterIGivenX, models, prior);
      }
      else {
        recomputeModels(relation, probClusterIGivenX, models, prior);
      }
      // reassign probabilities
      loglikelihood = assignProbabilitiesToInstances(relation, models, probClusterIGivenX, null);

      LOG.statistics(likestat.setDouble(loglikelihood));
      if(loglikelihood - bestloglikelihood > delta) {
        lastimprovement = it;
        bestloglikelihood = loglikelihood;
      }
      if(it >= miniter && (Math.abs(loglikelihood - oldloglikelihood) <= delta || lastimprovement < it >> 1)) {
        break;
      }
    }
    LOG.statistics(new LongStatistic(KEY + ".iterations", it));

    // fill result with clusters and models
    List<ModifiableDBIDs> hardClusters = new ArrayList<>(k);
    for(int i = 0; i < k; i++) {
      hardClusters.add(DBIDUtil.newArray());
    }

    // provide a hard clustering
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      hardClusters.get(argmax(probClusterIGivenX.get(iditer))).add(iditer);
    }
    Clustering<M> result = new Clustering<>();
    Metadata.of(result).setLongName("EM Clustering");
    // provide models within the result
    for(int i = 0; i < k; i++) {
      result.addToplevelCluster(new Cluster<>(hardClusters.get(i), models.get(i).finalizeCluster()));
    }
    if(soft) {
      Metadata.hierarchyOf(result).addChild(new MaterializedRelation<>("EM Cluster Probabilities", SOFT_TYPE, relation.getDBIDs(), probClusterIGivenX));
    }
    else {
      probClusterIGivenX.destroy();
    }
    return result;
  }

  /**
   * Recompute the cluster models (covariance matrixes).
   * 
   * @param <O> Object type
   * @param relation Vector data
   * @param probClusterIGivenX Object probabilities
   * @param models Cluster models to update
   * @param prior MAP prior (use 0 for MLE)
   */
  public static <O> void recomputeModels(Relation<? extends O> relation, WritableDataStore<double[]> probClusterIGivenX, List<? extends EMClusterModel<? super O, ?>> models, double prior) {
    final int k = models.size();
    boolean needsTwoPass = false;
    for(EMClusterModel<?, ?> m : models) {
      m.beginEStep();
      needsTwoPass |= m.needsTwoPass();
    }
    // First pass, only for two-pass models.
    if(needsTwoPass) {
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        double[] clusterProbabilities = probClusterIGivenX.get(iditer);
        O instance = relation.get(iditer);
        for(int i = 0; i < k; i++) {
          final double prob = clusterProbabilities[i];
          if(prob > 1e-10) {
            models.get(i).firstPassE(instance, prob);
          }
        }
      }
      for(EMClusterModel<?, ?> m : models) {
        m.finalizeFirstPassE();
      }
    }
    double[] wsum = new double[k];
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double[] clusterProbabilities = probClusterIGivenX.get(iditer);
      O instance = relation.get(iditer);
      for(int i = 0; i < k; i++) {
        final double prob = clusterProbabilities[i];
        if(prob > 1e-10) {
          models.get(i).updateE(instance, prob);
        }
        wsum[i] += prob;
      }
    }
    for(int i = 0; i < k; i++) {
      // MLE / MAP
      final double weight = prior <= 0. ? wsum[i] / relation.size() : (wsum[i] + prior - 1) / (relation.size() + prior * k - k);
      models.get(i).finalizeEStep(weight, prior);
    }
  }

  /**
   * Recompute the cluster models (covariance matrixes) with hard assignment.
   * 
   * @param <O> Object type
   * @param relation Vector data
   * @param probClusterIGivenX Object probabilities
   * @param models Cluster models to update
   * @param prior MAP prior (use 0 for MLE)
   */
  public static <O> void recomputeModelsHard(Relation<? extends O> relation, WritableDataStore<double[]> probClusterIGivenX, List<? extends EMClusterModel<? super O, ?>> models, double prior) {
    final int k = models.size();
    boolean needsTwoPass = false;
    for(EMClusterModel<?, ?> m : models) {
      m.beginEStep();
      needsTwoPass |= m.needsTwoPass();
    }
    // First pass, only for two-pass models.
    if(needsTwoPass) {
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        int argmax = VMath.argmax(probClusterIGivenX.get(iditer));
        models.get(argmax).firstPassE(relation.get(iditer), 1.);
      }
      for(EMClusterModel<?, ?> m : models) {
        m.finalizeFirstPassE();
      }
    }
    double[] wsum = new double[k];
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      int argmax = VMath.argmax(probClusterIGivenX.get(iditer));
      models.get(argmax).updateE(relation.get(iditer), 1.);
      wsum[argmax] += 1.;
    }
    for(int i = 0; i < k; i++) {
      // MLE / MAP
      final double weight = prior <= 0. ? wsum[i] / relation.size() : (wsum[i] + prior - 1) / (relation.size() + prior * k - k);
      models.get(i).finalizeEStep(weight, prior);
    }
  }

  /**
   * Assigns the current probability values to the instances in the database and
   * compute the expectation value of the current mixture of distributions.
   * <p>
   * Computed as the sum of the logarithms of the prior probability of each
   * instance.
   * 
   * @param relation the database used for assignment to instances
   * @param models Cluster models
   * @param probClusterIGivenX Output storage for cluster probabilities
   * @param loglikelihoods Per-object log likelihood, for EM Outlier; may be
   *        {@code null} if not used
   * @param <O> Object type
   * @return the expectation value of the current mixture of distributions
   */
  public static <O> double assignProbabilitiesToInstances(Relation<? extends O> relation, List<? extends EMClusterModel<? super O, ?>> models, WritableDataStore<double[]> probClusterIGivenX, WritableDoubleDataStore loglikelihoods) {
    final int k = models.size();
    double emSum = 0.;
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      O vec = relation.get(iditer);
      double[] probs = probClusterIGivenX.get(iditer);
      probs = probs != null ? probs : new double[k];
      for(int i = 0; i < k; i++) {
        double v = models.get(i).estimateLogDensity(vec);
        probs[i] = v > MIN_LOGLIKELIHOOD ? v : MIN_LOGLIKELIHOOD;
      }
      final double logP = logSumExp(probs);
      for(int i = 0; i < k; i++) {
        probs[i] = FastMath.exp(probs[i] - logP);
      }
      probClusterIGivenX.put(iditer, probs);
      if(loglikelihoods != null) {
        loglikelihoods.put(iditer, logP);
      }
      emSum += logP;
    }
    return emSum / relation.size();
  }

  /**
   * Compute log(sum(exp(x_i))), with attention to numerical issues.
   * 
   * @param x Input
   * @return Result
   */
  public static double logSumExp(double[] x) {
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
    return acc > 1. ? (max + Math.log(acc)) : max;
  }

  /**
   * Compute log(exp(a)+exp(b)), with attention to numerical issues.
   *
   * @param a Input 1
   * @param b Input 2
   * @return Result
   */
  protected static double logSumExp(double a, double b) {
    return (a > b ? a : b) + Math.log(a > b ? FastMath.exp(b - a) + 1 : FastMath.exp(a - b) + 1);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par<O, M extends MeanModel> implements Parameterizer {
    /**
     * Parameter to specify the number of clusters to find.
     */
    public static final OptionID K_ID = new OptionID("em.k", "The number of clusters to find.");

    /**
     * Parameter to specify the termination criterion for maximization of E(M):
     * E(M) - E(M') &lt; em.delta, must be a double equal to or greater than 0.
     */
    public static final OptionID DELTA_ID = new OptionID("em.delta", //
        "The termination criterion for maximization of E(M): E(M) - E(M') < em.delta");

    /**
     * Parameter to specify the EM cluster models to use.
     */
    public static final OptionID MODEL_ID = new OptionID("em.model", "Model factory.");

    /**
     * Parameter to specify a minimum number of iterations.
     */
    public static final OptionID MINITER_ID = new OptionID("em.miniter", "Minimum number of iterations.");

    /**
     * Parameter to specify the maximum number of iterations.
     */
    public static final OptionID MAXITER_ID = new OptionID("em.maxiter", "Maximum number of iterations.");

    /**
     * Parameter to specify the MAP prior
     */
    public static final OptionID PRIOR_ID = new OptionID("em.map.prior", "Regularization factor for MAP estimation.");

    /**
     * Parameter to use hard assignments during optimization
     */
    public static final OptionID HARD_ID = new OptionID("em.hard", "Use hard (k-means-style hard) cluster assignments during optimization.");

    /**
     * Parameter to specify the saving of soft assignments
     */
    public static final OptionID SOFT_ID = new OptionID("em.soft", "Return soft assignment of clusters.");

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
    protected EMClusterModelFactory<O, M> mfactory;

    /**
     * Minimum number of iterations.
     */
    protected int miniter = 1;

    /**
     * Maximum number of iterations.
     */
    protected int maxiter = -1;

    /**
     * Prior to enable MAP estimation (use 0 for MLE).
     */
    double prior = 0.;

    /**
     * Use hard assignments during optimization.
     */
    boolean hard = false;

    /**
     * Return soft assignments?
     */
    boolean soft = false;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<EMClusterModelFactory<O, M>>(MODEL_ID, EMClusterModelFactory.class, MultivariateGaussianModelFactory.class) //
          .grab(config, x -> mfactory = x);
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
      new DoubleParameter(DELTA_ID, 1e-7)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .grab(config, x -> delta = x);
      new IntParameter(MINITER_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .setOptional(true) //
          .grab(config, x -> miniter = x);
      new IntParameter(MAXITER_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .setOptional(true) //
          .grab(config, x -> maxiter = x);
      new Flag(HARD_ID) //
          .grab(config, x -> hard = x);
      new Flag(SOFT_ID) //
          .grab(config, x -> soft = x);
      new DoubleParameter(PRIOR_ID) //
          .setOptional(true) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> prior = x);
    }

    @Override
    public EM<O, M> make() {
      return new EM<>(mfactory, k, delta, miniter, maxiter, hard, soft, prior);
    }
  }
}
