/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2020
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

import static elki.math.linearalgebra.VMath.times;
import static elki.math.linearalgebra.VMath.minus;
import static elki.math.linearalgebra.VMath.argmax;
import static elki.math.linearalgebra.VMath.timesTranspose;

import java.util.ArrayList;
import java.util.List;

import elki.clustering.ClusteringAlgorithm;
import elki.clustering.em.KDTree.ClusterData;
import elki.clustering.em.QuadraticProblem.ProblemData;
import elki.clustering.kmeans.KMeans;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.MeanModel;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.MaterializedRelation;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.Duration;
import elki.logging.statistics.LongStatistic;
import elki.math.linearalgebra.VMath;
import elki.result.Metadata;
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
 * Mixture Modeling (GMM), calculated on a kd-tree. If supported, tries to prune
 * during calculation.
 *
 * Reference:
 * <p>
 * A. W. Moore:<br>
 * Very Fast EM-based Mixture Model Clustering using Multiresolution
 * kd-trees.<br>
 * Neural Information Processing Systems (NIPS 1998)
 * <p>
 * 
 * @author Robert Gehde
 * @param <M> model type to produce
 */
@Title("EMKD-Clustering: Clustering by Expectation Maximization on a KD-Tree")
@Description("Cluster data via Gaussian mixture modeling and the EMKD algorithm")
@Reference(authors = "Andrew W. Moore", //
    booktitle = "Advances in Neural Information Processing Systems 11 (NIPS 1998)", //
    title = "Very Fast EM-based Mixture Model Clustering using Multiresolution", //
    bibkey = "DBLP:conf/nips/Moore98")
public class EMKD<M extends MeanModel> implements ClusteringAlgorithm<Clustering<M>> {
  /**
   * Factory for producing the initial cluster model.
   */
  private EMClusterModelFactory<NumberVector, M> mfactory;

  /**
   * Logging object
   */
  private static final Logging LOG = Logging.getLogger(EMKD.class);

  private static final double MIN_LOGLIKELIHOOD = -100000;

  /**
   * Retain soft assignments.
   */
  private boolean soft;

  /**
   * Delta parameter
   */
  private double delta;

  /**
   * Soft assignment result type.
   */
  public static final SimpleTypeInformation<double[]> SOFT_TYPE = new SimpleTypeInformation<>(double[].class);

  /**
   * number of models
   */
  private int k = 3;

  /**
   * minimum leaf size
   */
  private double mbw;

  /**
   * tau, low for precise, high for fast results.
   */
  private double tau;

  /**
   * drop one class if the maximum weight of a class in the bounding box is
   * lower than tauClass * wmin_max, where wmin_max is the maximum minimum
   * weight of all classes
   */
  private double tauClass;

  /**
   * if true, will check the difference of upper and lower limits of the models
   * to check for pruning opportunities
   */
  private boolean ignorePrune;

  /**
   * minimum amount of iterations
   */
  private int miniter;

  /**
   * maximum amount of iterations
   */
  private int maxiter;

  protected ArrayModifiableDBIDs sorted;

  /**
   * 
   * Constructor.
   *
   * @param k number of classes
   * @param mbw minimum relative size of leaf nodes
   * @param tau pruning parameter
   * @param tauclass pruning parameter for single classes
   * @param delta delta parameter
   * @param mfactory EM cluster model factory
   * @param miniter Minimum number of iterations
   * @param maxiter Maximum number of iterations
   * @param ignorePrune dont prune during calculation
   * @param soft Include soft assignments
   */
  public EMKD(int k, double mbw, double tau, double tauclass, double delta, EMClusterModelFactory<NumberVector, M> mfactory, int miniter, int maxiter, boolean ignorePrune, boolean soft) {
    this.k = k;
    this.mbw = mbw;
    this.tau = tau;
    this.tauClass = tauclass;
    this.delta = delta;
    this.mfactory = mfactory;
    this.miniter = miniter;
    this.maxiter = maxiter;
    this.ignorePrune = ignorePrune;
    this.soft = soft;
  }

