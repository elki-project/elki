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

import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.data.model.MeanModel;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDArrayIter;
import elki.database.relation.Relation;

import net.jafama.FastMath;

class KDTree {
  KDTree leftChild, rightChild;

  int leftBorder;

  int rightBorder;

  boolean isLeaf = false;

  double[] center;

  int size;

  double[][] cov;

  double[][] hyperboundingbox;

  public KDTree(Relation<? extends NumberVector> relation, ArrayModifiableDBIDs sorted, int left, int right, double[] dimwidth, double mbw) {
    /**
     * other kdtrees seem to look at [left, right[
     */
    DBIDArrayIter iter = sorted.iter();
    int dim = relation.get(iter).toArray().length;

    leftBorder = left;
    rightBorder = right;
    center = new double[dim];
    cov = new double[dim][dim];
    hyperboundingbox = new double[3][dim];
    // size
    size = right - left;
    iter.seek(left);
    hyperboundingbox[0] = relation.get(iter).toArray();
    hyperboundingbox[1] = relation.get(iter).toArray();

    for(int i = 0; i < size; i++) {
      NumberVector vector = relation.get(iter);
      for(int d = 0; d < dim; d++) {
        double value = vector.doubleValue(d);
        // bounding box
        if(value < hyperboundingbox[0][d])
          hyperboundingbox[0][d] = value;
        else if(value > hyperboundingbox[1][d])
          hyperboundingbox[1][d] = value;
        // center
        center[d] += value;
      }
      if(iter.valid())
        iter.advance();
    }

    for(int i = 0; i < dim; i++) {
      center[i] = center[i] / size;
      hyperboundingbox[2][i] = FastMath.abs(hyperboundingbox[1][i] - hyperboundingbox[0][i]);
    }
    iter.seek(left);

    // cov - is this "textbook"?
    // here lies a problem. It seems that this makes it impossible to
    // implement circle and stuff.
    // to know this though, i need to take a second look at the paper and what
    // gets calculated where.
    for(int i = 0; i < size; i++) {
      NumberVector vector = relation.get(iter);
      for(int d1 = 0; d1 < dim; d1++) {
        double value1 = vector.doubleValue(d1);
        for(int d2 = 0; d2 < dim; d2++) {
          double value2 = vector.doubleValue(d2);
          cov[d1][d2] += (value1 - center[d1]) * (value2 - center[d2]);
        }
      }
      if(iter.valid())
        iter.advance();
    }
    for(int d1 = 0; d1 < dim; d1++) {
      for(int d2 = 0; d2 < dim; d2++) {
        cov[d1][d2] = cov[d1][d2] / (double) size;
      }
    }

    final int splitDim = argmax(hyperboundingbox[2]);
    if(hyperboundingbox[2][splitDim] < mbw * dimwidth[splitDim]) {
      isLeaf = true;
      return;
    }

    double splitpoint = center[splitDim];
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
    leftChild = new KDTree(relation, sorted, left, r, dimwidth, mbw);
    rightChild = new KDTree(relation, sorted, r, right, dimwidth, mbw);
  }

