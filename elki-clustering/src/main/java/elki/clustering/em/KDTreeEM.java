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

import static elki.math.linearalgebra.VMath.*;

import java.util.ArrayList;
import java.util.List;

import elki.clustering.ClusteringAlgorithm;
import elki.clustering.em.KDTree.ClusterData;
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
import elki.math.MathUtil;
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

import net.jafama.FastMath;

/**
 * Clustering by expectation maximization (EM-Algorithm), also known as Gaussian
 * Mixture Modeling (GMM), calculated on a kd-tree. If supported, tries to prune
 * during calculation.
 * <p>
 * Reference:
 * <p>
 * A. W. Moore:<br>
 * Very Fast EM-based Mixture Model Clustering using Multiresolution
 * kd-trees.<br>
 * Neural Information Processing Systems (NIPS 1998)
 *
 * @author Robert Gehde
 */
@Title("Clustering by Expectation Maximization on a KD-Tree")
@Description("Cluster data via Gaussian mixture modeling and the KDTreeEM algorithm")
@Reference(authors = "Andrew W. Moore", //
    booktitle = "Advances in Neural Information Processing Systems 11 (NIPS 1998)", //
    title = "Very Fast EM-based Mixture Model Clustering using Multiresolution", //
    bibkey = "DBLP:conf/nips/Moore98")
public class KDTreeEM implements ClusteringAlgorithm<Clustering<EMModel>> {
  /**
   * Logging object
   */
  private static final Logging LOG = Logging.getLogger(KDTreeEM.class);

  /**
   * Factory for producing the initial cluster model.
   */
  private TextbookMultivariateGaussianModelFactory mfactory;

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
   * Drop one class if the maximum weight of a class in the bounding box is
   * lower than tauClass * wmin_max, where wmin_max is the maximum minimum
   * weight of all classes
   */
  private double tauClass;

  /**
   * minimum amount of iterations
   */
  private int miniter;

  /**
   * maximum amount of iterations
   */
  private int maxiter;

  /**
   * kd-tree object order
   */
  protected ArrayModifiableDBIDs sorted;

  /**
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
   * @param soft Include soft assignments
   */
  public KDTreeEM(int k, double mbw, double tau, double tauclass, double delta, TextbookMultivariateGaussianModelFactory mfactory, int miniter, int maxiter, boolean soft) {
    this.k = k;
    this.mbw = mbw;
    this.tau = tau;
    this.tauClass = tauclass;
    this.delta = delta;
    this.mfactory = mfactory;
    this.miniter = miniter;
    this.maxiter = maxiter;
    this.soft = soft;
  }

  /**
   * Calculates the EM Clustering with the given values by calling makeStats and
   * calculation the new models from the given results
   * 
   * @param relation Data Relation
   * @return Clustering KDTreeEM Clustering
   */
  public Clustering<EMModel> run(Relation<? extends NumberVector> relation) {
    DBIDIter iter = relation.iterDBIDs();

    // Build the kd-tree
    sorted = DBIDUtil.newArray(relation.getDBIDs());
    double[] dimWidth = analyseDimWidth(relation);
    Duration buildtime = LOG.newDuration(this.getClass().getName() + ".kdtree.buildtime").begin();
    KDTree tree = new KDTree(relation, sorted, 0, sorted.size(), dimWidth, mbw);
    LOG.statistics(buildtime.end());

    // Create initial models
    List<TextbookMultivariateGaussianModel> models = mfactory.buildInitialModels(relation, k);
    WritableDataStore<double[]> probClusterIGivenX = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_SORTED, double[].class);

    DoubleStatistic likeStat = new DoubleStatistic(this.getClass().getName() + ".loglikelihood");

    // Cache for the quadratic problem to reduce number of created arrays
    int dim = relation.get(iter).getDimensionality();
    ConstrainedQuadraticProblemSolver solver = new ConstrainedQuadraticProblemSolver(dim);
    double piPow = FastMath.pow(MathUtil.SQRTPI, dim);

