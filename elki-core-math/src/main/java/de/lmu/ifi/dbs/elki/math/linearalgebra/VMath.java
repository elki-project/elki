package de.lmu.ifi.dbs.elki.math.linearalgebra;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Arrays;

/**
 * Class providing basic vector mathematics, for low-level vectors stored as
 * {@code double[]}. While this is less nice syntactically, it reduces memory
 * usage and VM overhead.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @apiviz.landmark
 */
public final class VMath {
  /**
   * A small number to handle numbers near 0 as 0.
   */
  public static final double DELTA = 1E-5;

  /**
   * Error message (in assertions!) when vector dimensionalities do not agree.
   */
  public static final String ERR_VEC_DIMENSIONS = "Vector dimensions do not agree.";

  /**
   * Error message (in assertions!) when matrix dimensionalities do not agree.
   */
  public static final String ERR_MATRIX_DIMENSIONS = "Matrix dimensions do not agree.";

  /**
   * Error message (in assertions!) when matrix dimensionalities do not agree.
   */
  public static final String ERR_MATRIX_INNERDIM = "Matrix inner dimensions do not agree.";

  /**
   * Error message (in assertions!) when dimensionalities do not agree.
   */
  private static final String ERR_DIMENSIONS = "Dimensionalities do not agree.";

  /**
   * Fake constructor. Static class.
   */
  private VMath() {
    // Cannot be instantiated
  }

  /**
   * Returns a randomly created vector of length 1.0
   * 
   * @param dimensionality dimensionality
   * @return Random vector of length 1.0
   */
  public static final double[] randomNormalizedVector(final int dimensionality) {
    final double[] v = new double[dimensionality];
    for(int i = 0; i < dimensionality; i++) {
      v[i] = Math.random();
    }
    double norm = euclideanLength(v);
    if(norm != 0) {
      for(int row = 0; row < v.length; row++) {
        v[row] /= norm;
      }
      return v;
    }
    else {
      return randomNormalizedVector(dimensionality);
    }
  }

  /**
   * Returns the ith unit vector of the specified dimensionality.
   * 
   * @param dimensionality the dimensionality of the vector
   * @param i the index
   * @return the ith unit vector of the specified dimensionality
   */
  public static final double[] unitVector(final int dimensionality, final int i) {
    final double[] v = new double[dimensionality];
    v[i] = 1;
    return v;
  }

  /**
   * Returns a copy of this vector.
   * 
   * @param v original vector
   * @return a copy of this vector
   */
  public static final double[] copy(final double[] v) {
    return Arrays.copyOf(v, v.length);
  }

  /**
   * Transpose vector to a matrix.
   * 
   * @param v Vector
   * @return Matrix
   */
  public static final double[][] transpose(final double[] v) {
    double[][] re = new double[v.length][1];
    for(int i = 0; i < v.length; i++) {
      re[i][0] = v[i];
    }
    return re;
  }

  /**
   * Computes v1 + v2 for vectors.
   * 
   * @param v1 first vector
   * @param v2 second vector
   * @return the sum v1 + v2
   */
  public static final double[] plus(final double[] v1, final double[] v2) {
    assert (v1.length == v2.length) : ERR_VEC_DIMENSIONS;
    final double[] result = new double[v1.length];
    for(int i = 0; i < result.length; i++) {
      result[i] = v1[i] + v2[i];
    }
    return result;
  }

  /**
   * Computes v1 + v2 * s2
   * 
   * @param v1 first vector
   * @param v2 second vector
   * @param s2 the scalar
   * @return the result of v1 + v2 * s2
   */
  public static final double[] plusTimes(final double[] v1, final double[] v2, final double s2) {
    assert (v1.length == v2.length) : ERR_VEC_DIMENSIONS;
    final double[] result = new double[v1.length];
    for(int i = 0; i < result.length; i++) {
      result[i] = v1[i] + v2[i] * s2;
    }
    return result;
  }

  /**
   * Computes v1 * s1 + v2
   * 
   * @param v1 first vector
   * @param s1 the scalar for v1
   * @param v2 second vector
   * @return the result of v1 * s1 + v2
   */
  public static final double[] timesPlus(final double[] v1, final double s1, final double[] v2) {
    assert (v1.length == v2.length) : ERR_VEC_DIMENSIONS;
    final double[] result = new double[v1.length];
    for(int i = 0; i < result.length; i++) {
      result[i] = v1[i] * s1 + v2[i];
    }
    return result;
  }

  /**
   * Computes v1 * s1 + v2 * s2
   * 
   * @param v1 first vector
   * @param s1 the scalar for v1
   * @param v2 second vector
   * @param s2 the scalar for v2
   * @return the result of v1 * s1 + v2 * s2
   */
  public static final double[] timesPlusTimes(final double[] v1, final double s1, final double[] v2, final double s2) {
    assert (v1.length == v2.length) : ERR_VEC_DIMENSIONS;
    final double[] result = new double[v1.length];
    for(int i = 0; i < result.length; i++) {
      result[i] = v1[i] * s1 + v2[i] * s2;
    }
    return result;
  }

  /**
   * Computes v1 = v1 + v2, overwriting v1
   * 
   * @param v1 first vector (overwritten)
   * @param v2 second vector
   * @return v1 = v1 + v2
   */
  public static final double[] plusEquals(final double[] v1, final double[] v2) {
    assert (v1.length == v2.length) : ERR_VEC_DIMENSIONS;
    for(int i = 0; i < v1.length; i++) {
      v1[i] += v2[i];
    }
    return v1;
  }

  /**
   * Computes v1 = v1 + v2 * s2, overwriting v1
   * 
   * @param v1 first vector
   * @param v2 another vector
   * @param s2 scalar factor for v2
   * @return v1 = v1 + v2 * s2
   */
  public static final double[] plusTimesEquals(final double[] v1, final double[] v2, final double s2) {
    assert (v1.length == v2.length) : ERR_VEC_DIMENSIONS;
    for(int i = 0; i < v1.length; i++) {
      v1[i] += s2 * v2[i];
    }
    return v1;
  }

  /**
   * Computes v1 = v1 * s1 + v2, overwriting v1
   * 
   * @param v1 first vector
   * @param s1 scalar factor for v1
   * @param v2 another vector
   * @return v1 = v1 * s1 + v2
   */
  public static final double[] timesPlusEquals(final double[] v1, final double s1, final double[] v2) {
    assert (v1.length == v2.length) : ERR_VEC_DIMENSIONS;
    for(int i = 0; i < v1.length; i++) {
      v1[i] = v1[i] * s1 + v2[i];
    }
    return v1;
  }

