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
import elki.logging.Logging;
import elki.utilities.datastructures.arraylike.IntegerArray;
import elki.utilities.pairs.DoubleDoublePair;
import elki.clustering.em.QuadraticProblem;
import elki.clustering.em.QuadraticProblem.ProblemData;

import net.jafama.FastMath;

class KDTree {
  private final Logging PARENTLOG;

  KDTree leftChild, rightChild;

  int leftBorder, rightBorder;

  boolean isLeaf = false;

  int size;

  double[] summedPoints;

  double[][] summedPointsXPointsT;

  Boundingbox boundingBox;

  ProblemData[] cache;

  public KDTree(Relation<? extends NumberVector> relation, ArrayModifiableDBIDs sorted, int left, int right, double[] dimWidth, double mbw, Logging parentLog, ProblemData[] arrayCache) {
    this.PARENTLOG = parentLog;
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
      leftChild = new KDTree(relation, sorted, left, r, dimWidth, mbw, PARENTLOG, arrayCache);
      rightChild = new KDTree(relation, sorted, r, right, dimWidth, mbw, PARENTLOG, arrayCache);

      // fill up statistics from childnodes
      summedPoints = plus(leftChild.summedPoints, rightChild.summedPoints);
      summedPointsXPointsT = plus(leftChild.summedPointsXPointsT, rightChild.summedPointsXPointsT);
    }
  }

  /**
   * we need: get methods for model information
   * irrc we need a symetric cov
   * this is only possible for gaussian models
   * assume all models are same class
   * 
   * @param boundingBox
   * @param numP
   * @param models
   * @return
   */
  public int[] checkStoppingCondition(Boundingbox boundingBox, ArrayList<? extends EMClusterModel<NumberVector, ? extends MeanModel>> models, double[] weightsKnown, ClusterData[] summedData, int[] indices, double tau, double tauLoglike, double tauClass) {
    // Calculate Limits of the given Model inside the boundingbox
    DoubleDoublePair[] limits = new DoubleDoublePair[models.size()];
    double[][] maxPnts = new double[models.size()][summedPoints.length];
    double[][] minPnts = new double[models.size()][summedPoints.length];
    if(models.get(0) instanceof MultivariateGaussianModel) {
      for(int i : indices) {
        limits[i] = evaluateGaussianLimits(boundingBox, (MultivariateGaussianModel) models.get(i), minPnts[i], maxPnts[i]);
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
      double minPossibleWeight = weightsKnown[i] + wmin * size;
      // calculate the maximum possible error in this node
      double maximumError = size * (wmaxs[i] - wmin);
      // tau
      // Set tau = 0.01 for very high accuracy
      // Set tau = 0.2 for typical performance
      // Set tau = 0.6 for fast but innaccurate
      if(maximumError > tau * minPossibleWeight)
        prune = false;
    }
    // check for possible log likelihood derivation
    if(prune) {
      // est loglike
      double[] estloglikes = new double[summedData.length];
      for(int j = 0; j < summedData.length; j++) {
        estloglikes[j] = summedData[j] == null ? 0 : summedData[j].summedLoglike;
      }
      double estloglike = logSumExp(estloglikes);
      // for every model
      for(int i : indices) {
        // check log likelihood

        double minWeights = 0.0;
        double maxWeights = 0.0;
        for(int j : indices) {
          minWeights += models.get(j).getWeight() * FastMath.exp(models.get(j).estimateLogDensity(new DoubleVector(minPnts[i])));
          maxWeights += models.get(j).getWeight() * FastMath.exp(models.get(j).estimateLogDensity(new DoubleVector(maxPnts[i])));
        }

        double minLoglike = size * FastMath.log(minWeights);
        double maxLoglike = size * FastMath.log(maxWeights);

        // likelihood_tau = 0.01 for high acc,
        // 0.5 for normal, 0.9 for approximate.
        if(maxLoglike - minLoglike > tauLoglike * FastMath.max(100.0, FastMath.abs(estloglike))) {
          prune = false;
        }
      }

    }

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
      return result.toArray();
    }
    // return empty array -> full prune
    return new int[0];
  }

  /**
   * calculates from y(x) = 0.5 * x^T a x + b^T x
   * and returns 2 * y(x)
   * 
   * @param hyperboundingbox
   * @param model
   * @param a
   * @return
   */
  public DoubleDoublePair evaluateGaussianLimits(Boundingbox hyperboundingbox, MultivariateGaussianModel model, double[] minpnt, double[] maxpnt) {
    // translate problem by mean
    double[] mean = model.mean;
    double[] transmin = minus(hyperboundingbox.getLowerBounds(), mean);
    double[] transmax = minus(hyperboundingbox.getUpperBounds(), mean);
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
    QuadraticProblem quadmax = constrMahalanobisSqdMax(bbTranslated, covInv);
    double mahalanobisSQDmax = quadmax.maximumValue;
    System.arraycopy(quadmax.argmaxPoint, 0, maxpnt, 0, quadmax.argmaxPoint.length);
    // minimizes mahalanobis dist (invert covinv and result
    QuadraticProblem quadmin = constrMahalanobisSqdMax(bbTranslated, times(covInv, -1.0));
    double mahalanobisSQDmin = -1.0 * quadmin.maximumValue;
    System.arraycopy(quadmax.argmaxPoint, 0, minpnt, 0, quadmax.argmaxPoint.length);

    double amin = calculateGaussian(mahalanobisSQDmax, covdet, mean.length);
    double amax = calculateGaussian(mahalanobisSQDmin, covdet, mean.length);

    return new DoubleDoublePair(amin, amax);
  }

  /**
   * Calculates gaussian density from covdet and squared mahalanobis distance
   * 
   * @param squaredMahalanobis
   * @param covarianceDet
   * @param dim
   * @return
   */
  private double calculateGaussian(double squaredMahalanobis, double covarianceDet, int dim) {
    double num = /*(mahalanobisSQD / 2 > 400) ? 0.0 :*/ FastMath.exp(-squaredMahalanobis / 2);
    // 400 is from original implementation
    double den = cache[dim - 1].piPow * FastMath.sqrt(covarianceDet);
    return num / den;
  }

  /**
   * constrained_mahalanobis_sqd_max in gaussian.c
   * 
   * created if i need more prestuff
   * 
   * @param bbTranslated
   * @param a
   * @param qmax
   * @return QuadraticProblem containing the solution
   */
  private QuadraticProblem constrMahalanobisSqdMax(Boundingbox bbTranslated, double[][] a) {
    double c = 0.0;
    double[] b = new double[a.length];
    QuadraticProblem qp = new QuadraticProblem(a, b, c, bbTranslated, cache);
    qp.maximumValue *= 2; // because we have a .5 factor in the calculation
    return qp;
  }

  ////////////////////////////////////// gaussian.c
  /**
   * calculate sufficient stats to update clusters
   * 
   * @param numP
   * @param models
   * @param knownWeights
   * @param indices
   * @param resultData
   * @param tau
   * @return
   */
  public double makeStats(int numP, ArrayList<? extends EMClusterModel<NumberVector, ? extends MeanModel>> models, double[] knownWeights, int[] indices, ClusterData[] resultData, double tau, double tauLoglike, double tauClass) {
    int k = models.size();
    boolean prune = isLeaf;
    int[] nextIndices = indices;
    if(!prune) {
      nextIndices = checkStoppingCondition(boundingBox, models, knownWeights, resultData, indices, tau, tauLoglike, tauClass);
      if(nextIndices.length == 0) {
        prune = true;
      }
    }
    if(prune) {
      // logarithmic probabilities of clusters in this node
      double[] logProb = new double[k];
      DoubleVector midpoint = new DoubleVector(times(summedPoints, 1.0 / size));
      for(int i = 0; i < k; i++) {
        logProb[i] = models.get(i).estimateLogDensity(midpoint);
      }

      double logDenSum = logSumExp(logProb);
      logProb = minus(logProb, logDenSum);
      for(int c = 0; c < logProb.length; c++) {
        double prob = FastMath.exp(logProb[c]);
        double logAPrio = logProb[c] + FastMath.log(size);
        double[] tCenter = times(summedPoints, prob);
        double[][] tCovariance = times(summedPointsXPointsT, prob);
        knownWeights[c] += FastMath.exp(logAPrio);

        if(resultData[c] != null) {
          resultData[c].increment(logAPrio, tCenter, tCovariance, logProb[c] * size);
        }
        else {
          resultData[c] = new ClusterData(logAPrio, tCenter, tCovariance);
        }
      }
      // pointsWorkedThisStep += node.size;
      return logDenSum * size; // yes, times size.
    }
    else {
      assert nextIndices != null;
      double l = leftChild.makeStats(numP, models, knownWeights, nextIndices, resultData, tau, tauLoglike, tauClass);
      double r = rightChild.makeStats(numP, models, knownWeights, nextIndices, resultData, tau, tauLoglike, tauClass);
      return l + r;
    }
  }

  /**
   * checks if any model in the list does not support checking condition
   * 
   * @param models
   * @return
   */
  public static boolean supportsStoppingCondition(ArrayList<? extends EMClusterModel<NumberVector, ? extends MeanModel>> models) {
    boolean b = true;
    for(EMClusterModel<NumberVector, ? extends MeanModel> emClusterModel : models) {
      b = b && emClusterModel instanceof MultivariateGaussianModel;
    }
    return b;
  }

  /**
   * Boundingbox of a KDTree Node
   * 
   * @author robert
   *
   */
  static public class Boundingbox {

    private double[] hilim, lolim, midpoint, dist;

    public Boundingbox(double[] lolim, double[] hilim) {
      this.hilim = hilim;
      this.lolim = lolim;
      this.midpoint = timesPlusTimes(lolim, 0.5, hilim, 0.5);
      this.dist = minus(hilim, lolim);
    }

    public void printHiLo() {
      System.out.println("Boundingbox");
      System.out.println(Arrays.toString(getLowerBounds()));
      System.out.println(Arrays.toString(getUpperBounds()));

    }

    public double getLowerBound(int k) {
      return lolim[k];
    }

    public double getUpperBound(int k) {
      return hilim[k];
    }

    public double getDifference(int k) {
      return dist[k];
    }

    public double[] getLowerBounds() {
      return lolim;
    }

    public double[] getUpperBounds() {
      return hilim;
    }

    public double[] getDifferences() {
      return dist;
    }

    public double[] getMidPoint() {
      return midpoint;
    }

    public void reduceBoundingboxTo(Boundingbox redBox, int reducedAtt) {
      if(reducedAtt > 0) {
        System.arraycopy(lolim, 0, redBox.getLowerBounds(), 0, reducedAtt);
        System.arraycopy(hilim, 0, redBox.getUpperBounds(), 0, reducedAtt);
        System.arraycopy(midpoint, 0, redBox.getMidPoint(), 0, reducedAtt);
        System.arraycopy(dist, 0, redBox.getDifferences(), 0, reducedAtt);
      }

      if(midpoint.length - (reducedAtt) > 1) {
        int l = redBox.midpoint.length - reducedAtt;
        System.arraycopy(lolim, reducedAtt + 1, redBox.getLowerBounds(), reducedAtt, l);
        System.arraycopy(hilim, reducedAtt + 1, redBox.getUpperBounds(), reducedAtt, l);
        System.arraycopy(midpoint, reducedAtt + 1, redBox.getMidPoint(), reducedAtt, l);
        System.arraycopy(dist, reducedAtt + 1, redBox.getDifferences(), reducedAtt, l);
      }
    }

    public boolean weaklyInsideBounds(double[] opt) {
      for(int i = 0; i < opt.length; i++) {
        if(opt[i] < getLowerBound(i) || opt[i] > getUpperBound(i)) {
          return false;
        }
      }
      return true;
    }
  }

  /**
   * This class holds information collected by makeStats
   * 
   * @author robert
   *
   */
  static class ClusterData {
    /**
     * summed likelihoods
     */
    double summedLogWeights_apriori, summedLoglike;

    /**
     * summed points, used for mean calculation
     */
    double[] summedPoints_mean;

    /**
     * summed squared points, used for covariance calculation
     */
    double[][] summedPointsSquared_cov;

    public ClusterData(double logApriori, double[] center, double[][] cov) {
      this.summedLogWeights_apriori = logApriori;
      this.summedPoints_mean = center;
      this.summedPointsSquared_cov = cov;
      summedLoglike = 0.0;
    }

    void increment(double logApriori, double[] center, double[][] cov, double loglike) {
      this.summedLogWeights_apriori = FastMath.log(FastMath.exp(logApriori) + FastMath.exp(this.summedLogWeights_apriori));
      this.summedPoints_mean = plusEquals(this.summedPoints_mean, center);
      this.summedPointsSquared_cov = plusEquals(this.summedPointsSquared_cov, cov);
      this.summedLoglike = FastMath.log(FastMath.exp(this.summedLoglike) + FastMath.exp(loglike));
    }
  }

  /**
   * ensures d to be in [0,1]
   * 
   * @param d double param
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
