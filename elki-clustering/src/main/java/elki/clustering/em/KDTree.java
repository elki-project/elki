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
import net.jafama.FastMath;

class KDTree {
  private final Logging PARENTLOG;

  KDTree leftChild, rightChild;

  int leftBorder;

  int rightBorder;

  boolean isLeaf = false;

  double[] summedPoints;

  int size;

  double[][] summedPointsXPointsT;

  Boundingbox hyperboundingbox;

  public KDTree(Relation<? extends NumberVector> relation, ArrayModifiableDBIDs sorted, int left, int right, double[] dimwidth, double mbw, Logging parentLog) {
    this.PARENTLOG = parentLog;
    /**
     * other kdtrees seem to look at [left, right[
     */
    DBIDArrayIter iter = sorted.iter();
    int dim = relation.get(iter).toArray().length;

    leftBorder = left;
    rightBorder = right;
    summedPoints = new double[dim];
    summedPointsXPointsT = new double[dim][dim];
    double[][] hyperboundingboxvalues = new double[3][dim];
    // size
    size = right - left;
    iter.seek(left);
    hyperboundingboxvalues[0] = relation.get(iter).toArray();
    hyperboundingboxvalues[1] = relation.get(iter).toArray();

    for(int i = 0; i < size; i++) {
      NumberVector vector = relation.get(iter);
      for(int d = 0; d < dim; d++) {
        double value = vector.doubleValue(d);
        // bounding box
        if(value < hyperboundingboxvalues[0][d])
          hyperboundingboxvalues[0][d] = value;
        else if(value > hyperboundingboxvalues[1][d])
          hyperboundingboxvalues[1][d] = value;
      }
      if(iter.valid())
        iter.advance();
    }

    for(int i = 0; i < dim; i++) {
      hyperboundingboxvalues[2][i] = FastMath.abs(hyperboundingboxvalues[1][i] - hyperboundingboxvalues[0][i]);
    }

    hyperboundingbox = new Boundingbox(hyperboundingboxvalues);

    iter.seek(left);
    // handle leaf -> calculate summedPoints and summedPointsXPointsT
    final int splitDim = argmax(hyperboundingbox.getAllDiffs());
    if(hyperboundingbox.getDiff(splitDim) < mbw * dimwidth[splitDim]) {
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
    else { // calculate mid-node
      // split points
      // paper implementation splits at mid boundingbox and not with points?
      double splitpoint = hyperboundingbox.getLo(splitDim) + 0.5 * hyperboundingbox.getDiff(splitDim);
      int l = left, r = right - 1;
      while(true) {
        while(l <= r && relation.get(iter.seek(l)).doubleValue(splitDim) <= splitpoint) {
          ++l;
        }
        while(l <= r && relation.get(iter.seek(r)).doubleValue(splitDim) >= splitpoint) {
          --r;
        }
        if(l >= r) {
          break;
        }
        sorted.swap(l++, r--);
      }

      assert relation.get(iter.seek(r)).doubleValue(splitDim) <= splitpoint : relation.get(iter.seek(r)).doubleValue(splitDim) + " not less than " + splitpoint;
      ++r;
      if(r == right) { // Duplicate points!
        isLeaf = true;
        return;
      }
      leftChild = new KDTree(relation, sorted, left, r, dimwidth, mbw, PARENTLOG);
      rightChild = new KDTree(relation, sorted, r, right, dimwidth, mbw, PARENTLOG);

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
   * @param hyperboundingbox
   * @param numP
   * @param models
   * @return
   */
  public int[] checkStoppingCondition(Boundingbox hyperboundingbox, int numP, ArrayList<? extends EMClusterModel<NumberVector, ? extends MeanModel>> models, double[] weightsKnown, int[] indices, double tau) {
    DoubleDoublePair[] limits = new DoubleDoublePair[models.size()];
    if(models.get(0) instanceof MultivariateGaussianModel) {
      for(int i : indices) {
        limits[i] = evaluateGaussianLimits(hyperboundingbox, (MultivariateGaussianModel) models.get(i));
      }
    }
    else {
      if(PARENTLOG.isVerbose())
        PARENTLOG.verbose("Model doesn't support checking of stoppingcondition");
      return indices;
    }
    // assume weight not logarithmic
    double sumprobXlimitLO = 0.0;
    double sumprobXlimitHI = 0.0;
    for(int i : indices) {
      sumprobXlimitLO += models.get(i).getWeight() * limits[i].first;
      sumprobXlimitHI += models.get(i).getWeight() * limits[i].second;
    }
    boolean prune = true;
    double wmin_max = Double.NEGATIVE_INFINITY;
    double[] wmaxs = new double[models.size()];
    for(int i : indices) {
      double weight = models.get(i).getWeight();
      double wminDenom = sumprobXlimitHI + weight * (limits[i].first - limits[i].second);
      double wmaxDenom = sumprobXlimitLO + weight * (limits[i].second - limits[i].first);

      // calculate wmin and wmax and ensure range
      double wmin = (weight * limits[i].first) / wminDenom;
      wmin = wmin < 0.0 ? 0.0 : wmin > 1.0 ? 1.0 : wmin;
      wmin_max = wmin > wmin_max ? wmin : wmin_max;
      wmaxs[i] = (weight * limits[i].second) / wmaxDenom;
      wmaxs[i] = wmaxs[i] < 0.0 ? 0.0 : wmaxs[i] > 1.0 ? 1.0 : wmaxs[i];
      // original implementation uses a minimum possible wtotal
      double wtotal = weightsKnown[i] + wmin * size;

      double maxerror = size * (wmaxs[i] - wmin);
      // from original impl
      // tau
      // Set tau = 0.01 for very high accuracy
      // Set tau = 0.2 for typical performance
      // Set tau = 0.6 for fast but innaccurate
      if(maxerror > tau * wtotal)
        prune = false;
      // System.out.println("wtotal: " + wtotal + "; maxerror: " + maxerror);
    }
    // TODO check for large loglikelihood deviation
    if(!prune) {
      IntegerArray result = new IntegerArray();
      for(int i : indices) {
        if(wmaxs[i] > 0.01 * wmin_max) {
          // value from original impl. in paper its 10^-4
          result.add(i);
        }
      }
      return result.toArray();
    }
    return new int[0];
  }

  ////////////////////////////////////// gaussian.c
  /**
   * calculates from y(x) = 0.5 * x^T a x + b^T x
   * and returns 2 * y(x)
   * 
   * @param hyperboundingbox
   * @param model
   * @param a
   * @return
   */
  public DoubleDoublePair evaluateGaussianLimits(Boundingbox hyperboundingbox, MultivariateGaussianModel model) {
    // translate problem by mean
    double[] mean = model.mean;
    double[][] hrTranslated = new double[3][mean.length];
    hrTranslated[0] = minus(hyperboundingbox.getAllLo(), mean);
    hrTranslated[1] = minus(hyperboundingbox.getAllHi(), mean);
    hrTranslated[2] = hyperboundingbox.getAllDiffs().clone();
    Boundingbox bbTranslated = new Boundingbox(hrTranslated);

    // invert and insure symmetric
    double[][] covInv = inverse(model.covariance.clone());// TODO dont know if
                                                          // clone is needed
    double covdet = FastMath.exp(2 * MultivariateGaussianModel.getHalfLogDeterminant(model.chol));
    for(int i = 0; i < covInv.length; i++) {
      for(int j = i; j < covInv[i].length; j++) {
        if(Math.abs(covInv[i][j] - covInv[j][i]) > 1e-10) {
          System.err.println("covinv matrix nonsymmetric");
        }
        covInv[j][i] = covInv[i][j] = (covInv[i][j] + covInv[j][i]) / 2;
        // if this fires we can just force symetry
      }
    }
    // maximizes mahalanobis dist
    QuadraticProblem quadmax = constrMahalanobisSqdMax(bbTranslated, covInv);
    double mahalanobisSQDmax = quadmax.maximumvalue;
    // minimizes mahalanobis dist
    QuadraticProblem quadmin = constrMahalanobisSqdMin(bbTranslated, covInv);
    double mahalanobisSQDmin = quadmin.maximumvalue;
    double amin = calculateGaussMahaSQDCovdet(mahalanobisSQDmax, covdet, mean.length);
    double amax = calculateGaussMahaSQDCovdet(mahalanobisSQDmin, covdet, mean.length);

    // if(!isLeaf) {
    // System.out.println("mahalmax: " + quadmax.maximumvalue + " at Point: " +
    // Arrays.toString(plus(quadmax.maxpoint, mean)));
    // System.out.println("min_a: " + amin);
    // System.out.println("mahalmin: " + quadmin.maximumvalue + " at Point: " +
    // Arrays.toString(plus(quadmin.maxpoint, mean)));
    // System.out.println("max_a: " + amax);
    // }
    // result[0] = getlowlimit
    // result[1] = gethighlimit
    return new DoubleDoublePair(amin, amax);
  }

  private double calculateGaussMahaSQDCovdet(double mahalanobisSQD, double covdet, int dim) {
    double num = (mahalanobisSQD / 2 > 400) ? 0.0 : FastMath.exp(-mahalanobisSQD / 2);
    // 400 is from original implementation
    double den = FastMath.pow(2.5066283, (dim)) * FastMath.sqrt(covdet);
    // root 2 pi from original impl
    // TODO in original impl the power is cached
    return num / den;
  }

  private QuadraticProblem constrMahalanobisSqdMin(Boundingbox bbTranslated, double[][] a) {
    double[][] ma = times(a, -1.0);
    QuadraticProblem qp = constrMahalanobisSqdMax(bbTranslated, ma);
    qp.maximumvalue *= -1;
    return qp;
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
    QuadraticProblem qp = new QuadraticProblem(a, b, c, bbTranslated);
    qp.maximumvalue *= 2; // because we have a .5 factor in the calculation
    return qp;
  }

  ////////////////////////////////////// gaussian.c
  /**
   * calculate sufficient stats to update clusters
   * @param numP
   * @param models
   * @param knownWeights
   * @param indices
   * @param resultData
   * @param tau
   * @return
   */
  public double makeStats(int numP, ArrayList<? extends EMClusterModel<NumberVector, ? extends MeanModel>> models, double[] knownWeights, int[] indices, ClusterData[] resultData, double tau) {
    int k = models.size();
    boolean prune = isLeaf;
    int[] nextIndices = null;
    if(!prune) {
      nextIndices = checkStoppingCondition(hyperboundingbox, numP, models, knownWeights, indices, tau);
      if(nextIndices.length == 0) {
        prune = true;
      }
    }
    if(prune) {
      // logarithmic probabilities of clusters in this node
      double[] logProb = new double[k];

      for(int i = 0; i < k; i++) {
        logProb[i] = models.get(i).estimateLogDensity(DoubleVector.copy(times(summedPoints, 1.0 / size)));
      }

      double logDenSum = logSumExp(logProb);
      logProb = minus(logProb, logDenSum);
      for(int c = 0; c < logProb.length; c++) {

        double prob = FastMath.exp(logProb[c]);
        double logAPrio = logProb[c] + FastMath.log(size);
        double[] tcenter = times(summedPoints, prob);
        double[][] tcov = times(summedPointsXPointsT, prob);
        knownWeights[c] += FastMath.exp(logAPrio);

        if(resultData[c] != null) {
          resultData[c].increment(logAPrio, tcenter, tcov);
        }
        else {
          resultData[c] = new ClusterData(logAPrio, tcenter, tcov);
        }

        // ~~ThisStep is for the approximation part currently left out
        // weightedAppliedThisStep[c] += FastMath.exp(logAPrio);
      }
      // pointsWorkedThisStep += node.size;
      return logDenSum * size; // yes, times size.
    }
    else {
      assert nextIndices != null;
      double l = leftChild.makeStats(numP, models, knownWeights, nextIndices, resultData, tau);
      double r = rightChild.makeStats(numP, models, knownWeights, nextIndices, resultData, tau);
      return l + r;
    }
  }

  static public class Boundingbox {

    private double[][] hyperboundingbox;

    public Boundingbox(double[][] box) {
      assert box == null || box.length == 3;
      hyperboundingbox = box;
    }

    public void printHiLo() {
      System.out.println("Boundingbox");
      System.out.println(Arrays.toString(getAllLo()));
      System.out.println(Arrays.toString(getAllHi()));

    }

    public void setBB(double[][] box) {
      assert box == null || box.length == 3;
      this.hyperboundingbox = box;
    }

    public double[][] getBB() {
      return hyperboundingbox;
    }

    public double getLo(int k) {
      return hyperboundingbox[0][k];
    }

    public double getHi(int k) {
      return hyperboundingbox[1][k];
    }

    public double getDiff(int k) {
      return hyperboundingbox[2][k];
    }

    public double[] getAllLo() {
      return hyperboundingbox[0];
    }

    public double[] getAllHi() {
      return hyperboundingbox[1];
    }

    public double[] getAllDiffs() {
      return hyperboundingbox[2];
    }

    public double[] getMidPoint() {
      return times(plus(getAllHi(), getAllLo()), 0.5);
    }

    public boolean weaklyInsideBounds(double[] opt) {
      for(int i = 0; i < opt.length; i++) {
        if(opt[i] < getLo(i) || opt[i] > getHi(i)) {
          return false;
        }
      }
      return true;
    }
  }

  static class ClusterData {
    double summedLogWeights_apriori;

    double[] summedPoints_mean;

    double[][] summedPointsSquared_cov;

    public ClusterData(double logApriori, double[] center, double[][] cov) {
      this.summedLogWeights_apriori = logApriori;
      this.summedPoints_mean = center;
      this.summedPointsSquared_cov = cov;
    }

    void combine(ClusterData other) {
      this.summedLogWeights_apriori = FastMath.log(FastMath.exp(other.summedLogWeights_apriori) + FastMath.exp(this.summedLogWeights_apriori));

      this.summedPoints_mean = plus(this.summedPoints_mean, other.summedPoints_mean);

      this.summedPointsSquared_cov = plus(this.summedPointsSquared_cov, other.summedPointsSquared_cov);
    }

    void increment(double logApriori, double[] center, double[][] cov) {
      this.summedLogWeights_apriori = FastMath.log(FastMath.exp(logApriori) + FastMath.exp(this.summedLogWeights_apriori));
      this.summedPoints_mean = plusEquals(this.summedPoints_mean, center);
      this.summedPointsSquared_cov = plusEquals(this.summedPointsSquared_cov, cov);
    }
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
