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

import java.util.List;

import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.data.model.MeanModel;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDArrayIter;
import elki.database.relation.Relation;
import elki.math.MathUtil;
import elki.utilities.datastructures.arraylike.IntegerArray;
import elki.utilities.documentation.Reference;

import net.jafama.FastMath;

/**
 * KDTree class with the statistics needed for the calculation of KDTreeEM
 * clustering as given in the following paper
 * <p>
 * Reference:
 * <p>
 * A. W. Moore<br>
 * Very Fast EM-based Mixture Model Clustering using Multiresolution
 * kd-trees<br>
 * Neural Information Processing Systems (NIPS 1998)
 * 
 * @author Robert Gehde
 */
@Reference(authors = "A. W. Moore", //
    title = "Very Fast EM-based Mixture Model Clustering using Multiresolution kd-Trees", //
    booktitle = "Advances in Neural Information Processing Systems 11 (NIPS 1998)", //
    bibkey = "DBLP:conf/nips/Moore98")
class KDTree {
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
   * Sum over all squared elements (x^t * x),
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
   * Uses
   * points between the indices left and right for calculation
   *
   * @param relation datapoints for the construction
   * @param sorted sorted id array
   * @param left leftmost datapoint used for construction
   * @param right rightmost datapoint used for construction
   * @param dimWidth Array containing the width of all dimensions on the
   *        complete dataset
   * @param mbw factor when to stop construction. Stop if splitdimwidth < mbw *
   *        dimwidth[splitdim]
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

