package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.birch;

/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2017
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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;

/**
 * Clustering Feature of BIRCH
 * 
 * @author Erich Schubert
 */
public class ClusteringFeature {
  /**
   * Number of objects
   */
  int n;

  /**
   * Linear sum.
   */
  double[] ls;

  /**
   * Sum of squares (see original thesis, this is a scalar).
   */
  double ss;

  /**
   * Constructor.
   *
   * @param dimensionality Dimensionality
   */
  public ClusteringFeature(int dimensionality) {
    this.n = 0;
    this.ls = new double[dimensionality];
  }

  /**
   * Add a number vector to the current node.
   *
   * @param nv Vector to add
   */
  protected void addToStatistics(NumberVector nv) {
    final int d = nv.getDimensionality();
    assert (d == ls.length);
    this.n++;
    for(int i = 0; i < d; i++) {
      double v = nv.doubleValue(i);
      ls[i] += v;
      ss += v * v;
    }
  }

  /**
   * Merge an other clustering features.
   * 
   * @param other Other CF
   */
  protected void addToStatistics(ClusteringFeature other) {
    n += other.n;
    VMath.plusEquals(ls, other.ls);
    ss += other.ss;
  }

  /**
   * Reset the CF to zero. For use in splitting.
   */
  protected void resetStatistics() {
    n = 0;
    Arrays.fill(ls, 0.);
    ss = 0;
  }

  /**
   * Centroid value in dimension i.
   * 
   * @param i Dimension
   * @return Average, or zero
   */
  public double centroid(int i) {
    return n > 0 ? ls[i] / n : 0.;
  }

  /**
   * Compute the radius of a cluster feature.
   * 
   * Based on Appendix A of Data clustering for very large datasets plus
   * applications, Tian Zhang, Doctoral Dissertation, 1997.
   * 
   * @return Radius
   */
  public double radius() {
    if(n <= 1) {
      return 0.;
    }
    double sum = sumOfSumOfSquares();
    sum /= n;
    for(int i = 0; i < ls.length; i++) {
      double v = ls[i] / n;
      sum -= v * v;
    }
    return Math.sqrt(sum);
  }

  /**
   * Compute the radius of a cluster feature.
   * 
   * Based on Appendix A of Data clustering for very large datasets plus
   * applications, Tian Zhang, Doctoral Dissertation, 1997.
   * 
   * @param nv Virtual point to be added.
   * @return Radius
   */
  public double radiusWith(NumberVector nv) {
    if(n <= 0) {
      return 0.;
    }
    double sum = sumOfSumOfSquares();
    for(int i = 0; i < ls.length; i++) {
      double v = nv.doubleValue(i);
      sum += v * v;
    }
    sum /= n + 1;
    for(int i = 0; i < ls.length; i++) {
      double v = (ls[i] + nv.doubleValue(i)) / (n + 1);
      sum -= v * v;
    }
    return Math.sqrt(sum);
  }

  /**
   * Compute the diameter of a cluster feature.
   * 
   * Based on Appendix A of Data clustering for very large datasets plus
   * applications, Tian Zhang, Doctoral Dissertation, 1997.
   * 
   * @return Diameter
   */
  public double diameter() {
    if(n <= 1) {
      return 0.;
    }
    double v = (ss * n - sumOfSquaresOfSums()) * 2. / (n * (n - 1.));
    return v > 0. ? Math.sqrt(v) : 0.;
  }

  /**
   * Compute the diameter of a cluster feature.
   * 
   * Based on Appendix A of Data clustering for very large datasets plus
   * applications, Tian Zhang, Doctoral Dissertation, 1997.
   * 
   * @param nv Additional vector
   * @return Diameter
   */
  public double diameterSqWith(NumberVector nv) {
    if(n <= 0) {
      return 0.;
    }
    double sum1 = ss, sum2 = 0.;
    for(int i = 0; i < ls.length; i++) {
      double x = nv.doubleValue(i);
      sum1 += x * x;
      double v = ls[i] + x;
      sum2 += v * v;
    }
    double diameter = (sum1 * (n + 1) - sum2) * 2. / ((n + 1.) * n);
    return diameter > 0 ? diameter : 0.;
  }

  /**
   * Sum over all dimensions of sums of squares.
   *
   * @return Sum of SS
   */
  public double sumOfSumOfSquares() {
    return ss;
  }

  /**
   * Sum over all dimensions of squares of linear sums.
   *
   * @return Sum of LS
   */
  public double sumOfSquaresOfSums() {
    double sum = 0.;
    for(int i = 0; i < ls.length; i++) {
      double v = ls[i];
      sum += v * v;
    }
    return sum;
  }

  /**
   * Dimensionality of the clustering feature.
   * 
   * @return Dimensionality
   */
  public int getDimensionality() {
    return ls.length;
  }
}