  /**
   * Calculates the EM Clustering with the given values by calling makeStats and
   * calculation the new models from the given results
   * 
   * 
   * @param relation Data Relation
   * @return Clustering EMKD Clustering
   */
  public Clustering<M> run(Relation<? extends NumberVector> relation) {

    if(relation.size() == 0) {
      throw new IllegalArgumentException("database empty: must contain elements");
    }
    if(k == 0) {
      throw new IllegalArgumentException("k is 0: no clusters to calculate");
    }

    DBIDIter iter = relation.iterDBIDs();
    int d = relation.get(iter).getDimensionality();

    // Cache for the quadratic problem to reduce number of created arrays
    ProblemData[] dataArrays = new ProblemData[d];
    for(int i = 0; i < d; i++) {
      dataArrays[i] = new ProblemData(i + 1);
    }
    // build kd-tree
    sorted = DBIDUtil.newArray(relation.getDBIDs());
    double[] dimWidth = analyseDimWidth(relation);
    Duration buildtime = LOG.newDuration(this.getClass().getName() + ".Tree.buildtime").begin();
    KDTree tree = new KDTree(relation, sorted, 0, sorted.size(), dimWidth, mbw, dataArrays);
    LOG.statistics(buildtime.end());

    // initial models
    ArrayList<? extends EMClusterModel<NumberVector, M>> models = new ArrayList<EMClusterModel<NumberVector, M>>(mfactory.buildInitialModels(relation, k));
    WritableDataStore<double[]> probClusterIGivenX = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_SORTED, double[].class);
    if(!KDTree.supportsStoppingCondition(models)) {
      LOG.warning("Model list contains models that does not support the calculation of stopping conditions!");
    }
    // assuming all models are the same
    if(models.get(0) instanceof TwoPassMultivariateGaussianModel) {
      LOG.warning("TwoPassMultivariateGaussianModel has the same behaviour as MultivariateGaussianModel in EMKD\nbut doesn't support the calculation of stopping condition.\nBetter use MultivariateGaussianModel instead");
    }
    if(models.get(0) instanceof TextbookMultivariateGaussianModel) {
      LOG.warning("TextbookMultivariateGaussianModel has the same behaviour as MultivariateGaussianModel in EMKD\nbut doesn't support the calculation of stopping condition.\nBetter use MultivariateGaussianModel instead");
    }

    // double exactloglikelihood = assignProbabilitiesToInstances(relation,
    // models, probClusterIGivenX);
    DoubleStatistic likeStat = new DoubleStatistic(this.getClass().getName() + ".modelloglikelihood");

    // iteration unless no change
    int it = 0, lastImprovement = 0;
    double bestLogLikelihood = Double.NEGATIVE_INFINITY;
    double logLikelihood = 0.0;

    // double bestloglikelihood = loglikelihood; // For detecting instabilities.
    for(; it < maxiter || maxiter < 0; it++) {
      // Array that contains indices used in makeStats
      // Necessary because we drop unlikely classes in the progress
      int[] indices = new int[models.size()];
      for(int i = 0; i < indices.length; i++) {
        indices[i] = i;
      }

      final double oldLogLikelihood = logLikelihood;

      // recalculate probabilities
      ClusterData[] newstats = new ClusterData[k];
      for(int i = 0; i < newstats.length; i++) {
        newstats[i] = new ClusterData(d);
      }
      logLikelihood = tree.makeStats(models, indices, newstats, tau, tauClass, !ignorePrune) / relation.size();
      // LOG.warning(it+"; "+ Arrays.toString(KDTree.debugCount));
      // newstats now contains necessary info for updatecluster
      updateClusters(newstats, models, relation.size());
      // log new likelihood

      LOG.statistics(likeStat.setDouble(logLikelihood));
      // check stopping condition
      if(logLikelihood - bestLogLikelihood > delta) {
        lastImprovement = it;
        bestLogLikelihood = logLikelihood;
      }
      if(it >= miniter && (Math.abs(logLikelihood - oldLogLikelihood) <= delta || lastImprovement < it >> 1)) {
        break;
      }
    }

    // fill result with clusters and models
    List<ModifiableDBIDs> hardClusters = new ArrayList<>(k);
    for(int i = 0; i < k; i++) {
      hardClusters.add(DBIDUtil.newArray());
    }