  /**
   * This methods checks the different stopping conditions given in the paper,
   * thus calculating the Dimensions, that will be considered for child-trees.
   * If this method returns a non-empty subset of the input dimension set, it
   * means that missing dimensions are dropped because their weight was too
   * small. If it returns an empty array (length = 0) it means that the expected
   * error of all remaining models is small enough to consider this node a leaf
   * node.
   * 
   * @param models list of all models
   * @param summedData accumulator object for statistics needed for KDTreeEM
   *        clustering
   * @param indices list of indices to check
   * @param tau maximum error, stop if all models have w_max - w_min < tau *
   *        w_total
   * @param tauClass drop model if w_max < tauClass * max(W_min)
   * @param cache Cache
   * @param piPow Dimensionality scaling factor
   * @return indices that are not pruned
   */
  public int[] checkStoppingCondition(List<? extends EMClusterModel<? super NumberVector, ? extends MeanModel>> models, ClusterData[] summedData, int[] indices, double tau, double tauClass, ConstrainedQuadraticProblemSolver solver, double piPow) {
    if(!(models.get(0) instanceof TextbookMultivariateGaussianModel)) {
      return indices;
    }
    // Calculate limits of the given Model inside the bounding box
    double[][] maxPnts = new double[models.size()][sum.length];
    double[][] minPnts = new double[models.size()][sum.length];
    double[][] limits = new double[models.size()][2];
    for(int i : indices) {
      calculateModelLimits((TextbookMultivariateGaussianModel) models.get(i), minPnts[i], maxPnts[i], limits[i], solver, piPow);
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
    for(int i : indices) {
      // calculate denominators for minimum/maximum weight estimation
      double weight = models.get(i).getWeight();
      double wminDenom = minDenomTotal + weight * (limits[i][0] - limits[i][1]);
      double wmaxDenom = maxDenomTotal + weight * (limits[i][1] - limits[i][0]);
      // calculate minimum weight estimation
      double wmin = MathUtil.clamp((weight * limits[i][0]) / wminDenom, 0, 1);
      maxMinWeight = wmin > maxMinWeight ? wmin : maxMinWeight;
      // calculate maximum weight estimation
      wmaxs[i] = MathUtil.clamp((weight * limits[i][1]) / wmaxDenom, 0, 1);
      double minPossibleWeight = summedData[i].summedLogWeights_apriori + wmin * (right - left);
      // calculate the maximum possible error in this node
      double maximumError = (right - left) * (wmaxs[i] - wmin);
      // pruning check, if error to big for this model, dont prune
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
   * @param cache Cache
   * @param piPow Dimensionality scaling factor
   * @param ret Return array
   */
  public void calculateModelLimits(TextbookMultivariateGaussianModel model, double[] minpnt, double[] maxpnt, double[] ret, ConstrainedQuadraticProblemSolver solver, double piPow) {
    double[] min = minusEquals(minus(midpoint, model.mean), halfwidth);
    double[] max = plusTimes(min, halfwidth, 2);

    // invert and ensure symmetric (array is cloned in inverse)
    double[][] covInv = inverse(model.covariance);
    double covdetsqrt = FastMath.exp(MultivariateGaussianModel.getHalfLogDeterminant(model.chol));

    // maximizes Mahalanobis dist
    final double[] b = new double[covInv.length];
    double mahalanobisSQDmax = 2 * solver.solve(covInv, b, 0, min, max, maxpnt);
    // minimizes Mahalanobis dist (invert covinv and result)
    double mahalanobisSQDmin = -2 * solver.solve(timesEquals(covInv, -1.0), b, 0, min, max, minpnt);

    final double f = 1 / (piPow * covdetsqrt);
    ret[0] = FastMath.exp(mahalanobisSQDmax * -.5) * f;
    ret[1] = FastMath.exp(mahalanobisSQDmin * -.5) * f;
  }

  /**
   * Calculates the statistics on the kd-tree needed for the calculation of the
   * new models
   * 
   * @param models list of all models
   * @param indices list of indices to use in calculation, initially all
   * @param resultData result target array for statistics, initially empty
   * @param tau parameter for calculation pruning by weight error
   * @param tauClass parameter for class dropping if class has no impact
   * @param cache Cache
   * @param piPow Dimensionality scaling factor
   * @return log likelihood of the model
   */
  public double makeStats(List<? extends EMClusterModel<? super NumberVector, ? extends MeanModel>> models, int[] indices, ClusterData[] resultData, double tau, double tauClass, ConstrainedQuadraticProblemSolver solver, double piPow) {
    // Only one possible cluster remaining.
    if(indices.length == 1) {
      DoubleVector midpoint = DoubleVector.wrap(times(sum, 1.0 / (right - left)));
      double logDenSum = models.get(indices[0]).estimateLogDensity(midpoint);
      resultData[indices[0]].increment(FastMath.log(right - left), 1., sum, sumSq);
      return logDenSum * (right - left);
    }
    // check for pruning possibility
    if(leftChild != null) {
      int[] nextIndices = checkStoppingCondition(models, resultData, indices, tau, tauClass, solver, piPow);
      if(nextIndices != null) {
        return leftChild.makeStats(models, nextIndices, resultData, tau, tauClass, solver, piPow) //
            + rightChild.makeStats(models, nextIndices, resultData, tau, tauClass, solver, piPow);
      }
    }
    DoubleVector midpoint = DoubleVector.wrap(times(sum, 1.0 / (right - left)));
    // logarithmic probabilities of clusters in this node
    double[] logProb = new double[indices.length];
    for(int i = 0; i < indices.length; i++) {
      logProb[i] = models.get(indices[i]).estimateLogDensity(midpoint);
    }
    double logDenSum = EM.logSumExp(logProb);
    minusEquals(logProb, logDenSum); // total probability 1
    // calculate necessary statistics at this node
    for(int i = 0; i < indices.length; i++) {
      resultData[indices[i]].increment(logProb[i] + FastMath.log(right - left), FastMath.exp(logProb[i]), sum, sumSq);
    }
    return logDenSum * (right - left);
  }

  /**
   * This class holds information collected by makeStats
   */
  protected static class ClusterData {
    /**
     * Sum of likelihoods
     */
    double summedLogWeights_apriori;

    /**
     * Sum of points, used for mean calculation
     */
    double[] summedPoints_mean;

    /**
     * Sum of squared points, used for covariance calculation.
     */
    double[][] summedPointsSquared_cov;

    /**
     * Constructor for an empty clusterdata object for d dimensions
     * 
     * @param d Dimension
     */
    public ClusterData(int d) {
      this.summedLogWeights_apriori = Double.NEGATIVE_INFINITY;
      this.summedPoints_mean = new double[d];
      this.summedPointsSquared_cov = new double[d][d];
    }

    /**
     * increment the statistics in this object by the given values
     * 
     * @param logApriori logarithmic apriori probability of the node
     * @param w Linear scaling factor for the
     * @param linearSum summed points of the node
     * @param squaredSum summed points^t * points of the node
     * @param loglike log likelihood in this node
     */
    void increment(double logApriori, double w, double[] linearSum, double[][] squaredSum) {
      this.summedLogWeights_apriori = this.summedLogWeights_apriori == Double.NEGATIVE_INFINITY ? logApriori : //
          EM.logSumExp(logApriori, this.summedLogWeights_apriori);
      plusTimesEquals(this.summedPoints_mean, linearSum, w);
      plusTimesEquals(this.summedPointsSquared_cov, squaredSum, w);
    }
  }
}
