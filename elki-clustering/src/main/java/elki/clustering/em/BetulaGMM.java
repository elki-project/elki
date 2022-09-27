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
import java.util.Map;

import elki.clustering.ClusteringAlgorithm;
import elki.clustering.em.models.BetulaClusterModel;
import elki.clustering.em.models.BetulaClusterModelFactory;
import elki.clustering.kmeans.AbstractKMeans;
import elki.clustering.kmeans.KMeans;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.EMModel;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.MaterializedRelation;
import elki.database.relation.Relation;
import elki.index.tree.betula.CFTree;
import elki.index.tree.betula.features.ClusterFeature;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.Duration;
import elki.logging.statistics.LongStatistic;
import elki.result.Metadata;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.jafama.FastMath;

/**
 * Clustering by expectation maximization (EM-Algorithm), also known as Gaussian
 * Mixture Modeling (GMM), with optional MAP regularization. This version uses
 * the BIRCH cluster feature centers only for responsibility estimation; the CF
 * variances are only used for computing the models.
 * <p>
 * Reference:
 * <p>
 * Andreas Lang and Erich Schubert<br>
 * BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees<br>
 * Information Systems
 * 
 * @author Andreas Lang
 * @since 0.8.0
 */
@Reference(authors = "Andreas Lang and Erich Schubert", //
    title = "BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees", //
    booktitle = "Information Systems", //
    url = "https://doi.org/10.1016/j.is.2021.101918", //
    bibkey = "DBLP:journals/is/LangS22")
