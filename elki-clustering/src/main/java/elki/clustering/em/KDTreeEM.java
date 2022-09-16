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

import static elki.math.linearalgebra.VMath.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import elki.clustering.ClusteringAlgorithm;
import elki.clustering.em.models.TextbookMultivariateGaussianModel;
import elki.clustering.em.models.TextbookMultivariateGaussianModelFactory;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.data.model.EMModel;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.ids.*;
import elki.database.relation.MaterializedRelation;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.Duration;
import elki.logging.statistics.LongStatistic;
import elki.math.MathUtil;
import elki.math.linearalgebra.ConstrainedQuadraticProblemSolver;
import elki.result.Metadata;
import elki.utilities.datastructures.arraylike.IntegerArray;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
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
 * @since 0.8.0
 */
@Description("Gaussian mixture modeling accelerated using a kd-tree")
@Reference(authors = "Andrew W. Moore", //
    booktitle = "Advances in Neural Information Processing Systems 11 (NIPS 1998)", //
    title = "Very Fast EM-based Mixture Model Clustering using Multiresolution kd-trees", //
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
   * @param exactAssign Perform exact assignments at the end
   */
  public KDTreeEM(int k, double mbw, double tau, double tauclass, double delta, TextbookMultivariateGaussianModelFactory mfactory, int miniter, int maxiter, boolean soft, boolean exactAssign) {
    this.k = k;
    this.mbw = mbw;
    this.tau = tau;
    this.tauClass = tauclass;
    this.delta = delta;
    this.mfactory = mfactory;
    this.miniter = miniter;
    this.maxiter = maxiter;
    this.soft = soft;
    this.exactAssign = exactAssign;
  }

  /**
   * Current clusters.
   */
  private List<TextbookMultivariateGaussianModel> models;

  /**
   * Models for next iteration.
   */
  private List<TextbookMultivariateGaussianModel> newmodels;

  /**
   * Solver for quadratic problems
   */
  private ConstrainedQuadraticProblemSolver solver;

  /**
   * Gaussian scaling factor for likelihood.
   */
  private double ipiPow;

  /**
   * Cluster weights
   */
  private double[] wsum;

  /**
   * Perform exact cluster assignments
   */
  protected boolean exactAssign = false;

  /**
   * Calculates the EM Clustering with the given values by calling makeStats and
   * calculation the new models from the given results
   * 
   * @param relation Data Relation
   * @return Clustering KDTreeEM Clustering
   */
  public Clustering<EMModel> run(Relation<? extends NumberVector> relation) {
    DBIDIter iter = relation.iterDBIDs();
    int dim = relation.get(iter).getDimensionality();

    // Build the kd-tree
    sorted = DBIDUtil.newArray(relation.getDBIDs());
    double[] dimWidth = analyseDimWidth(relation);
    Duration buildtime = LOG.newDuration(this.getClass().getName() + ".kdtree.buildtime").begin();
    KDTree tree = new KDTree(relation, sorted, 0, sorted.size(), dimWidth, mbw);
    LOG.statistics(buildtime.end());

    // Create initial models
    models = mfactory.buildInitialModels(relation, k);
    // Models for the next iteration
    newmodels = new ArrayList<>(k);
    for(int i = 0; i < k; i++) {
      newmodels.add(new TextbookMultivariateGaussianModel(0, new double[dim]));
    }
    wsum = new double[k];

    DoubleStatistic likeStat = new DoubleStatistic(this.getClass().getName() + ".loglikelihood");

    // Cache for the quadratic problem to reduce number of created arrays
    solver = new ConstrainedQuadraticProblemSolver(dim);
    ipiPow = 1 / FastMath.pow(MathUtil.SQRTPI, dim);

    // iteration unless no change
    int it = 0, lastImprovement = 0;
    double bestLogLikelihood = Double.NEGATIVE_INFINITY, logLikelihood = 0.0;
    for(; it < maxiter || maxiter < 0; it++) {
      // Array that contains indices used in makeStats
      // Necessary because we drop unlikely classes in the progress
      final double oldLogLikelihood = logLikelihood;
      for(TextbookMultivariateGaussianModel c : newmodels) {
        c.beginEStep();
      }
      Arrays.fill(wsum, 0.);
      logLikelihood = makeStats(tree, MathUtil.sequence(0, k), null) / relation.size();
      for(int i = 0; i < k; i++) {
        final double weight = wsum[i] / relation.size();
        if(weight <= Double.MIN_NORMAL) {
          LOG.warning("A cluster has degenerated by pruning.");
          newmodels.get(i).clone(models.get(i));
          continue;
        }
        newmodels.get(i).finalizeEStep(weight, 0);
      }
      // Swap old and new models:
      List<TextbookMultivariateGaussianModel> tmp = newmodels;
      newmodels = models;
      models = tmp;
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

    // TODO: use approximate values from the kd-tree here, too!
    WritableDataStore<double[]> probClusterIGivenX = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_SORTED, double[].class);
    if(exactAssign) {
      logLikelihood = EM.assignProbabilitiesToInstances(relation, models, probClusterIGivenX, null);
    }
    else {
      logLikelihood = makeStats(tree, MathUtil.sequence(0, k), probClusterIGivenX) / relation.size();
    }
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
    solver = null;
    newmodels = null;
    return result;
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
   * This methods checks the different stopping conditions given in the paper,
   * thus calculating the Dimensions, that will be considered for child-trees.
   * If this method returns a non-empty subset of the input dimension set, it
   * means that missing dimensions are dropped because their weight was too
   * small. If it returns a null array it means that the expected error of all
   * remaining models is small enough to consider this node a leaf node.
   * 
   * @param node kd tree node
   * @param indices list of indices to check
   * @return indices that are not pruned, null if everything was pruned
   */
  private int[] checkStoppingCondition(KDTree node, int[] indices) {
    if(!(models.get(0) instanceof TextbookMultivariateGaussianModel)) {
      return indices;
    }
    // Calculate limits of the given Model inside the bounding box
    double[][] maxPnts = new double[models.size()][node.sum.length];
    double[][] minPnts = new double[models.size()][node.sum.length];
    double[][] limits = new double[models.size()][2];
    for(int i : indices) {
      calculateModelLimits(node, models.get(i), minPnts[i], maxPnts[i], limits[i]);
    }
    // calculate the complete sum of weighted limits for denominator calculation
    double maxDenomTotal = 0.0, minDenomTotal = 0.0;
    for(int i : indices) {
      maxDenomTotal += models.get(i).getWeight() * limits[i][0];
      minDenomTotal += models.get(i).getWeight() * limits[i][1];
    }
    boolean prune = true;
    double maxMinWeight = Double.NEGATIVE_INFINITY;
    double[] wmaxs = new double[models.size()];
    final double size = node.right - node.left;
    for(int i : indices) {
      // calculate denominators for minimum/maximum weight estimation
      double weight = models.get(i).getWeight();
      double wminDenom = maxDenomTotal + weight * (limits[i][0] - limits[i][1]);
      double wmaxDenom = minDenomTotal + weight * (limits[i][1] - limits[i][0]);
      // calculate minimum weight estimation
      double wmin = MathUtil.clamp((weight * limits[i][0]) / wminDenom, 0, 1);
      maxMinWeight = wmin > maxMinWeight ? wmin : maxMinWeight;
      // calculate maximum weight estimation
      wmaxs[i] = MathUtil.clamp((weight * limits[i][1]) / wmaxDenom, 0, 1);
      double minPossibleWeight = newmodels.get(i).getWeight() + wmin * size;
      // calculate the maximum possible error in this node
      double maximumError = size * (wmaxs[i] - wmin);
      // pruning check, if error to big for this model, don't prune
      if(maximumError > tau * minPossibleWeight) {
        prune = false;
      }
    }
    if(prune) { // Everything pruned.
      return null;
    }
    if(tauClass <= 0.0) { // No class pruning.
      return indices;
    }
    IntegerArray result = new IntegerArray(indices.length);
    for(int i : indices) {
      // drop one class if the maximum weight of a class in the bounding box
      // is lower than tauClass * maxMinWeight, where maxMinWeight is the
      // maximum minimal weight estimate of all classes
      if(wmaxs[i] >= tauClass * maxMinWeight) {
        result.add(i);
      }
    }
    // return updated list of indices
    return result.toArray();
  }

  /**
   * Calculates the model limits inside this node by translating the Gaussian
   * model into a squared function.
   * 
   * @param model model to calculate the limits for
   * @param minpnt result array for argmin
   * @param maxpnt result array for argmax
   * @param ret Return array
   */
  private void calculateModelLimits(KDTree node, TextbookMultivariateGaussianModel model, double[] minpnt, double[] maxpnt, double[] ret) {
    double[] min = minus(node.midpoint, node.halfwidth); // will be modified!
    double[] max = plusTimes(min, node.halfwidth, 2); // will be modified!
    model.calculateModelLimits(min, max, solver, ipiPow, minpnt, maxpnt, ret);
  }

  /**
   * Calculates the statistics on the kd-tree needed for the calculation of the
   * new models
   * 
   * @param node next node
   * @param indices list of indices to use in calculation, initially all
   * @param probs cluster assignment
   * @return log likelihood of the model
   */
  private double makeStats(KDTree node, int[] indices, WritableDataStore<double[]> probs) {
    // Only one possible cluster remaining.
    final int size = node.right - node.left;
    if(indices.length == 1) {
      DoubleVector midpoint = DoubleVector.wrap(times(node.sum, 1.0 / size));
      double logDenSum = models.get(indices[0]).estimateLogDensity(midpoint);
      wsum[indices[0]] += size;
      newmodels.get(indices[0]).updateE(node.sum, node.sumSq, 1., size);
      if(probs != null) {
        double[] p = new double[k];
        p[indices[0]] = 1;
        for(DBIDArrayIter it = sorted.iter().seek(node.left); it.getOffset() < node.right; it.advance()) {
          probs.put(it, p);
        }
      }
      return logDenSum * size;
    }
    // check for pruning possibility
    if(node.leftChild != null) {
      int[] nextIndices = checkStoppingCondition(node, indices);
      if(nextIndices != null) {
        return makeStats(node.leftChild, nextIndices, probs) + makeStats(node.rightChild, nextIndices, probs);
      }
    }
    DoubleVector midpoint = DoubleVector.wrap(times(node.sum, 1. / size));
    // logarithmic probabilities of clusters in this node
    double[] logProb = new double[indices.length];
    for(int i = 0; i < indices.length; i++) {
      logProb[i] = models.get(indices[i]).estimateLogDensity(midpoint);
    }
    double logDenSum = EM.logSumExp(logProb);
    minusEquals(logProb, logDenSum); // total probability 1
    // calculate necessary statistics at this node
    double[] ps = probs != null ? new double[k] : null;
    for(int i = 0; i < indices.length; i++) {
      final double p = FastMath.exp(logProb[i]);
      wsum[indices[i]] += p * size;
      newmodels.get(indices[i]).updateE(node.sum, node.sumSq, p, p * size);
      if(ps != null) {
        ps[indices[i]] = p;
      }
    }
    if(probs != null) {
      for(DBIDArrayIter it = sorted.iter().seek(node.left); it.getOffset() < node.right; it.advance()) {
        probs.put(it, ps);
      }
    }
    return logDenSum * size;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * KDTree class with the statistics needed for EM clustering.
   * 
   * @author Robert Gehde
   */
  static class KDTree {
    /**
     * Child nodes:
     */
    KDTree leftChild, rightChild;

    /**
     * Interval in sorted list
     */
    int left, right;

    /**
     * Sum of contained vectors
     */
    double[] sum;

    /**
     * Sum over all squared elements (x^T * x),
     * needed for covariance calculation
     */
    double[][] sumSq;

    /**
     * Middle point of bounding box
     */
    double[] midpoint;

    /**
     * Half width of the rectangle.
     */
    double[] halfwidth;

    /**
     * Constructor for a KDTree with statistics needed for KDTreeEM calculation.
     * Uses points between the indices left and right for calculation
     *
     * @param relation datapoints for the construction
     * @param sorted sorted id array
     * @param left leftmost datapoint used for construction
     * @param right rightmost datapoint used for construction
     * @param dimWidth Array containing the width of all dimensions on the
     *        complete dataset
     * @param mbw factor when to stop construction. Stop if splitdimwidth &lt;
     *        mbw * dimwidth[splitdim]
     */
    public KDTree(Relation<? extends NumberVector> relation, ArrayModifiableDBIDs sorted, int left, int right, double[] dimWidth, double mbw) {
      DBIDArrayIter iter = sorted.iter();
      int dim = relation.get(iter).toArray().length;

      // Store data range
      this.left = left;
      this.right = right;
      computeBoundingBox(relation, iter);

      // Decide if we need to split:
      int splitDim = argmax(halfwidth);
      double maxDiff = 2 * halfwidth[splitDim];
      if(maxDiff < mbw * dimWidth[splitDim]) {
        // Aggregate data for leaf directly:
        aggregateStats(relation, iter, dim);
        return;
      }
      // Split points at midpoint according to paper
      double splitPoint = midpoint[splitDim];
      int l = left, r = right - 1;
      while(true) {
        while(l <= r && relation.get(iter.seek(l)).doubleValue(splitDim) <= splitPoint) {
          ++l;
        }
        while(l <= r && relation.get(iter.seek(r)).doubleValue(splitDim) >= splitPoint) {
          --r;
        }
        if(l >= r) {
          break;
        }
        sorted.swap(l++, r--);
      }

      assert relation.get(iter.seek(r)).doubleValue(splitDim) <= splitPoint : relation.get(iter.seek(r)).doubleValue(splitDim) + " not less than " + splitPoint;
      ++r;
      if(r != right) { // Could be all duplicate points!
        leftChild = new KDTree(relation, sorted, left, r, dimWidth, mbw);
        rightChild = new KDTree(relation, sorted, r, right, dimWidth, mbw);
        // aggregate statistics from child nodes
        sum = plus(leftChild.sum, rightChild.sum);
        sumSq = plus(leftChild.sumSq, rightChild.sumSq);
      }
      else {
        // Aggregate data for leaf:
        aggregateStats(relation, iter, dim);
      }
    }

    /**
     * Compute the bounding box.
     *
     * @param relation Data relation
     * @param iter Iterator
     */
    private void computeBoundingBox(Relation<? extends NumberVector> relation, DBIDArrayIter iter) {
      double[] b1 = relation.get(iter.seek(left)).toArray(), b2 = b1.clone();
      for(iter.advance(); iter.getOffset() < right; iter.advance()) {
        NumberVector vector = relation.get(iter);
        for(int d = 0; d < b1.length; d++) {
          final double value = vector.doubleValue(d);
          b1[d] = value < b1[d] ? value : b1[d];
          b2[d] = value > b2[d] ? value : b2[d];
        }
      }
      // Convert min/max to midpoint + halfwidth:
      for(int d = 0; d < b1.length; d++) {
        final double l = b1[d], u = b2[d];
        b1[d] = (l + u) * 0.5;
        b2[d] = (u - l) * 0.5;
      }
      midpoint = b1;
      halfwidth = b2;
    }

    /**
     * Aggregate the statistics for a leaf node.
     *
     * @param relation Data relation
     * @param iter Iterator
     * @param dim Dimensionality
     */
    private void aggregateStats(Relation<? extends NumberVector> relation, DBIDArrayIter iter, int dim) {
      // statistics of this node
      this.sum = new double[dim];
      this.sumSq = new double[dim][dim];
      for(iter.seek(left); iter.getOffset() < right; iter.advance()) {
        NumberVector vector = relation.get(iter);
        for(int d1 = 0; d1 < dim; d1++) {
          double value1 = vector.doubleValue(d1);
          sum[d1] += value1;
          for(int d2 = 0; d2 < dim; d2++) {
            double value2 = vector.doubleValue(d2);
            sumSq[d1][d2] += value1 * value2;
          }
        }
      }
    }
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
     * Stop going down the kd-tree when possible weight error e &lt; tau *
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
     * Parameter to produce more precise final assignments
     */
    public static final OptionID EXACT_ASSIGN_ID = new OptionID("emkd.exactassign", "Assign each point individually, not using the kd-tree in the final step.");

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

    /**
     * Perform the slower exact assignment step.
     */
    boolean exactAssign = false;

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
      new Flag(EXACT_ASSIGN_ID) //
          .grab(config, x -> exactAssign = x);
    }

    @Override
    public KDTreeEM make() {
      return new KDTreeEM(k, mbw, tau, tauclass, delta, mfactory, miniter, maxiter, soft, exactAssign);
    }
  }
}