  /**
   * Computes v1 = v1 * s1 + v2 * s2, overwriting v1
   * 
   * @param v1 first vector
   * @param s1 scalar for v1
   * @param v2 another vector
   * @param s2 scalar for v2
   * @return v1 = v1 * s1 + v2 * s2
   */
  public static final double[] timesPlusTimesEquals(final double[] v1, final double s1, final double[] v2, final double s2) {
    assert (v1.length == v2.length) : ERR_VEC_DIMENSIONS;
    for(int i = 0; i < v1.length; i++) {
      v1[i] = v1[i] * s1 + v2[i] * s2;
    }
    return v1;
  }

  /**
   * Computes v1 + d
   * 
   * @param v1 vector to add to
   * @param d value to add
   * @return v1 + d
   */
  public static final double[] plus(final double[] v1, final double d) {
    final double[] result = new double[v1.length];
    for(int i = 0; i < result.length; i++) {
      result[i] = v1[i] + d;
    }
    return result;
  }

  /**
   * Computes v1 = v1 + d, overwriting v1
   * 
   * @param v1 vector to add to
   * @param d value to add
   * @return Modified vector
   */
  public static final double[] plusEquals(final double[] v1, final double d) {
    for(int i = 0; i < v1.length; i++) {
      v1[i] += d;
    }
    return v1;
  }

  /**
   * Computes v1 - v2
   * 
   * @param v1 first vector
   * @param v2 the vector to be subtracted from this vector
   * @return v1 - v2
   */
  public static final double[] minus(final double[] v1, final double[] v2) {
    final double[] sub = new double[v1.length];
    for(int i = 0; i < v1.length; i++) {
      sub[i] = v1[i] - v2[i];
    }
    return sub;
  }

  /**
   * Computes v1 - v2 * s2
   * 
   * @param v1 first vector
   * @param v2 the vector to be subtracted from this vector
   * @param s2 the scaling factor for v2
   * @return v1 - v2 * s2
   */
  public static final double[] minusTimes(final double[] v1, final double[] v2, final double s2) {
    final double[] sub = new double[v1.length];
    for(int i = 0; i < v1.length; i++) {
      sub[i] = v1[i] - v2[i] * s2;
    }
    return sub;
  }

  /**
   * Computes v1 * s1 - v2
   * 
   * @param v1 first vector
   * @param s1 the scaling factor for v1
   * @param v2 the vector to be subtracted from this vector
   * @return v1 * s1 - v2
   */
  public static final double[] timesMinus(final double[] v1, final double s1, final double[] v2) {
    final double[] sub = new double[v1.length];
    for(int i = 0; i < v1.length; i++) {
      sub[i] = v1[i] * s1 - v2[i];
    }
    return sub;
  }

  /**
   * Computes v1 * s1 - v2 * s2
   * 
   * @param v1 first vector
   * @param s1 the scaling factor for v1
   * @param v2 the vector to be subtracted from this vector
   * @param s2 the scaling factor for v2
   * @return v1 * s1 - v2 * s2
   */
  public static final double[] timesMinusTimes(final double[] v1, final double s1, final double[] v2, final double s2) {
    final double[] sub = new double[v1.length];
    for(int i = 0; i < v1.length; i++) {
      sub[i] = v1[i] * s1 - v2[i] * s2;
    }
    return sub;
  }

  /**
   * Computes v1 = v1 - v2, overwriting v1
   * 
   * @param v1 vector
   * @param v2 another vector
   * @return v1 = v1 - v2
   */
  public static final double[] minusEquals(final double[] v1, final double[] v2) {
    assert (v1.length == v2.length) : ERR_VEC_DIMENSIONS;
    for(int i = 0; i < v1.length; i++) {
      v1[i] -= v2[i];
    }
    return v1;
  }

  /**
   * Computes v1 = v1 - v2 * s2, overwriting v1
   * 
   * @param v1 vector
   * @param v2 another vector
   * @param s2 scalar for v2
   * @return v1 = v1 - v2 * s2
   */
  public static final double[] minusTimesEquals(final double[] v1, final double[] v2, final double s2) {
    assert (v1.length == v2.length) : ERR_VEC_DIMENSIONS;
    for(int i = 0; i < v1.length; i++) {
      v1[i] -= v2[i] * s2;
    }
    return v1;
  }

  /**
   * Computes v1 = v1 * s1 - v2, overwriting v1
   * 
   * @param v1 vector
   * @param s1 scalar for v1
   * @param v2 another vector
   * @return v1 = v1 * s1 - v2
   */
  public static final double[] timesMinusEquals(final double[] v1, final double s1, final double[] v2) {
    assert (v1.length == v2.length) : ERR_VEC_DIMENSIONS;
    for(int i = 0; i < v1.length; i++) {
      v1[i] = v1[i] * s1 - v2[i];
    }
    return v1;
  }

  /**
   * Computes v1 = v1 * s1 - v2 * s2, overwriting v1
   * 
   * @param v1 vector
   * @param s1 scalar for v1
   * @param v2 another vector
   * @param s2 Scalar
   * @return v1 = v1 * s1 - v2 * s2
   */
  public static final double[] timesMinusTimesEquals(final double[] v1, final double s1, final double[] v2, final double s2) {
    assert (v1.length == v2.length) : ERR_VEC_DIMENSIONS;
    for(int i = 0; i < v1.length; i++) {
      v1[i] = v1[i] * s1 - v2[i] * s2;
    }
    return v1;
  }

  /**
   * Compute v1 - d
   * 
   * @param v1 original vector
   * @param d Value to subtract
   * @return v1 - d
   */
  public static final double[] minus(final double[] v1, final double d) {
    final double[] result = new double[v1.length];
    for(int i = 0; i < v1.length; i++) {
      result[i] = v1[i] - d;
    }
    return result;
  }

  /**
   * Computes v1 = v1 - d, overwriting v1
   * 
   * @param v1 original vector
   * @param d Value to subtract
   * @return v1 = v1 - d
   */
  public static final double[] minusEquals(final double[] v1, final double d) {
    for(int i = 0; i < v1.length; i++) {
      v1[i] -= d;
    }
    return v1;
  }

  /**
   * Computes v1 * s1
   * 
   * @param v1 original vector
   * @param s1 the scalar to be multiplied
   * @return v1 * s1
   */
  public static final double[] times(final double[] v1, final double s1) {
    final double[] v = new double[v1.length];
    for(int i = 0; i < v1.length; i++) {
      v[i] = v1[i] * s1;
    }
    return v;
  }

  /**
   * Computes v1 = v1 * s1, overwriting v1
   * 
   * @param v1 original vector
   * @param s scalar
   * @return v1 = v1 * s1
   */
  public static final double[] timesEquals(final double[] v1, final double s) {
    for(int i = 0; i < v1.length; i++) {
      v1[i] *= s;
    }
    return v1;
  }