  /*
  public boolean checkStoppingCondition(int numP) {
    DBIDArrayIter  it = sorted.iter().seek(leftBorder);
    double[][] mahaDists = new double[k][2];
    // mahaDists[c][0 = min; 1 = max]
    for(int c = 0; c < mahaDists.length; c++) {
      mahaDists[c][0] = Integer.MAX_VALUE;
    }
    
    for(int i = 0; i< size; i++) {
      double[] tdis = currentMahalanobis.get(it);
      for(int c = 0; c < tdis.length; c++) {
        mahaDists[c][0] = mahaDists[c][0] < tdis[c] ? mahaDists[c][0] : tdis[c];
        mahaDists[c][1] = mahaDists[c][1] > tdis[c] ? mahaDists[c][1] : tdis[c];
      }
      if(it.valid())
        it.advance();
    }
    //note that from here mahaDists describes logdensity and is [c][0 = max; 1 = min]
    double maxsum = 0;
    double minsum = 0;
    for(int c = 0; c < mahaDists.length; c++) {
      mahaDists[c][0] =  -.5 * mahaDists[c][0] + classes[c].logNormDet;
      mahaDists[c][1] =  -.5 * mahaDists[c][1] + classes[c].logNormDet;
      maxsum += FastMath.exp(mahaDists[c][0]+ classes[c].data.logApriori_sw) ;
      minsum += FastMath.exp(mahaDists[c][1]+ classes[c].data.logApriori_sw);
    }
    //wmin = amin*p / amin*p + sum_other(amax * p)
    //i guess that the other formular is "similar" it is analog
    // from here on mahaDists describes wmax, wmin
    for(int c = 0; c < mahaDists.length; c++) {
      mahaDists[c][0] =  FastMath.exp(mahaDists[c][0]+ classes[c].data.logApriori_sw) 
          / (minsum-FastMath.exp(mahaDists[c][1]+ classes[c].data.logApriori_sw) + FastMath.exp(mahaDists[c][0]+ classes[c].data.logApriori_sw));
      mahaDists[c][1] =  FastMath.exp(mahaDists[c][1]+ classes[c].data.logApriori_sw) 
          / (maxsum-FastMath.exp(mahaDists[c][0]+ classes[c].data.logApriori_sw) + FastMath.exp(mahaDists[c][1]+ classes[c].data.logApriori_sw));
      // check dis
      double d = mahaDists[c][0]- mahaDists[c][1];
      if(d > weightedAppliedThisStep[c]/pointsWorkedThisStep + (numP - pointsWorkedThisStep)*mahaDists[c][1]*tau) {
        return false;
      }
    }
    return true;
  }
  */
  public ClusterData[] makeStats(int numP, ArrayList<? extends EMClusterModel<NumberVector, ? extends MeanModel>> models) {
    int k = models.size();
    if(isLeaf /*||node.checkStoppingCondition(numP)*/) {
      ClusterData[] res = new ClusterData[k];
      // logarithmic probabilities of clusters in this node
      double[] logProb = new double[k];

      for(int i = 0; i < k; i++) {
        logProb[i] = models.get(i).estimateLogDensity(DoubleVector.copy(center));
      }

      double logDenSum = logSumExp(logProb);
      logProb = minus(logProb, logDenSum);

      for(int c = 0; c < logProb.length; c++) {
        double prob = FastMath.exp(logProb[c]);
        double logAPrio = logProb[c] + FastMath.log(size);
        double[] tcenter = times(center, prob * size);
        double[][] tcov = times(timesTranspose(center, center), prob * size);
        res[c] = new ClusterData(logAPrio, tcenter, tcov);
        // ~~ThisStep is for the approximation part currently left out
        // weightedAppliedThisStep[c] += FastMath.exp(logAPrio);
      }
      // pointsWorkedThisStep += node.size;
      return res;
    }
    else {
      ClusterData[] lData = leftChild.makeStats(numP, models);
      ClusterData[] rData = rightChild.makeStats(numP, models);
      for(int c = 0; c < lData.length; c++) {
        lData[c].combine(rData[c]);
      }
      return lData;
    }
  }

  static class ClusterData {
    double logApriori_sw;

    double[] center_swx;

    double[][] cov_swxx;

    public ClusterData(double logApriori, double[] center, double[][] cov) {
      this.logApriori_sw = logApriori;
      this.center_swx = center;
      this.cov_swxx = cov;
    }

    void combine(ClusterData other) {
      this.logApriori_sw = FastMath.log(FastMath.exp(other.logApriori_sw) + FastMath.exp(this.logApriori_sw));

      this.center_swx = plus(this.center_swx, other.center_swx);

      this.cov_swxx = plus(this.cov_swxx, other.cov_swxx);
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