public class BetulaGMM implements ClusteringAlgorithm<Clustering<EMModel>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(BetulaGMMWeighted.class);

  /**
   * CFTree factory.
   */
  CFTree.Factory<?> cffactory;

  /**
   * Number of cluster centers to initialize.
   */
  int k;

  /**
   * Delta parameter
   */
  private double delta;

  /**
   * Maximum number of iterations.
   */
  int maxiter;

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
  protected static final double MIN_LOGLIKELIHOOD = -100000;

  /**
   * Soft assignment result type.
   */
  public static final SimpleTypeInformation<double[]> SOFT_TYPE = new SimpleTypeInformation<>(double[].class);

  /**
   * Maximum number of iterations.
   */
  BetulaClusterModelFactory<?> initializer;

  /**
   * Constructor.
   *
   * @param cffactory CFTree factory
   * @param k Number of clusters
   * @param maxiter Maximum number of iterations
   * @param soft Return soft clustering results
   * @param initialization Initialization method
   * @param prior MAP prior
   */
  public BetulaGMM(CFTree.Factory<?> cffactory, double delta, int k, int maxiter, boolean soft, BetulaClusterModelFactory<?> initialization, double prior) {
    super();
    this.cffactory = cffactory;
    this.delta = delta;
    this.k = k;
    this.maxiter = maxiter;
    this.soft = soft;
    this.initializer = initialization;
    this.prior = prior;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Run the clustering algorithm.
   *
   * @param relation Input data
   * @return Clustering
   */
  public Clustering<EMModel> run(Relation<NumberVector> relation) {
    if(relation.size() == 0) {
      throw new IllegalArgumentException("database empty: must contain elements");
    }
    // generate Tree
    CFTree<?> tree = cffactory.newTree(relation.getDBIDs(), relation, false);

    // Store clustering features:
    Duration modeltime = LOG.newDuration(getClass().getName() + ".modeltime").begin();
    ArrayList<? extends ClusterFeature> cfs = tree.getLeaves();
    // Initialize EM Model
    List<? extends BetulaClusterModel> models = initializer.buildInitialModels(cfs, k, tree);
    Map<ClusterFeature, double[]> probClusterIGivenX = new Reference2ObjectOpenHashMap<>(cfs.size());
    double loglikelihood = assignProbabilitiesToInstances(cfs, models, probClusterIGivenX);
    DoubleStatistic likestat = new DoubleStatistic(this.getClass().getName() + ".modelloglikelihood");
    LOG.statistics(likestat.setDouble(loglikelihood));

    // iteration unless no change
    int it = 0, lastimprovement = 0;
    double bestloglikelihood = Double.NEGATIVE_INFINITY; // log likelihood
    for(++it; it < maxiter || maxiter < 0; it++) {
      final double oldloglikelihood = loglikelihood;
      recomputeCovarianceMatrices(cfs, probClusterIGivenX, models, prior, tree.getRoot().getCF().getWeight());
      // reassign probabilities
      loglikelihood = assignProbabilitiesToInstances(cfs, models, probClusterIGivenX);

      LOG.statistics(likestat.setDouble(loglikelihood));
      if(loglikelihood - bestloglikelihood > delta) {
        lastimprovement = it;
        bestloglikelihood = loglikelihood;
      }
      if(Math.abs(loglikelihood - oldloglikelihood) <= delta || lastimprovement < it >> 1) {
        break;
      }
    }
    LOG.statistics(new LongStatistic(this.getClass().getName() + ".iterations", it));
    LOG.statistics(modeltime.end());

    // fill result with clusters and models
    List<ModifiableDBIDs> hardClusters = new ArrayList<>(k);
    for(int i = 0; i < k; i++) {
      hardClusters.add(DBIDUtil.newArray());
    }

    WritableDataStore<double[]> finalClusterIGivenX = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_SORTED, double[].class);
    loglikelihood = assignProbabilitiesToInstances(relation, models, finalClusterIGivenX);
    LOG.statistics(new DoubleStatistic(this.getClass().getName() + ".loglikelihood", loglikelihood));

    // provide a hard clustering
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      hardClusters.get(argmax(finalClusterIGivenX.get(iditer))).add(iditer);
    }
    Clustering<EMModel> result = new Clustering<>();
    Metadata.of(result).setLongName("EM Clustering");
    // provide models within the result
    for(int i = 0; i < k; i++) {
      result.addToplevelCluster(new Cluster<>(hardClusters.get(i), models.get(i).finalizeCluster()));
    }
    if(isSoft()) {
      Metadata.hierarchyOf(result).addChild(new MaterializedRelation<>("EM Cluster Probabilities", SOFT_TYPE, relation.getDBIDs(), finalClusterIGivenX));
    }
    return result;
  }

  private boolean isSoft() {
    return soft;
  }

  /**
   * Assigns the current probability values to the instances in the database and
   * compute the expectation value of the current mixture of distributions.
   * <p>
   * Computed as the sum of the logarithms of the prior probability of each
   * instance.
   * 
   * @param cfs the cluster features to evaluate
   * @param models Cluster models
   * @param probClusterIGivenX Output storage for cluster probabilities
   * @return the expectation value of the current mixture of distributions
   */
  public double assignProbabilitiesToInstances(ArrayList<? extends ClusterFeature> cfs, List<? extends BetulaClusterModel> models, Map<ClusterFeature, double[]> probClusterIGivenX) {
    final int k = models.size();
    double emSum = 0.;
    int n = 0;
    for(int i = 0; i < cfs.size(); i++) {
      ClusterFeature cfsi = cfs.get(i);
      double[] probs = new double[k];
      for(int j = 0; j < k; j++) {
        final double v = models.get(j).estimateLogDensity((NumberVector) cfsi);
        probs[j] = v > MIN_LOGLIKELIHOOD ? v : MIN_LOGLIKELIHOOD;
      }
      final double logP = EM.logSumExp(probs);
      for(int j = 0; j < k; j++) {
        probs[j] = FastMath.exp(probs[j] - logP);
      }
      probClusterIGivenX.put(cfsi, probs);
      emSum += logP * cfsi.getWeight();
      n += cfsi.getWeight();
    }
    return emSum / n;
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
   * @return the expectation value of the current mixture of distributions
   */
  public double assignProbabilitiesToInstances(Relation<? extends NumberVector> relation, List<? extends BetulaClusterModel> models, WritableDataStore<double[]> probClusterIGivenX) {
    final int k = models.size();
    double emSum = 0.;
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      NumberVector vec = relation.get(iditer);
      double[] probs = new double[k];
      for(int i = 0; i < k; i++) {
        double v = models.get(i).estimateLogDensity(vec);
        probs[i] = v > MIN_LOGLIKELIHOOD ? v : MIN_LOGLIKELIHOOD;
      }
      final double logP = EM.logSumExp(probs);
      for(int i = 0; i < k; i++) {
        probs[i] = FastMath.exp(probs[i] - logP);
      }
      probClusterIGivenX.put(iditer, probs);
      emSum += logP;
    }
    return emSum / relation.size();
  }

  /**
   * Recompute the covariance matrixes.
   * 
   * @param cfs Cluster features to evaluate
   * @param probClusterIGivenX Object probabilities
   * @param models Cluster models to update
   * @param prior MAP prior (use 0 for MLE)
   * @param n data set size
   */
  public void recomputeCovarianceMatrices(ArrayList<? extends ClusterFeature> cfs, Map<ClusterFeature, double[]> probClusterIGivenX, List<? extends BetulaClusterModel> models, double prior, int n) {
    final int k = models.size();
    boolean needsTwoPass = false;
    for(BetulaClusterModel m : models) {
      m.beginEStep();
      needsTwoPass |= m.needsTwoPass();
    }
    // First pass, only for two-pass models.
    if(needsTwoPass) {
      throw new IllegalStateException("Not Implemented");
    }
    double[] wsum = new double[k];
    for(int i = 0; i < cfs.size(); i++) {
      ClusterFeature cfsi = cfs.get(i);
      double[] clusterProbabilities = probClusterIGivenX.get(cfsi);
      for(int j = 0; j < clusterProbabilities.length; j++) {
        final double prob = clusterProbabilities[j];
        if(prob > 1e-10) {
          models.get(j).updateE(cfsi, prob * cfsi.getWeight());
        }
        wsum[j] += prob * cfsi.getWeight();
      }
    }
    for(int i = 0; i < models.size(); i++) {
      // MLE / MAP
      final double weight = prior <= 0. ? wsum[i] / n : (wsum[i] + prior - 1) / (n + prior * k - k);
      models.get(i).finalizeEStep(weight, prior);
    }
  }

  /**
   * Parameterizer
   * 
   * @author Andreas Lang
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter to specify the EM cluster models to use.
     */
    public static final OptionID INIT_ID = new OptionID("em.model", //
        "Model factory.");

    /**
     * Parameter to specify the termination criterion for maximization of E(M):
     * E(M) - E(M') &lt; em.delta, must be a double equal to or greater than 0.
     */
    public static final OptionID DELTA_ID = new OptionID("em.delta", //
        "The termination criterion for maximization of E(M): " + //
            "E(M) - E(M') < em.delta");

    /**
     * Parameter to specify the MAP prior
     */
    public static final OptionID PRIOR_ID = new OptionID("em.map.prior", "Regularization factor for MAP estimation.");

    /**
     * CFTree factory.
     */
    CFTree.Factory<?> cffactory;

    /**
     * k Parameter.
     */
    protected int k;

    /**
     * Maximum number of iterations.
     */
    protected int maxiter = -1;

    /**
     * Stopping threshold
     */
    protected double delta;

    /**
     * Retain soft assignments.
     */
    protected boolean soft;

    protected double prior = 0.;

    /**
     * initialization method
     */
    protected BetulaClusterModelFactory<?> initialization;

    @Override
    public void configure(Parameterization config) {
      cffactory = config.tryInstantiate(CFTree.Factory.class);
      new ObjectParameter<BetulaClusterModelFactory<?>>(INIT_ID, BetulaClusterModelFactory.class) //
          .grab(config, x -> initialization = x);
      new IntParameter(AbstractKMeans.K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
      new DoubleParameter(DELTA_ID, 1e-7)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .grab(config, x -> delta = x);
      new DoubleParameter(PRIOR_ID) //
          .setOptional(true) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> prior = x);
      new IntParameter(KMeans.MAXITER_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .setOptional(true) //
          .grab(config, x -> maxiter = x);
    }

    @Override
    public BetulaGMM make() {
      return new BetulaGMM(cffactory, delta, k, maxiter, soft, initialization, prior);
    }
  }
}