  /**
   * Matrix multiplication: v1 * m2
   * 
   * @param v1 vector
   * @param m2 other matrix
   * @return Matrix product, v1 * m2
   */
  public static final double[][] times(final double[] v1, final double[][] m2) {
    assert (m2.length == 1) : ERR_MATRIX_INNERDIM;
    final int columndimension = m2[0].length;
    final double[][] re = new double[v1.length][columndimension];
    for(int j = 0; j < columndimension; j++) {
      for(int i = 0; i < v1.length; i++) {
        re[i][j] = v1[i] * m2[0][j];
      }
    }
    return re;
  }

  /**
   * Matrix multiplication: v1 * m2
   * 
   * @param v1 vector
   * @param m2 other matrix
   * @return Matrix product, v1 * m2
   */
  public static final double[][] times(final double[] v1, final Matrix m2) {
    return times(v1, m2.getArrayRef());
  }

  /**
   * Linear algebraic matrix multiplication, v1<sup>T</sup> * m2
   * 
   * @param v1 vector
   * @param m2 other matrix
   * @return Matrix product, v1<sup>T</sup> * m2
   */
  public static final double[][] transposeTimes(final double[] v1, final double[][] m2) {
    assert (m2.length == v1.length) : ERR_MATRIX_INNERDIM;
    final int columndimension = m2[0].length;
    final double[][] re = new double[1][columndimension];
    for(int j = 0; j < columndimension; j++) {
      double s = 0;
      for(int k = 0; k < v1.length; k++) {
        s += v1[k] * m2[k][j];
      }
      re[0][j] = s;
    }
    return re;
  }

  /**
   * Linear algebraic matrix multiplication, v1<sup>T</sup> * m2
   * 
   * @param v1 vector
   * @param m2 other matrix
   * @return Matrix product, v1<sup>T</sup> * m2
   */
  public static final double[][] transposeTimes(final double[] v1, final Matrix m2) {
    return transposeTimes(v1, m2.getArrayRef());
  }

  /**
   * Linear algebraic matrix multiplication, v1<sup>T</sup> * v2
   * 
   * @param v1 vector
   * @param v2 other vector
   * @return Matrix product, v1<sup>T</sup> * v2
   */
  public static final double transposeTimes(final double[] v1, final double[] v2) {
    assert (v2.length == v1.length) : ERR_MATRIX_INNERDIM;
    double s = 0;
    for(int k = 0; k < v1.length; k++) {
      s += v1[k] * v2[k];
    }
    return s;
  }

  /**
   * Linear algebraic matrix multiplication, v1 * m2^T
   * 
   * @param v1 vector
   * @param m2 other matrix
   * @return Matrix product, v1 * m2^T
   */
  public static final double[][] timesTranspose(final double[] v1, final double[][] m2) {
    assert (m2[0].length == 1) : ERR_MATRIX_INNERDIM;

    final double[][] re = new double[v1.length][m2.length];
    for(int j = 0; j < m2.length; j++) {
      for(int i = 0; i < v1.length; i++) {
        re[i][j] = v1[i] * m2[j][0];
      }
    }
    return re;
  }

  /**
   * Linear algebraic matrix multiplication, v1 * m2^T
   * 
   * @param v1 vector
   * @param m2 other matrix
   * @return Matrix product, v1 * m2^T
   */
  public static final double[][] timesTranspose(final double[] v1, final Matrix m2) {
    return timesTranspose(v1, m2.getArrayRef());
  }

  /**
   * Linear algebraic matrix multiplication, v1 * v2^T
   * 
   * @param v1 vector
   * @param v2 other vector
   * @return Matrix product, v1 * v2^T
   */
  public static final double[][] timesTranspose(final double[] v1, final double[] v2) {
    final double[][] re = new double[v1.length][v2.length];
    for(int j = 0; j < v2.length; j++) {
      for(int i = 0; i < v1.length; i++) {
        re[i][j] = v1[i] * v2[j];
      }
    }
    return re;
  }

  /**
   * Returns the scalar product (dot product) of this vector and the specified
   * vector v.
   * 
   * This is the same as transposeTimes.
   * 
   * @param v1 vector
   * @param v2 other vector
   * @return double the scalar product of vectors v1 and v2
   */
  public static final double scalarProduct(final double[] v1, final double[] v2) {
    assert (v1.length == v2.length) : ERR_VEC_DIMENSIONS;
    double scalarProduct = 0.0;
    for(int row = 0; row < v1.length; row++) {
      scalarProduct += v1[row] * v2[row];
    }
    return scalarProduct;
  }

  /**
   * Sum of the vector
   * 
   * @param v1 vector
   * @return sum of this vector
   */
  public static final double sum(final double[] v1) {
    double acc = 0.;
    for(int row = 0; row < v1.length; row++) {
      acc += v1[row];
    }
    return acc;
  }

  /**
   * Squared Euclidean length of the vector
   * 
   * @param v1 vector
   * @return squared Euclidean length of this vector
   */
  public static final double squareSum(final double[] v1) {
    double acc = 0.0;
    for(int row = 0; row < v1.length; row++) {
      final double v = v1[row];
      acc += v * v;
    }
    return acc;
  }

  /**
   * Euclidean length of the vector
   * 
   * @param v1 vector
   * @return Euclidean length of this vector
   */
  public static final double euclideanLength(final double[] v1) {
    double acc = 0.0;
    for(int row = 0; row < v1.length; row++) {
      final double v = v1[row];
      acc += v * v;
    }
    return Math.sqrt(acc);
  }

  /**
   * Normalizes v1 to the length of 1.0.
   * 
   * @param v1 vector
   * @return normalized copy of v1
   */
  public static final double[] normalize(final double[] v1) {
    double norm = 1. / euclideanLength(v1);
    double[] re = new double[v1.length];
    if(norm < Double.POSITIVE_INFINITY) {
      for(int row = 0; row < v1.length; row++) {
        re[row] = v1[row] * norm;
      }
    }
    return re;
  }

  /**
   * Normalizes v1 to the length of 1.0.
   * 
   * @param v1 vector
   * @return normalized v1
   */
  public static final double[] normalizeEquals(final double[] v1) {
    double norm = 1. / euclideanLength(v1);
    if(norm < Double.POSITIVE_INFINITY) {
      for(int row = 0; row < v1.length; row++) {
        v1[row] *= norm;
      }
    }
    return v1;
  }

  /**
   * Projects this row vector into the subspace formed by the specified matrix
   * v.
   * 
   * @param m2 the subspace matrix
   * @return the projection of p into the subspace formed by v
   */
  public static final double[] project(final double[] v1, final double[][] m2) {
    assert (v1.length == m2.length) : ERR_DIMENSIONS;
    final int columndimension = m2[0].length;

    double[] sum = new double[v1.length];
    for(int i = 0; i < columndimension; i++) {
      // TODO: optimize - copy less.
      double[] v_i = getCol(m2, i);
      plusTimesEquals(sum, v_i, scalarProduct(v1, v_i));
    }
    return sum;
  }