    // iteration unless no change
    int it = 0, lastImprovement = 0;
    double bestLogLikelihood = Double.NEGATIVE_INFINITY, logLikelihood = 0.0;
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
        newstats[i] = new ClusterData(dim);
      }
      logLikelihood = tree.makeStats(models, indices, newstats, tau, tauClass, solver, piPow) / relation.size();
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

    logLikelihood = EM.assignProbabilitiesToInstances(relation, models, probClusterIGivenX);
    LOG.statistics(new LongStatistic(this.getClass().getName() + ".iterations", it));
    LOG.statistics(new DoubleStatistic(this.getClass().getName() + ".loglikelihood", logLikelihood));

    // provide a hard clustering
    // add each point to cluster of max density
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      hardClusters.get(argmax(probClusterIGivenX.get(iditer))).add(iditer);
    }
    Clustering<EMModel> result = new Clustering<>();
    Metadata.of(result).setLongName("KDTreeEM Clustering");
    // provide models within the result
    for(int i = 0; i < k; i++) {
      result.addToplevelCluster(new Cluster<>(hardClusters.get(i), models.get(i).finalizeCluster()));
    }
    if(soft) {
      Metadata.hierarchyOf(result).addChild(new MaterializedRelation<>("KDTreeEM Cluster Probabilities", SOFT_TYPE, relation.getDBIDs(), probClusterIGivenX));
    }
    else {
      probClusterIGivenX.destroy();
    }
    return result;
  }

  /**
   * Update cluster models according to the statistics calculated by makestats
   * 
   * @param newstats new statistics
   * @param models models to update
   * @param size number of elements to cluster
   */
  private void updateClusters(ClusterData[] newstats, List<TextbookMultivariateGaussianModel> models, int size) {
    for(int i = 0; i < k; i++) {
      if(newstats[i].summedLogWeights_apriori == Double.NEGATIVE_INFINITY) {
        LOG.warning("A model wasn't visited during tree traversion. The model has not been updated!");
        continue;
      }
      // for this model
      TextbookMultivariateGaussianModel model = models.get(i);

      // calculate model statistics according to the paper
      double weight = FastMath.exp(newstats[i].summedLogWeights_apriori) / size;
      double[] center = times(newstats[i].summedPoints_mean, FastMath.exp(-newstats[i].summedLogWeights_apriori));
      double[][] covariance = minusEquals(times(newstats[i].summedPointsSquared_cov, FastMath.exp(-newstats[i].summedLogWeights_apriori)), timesTranspose(center, center));

      // set Values of the model
      model.setWeight(weight);
      model.setCenter(center);
      model.updateCovariance(covariance);
    }
  }

  /**
   * Helper method to retrieve the widths of all data in all dimensions.
   * 
   * @param relation Relation to analyze
   * @return width of each dimension
   */
  private double[] analyseDimWidth(Relation<? extends NumberVector> relation) {
    DBIDIter it = relation.iterDBIDs();
    NumberVector first = relation.get(it);
    final int d = first.getDimensionality();
    double[] lowerBounds = first.toArray(), upperBounds = lowerBounds.clone();
    // find upper and lower bound
    for(it.advance(); it.valid(); it.advance()) {
      NumberVector x = relation.get(it);
      for(int i = 0; i < d; i++) {
        final double t = x.doubleValue(i);
        lowerBounds[i] = lowerBounds[i] < t ? lowerBounds[i] : t;
        upperBounds[i] = upperBounds[i] > t ? upperBounds[i] : t;
      }
    }
    return minusEquals(upperBounds, lowerBounds);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * @author Robert Gehde
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter to specify the number of clusters to find.
     */
    public static final OptionID K_ID = EM.Par.K_ID;

    /**
     * Parameter to specify the termination criterion for maximization of E(M):
     * E(M) - E(M') &lt; em.delta, must be a double equal to or greater than 0.
     */
    public static final OptionID DELTA_ID = EM.Par.DELTA_ID;

    /**
     * Parameter to specify the termination criterion for kd-tree construction.
     * Stop splitting nodes when the width is smaller then mbw * dataset_width.
     * Must be between 0 and 1.
     */
    public static final OptionID MBW_ID = new OptionID("emkd.mbw", //
        "Pruning criterion for the KD-Tree during construction. Stop splitting when leafwidth < mbw * width.");

    /**
     * Parameter to specify the pruning criterion during the algorithm.
     * Stop going down the kd-tree when possible weight error e < tau *
     * totalweight. Must be between 0 and 1. Low for precise, high for fast
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
     * Parameter to specify a minimum number of iterations
     */
    public static final OptionID MINITER_ID = EM.Par.MINITER_ID;

    /**
     * Parameter to specify a maximum number of iterations
     */
    public static final OptionID MAXITER_ID = EM.Par.MAXITER_ID;

    /**
     * Parameter to specify the saving of soft assignments
     */
    public static final OptionID SOFT_ID = EM.Par.SOFT_ID;

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
     * Cluster model factory.
     */
    protected TextbookMultivariateGaussianModelFactory mfactory;

    /**
     * Minimum number of iterations.
     */
    protected int miniter = 1;

    /**
     * Maximum number of iterations.
     */
    protected int maxiter = -1;

    /**
     * Retain soft assignments?
     */
    boolean soft = false;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
      new DoubleParameter(MBW_ID, 0.01)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE) //
          .grab(config, x -> mbw = x);
      new DoubleParameter(TAU_ID, 0.01)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE) //
          .grab(config, x -> tau = x);
      new DoubleParameter(TAU_CLASS_ID, 0.0001)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE) //
          .grab(config, x -> tauclass = x);
      new DoubleParameter(DELTA_ID, 1e-7)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .grab(config, x -> delta = x);
      mfactory = config.tryInstantiate(TextbookMultivariateGaussianModelFactory.class);
      new IntParameter(MINITER_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .setOptional(true) //
          .grab(config, x -> miniter = x);
      new IntParameter(MAXITER_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .setOptional(true) //
          .grab(config, x -> maxiter = x);
      new Flag(SOFT_ID) //
          .grab(config, x -> soft = x);
    }

    @Override
    public KDTreeEM make() {
      return new KDTreeEM(k, mbw, tau, tauclass, delta, mfactory, miniter, maxiter, soft);
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }
}
