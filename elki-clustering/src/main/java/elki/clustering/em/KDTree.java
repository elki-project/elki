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

import elki.clustering.em.QuadraticProblem.ProblemData;
import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.data.model.MeanModel;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDArrayIter;
import elki.database.relation.Relation;
import elki.utilities.datastructures.arraylike.IntegerArray;
import elki.utilities.documentation.Reference;

import net.jafama.FastMath;

/**
 * KDTree class with the statistics needed for the calculation of EMKD
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
   * child-trees
   */
  KDTree leftChild, rightChild;

  /**
   * borders in sorted list
   */
  int leftBorder, rightBorder;

  /**
   * is this KDTree a leaf?
   */
  boolean isLeaf = false;

  /**
   * number of elements in this node
   */
  int size;

  /**
   * sum over all elements, needed for center calculation
   */
  double[] summedPoints;

  /**
   * sum over all squared elements (x^t * x),
   * needed for covariance calculation
   */
  double[][] summedPointsXPointsT;

  /**
   * boundingbox of elements in this node
   */
  Boundingbox boundingBox;

  /**
   * 
   * Constructor for a KDTree with statistics needed for EMKD calculation. Uses
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

    // point range
    this.leftBorder = left;
    this.rightBorder = right;
    this.size = right - left;

    // statistics of this node
    this.summedPoints = new double[dim];
    this.summedPointsXPointsT = new double[dim][dim];

    // calculate bounding box
    double[] bounds1 = relation.get(iter.seek(left)).toArray();
    double[] bounds2 = bounds1.clone();

    for(iter.advance(); iter.getOffset() < right; iter.advance()) {
      NumberVector vector = relation.get(iter);
      for(int d = 0; d < dim; d++) {
        double value = vector.doubleValue(d);
        // bounding box
        bounds1[d] = value < bounds1[d] ? value : bounds1[d];
        bounds2[d] = value > bounds2[d] ? value : bounds2[d];
      }
    }
    // Convert min/max to midpoint + halfwidth:
    for(int d = 0; d < dim; d++) {
      double l = bounds1[d], u = bounds2[d];
      bounds1[d] = (l + u) * 0.5;
      bounds2[d] = (u - l) * 0.5;
    }
    boundingBox = new Boundingbox(bounds1, bounds2);

    // handle leaf -> calculate summedPoints and summedPointsXPointsT
    int splitDim = 0;
    double maxDiff = Double.NEGATIVE_INFINITY;
    for(int d = 0; d < dim; d++) {
      double diff = 2 * boundingBox.halfwidth[d];
      if(diff > maxDiff) {
        splitDim = d;
        maxDiff = diff;
      }
    }
    if(maxDiff < mbw * dimWidth[splitDim]) {
      isLeaf = true;
      for(iter.seek(left); iter.getOffset() < right; iter.advance()) {
        NumberVector vector = relation.get(iter);
        for(int d1 = 0; d1 < dim; d1++) {
          double value1 = vector.doubleValue(d1);
          summedPoints[d1] += value1;
          for(int d2 = 0; d2 < dim; d2++) {
            double value2 = vector.doubleValue(d2);
            summedPointsXPointsT[d1][d2] += value1 * value2;
          }
        }
      }
      return;
    }
    // calculate nonleaf node
    // split points at midpoint according to paper
    double splitPoint = boundingBox.midpoint[splitDim];
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
    if(++r == right) { // Duplicate points!
      isLeaf = true;
    }
    else {
      leftChild = new KDTree(relation, sorted, left, r, dimWidth, mbw);
      rightChild = new KDTree(relation, sorted, r, right, dimWidth, mbw);
    }

    // fill up statistics from child nodes
    summedPoints = plus(leftChild.summedPoints, rightChild.summedPoints);
    summedPointsXPointsT = plus(leftChild.summedPointsXPointsT, rightChild.summedPointsXPointsT);
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
   * @param summedData accumulator object for statistics needed for emkd
   *        clustering
   * @param indices list of indices to check
   * @param tau maximum error, stop if all models have w_max - w_min < tau *
   *        w_total
   * @param tauClass drop model if w_max < tauClass * max(W_min)
   * @param cache Cache
   * @param piPow Dimensionality scaling factor
   * @return indices that are not pruned
   */
  public int[] checkStoppingCondition(List<? extends EMClusterModel<NumberVector, ? extends MeanModel>> models, ClusterData[] summedData, int[] indices, double tau, double tauClass, ProblemData[] cache, double piPow) {
    if(!(models.get(0) instanceof MultivariateGaussianModel)) {
      return indices;
    }
    // Calculate limits of the given Model inside the boundingbox
    double[][] maxPnts = new double[models.size()][summedPoints.length];
    double[][] minPnts = new double[models.size()][summedPoints.length];
    double[][] limits = new double[models.size()][2];
    for(int i : indices) {
      calculateModelLimits((MultivariateGaussianModel) models.get(i), minPnts[i], maxPnts[i], limits[i], cache, piPow);
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
      double wmin = bound((weight * limits[i][0]) / wminDenom);
      maxMinWeight = wmin > maxMinWeight ? wmin : maxMinWeight;
      // calculate maximum weight estimation
      wmaxs[i] = bound((weight * limits[i][1]) / wmaxDenom);
      double minPossibleWeight = summedData[i].summedLogWeights_apriori + wmin * size;
      // calculate the maximum possible error in this node
      double maximumError = size * (wmaxs[i] - wmin);
      // pruning check, if error to big for this model, dont prune
      if(maximumError > tau * minPossibleWeight) {
        prune = false;
      }
    }
    // if no complete prune is possible, check dropping of classes
    if(prune) {
      // return empty array -> full prune
      return new int[0];
    }
    if(tauClass == 0.0) {
      return indices;
    }
    IntegerArray result = new IntegerArray();
    for(int i : indices) {
      // drop one class if the maximum weight of a class in the bounding box
      // is lower than tauClass * maxMinWeight, where maxMinWeight is the
      // maximum minimal weight estimate of all classes
      if(wmaxs[i] > tauClass * maxMinWeight) {
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
  public void calculateModelLimits(MultivariateGaussianModel model, double[] minpnt, double[] maxpnt, double[] ret, ProblemData[] cache, double piPow) {
    Boundingbox bbTranslated = new Boundingbox(minus(boundingBox.midpoint, model.mean), boundingBox.halfwidth.clone());

    // invert and ensure symmetric (array is cloned in inverse)
    double[][] covInv = inverse(model.covariance);
    double covdet = FastMath.exp(2 * MultivariateGaussianModel.getHalfLogDeterminant(model.chol));

    // maximizes Mahalanobis dist
    final double[] b = new double[covInv.length];
    double mahalanobisSQDmax = 2 * QuadraticProblem.solve(covInv, b, 0, bbTranslated, cache, maxpnt);
    // minimizes Mahalanobis dist (invert covinv and result)
    double mahalanobisSQDmin = -2 * QuadraticProblem.solve(timesEquals(covInv, -1.0), b, 0, bbTranslated, cache, minpnt);

    final double f = 1 / (piPow * FastMath.sqrt(covdet));
    ret[0] = FastMath.exp(mahalanobisSQDmax * -.5) * f;
    ret[1] = FastMath.exp(mahalanobisSQDmin * -.5) * f;
  }

  /**
   * calculates the statistics on the kd-tree needed for the calculation of the
   * new models
   * 
   * @param models list of all models
   * @param indices list of indices to use in calculation, initially all
   * @param resultData result target array for statistics, initially empty
   * @param tau parameter for calculation pruning by weight error
   * @param tauClass parameter for class dropping if class has no impact
   * @param pruneFlag flag if pruning should be done
   * @param cache Cache
   * @param piPow Dimensionality scaling factor
   * @return log likelihood of the model
   */
  public double makeStats(List<? extends EMClusterModel<NumberVector, ? extends MeanModel>> models, int[] indices, ClusterData[] resultData, double tau, double tauClass, boolean pruneFlag, ProblemData[] cache, double piPow) {
    // check for pruning possibility
    if(!isLeaf && pruneFlag) {
      int[] nextIndices = checkStoppingCondition(models, resultData, indices, tau, tauClass, cache, piPow);
      if(nextIndices.length > 0) {
        // if this is not a leaf node, call child nodes
        assert nextIndices != null;
        return leftChild.makeStats(models, nextIndices, resultData, tau, tauClass, pruneFlag, cache, piPow) //
            + rightChild.makeStats(models, nextIndices, resultData, tau, tauClass, pruneFlag, cache, piPow);
      }
    }
    // logarithmic probabilities of clusters in this node
    int k = models.size();
    DoubleVector midpoint = DoubleVector.wrap(times(summedPoints, 1.0 / size));
    double[] logProb = new double[k];
    for(int i = 0; i < k; i++) {
      logProb[i] = models.get(i).estimateLogDensity(midpoint);
    }
    double logDenSum = EM.logSumExp(logProb);
    minusEquals(logProb, logDenSum);
    // calculate necessary statistics at this node
    for(int c : indices) {
      resultData[c].increment(logProb[c] + FastMath.log(size), FastMath.exp(logProb[c]), //
          summedPoints, summedPointsXPointsT);
    }
    return logDenSum * size;
  }

  /**
   * Bounding box of a KDTree node.
   */
  public static class Boundingbox {
    private double[] midpoint, halfwidth;

    /**
     * Constructs a bounding box.
     *
     * @param midpoint Midpoint
     * @param halfwidth Half width
     */
    public Boundingbox(double[] midpoint, double[] halfwidth) {
      this.midpoint = midpoint;
      this.halfwidth = halfwidth;
    }

    /**
     * get lower bound at dimension k
     * 
     * @param k dimension
     * @return lower bound at dimension k
     */
    public double getLowerBound(int k) {
      return midpoint[k] - halfwidth[k];
    }

    /**
     * get upper bound at dimension k
     * 
     * @param k dimension
     * @return upper bound at dimension k
     */
    public double getUpperBound(int k) {
      return midpoint[k] + halfwidth[k];
    }

    /**
     * get difference between lower and upper bounds at dimension k
     * 
     * @param k dimension
     * @return difference at dimension k
     */
    public double getHalfwidth(int k) {
      return halfwidth[k];
    }

    /**
     * get the middle point of the bounding box. returns the backend object, so
     * changes will change the bounding box
     * 
     * @return array containing midpoint
     */
    public double[] getMidPoint() {
      return midpoint;
    }

    /**
     * Reduces this bounding box by omitting reducedDim and saving the result to
     * redBox.
     * 
     * @param redBox target bounding box
     * @param reducedDim dimension to omit
     */
    public void reduceBoundingboxTo(Boundingbox redBox, int reducedDim) {
      if(reducedDim > 0) {
        System.arraycopy(halfwidth, 0, redBox.halfwidth, 0, reducedDim);
        System.arraycopy(midpoint, 0, redBox.midpoint, 0, reducedDim);
      }

      if(midpoint.length - (reducedDim) > 1) {
        int l = redBox.midpoint.length - reducedDim;
        System.arraycopy(halfwidth, reducedDim + 1, redBox.halfwidth, reducedDim, l);
        System.arraycopy(midpoint, reducedDim + 1, redBox.midpoint, reducedDim, l);
      }
    }

    /**
     * Checks if the point is inside or on the bounds of this bounding box
     * 
     * @param point point to check
     * @return true if point is weakly inside bounds
     */
    public boolean weaklyInsideBounds(double[] point) {
      for(int i = 0; i < midpoint.length; i++) {
        if(Math.abs(midpoint[i] - point[i]) > halfwidth[i]) {
          return false;
        }
      }
      return true;
    }
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
          logSumExp(logApriori, this.summedLogWeights_apriori);
      plusTimesEquals(this.summedPoints_mean, linearSum, w);
      plusTimesEquals(this.summedPointsSquared_cov, squaredSum, w);
    }

    /**
     * Compute log(exp(a)+exp(b)), with attention to numerical issues.
     * 
     * @param a Input 1
     * @param b Input 2
     * @return Result
     */
    protected static double logSumExp(double a, double b) {
      return (a > b ? a : b) + FastMath.log(a > b ? FastMath.exp(b - a) + 1 : FastMath.exp(a - b) + 1);
    }
  }

  /**
   * Ensures d to be in [0,1].
   * 
   * @param d double to be bound to [0,1]
   * @return 0 if d < 0, 1 if d > 0, d else
   */
  private static double bound(double d) {
    return d < 0.0 ? 0.0 : d > 1.0 ? 1.0 : d;
  }
}