  /**
   * Projects this row vector into the subspace formed by the specified matrix
   * v.
   * 
   * @param m2 the subspace matrix
   * @return the projection of p into the subspace formed by v
   */
  public static final double[] project(final double[] v1, final Matrix m2) {
    return project(v1, m2.getArrayRef());
  }

  /**
   * Compute the hash code for the vector
   * 
   * @param v1 elements
   * @return hash code
   */
  public static final int hashCode(final double[] v1) {
    return Arrays.hashCode(v1);
  }

  /**
   * Compare for equality.
   * 
   * @param v1 first vector
   * @param v2 second vector
   * @return comparison result
   */
  public static final boolean equals(final double[] v1, final double[] v2) {
    return Arrays.equals(v1, v2);
  }

  /**
   * Reset the Vector to 0.
   * 
   * @param v1 vector
   */
  public static final void clear(final double[] v1) {
    Arrays.fill(v1, 0.0);
  }

  /**
   * Rotate vector by 90 degrees.
   * 
   * @param v1 first vector
   * @return modified v1, rotated by 90 degrees
   */
  public static final double[] rotate90Equals(final double[] v1) {
    assert (v1.length == 2) : "rotate90Equals is only valid for 2d vectors.";
    double temp = v1[0];
    v1[0] = v1[1];
    v1[1] = -temp;
    return v1;
  }

  // *********** MATRIX operations

  /**
   * Returns the unit matrix of the specified dimension.
   * 
   * @param dim the dimensionality of the unit matrix
   * @return the unit matrix of the specified dimension
   */
  public static final double[][] unitMatrix(final int dim) {
    final double[][] e = new double[dim][dim];
    for(int i = 0; i < dim; i++) {
      e[i][i] = 1;
    }
    return e;
  }

  /**
   * Returns the zero matrix of the specified dimension.
   * 
   * @param dim the dimensionality of the unit matrix
   * @return the zero matrix of the specified dimension
   */
  public static final double[][] zeroMatrix(final int dim) {
    final double[][] z = new double[dim][dim];
    return z;
  }

  /**
   * Generate matrix with random elements
   * 
   * @param m Number of rows.
   * @param n Number of columns.
   * @return An m-by-n matrix with uniformly distributed random elements.
   */
  public static final double[][] random(final int m, final int n) {
    final double[][] A = new double[m][n];
    for(int i = 0; i < m; i++) {
      for(int j = 0; j < n; j++) {
        A[i][j] = Math.random();
      }
    }
    return A;
  }

  /**
   * Generate identity matrix
   * 
   * @param m Number of rows.
   * @param n Number of columns.
   * @return An m-by-n matrix with ones on the diagonal and zeros elsewhere.
   */
  public static final double[][] identity(final int m, final int n) {
    final double[][] A = new double[m][n];
    for(int i = 0; i < Math.min(m, n); i++) {
      A[i][i] = 1.0;
    }
    return A;
  }

  /**
   * Returns a quadratic Matrix consisting of zeros and of the given values on
   * the diagonal.
   * 
   * @param v1 the values on the diagonal
   * @return the resulting matrix
   */
  public static final double[][] diagonal(final double[] v1) {
    final double[][] result = new double[v1.length][v1.length];
    for(int i = 0; i < v1.length; i++) {
      result[i][i] = v1[i];
    }
    return result;
  }

  /**
   * Make a deep copy of a matrix.
   * 
   * @param m1 Input matrix
   * @return a new matrix containing the same values as this matrix
   */
  public static final double[][] copy(final double[][] m1) {
    final int columndimension = m1[0].length;
    final double[][] X = new double[m1.length][columndimension];
    for(int i = 0; i < m1.length; i++) {
      System.arraycopy(m1[i], 0, X[i], 0, columndimension);
    }
    return X;
  }

  /**
   * Make a one-dimensional row packed copy of the internal array.
   * 
   * @param m1 Input matrix
   * @return Matrix elements packed in a one-dimensional array by rows.
   */
  public static final double[] rowPackedCopy(final double[][] m1) {
    final int columndimension = m1[0].length;
    double[] vals = new double[m1.length * columndimension];
    for(int i = 0; i < m1.length; i++) {
      System.arraycopy(m1[i], 0, vals, i * columndimension, columndimension);
    }
    return vals;
  }

  /**
   * Make a one-dimensional column packed copy of the internal array.
   * 
   * @param m1 Input matrix
   * @return Matrix elements packed in a one-dimensional array by columns.
   */
  public static final double[] columnPackedCopy(final double[][] m1) {
    final int columndimension = m1[0].length;
    final double[] vals = new double[m1.length * columndimension];
    for(int i = 0; i < m1.length; i++) {
      for(int j = 0; j < columndimension; j++) {
        vals[i + j * m1.length] = m1[i][j];
      }
    }
    return vals;
  }

  /**
   * Get a submatrix.
   * 
   * @param m1 Input matrix
   * @param r0 Initial row index
   * @param r1 Final row index
   * @param c0 Initial column index
   * @param c1 Final column index
   * @return m1(r0:r1,c0:c1)
   */
  public static final double[][] getMatrix(final double[][] m1, final int r0, final int r1, final int c0, final int c1) {
    final double[][] X = new double[r1 - r0 + 1][c1 - c0 + 1];
    for(int i = r0; i <= r1; i++) {
      System.arraycopy(m1[i], c0, X[i - r0], 0, c1 - c0 + 1);
    }
    return X;
  }

  /**
   * Get a submatrix.
   * 
   * @param m1 Input matrix
   * @param r Array of row indices.
   * @param c Array of column indices.
   * @return m1(r(:),c(:))
   */
  public static final double[][] getMatrix(final double[][] m1, final int[] r, final int[] c) {
    final double[][] X = new double[r.length][c.length];
    for(int i = 0; i < r.length; i++) {
      for(int j = 0; j < c.length; j++) {
        X[i][j] = m1[r[i]][c[j]];
      }
    }
    return X;
  }

  /**
   * Get a submatrix.
   * 
   * @param m1 Input matrix
   * @param r Array of row indices.
   * @param c0 Initial column index
   * @param c1 Final column index
   * @return m1(r(:),c0:c1)
   */
  public static final double[][] getMatrix(final double[][] m1, final int[] r, final int c0, final int c1) {
    final double[][] X = new double[r.length][c1 - c0 + 1];
    for(int i = 0; i < r.length; i++) {
      System.arraycopy(m1[r[i]], c0, X[i], 0, c1 - c0 + 1);
    }
    return X;
  }

