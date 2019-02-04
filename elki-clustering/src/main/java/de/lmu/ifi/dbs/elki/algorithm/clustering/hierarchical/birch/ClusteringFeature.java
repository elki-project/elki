/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2019
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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.birch;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;

/**
 * Clustering Feature of BIRCH
 * 
 * @author Erich Schubert
 * @since 0.7.5
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

  /**
   * Compute the sum of squares of a vector.
   *
   * @param v Vector
   * @return Sum of squares
   */
  public static double sumOfSquares(NumberVector v) {
    final int dim = v.getDimensionality();
    double sum = 0;
    for(int d = 0; d < dim; d++) {
      double x = v.doubleValue(d);
      sum += x * x;
    }
    return sum;
  }
}
