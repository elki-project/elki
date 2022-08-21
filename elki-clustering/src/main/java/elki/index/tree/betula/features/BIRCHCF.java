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
package elki.index.tree.betula.features;

import java.util.Arrays;

import elki.data.NumberVector;
import elki.math.linearalgebra.VMath;
import elki.utilities.Priority;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Clustering Feature of BIRCH, only for comparison
 * 
 * @author Erich Schubert
 * @since 0.8.0
 */
public class BIRCHCF implements ClusterFeature {
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
  public BIRCHCF(int dimensionality) {
    this.n = 0;
    this.ls = new double[dimensionality];
  }

  @Override
  public void addToStatistics(NumberVector nv) {
    final int d = nv.getDimensionality();
    assert d == ls.length;
    this.n++;
    for(int i = 0; i < d; i++) {
      final double v = nv.doubleValue(i);
      ls[i] += v;
      ss += v * v;
    }
  }

  @Override
  public void addToStatistics(ClusterFeature other) {
    addToStatistics((BIRCHCF) other);
  }

  // @Override
  public void addToStatistics(BIRCHCF other) {
    n += other.n;
    VMath.plusEquals(ls, other.ls);
    ss += other.ss;
  }

  @Override
  public void resetStatistics() {
    n = 0;
    Arrays.fill(ls, 0.);
    ss = 0;
  }

  @Override
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
      final double v = ls[i];
      sum += v * v;
    }
    return sum;
  }

  @Override
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
      final double x = v.doubleValue(d);
      sum += x * x;
    }
    return sum;
  }

  /**
   * Get the linear sum of component i.
   * 
   * @param i Component
   * @return linear sum
   */
  public double ls(int i) {
    return ls[i];
  }

  @Override
  public double variance(int i) {
    double v = variance();
    return v >= 0. ? v / ls.length : 0.;
  }

  @Override
  public int getWeight() {
    return n;
  }

  @Override
  public double variance() {
    double v = ss / n;
    for(int d = 0; d < ls.length; d++) {
      final double s = ls[d] / n;
      v -= s * s;
    }
    return v >= 0. ? v : 0;
  }

  @Override
  public double sumdev() {
    double v = ss;
    for(int d = 0; d < ls.length; d++) {
      final double s = ls[d];
      v -= s * s / n;
    }
    return v;
  }

  @Override
  public double[][] covariance() {
    throw new IllegalStateException("This CF Model doesn't support this method.");
  }

  @Override
  public double[] toArray() {
    return VMath.times(ls, 1. / n);
  }

  /**
   * Factory for making cluster features.
   * 
   * @author Erich Schubert
   */
  @Priority(Priority.SUPPLEMENTARY)
  public static class Factory implements ClusterFeature.Factory<BIRCHCF> {
    /**
     * Static instance.
     */
    public static final Factory STATIC = new Factory();

    @Override
    public BIRCHCF make(int dim) {
      return new BIRCHCF(dim);
    }

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
     */
    public static class Par implements Parameterizer {
      @Override
      public Factory make() {
        return STATIC;
      }
    }
  }
}