  /**
   * Get a submatrix.
   * 
   * @param m1 Input matrix
   * @param r0 Initial row index
   * @param r1 Final row index
   * @param c Array of column indices.
   * @return m1(r0:r1,c(:))
   */
  public static final double[][] getMatrix(final double[][] m1, final int r0, final int r1, final int[] c) {
    final double[][] X = new double[r1 - r0 + 1][c.length];
    for(int i = r0; i <= r1; i++) {
      for(int j = 0; j < c.length; j++) {
        X[i - r0][j] = m1[i][c[j]];
      }
    }
    return X;
  }

  /**
   * Set a submatrix.
   * 
   * @param m1 Original matrix
   * @param r0 Initial row index
   * @param r1 Final row index
   * @param c0 Initial column index
   * @param c1 Final column index
   * @param m2 New values for m1(r0:r1,c0:c1)
   */
  public static final void setMatrix(final double[][] m1, final int r0, final int r1, final int c0, final int c1, final double[][] m2) {
    for(int i = r0; i <= r1; i++) {
      System.arraycopy(m2[i - r0], 0, m1[i], c0, c1 - c0 + 1);
    }
  }

  /**
   * Set a submatrix.
   * 
   * @param m1 Original matrix
   * @param r Array of row indices.
   * @param c Array of column indices.
   * @param m2 New values for m1(r(:),c(:))
   */
  public static final void setMatrix(final double[][] m1, final int[] r, final int[] c, final double[][] m2) {
    for(int i = 0; i < r.length; i++) {
      for(int j = 0; j < c.length; j++) {
        m1[r[i]][c[j]] = m2[i][j];
      }
    }
  }

  /**
   * Set a submatrix.
   * 
   * @param m1 Input matrix
   * @param r Array of row indices.
   * @param c0 Initial column index
   * @param c1 Final column index
   * @param m2 New values for m1(r(:),c0:c1)
   */
  public static final void setMatrix(final double[][] m1, final int[] r, final int c0, final int c1, final double[][] m2) {
    for(int i = 0; i < r.length; i++) {
      System.arraycopy(m2[i], 0, m1[r[i]], c0, c1 - c0 + 1);
    }
  }

  /**
   * Set a submatrix.
   * 
   * @param m1 Input matrix
   * @param r0 Initial row index
   * @param r1 Final row index
   * @param c Array of column indices.
   * @param m2 New values for m1(r0:r1,c(:))
   */
  public static final void setMatrix(final double[][] m1, final int r0, final int r1, final int[] c, final double[][] m2) {
    for(int i = r0; i <= r1; i++) {
      for(int j = 0; j < c.length; j++) {
        m1[i][c[j]] = m2[i - r0][j];
      }
    }
  }

  /**
   * Returns the <code>r</code>th row of this matrix as vector.
   * 
   * @param m1 Input matrix
   * @param r the index of the row to be returned
   * @return the <code>r</code>th row of this matrix
   */
  public static final double[] getRow(final double[][] m1, final int r) {
    return m1[r].clone();
  }

  /**
   * Sets the <code>r</code>th row of this matrix to the specified vector.
   * 
   * @param m1 Original matrix
   * @param r the index of the column to be set
   * @param row the value of the column to be set
   */
  public static final void setRow(final double[][] m1, final int r, final double[] row) {
    final int columndimension = getColumnDimensionality(m1);
    assert (row.length == columndimension) : ERR_DIMENSIONS;
    System.arraycopy(row, 0, m1[r], 0, columndimension);
  }

  /**
   * Get a column from a matrix as vector.
   * 
   * @param m1 Matrix to extract the column from
   * @param col Column number
   * @return Column
   */
  public static final double[] getCol(double[][] m1, int col) {
    double[] ret = new double[m1.length];
    for(int i = 0; i < ret.length; i++) {
      ret[i] = m1[i][col];
    }
    return ret;
  }

  /**
   * Sets the <code>c</code>th column of this matrix to the specified column.
   * 
   * @param m1 Input matrix
   * @param c the index of the column to be set
   * @param column the value of the column to be set
   */
  public static final void setCol(final double[][] m1, final int c, final double[] column) {
    assert (column.length == m1.length) : ERR_DIMENSIONS;
    for(int i = 0; i < m1.length; i++) {
      m1[i][c] = column[i];
    }
  }

  /**
   * Matrix transpose
   * 
   * @param m1 Input matrix
   * @return m1<sup>T</sup> as copy
   */
  public static final double[][] transpose(final double[][] m1) {
    final int columndimension = getColumnDimensionality(m1);
    final double[][] re = new double[columndimension][m1.length];
    for(int i = 0; i < m1.length; i++) {
      for(int j = 0; j < columndimension; j++) {
        re[j][i] = m1[i][j];
      }
    }
    return re;
  }

  /**
   * m3 = m1 + m2
   * 
   * @param m1 Input matrix
   * @param m2 another matrix
   * @return m1 + m1 in a new Matrix
   */
  public static final double[][] plus(final double[][] m1, final double[][] m2) {
    return plusEquals(copy(m1), m2);
  }

  /**
   * m3 = m1 + s2 * m2
   * 
   * @param m1 Input matrix
   * @param m2 another matrix
   * @param s2 scalar
   * @return m1 + s2 * m2 in a new Matrix
   */
  public static final double[][] plusTimes(final double[][] m1, final double[][] m2, final double s2) {
    return plusTimesEquals(copy(m1), m2, s2);
  }

  /**
   * m1 = m1 + m2, overwriting m1
   * 
   * @param m1 input matrix
   * @param m2 another matrix
   * @return m1 = m1 + m2
   */
  public static final double[][] plusEquals(final double[][] m1, final double[][] m2) {
    final int columndimension = getColumnDimensionality(m1);
    assert (getRowDimensionality(m1) == getRowDimensionality(m2) && columndimension == getColumnDimensionality(m2)) : ERR_MATRIX_DIMENSIONS;
    for(int i = 0; i < m1.length; i++) {
      for(int j = 0; j < columndimension; j++) {
        m1[i][j] += m2[i][j];
      }
    }
    return m1;
  }

  /**
   * m1 = m1 + s2 * m2, overwriting m1
   * 
   * @param m1 input matrix
   * @param m2 another matrix
   * @param s2 scalar for s2
   * @return m1 = m1 + s2 * m2, overwriting m1
   */
  public static final double[][] plusTimesEquals(final double[][] m1, final double[][] m2, final double s2) {
    final int columndimension = getColumnDimensionality(m1);
    assert (getRowDimensionality(m1) == getRowDimensionality(m2) && columndimension == getColumnDimensionality(m2)) : ERR_MATRIX_DIMENSIONS;
    for(int i = 0; i < m1.length; i++) {
      for(int j = 0; j < columndimension; j++) {
        m1[i][j] += s2 * m2[i][j];
      }
    }
    return m1;
  }

