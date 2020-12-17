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

import static elki.math.linearalgebra.VMath.argmax;
import static elki.math.linearalgebra.VMath.inverse;

import static elki.math.linearalgebra.VMath.plus;
import static elki.math.linearalgebra.VMath.plusEquals;
import static elki.math.linearalgebra.VMath.minus;
import static elki.math.linearalgebra.VMath.times;
import static elki.math.linearalgebra.VMath.timesPlusTimes;

import java.util.ArrayList;
import java.util.Arrays;

import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.data.model.MeanModel;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDArrayIter;
import elki.database.relation.Relation;
import elki.utilities.datastructures.arraylike.IntegerArray;
import elki.utilities.documentation.Reference;
import elki.utilities.pairs.DoubleDoublePair;

import elki.clustering.em.QuadraticProblem;
import elki.clustering.em.QuadraticProblem.ProblemData;

import net.jafama.FastMath;

/**
 * KDTree class with the statistics needed for the calculation of EMKD
 * clustering as given in the following paper
 * 
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
 */
@Reference(authors = "Andrew W. Moore", //
    booktitle = "Advances in Neural Information Processing Systems 11 (NIPS 1998)", //
    title = "Very Fast EM-based Mixture Model Clustering using Multiresolution", //
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
   * Array cache object for quadratic problem calculation
   */
  ProblemData[] cache;

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
   * @param arrayCache ArrayCache object for Quadratic Problem calls
   */
  public KDTree(Relation<? extends NumberVector> relation, ArrayModifiableDBIDs sorted, int left, int right, double[] dimWidth, double mbw, ProblemData[] arrayCache) {
    this.cache = arrayCache;
    /**
     * other kdtrees seem to look at [left, right[
     */
    DBIDArrayIter iter = sorted.iter();
    int dim = relation.get(iter).toArray().length;

    // point range
    leftBorder = left;
    rightBorder = right;
    size = right - left;

    // statistics of this node
    summedPoints = new double[dim];
    summedPointsXPointsT = new double[dim][dim];

    // bounding box of this node
    double[] lowerBounds = new double[dim];
    double[] upperBounds = new double[dim];

    // calculate bounding box
    iter.seek(left);
    lowerBounds = relation.get(iter).toArray();
    upperBounds = relation.get(iter).toArray();

    for(int i = 0; i < size; i++) {
      NumberVector vector = relation.get(iter);
      for(int d = 0; d < dim; d++) {
        double value = vector.doubleValue(d);
        // bounding box
        lowerBounds[d] = value < lowerBounds[d] ? value : lowerBounds[d];
        upperBounds[d] = value > upperBounds[d] ? value : upperBounds[d];
      }
      if(iter.valid())
        iter.advance();
    }
    boundingBox = new Boundingbox(lowerBounds, upperBounds);

    iter.seek(left);
    // handle leaf -> calculate summedPoints and summedPointsXPointsT
    final int splitDim = argmax(boundingBox.getDifferences());
    if(boundingBox.getDifference(splitDim) < mbw * dimWidth[splitDim]) {
      isLeaf = true;
      for(int i = 0; i < size; i++) {
        NumberVector vector = relation.get(iter);
        for(int d1 = 0; d1 < dim; d1++) {
          double value1 = vector.doubleValue(d1);
          summedPoints[d1] += value1;
          for(int d2 = 0; d2 < dim; d2++) {
            double value2 = vector.doubleValue(d2);
            summedPointsXPointsT[d1][d2] += (value1) * (value2);
          }
        }
        if(iter.valid())
          iter.advance();
      }
    }
    else { // calculate nonleaf node
      // split points at midpoint according to paper
      double splitPoint = boundingBox.getMidPoint()[splitDim];
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
      if(r == right) { // Duplicate points!
        isLeaf = true;
        return;
      }
      leftChild = new KDTree(relation, sorted, left, r, dimWidth, mbw, arrayCache);
      rightChild = new KDTree(relation, sorted, r, right, dimWidth, mbw, arrayCache);

      // fill up statistics from childnodes
      summedPoints = plus(leftChild.summedPoints, rightChild.summedPoints);
      summedPointsXPointsT = plus(leftChild.summedPointsXPointsT, rightChild.summedPointsXPointsT);
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
   * @param summedData accumulator object for statistics needed for emkd
   *        clustering
   * @param indices list of indices to check
   * @param tau maximum error, stop if all models have w_max - w_min < tau *
   *        w_total
   * @param tauClass drop model if w_max < tauClass * max(W_min)
   * @return indices that are not pruned
   */
  public int[] checkStoppingCondition(ArrayList<? extends EMClusterModel<NumberVector, ? extends MeanModel>> models, ClusterData[] summedData, int[] indices, double tau, double tauClass) {
    // Calculate Limits of the given Model inside the boundingbox
    DoubleDoublePair[] limits = new DoubleDoublePair[models.size()];
    double[][] maxPnts = new double[models.size()][summedPoints.length];
    double[][] minPnts = new double[models.size()][summedPoints.length];
    if(models.get(0) instanceof MultivariateGaussianModel) {
      for(int i : indices) {
        limits[i] = calculateModelLimits((MultivariateGaussianModel) models.get(i), minPnts[i], maxPnts[i]);
      }
    }
    else {
      return indices;
    }
    // calculate the complete sum of weighted limits for denominator calculation
    double maxDenomTotal = 0.0;
    double minDenomTotal = 0.0;
    for(int i : indices) {
      maxDenomTotal += models.get(i).getWeight() * limits[i].first;
      minDenomTotal += models.get(i).getWeight() * limits[i].second;
    }
    boolean prune = true;
    double maxMinWeight = Double.NEGATIVE_INFINITY;
    double[] wmaxs = new double[models.size()];
    for(int i : indices) {
      // calculate denominators for minimum/maximum weight estimation
      double weight = models.get(i).getWeight();
      double wminDenom = minDenomTotal + weight * (limits[i].first - limits[i].second);
      double wmaxDenom = maxDenomTotal + weight * (limits[i].second - limits[i].first);
      // calculate minimum weight estimation
      double wmin = bound((weight * limits[i].first) / wminDenom);
      maxMinWeight = wmin > maxMinWeight ? wmin : maxMinWeight;
      // calculate maximum weight estimation
      wmaxs[i] = bound((weight * limits[i].second) / wmaxDenom);
      double minPossibleWeight = summedData[i].summedLogWeights_apriori + wmin * size;
      // calculate the maximum possible error in this node
      double maximumError = size * (wmaxs[i] - wmin);
      // pruning check, if error to big for this model, dont prune
      if(maximumError > tau * minPossibleWeight)
        prune = false;
    }
    // if no complete prune is possible, check dropping of classes
    if(!prune) {
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
    // return empty array -> full prune
    return new int[0];
  }

  /**
   * calculates the model limits inside this node by
   * translating the gaussian model into a squared function
   * 
   * @param model model to calculate the limits for
   * @param minpnt result array for argmin
   * @param maxpnt result array for argmax
   * @return pair containing min at index 0 and max at index 1
   */
  public DoubleDoublePair calculateModelLimits(MultivariateGaussianModel model, double[] minpnt, double[] maxpnt) {
    // translate problem by mean
    double[] mean = model.mean;
    double[] transmin = minus(boundingBox.getLowerBounds(), mean);
    double[] transmax = minus(boundingBox.getUpperBounds(), mean);
    Boundingbox bbTranslated = new Boundingbox(transmin, transmax);

    // invert and ensure symmetric (array is cloned in inverse)
    double[][] covInv = inverse(model.covariance);
    double covdet = FastMath.exp(2 * MultivariateGaussianModel.getHalfLogDeterminant(model.chol));
    for(int i = 0; i < covInv.length; i++) {
      for(int j = i; j < covInv[i].length; j++) {
        if(Math.abs(covInv[i][j] - covInv[j][i]) > 1e-10) {
          covInv[j][i] = covInv[i][j] = (covInv[i][j] + covInv[j][i]) / 2;
        }
      }
    }

    // maximizes mahalanobis dist
    QuadraticProblem quadmax = calcConstrainedSQDMahalMax(bbTranslated, covInv);
    double mahalanobisSQDmax = quadmax.maximumValue;
    System.arraycopy(quadmax.argmaxPoint, 0, maxpnt, 0, quadmax.argmaxPoint.length);

    // minimizes mahalanobis dist (invert covinv and result
    QuadraticProblem quadmin = calcConstrainedSQDMahalMax(bbTranslated, times(covInv, -1.0));
    double mahalanobisSQDmin = -1.0 * quadmin.maximumValue;
    System.arraycopy(quadmax.argmaxPoint, 0, minpnt, 0, quadmax.argmaxPoint.length);

    double amin = calculateGaussian(mahalanobisSQDmax, covdet, mean.length);
    double amax = calculateGaussian(mahalanobisSQDmin, covdet, mean.length);

    return new DoubleDoublePair(amin, amax);
  }

  /**
   * Calculates gaussian density from covdet and squared mahalanobis distance
   * 
   * @param squaredMahalanobis squared mahalanobis distance
   * @param covarianceDet covariance determinant of the model
   * @param dim dimensions of the model
   * @return density of the model at the given mahalanobis distance
   */
  private double calculateGaussian(double squaredMahalanobis, double covarianceDet, int dim) {
    double num = FastMath.exp(-squaredMahalanobis / 2);
    double den = cache[dim - 1].piPow * FastMath.sqrt(covarianceDet);
    return num / den;
  }

  /**
   * calculates the maximum squared mahalanobis distance for the given inverted
   * covariance matrix and (translated) bounding box. The center of the model is
   * considered to be at 0.
   * 
   * @param bbTranslated translated boundingbox of the node
   * @param a inverted covariance matrix
   * @return QuadraticProblem containing the solution
   */
  private QuadraticProblem calcConstrainedSQDMahalMax(Boundingbox bbTranslated, double[][] a) {
    double c = 0.0;
    double[] b = new double[a.length];
    QuadraticProblem qp = new QuadraticProblem(a, b, c, bbTranslated, cache);
    qp.maximumValue *= 2; // because we have a .5 factor in the calculation
    return qp;
  }

  /**
   * calculates the statistics on the kd-tree needed for the calculation of the
   * new models
   * 
   * @param models list of all models
   * @param indices list of indices to use in calculation, topmost call with all
   *        indices
   * @param resultData result target array for statistics, topmost call with
   *        empty ClusterData array
   * @param tau parameter for calculation pruning by weight error
   * @param tauClass parameter for class dropping if class is not impactfull
   * @param pruneFlag flag if pruning should be done
   * @return log likelihood of the model
   */
  public double makeStats(ArrayList<? extends EMClusterModel<NumberVector, ? extends MeanModel>> models, int[] indices, ClusterData[] resultData, double tau, double tauClass, boolean pruneFlag) {
    int k = models.size();
    boolean prune = isLeaf;
    int[] nextIndices = new int[indices.length];
    // check for pruning possibility
    if(!prune && pruneFlag) {
      nextIndices = checkStoppingCondition(models, resultData, indices, tau, tauClass);
      if(nextIndices.length == 0) {
        prune = true;
      }
    }
    if(prune) {
      // logarithmic probabilities of clusters in this node
      double[] logProbr = new double[k];
      DoubleVector midpoint = new DoubleVector(times(summedPoints, 1.0 / size));
      for(int i = 0; i < k; i++) {
        logProbr[i] = models.get(i).estimateLogDensity(midpoint);
      }
      double logDenSum = logSumExp(logProbr);
      double[] logProb = minus(logProbr, logDenSum);
      // calculate necessary statistics at this node
      for(int c : indices) { // for(int c = 0; c < logProb.length; c++) {
        double prob = FastMath.exp(logProb[c]);
        double logAPrio = logProb[c] + FastMath.log(size);
        double[] tCenter = times(summedPoints, prob);
        double[][] tCovariance = times(summedPointsXPointsT, prob);

        // combine with exisiting statistics
        // objects should be created in caller
        resultData[c].increment(logAPrio, tCenter, tCovariance);
      }
      return logDenSum * size;
    }
    else {
      // if this is not a leaf node, call child nodes
      assert nextIndices != null;
      double l = leftChild.makeStats(models, nextIndices, resultData, tau, tauClass, pruneFlag);
      double r = rightChild.makeStats(models, nextIndices, resultData, tau, tauClass, pruneFlag);
      return l + r;
    }
  }

  /**
   * checks if any model in the list does not support checking condition
   * 
   * @param models list of models to check
   * @return true, if all models support stopping conditions
   */
  public static boolean supportsStoppingCondition(ArrayList<? extends EMClusterModel<NumberVector, ? extends MeanModel>> models) {
    boolean b = true;
    for(EMClusterModel<NumberVector, ? extends MeanModel> emClusterModel : models) {
      b = b && emClusterModel instanceof MultivariateGaussianModel;
    }
    return b;
  }

  /**
   * Bounding box of a KDTree Node
   * 
   */
  static public class Boundingbox {

    private double[] hilim, lolim, midpoint, dist;

    /**
     * Constructs a bounding box from lower and upper bounds
     *
     * @param lolim lower bounds
     * @param hilim upper bounds
     */
    public Boundingbox(double[] lolim, double[] hilim) {
      this.hilim = hilim;
      this.lolim = lolim;
      this.midpoint = timesPlusTimes(lolim, 0.5, hilim, 0.5);
      this.dist = minus(hilim, lolim);
    }

    /**
     * print lower and upper bounds
     */
    public void printHiLo() {
      System.out.println("Boundingbox");
      System.out.println(Arrays.toString(getLowerBounds()));
      System.out.println(Arrays.toString(getUpperBounds()));
    }

    /**
     * get lower bound at dimension k
     * 
     * @param k dimension
     * @return lower bound at dimension k
     */
    public double getLowerBound(int k) {
      return lolim[k];
    }

    /**
     * get upper bound at dimension k
     * 
     * @param k dimension
     * @return upper bound at dimension k
     */
    public double getUpperBound(int k) {
      return hilim[k];
    }

    /**
     * get difference between lower and upper bounds at dimension k
     * 
     * @param k dimension
     * @return difference at dimension k
     */
    public double getDifference(int k) {
      return dist[k];
    }

    /**
     * get all lower bounds. returns the backend object, so changes will change
     * the bounding box
     * 
     * @return array containing lower bounds
     */
    public double[] getLowerBounds() {
      return lolim;
    }

    /**
     * get all upper bounds. returns the backend object, so changes will change
     * the bounding box
     * 
     * @return array containing upper bounds
     */
    public double[] getUpperBounds() {
      return hilim;
    }

    /**
     * get all differences. returns the backend object, so changes will change
     * the bounding box
     * 
     * @return array containing differences
     */
    public double[] getDifferences() {
      return dist;
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
     * reduces this bounding box by omitting reducedDim and saving the result to
     * redBox
     * 
     * @param redBox target bounding box
     * @param reducedDim dimension to omit
     */
    public void reduceBoundingboxTo(Boundingbox redBox, int reducedDim) {
      if(reducedDim > 0) {
        System.arraycopy(lolim, 0, redBox.getLowerBounds(), 0, reducedDim);
        System.arraycopy(hilim, 0, redBox.getUpperBounds(), 0, reducedDim);
        System.arraycopy(midpoint, 0, redBox.getMidPoint(), 0, reducedDim);
        System.arraycopy(dist, 0, redBox.getDifferences(), 0, reducedDim);
      }

      if(midpoint.length - (reducedDim) > 1) {
        int l = redBox.midpoint.length - reducedDim;
        System.arraycopy(lolim, reducedDim + 1, redBox.getLowerBounds(), reducedDim, l);
        System.arraycopy(hilim, reducedDim + 1, redBox.getUpperBounds(), reducedDim, l);
        System.arraycopy(midpoint, reducedDim + 1, redBox.getMidPoint(), reducedDim, l);
        System.arraycopy(dist, reducedDim + 1, redBox.getDifferences(), reducedDim, l);
      }
    }

    /**
     * checks if the point is inside or on the bounds of this bounding box
     * 
     * @param point point to check
     * @return true if point is weakly inside bounds
     */
    public boolean weaklyInsideBounds(double[] point) {
      for(int i = 0; i < point.length; i++) {
        if(point[i] < getLowerBound(i) || point[i] > getUpperBound(i)) {
          return false;
        }
      }
      return true;
    }
  }

  /**
   * This class holds information collected by makeStats
   *
   */
  static class ClusterData {
    /**
     * summed likelihoods
     */
    double summedLogWeights_apriori;

    /**
     * summed points, used for mean calculation
     */
    double[] summedPoints_mean;

    /**
     * summed squared points, used for covariance calculation
     */
    double[][] summedPointsSquared_cov;

    /**
     * Constructor for a clusterdata object with no data so far for d Dimensions
     * 
     * @param d Dimension
     * 
     */
    public ClusterData(int d) {
      this.summedLogWeights_apriori = Double.NEGATIVE_INFINITY;
      this.summedPoints_mean = new double[d];
      this.summedPointsSquared_cov = new double[d][d];
    }

    /**
     * 
     * Creates this object with the given statistics and loglikelihood 0
     * 
     * @param logApriori logarithmic apriori probability of the node
     * @param scaledSummedPoints scaled summed points of the node
     * @param scaledSummedPointsSquared scaled summed points^t * points of the
     *        node
     */
    public ClusterData(double logApriori, double[] scaledSummedPoints, double[][] scaledSummedPointsSquared) {
      this.summedLogWeights_apriori = logApriori;
      this.summedPoints_mean = scaledSummedPoints;
      this.summedPointsSquared_cov = scaledSummedPointsSquared;
    }

    /**
     * increment the statistics in this object by the given values
     * 
     * @param logApriori logarithmic apriori probability of the node
     * @param scaledSummedPoints scaled summed points of the node
     * @param scaledSummedPointsSquared scaled summed points^t * points of the
     *        node
     * @param loglike log likelihood in this node
     */
    void increment(double logApriori, double[] scaledSummedPoints, double[][] scaledSummedPointsSquared) {
      if(this.summedLogWeights_apriori == Double.NEGATIVE_INFINITY) {
        this.summedLogWeights_apriori = logApriori;
      }
      else {
        this.summedLogWeights_apriori = FastMath.log(FastMath.exp(logApriori) + FastMath.exp(this.summedLogWeights_apriori));
      }
      this.summedPoints_mean = plusEquals(this.summedPoints_mean, scaledSummedPoints);
      this.summedPointsSquared_cov = plusEquals(this.summedPointsSquared_cov, scaledSummedPointsSquared);
    }
  }

  /**
   * ensures d to be in [0,1]
   * 
   * @param d double to be bound to [0,1]
   * @return 0 if d < 0, 1 if d > 0, d else
   */
  private static double bound(double d) {
    return d < 0.0 ? 0.0 : d > 1.0 ? 1.0 : d;
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
}