    logLikelihood = assignProbabilitiesToInstances(relation, models, probClusterIGivenX);
    LOG.statistics(new LongStatistic(this.getClass().getName() + ".iterations", it));
    LOG.statistics(new DoubleStatistic(this.getClass().getName() + ".loglikelihood", logLikelihood));

    // provide a hard clustering
    // add each point to cluster of max density
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      hardClusters.get(argmax(probClusterIGivenX.get(iditer))).add(iditer);
    }
    Clustering<M> result = new Clustering<>();
    Metadata.of(result).setLongName("EMKD Clustering");
    // provide models within the result
    for(int i = 0; i < k; i++) {
      result.addToplevelCluster(new Cluster<>(hardClusters.get(i), models.get(i).finalizeCluster()));
    }
    if(soft) {
      Metadata.hierarchyOf(result).addChild(new MaterializedRelation<>("EMKD Cluster Probabilities", SOFT_TYPE, relation.getDBIDs(), probClusterIGivenX));
    }
    else {
      probClusterIGivenX.destroy();
    }
    return result;
  }

  /**
   * update Cluster models according to the statistics calculated by makestats
   * 
   * @param newstats new statistics
   * @param models models to update
   * @param size number of elements to cluster
   */
  private void updateClusters(ClusterData[] newstats, ArrayList<? extends EMClusterModel<NumberVector, M>> models, int size) {
    for(int i = 0; i < k; i++) {
      if(newstats[i].summedLogWeights_apriori == Double.NEGATIVE_INFINITY) {
        LOG.warning("A model wasn't visited during tree traversion. The model has not been updated!");
        continue;
      }
      // for this model
      EMClusterModel<NumberVector, M> model = models.get(i);

      // calculate model statistics according to the paper
      double weight = FastMath.exp(newstats[i].summedLogWeights_apriori - FastMath.log(size));
      double[] center = times(newstats[i].summedPoints_mean, 1. / FastMath.exp(newstats[i].summedLogWeights_apriori));
      double[][] covariance = minus(times(newstats[i].summedPointsSquared_cov, 1. / FastMath.exp(newstats[i].summedLogWeights_apriori)), timesTranspose(center, center));

      // set Values of the model
      model.setWeight(weight);
      model.setCenter(center);
      model.updateCovariance(covariance);
    }
  }

  /**
   * helper method to retrieve the widths of all data in all dimensions
   * 
   * @param relation Relation to analyze
   * @return width of each dimension
   */
  private double[] analyseDimWidth(Relation<? extends NumberVector> relation) {
    DBIDIter it = relation.iterDBIDs();
    int d = relation.get(it).getDimensionality();
    double[] lowerBounds = new double[d];
    double[] upperBounds = new double[d];
    for(int i = 0; i < d; i++) {
      lowerBounds[i] = Double.MAX_VALUE;
      upperBounds[i] = Double.MIN_VALUE;
    }
    // find upper and lower bound
    for(; it.valid(); it.advance()) {
      NumberVector x = relation.get(it);
      for(int i = 0; i < d; i++) {
        double t = x.doubleValue(i);
        lowerBounds[i] = lowerBounds[i] < t ? lowerBounds[i] : t;
        upperBounds[i] = upperBounds[i] > t ? upperBounds[i] : t;
      }
    }
    // calculate widths
    return VMath.minus(upperBounds, lowerBounds);
  }

  /**
   * Taken from EM, used for final model assignment.
   * 
   * 
   * Assigns the current probability values to the instances in the database and
   * compute the expectation value of the current mixture of distributions.
   * <p>
   * Computed as the sum of the logarithms of the prior probability of each
   * instance.
   * 
   * @param relation the database used for assignment to instances
   * @param models Cluster models
   * @param probClusterIGivenX Output storage for cluster probabilities
   * @param <O> Object type
   * @return the expectation value of the current mixture of distributions
   */
  public static <O> double assignProbabilitiesToInstances(Relation<? extends O> relation, List<? extends EMClusterModel<O, ?>> models, WritableDataStore<double[]> probClusterIGivenX) {
    final int k = models.size();
    double emSum = 0.;

    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      O vec = relation.get(iditer);
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

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * @author Robert Gehde
   */
  public static class Par<M extends MeanModel> implements Parameterizer {
    /**
     * Parameter to specify the number of clusters to find, must be an integer
     * greater than 0.
     */
    public static final OptionID K_ID = new OptionID("emkd.k", "The number of clusters to find.");

    /**
     * Parameter to specify the termination criterion for kd-tree construction.
     * Stop splitting nodes when the width is smaller then mbw * dataset_width.
     * Must be between 0 and 1.
     */
    public static final OptionID MBW_ID = new OptionID("emkd.mbw", //
        "Pruning criterion for the KD-Tree during construction. Stop splitting when leafwidth < mbw * width.");

    /**
     * Parameter to specify the pruning criterium during the algorithm.
     * Stop going down the kd-tree when possible weight error e < tau *
     * totalweight. Musst be between 0 and 1. Low for precise, high for fast
     * results.
     */
    public static final OptionID TAU_ID = new OptionID("emkd.tau", //
        "Pruning criterion for the KD-Tree during algorithm. Stop traversing when error e < tau * totalweight.");

    /**
     * drop one class if the maximum weight of a class in the bounding box is
     * lower than tauClass * wmin_max, where wmin_max is the maximum minimum
     * weight of all classes
     */
    public static final OptionID TAU_CLASS_ID = new OptionID("emkd.tauclass", //
        "Parameter for pruning. Drop a class if w[c] < tauclass * max(wmins). Set to 0 to disable dropping of classes.");

    /**
     * Parameter to specify the termination criterion for maximization of E(M):
     * E(M) - E(M') &lt; em.delta, must be a double equal to or greater than 0.
     */
    public static final OptionID DELTA_ID = new OptionID("emkd.delta", //
        "The termination criterion for maximization of E(M): E(M) - E(M') < em.delta.");

    /**
     * Parameter to specify the EM cluster models to use.
     */
    public static final OptionID INIT_ID = new OptionID("emkd.model", "Model factory.");

    /**
     * Parameter to specify a minimum number of iterations
     */
    public static final OptionID MINITER_ID = new OptionID("emkd.miniter", "Minimum number of iterations.");

    /**
     * Parameter to specify if pruning should be ignored
     */
    public static final OptionID IGNORE_PRUNE_ID = new OptionID("emkd.ignorePrune", "Set to ignore pruning checks during calculation.");

    /**
     * Number of clusters.
     */
    protected int k;

    /**
     * construction threshold
     */
    protected double mbw;

    /**
     * cutoff threshold
     */
    protected double tau;

    /**
     * cutoff safety threshold
     */
    protected double tauclass;

    /**
     * Stopping threshold
     */
    protected double delta;

    /**
     * Initialization method
     */
    protected EMClusterModelFactory<NumberVector, M> initializer;

    /**
     * Minimum number of iterations.
     */
    protected int miniter = 1;

    /**
     * Maximum number of iterations.
     */
    protected int maxiter = -1;

    /**
     * Pruning on or off
     */
    protected boolean ignorePrune = true;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
      new DoubleParameter(MBW_ID, 0.01)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE) //
          .grab(config, x -> mbw = x);
      new Flag(IGNORE_PRUNE_ID) //
          .grab(config, x -> ignorePrune = x);
      new DoubleParameter(TAU_ID, 0.01)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE) //
          .grab(config, x -> tau = x);
      new DoubleParameter(TAU_CLASS_ID, 0.0001)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE) //
          .grab(config, x -> tauclass = x);
      new ObjectParameter<EMClusterModelFactory<NumberVector, M>>(INIT_ID, EMClusterModelFactory.class, MultivariateGaussianModelFactory.class) //
          .grab(config, x -> initializer = x);
      new DoubleParameter(DELTA_ID, 1e-7)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .grab(config, x -> delta = x);
      new IntParameter(MINITER_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .setOptional(true) //
          .grab(config, x -> miniter = x);
      new IntParameter(KMeans.MAXITER_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .setOptional(true) //
          .grab(config, x -> maxiter = x);
    }

    @Override
    public EMKD<M> make() {
      return new EMKD<>(k, mbw, tau, tauclass, delta, initializer, miniter, maxiter, ignorePrune, false);
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }
}