  /**
   * m3 = m1 - m2
   * 
   * @param m1 Input matrix
   * @param m2 another matrix
   * @return m1 - m2 in a new matrix
   */
  public static final double[][] minus(final double[][] m1, final double[][] m2) {
    return minusEquals(copy(m1), m2);
  }

  /**
   * m3 = m1 - s2 * m2
   * 
   * @param m1 Input matrix
   * @param m2 another matrix
   * @param s2 Scalar
   * @return m1 - s2 * m2 in a new Matrix
   */
  public static final double[][] minusTimes(final double[][] m1, final double[][] m2, final double s2) {
    return minusTimesEquals(copy(m1), m2, s2);
  }

  /**
   * m1 = m1 - m2, overwriting m1
   * 
   * @param m1 Input matrix
   * @param m2 another matrix
   * @return m1 - m2, overwriting m1
   */
  public static final double[][] minusEquals(final double[][] m1, final double[][] m2) {
    final int columndimension = getColumnDimensionality(m1);
    assert (getRowDimensionality(m1) == getRowDimensionality(m2) && columndimension == getColumnDimensionality(m2)) : ERR_MATRIX_DIMENSIONS;
    for(int i = 0; i < m1.length; i++) {
      for(int j = 0; j < columndimension; j++) {
        m1[i][j] -= m2[i][j];
      }
    }
    return m1;
  }

  /**
   * m1 = m1 - s2 * m2, overwriting m1
   * 
   * @param m1 Input matrix
   * @param m2 another matrix
   * @param s2 Scalar
   * @return m1 = m1 - s2 * m2, overwriting m1
   */
  public static final double[][] minusTimesEquals(final double[][] m1, final double[][] m2, final double s2) {
    assert (getRowDimensionality(m1) == getRowDimensionality(m2) && getColumnDimensionality(m1) == getColumnDimensionality(m2)) : ERR_MATRIX_DIMENSIONS;
    for(int i = 0; i < m1.length; i++) {
      final double[] row1 = m1[i];
      final double[] row2 = m2[i];
      for(int j = 0; j < row1.length; j++) {
        row1[j] -= s2 * row2[j];
      }
    }
    return m1;
  }

  /**
   * Multiply a matrix by a scalar, m3 = s1*m1
   * 
   * @param m1 Input matrix
   * @param s1 scalar
   * @return s1*m1, in a new matrix
   */
  public static final double[][] times(final double[][] m1, final double s1) {
    return timesEquals(copy(m1), s1);
  }

  /**
   * Multiply a matrix by a scalar in place, m1 = s1 * m1
   * 
   * @param m1 Input matrix
   * @param s1 scalar
   * @return m1 = s1 * m1, overwriting m1
   */
  public static final double[][] timesEquals(final double[][] m1, final double s1) {
    for(int i = 0; i < m1.length; i++) {
      final double[] row = m1[i];
      for(int j = 0; j < row.length; j++) {
        row[j] *= s1;
      }
    }
    return m1;
  }

  /**
   * Linear algebraic matrix multiplication, m1 * m2
   * 
   * @param m1 Input matrix
   * @param m2 another matrix
   * @return Matrix product, m1 * m2
   */
  public static final double[][] times(final double[][] m1, final double[][] m2) {
    final int columndimension = getColumnDimensionality(m1);
    final int bcolumndimension = getColumnDimensionality(m2);
    // Optimized implementation, exploiting the storage layout
    assert (m2.length == columndimension) : ERR_MATRIX_INNERDIM;
    final double[][] r2 = new double[m1.length][bcolumndimension];
    // Optimized ala Jama. jik order.
    final double[] Bcolj = new double[columndimension];
    for(int j = 0; j < bcolumndimension; j++) {
      // Make a linear copy of column j from B
      // TODO: use column getter from B?
      for(int k = 0; k < columndimension; k++) {
        Bcolj[k] = m2[k][j];
      }
      // multiply it with each row from A
      for(int i = 0; i < m1.length; i++) {
        final double[] Arowi = m1[i];
        double s = 0;
        for(int k = 0; k < columndimension; k++) {
          s += Arowi[k] * Bcolj[k];
        }
        r2[i][j] = s;
      }
    }
    return r2;
  }

  /**
   * Linear algebraic matrix multiplication, m1 * v2
   * 
   * @param m1 Input matrix
   * @param v2 a vector
   * @return Matrix product, m1 * v2
   */
  public static final double[] times(final double[][] m1, final double[] v2) {
    assert (v2.length == getColumnDimensionality(m1)) : ERR_MATRIX_INNERDIM;
    final double[] re = new double[m1.length];
    // multiply it with each row from A
    for(int i = 0; i < m1.length; i++) {
      final double[] Arowi = m1[i];
      double s = 0;
      for(int k = 0; k < Arowi.length; k++) {
        s += Arowi[k] * v2[k];
      }
      re[i] = s;
    }
    return re;
  }

  /**
   * Linear algebraic matrix multiplication, m1 * v2
   * 
   * @param m1 Input matrix
   * @param v2 a vector
   * @return Matrix product, m1 * v2
   */
  public static final double[] times(final Matrix m1, final double[] v2) {
    return times(m1.getArrayRef(), v2);
  }

  /**
   * Linear algebraic matrix multiplication, m1<sup>T</sup> * v2
   * 
   * @param m1 Input matrix
   * @param v2 another matrix
   * @return Matrix product, m1<sup>T</sup> * v2
   */
  public static final double[] transposeTimes(final double[][] m1, final double[] v2) {
    final int columndimension = getColumnDimensionality(m1);
    assert (v2.length == m1.length) : ERR_MATRIX_INNERDIM;
    final double[] re = new double[columndimension];
    // multiply it with each row from A
    for(int i = 0; i < columndimension; i++) {
      double s = 0;
      for(int k = 0; k < m1.length; k++) {
        s += m1[k][i] * v2[k];
      }
      re[i] = s;
    }
    return re;
  }

  /**
   * Linear algebraic matrix multiplication, m1<sup>T</sup> * v2
   * 
   * @param m1 Input matrix
   * @param v2 another matrix
   * @return Matrix product, m1<sup>T</sup> * v2
   */
  public static final double[] transposeTimes(final Matrix m1, final double[] v2) {
    return transposeTimes(m1.getArrayRef(), v2);
  }

  /**
   * Linear algebraic matrix multiplication, m1<sup>T</sup> * m2
   * 
   * @param m1 Input matrix
   * @param m2 another matrix
   * @return Matrix product, m1<sup>T</sup> * m2
   */
  public static final double[][] transposeTimes(final double[][] m1, final double[][] m2) {
    final int coldim1 = getColumnDimensionality(m1);
    final int coldim2 = getColumnDimensionality(m2);
    assert (m2.length == m1.length) : ERR_MATRIX_INNERDIM;
    final double[][] re = new double[coldim1][coldim2];
    final double[] Bcolj = new double[m1.length];
    for(int j = 0; j < coldim2; j++) {
      // Make a linear copy of column j from B
      for(int k = 0; k < m1.length; k++) {
        Bcolj[k] = m2[k][j];
      }
      // multiply it with each row from A
      for(int i = 0; i < coldim1; i++) {
        double s = 0;
        for(int k = 0; k < m1.length; k++) {
          s += m1[k][i] * Bcolj[k];
        }
        re[i][j] = s;
      }
    }
    return re;
  }

  /**
   * Linear algebraic matrix multiplication, a<sup>T</sup> * B * c
   * 
   * @param a vector on the left
   * @param B matrix
   * @param c vector on the right
   * @return Matrix product, a<sup>T</sup> * B * c
   */
  public static double transposeTimesTimes(final double[] a, final double[][] B, final double[] c) {
    assert (B.length == a.length) : ERR_MATRIX_INNERDIM;
    double sum = 0.0;
    for(int j = 0; j < B[0].length; j++) {
      // multiply it with each row from A
      double s = 0;
      for(int k = 0; k < a.length; k++) {
        s += a[k] * B[k][j];
      }
      sum += s * c[j];
    }
    return sum;
  }

  /**
   * Linear algebraic matrix multiplication, a<sup>T</sup> * B * c
   * 
   * @param a vector on the left
   * @param B matrix
   * @param c vector on the right
   * @return Matrix product, a<sup>T</sup> * B * c
   */
  public static double transposeTimesTimes(final double[] a, final Matrix B, final double[] c) {
    return transposeTimesTimes(a, B.getArrayRef(), c);
  }

  /**
   * Linear algebraic matrix multiplication, m1 * m2^T
   * 
   * @param m1 Input matrix
   * @param m2 another matrix
   * @return Matrix product, m1 * m2^T
   */
  public static final double[][] timesTranspose(final double[][] m1, final double[][] m2) {
    assert (getColumnDimensionality(m2) == getColumnDimensionality(m1)) : ERR_MATRIX_INNERDIM;
    final double[][] re = new double[m1.length][m2.length];
    for(int j = 0; j < re.length; j++) {
      final double[] Browj = m2[j];
      // multiply it with each row from A
      for(int i = 0; i < m1.length; i++) {
        final double[] Arowi = m1[i];
        double s = 0;
        for(int k = 0; k < Browj.length; k++) {
          s += Arowi[k] * Browj[k];
        }
        re[i][j] = s;
      }
    }
    return re;
  }

  /**
   * Linear algebraic matrix multiplication, m1^T * m2^T. Computed as (m2*m1)^T
   * 
   * @param m1 Input matrix
   * @param m2 another matrix
   * @return Matrix product, m1^T * m2^T
   */
  public static final double[][] transposeTimesTranspose(final double[][] m1, final double[][] m2) {
    // Optimized implementation, exploiting the storage layout
    assert (m1.length == getColumnDimensionality(m2)) : ERR_MATRIX_INNERDIM;
    final double[][] re = new double[getColumnDimensionality(m1)][m2.length];
    // Optimized ala Jama. jik order.
    final double[] Acolj = new double[m1.length];
    for(int j = 0; j < re.length; j++) {
      // Make a linear copy of column j from B
      for(int k = 0; k < m1.length; k++) {
        Acolj[k] = m1[k][j];
      }
      final double[] Xrow = re[j];
      // multiply it with each row from A
      for(int i = 0; i < m2.length; i++) {
        final double[] Browi = m2[i];
        double s = 0;
        for(int k = 0; k < m1.length; k++) {
          s += Browi[k] * Acolj[k];
        }
        Xrow[i] = s;
      }
    }
    return re;
  }

  /**
   * Linear algebraic matrix multiplication, (a-c)<sup>T</sup> * B * (a-c)
   * 
   * @param B matrix
   * @param a First vector
   * @param c Center vector
   * @return Matrix product, (a-c)<sup>T</sup> * B * (a-c)
   */
  public static double mahalanobisDistance(final double[][] B, final double[] a, final double[] c) {
    assert (B.length == a.length && a.length == c.length) : ERR_MATRIX_INNERDIM;
    double sum = 0.0;
    for(int j = 0; j < B[0].length; j++) {
      // multiply it with each row from A
      double s = 0;
      for(int k = 0; k < a.length; k++) {
        s += (a[k] - c[k]) * B[k][j];
      }
      sum += s * (a[j] - c[j]);
    }
    return sum;
  }

  /**
   * Linear algebraic matrix multiplication, (a-c)<sup>T</sup> * B * (a-c)
   * 
   * @param B matrix
   * @param a First vector
   * @param c Center vector
   * @return Matrix product, (a-c)<sup>T</sup> * B * (a-c)
   */
  public static double mahalanobisDistance(final Matrix B, final double[] a, final double[] c) {
    return mahalanobisDistance(B.getArrayRef(), a, c);
  }

  /**
   * getDiagonal returns array of diagonal-elements.
   * 
   * @param m1 Input matrix
   * @return values on the diagonal of the Matrix
   */
  public static final double[] getDiagonal(final double[][] m1) {
    final int dim = Math.min(getColumnDimensionality(m1), m1.length);
    final double[] diagonal = new double[dim];
    for(int i = 0; i < dim; i++) {
      diagonal[i] = m1[i][i];
    }
    return diagonal;
  }

  /**
   * Normalizes the columns of this matrix to length of 1.0.
   * 
   * @param m1 Input matrix
   */
  public static final void normalizeColumns(final double[][] m1) {
    final int columndimension = getColumnDimensionality(m1);
    for(int col = 0; col < columndimension; col++) {
      double norm = 0.0;
      for(int row = 0; row < m1.length; row++) {
        norm = norm + (m1[row][col] * m1[row][col]);
      }
      norm = Math.sqrt(norm);
      if(norm != 0) {
        for(int row = 0; row < m1.length; row++) {
          m1[row][col] /= norm;
        }
      }
      else {
        // TODO: else: throw an exception?
      }
    }
  }

  /**
   * Returns a matrix which consists of this matrix and the specified columns.
   * 
   * @param m1 Input matrix
   * @param m2 the columns to be appended
   * @return the new matrix with the appended columns
   */
  public static final double[][] appendColumns(final double[][] m1, final double[][] m2) {
    final int columndimension = getColumnDimensionality(m1);
    final int ccolumndimension = getColumnDimensionality(m2);
    assert (m1.length == m2.length) : "m.getRowDimension() != column.getRowDimension()";

    final int rcolumndimension = columndimension + ccolumndimension;
    final double[][] result = new double[m1.length][rcolumndimension];
    for(int i = 0; i < rcolumndimension; i++) {
      // FIXME: optimize - excess copying!
      if(i < columndimension) {
        setCol(result, i, getCol(m1, i));
      }
      else {
        setCol(result, i, getCol(m2, i - columndimension));
      }
    }
    return result;
  }

  /**
   * Returns an orthonormalization of this matrix.
   * 
   * @param m1 Input matrix
   * @return the orthonormalized matrix
   */
  public static final double[][] orthonormalize(final double[][] m1) {
    final int columndimension = getColumnDimensionality(m1);
    double[][] v = copy(m1);

    // FIXME: optimize - excess copying!
    for(int i = 1; i < columndimension; i++) {
      final double[] u_i = getCol(m1, i);
      final double[] sum = new double[m1.length];
      for(int j = 0; j < i; j++) {
        final double[] v_j = getCol(v, j);
        double scalar = scalarProduct(u_i, v_j) / scalarProduct(v_j, v_j);
        plusEquals(sum, times(v_j, scalar));
      }
      final double[] v_i = minus(u_i, sum);
      setCol(v, i, v_i);
    }

    normalizeColumns(v);
    return v;
  }

  /**
   * Compute hash code
   * 
   * @param m1 Input matrix
   * @return Hash code
   */
  public static final int hashCode(final double[][] m1) {
    return Arrays.hashCode(m1);
  }

  /**
   * Test for equality
   * 
   * @param m1 Input matrix
   * @param m2 Other matrix
   * @return Equality
   */
  public static final boolean equals(final double[][] m1, final double[][] m2) {
    return Arrays.equals(m1, m2);
  }

  /**
   * Compare two matrices with a delta parameter to take numerical errors into
   * account.
   * 
   * @param m1 Input matrix
   * @param m2 other matrix to compare with
   * @param maxdelta maximum delta allowed
   * @return true if delta smaller than maximum
   */
  public static final boolean almostEquals(final double[][] m1, final double[][] m2, final double maxdelta) {
    if(m1 == m2) {
      return true;
    }
    if(m2 == null) {
      return false;
    }
    if(m1.getClass() != m2.getClass()) {
      return false;
    }
    if(m1.length != m2.length) {
      return false;
    }
    final int columndimension = getColumnDimensionality(m1);
    if(columndimension != getColumnDimensionality(m2)) {
      return false;
    }
    for(int i = 0; i < m1.length; i++) {
      for(int j = 0; j < columndimension; j++) {
        if(Math.abs(m1[i][j] - m2[i][j]) > maxdelta) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Compare two matrices with a delta parameter to take numerical errors into
   * account.
   * 
   * @param m1 Input matrix
   * @param m2 other matrix to compare with
   * @return almost equals with delta {@link #DELTA}
   */
  public static final boolean almostEquals(final double[][] m1, final double[][] m2) {
    return almostEquals(m1, m2, DELTA);
  }

  /**
   * Returns the dimensionality of the rows of this matrix.
   * 
   * @param m1 Input matrix
   * @return the number of rows.
   */
  public static final int getRowDimensionality(final double[][] m1) {
    return m1.length;
  }

  /**
   * Returns the dimensionality of the columns of this matrix.
   * 
   * @param m1 Input matrix
   * @return the number of columns.
   */
  public static final int getColumnDimensionality(final double[][] m1) {
    return m1[0].length;
  }

  /**
   * Cross product for 3d vectors, i.e. <code>vo = v1 x v2</code>
   * 
   * @param vo Output vector
   * @param v1 First input vector
   * @param v2 Second input vector
   */
  public static void cross3D(double[] vo, double[] v1, double[] v2) {
    vo[0] = (v1[1] * v2[2]) - (v1[2] * v2[1]);
    vo[1] = (v1[2] * v2[0]) - (v1[0] * v2[2]);
    vo[2] = (v1[0] * v2[1]) - (v1[1] * v2[0]);
  }

  /**
   * Compute the angle between two vectors.
   *
   * @param v1 first vector
   * @param v2 second vector
   * @return Angle
   */
  public static double angle(double[] v1, double[] v2) {
    final int mindim = (v1.length >= v2.length) ? v1.length : v2.length;
    // Essentially, we want to compute this:
    // v1.transposeTimes(v2) / (v1.euclideanLength() * v2.euclideanLength());
    // We can just compute all three in parallel.
    double s = 0, e1 = 0, e2 = 0;
    for(int k = 0; k < mindim; k++) {
      final double r1 = v1[k];
      final double r2 = v2[k];
      s += r1 * r2;
      e1 += r1 * r1;
      e2 += r2 * r2;
    }
    for(int k = mindim; k < v1.length; k++) {
      final double r1 = v1[k];
      e1 += r1 * r1;
    }
    for(int k = mindim; k < v2.length; k++) {
      final double r2 = v2[k];
      e2 += r2 * r2;
    }
    double a = Math.sqrt((s / e1) * (s / e2));
    return (a < 1.) ? a : 1.;
  }

  /**
   * Compute the angle between two vectors.
   *
   * @param v1 first vector
   * @param v2 second vector
   * @param o Origin
   * @return Angle
   */
  public static double angle(double[] v1, double[] v2, double[] o) {
    final int mindim = (v1.length >= v2.length) ? v1.length : v2.length;
    // Essentially, we want to compute this:
    // v1' = v1 - o, v2' = v2 - o
    // v1'.transposeTimes(v2') / (v1'.euclideanLength()*v2'.euclideanLength());
    // We can just compute all three in parallel.
    double s = 0, e1 = 0, e2 = 0;
    for(int k = 0; k < mindim; k++) {
      final double ok = (k < o.length) ? o[k] : 0;
      final double r1 = v1[k] - ok;
      final double r2 = v2[k] - ok;
      s += r1 * r2;
      e1 += r1 * r1;
      e2 += r2 * r2;
    }
    for(int k = mindim; k < v1.length; k++) {
      final double ok = (k < o.length) ? o[k] : 0;
      final double r1 = v1[k] - ok;
      e1 += r1 * r1;
    }
    for(int k = mindim; k < v2.length; k++) {
      final double ok = (k < o.length) ? o[k] : 0;
      final double r2 = v2[k] - ok;
      e2 += r2 * r2;
    }
    double a = Math.sqrt((s / e1) * (s / e2));
    return (a < 1.) ? a : 1.;
  }
}
