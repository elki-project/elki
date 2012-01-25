package experimentalcode.erich;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
 * Class providing basic vector mathematics.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 */
public final class VMath {
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
  public final static double[] randomNormalizedVector(final int dimensionality) {
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
  public final static double[] unitVector(final int dimensionality, final int i) {
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
  public final static double[] copy(final double[] v) {
    return Arrays.copyOf(v, v.length);
  }

  /**
   * Transpose vector to a matrix.
   * 
   * @param v Vector
   * @return Matrix
   */
  public final static double[][] transpose(final double[] v) {
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
  public final static double[] plus(final double[] v1, final double[] v2) {
    assert (v1.length == v2.length) : "Vector dimensions must agree.";
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
  public final static double[] plusTimes(final double[] v1, final double[] v2, final double s2) {
    assert (v1.length == v2.length) : "Vector dimensions must agree.";
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
  public final static double[] timesPlus(final double[] v1, final double s1, final double[] v2) {
    assert (v1.length == v2.length) : "Vector dimensions must agree.";
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
  public final static double[] timesPlusTimes(final double[] v1, final double s1, final double[] v2, final double s2) {
    assert (v1.length == v2.length) : "Vector dimensions must agree.";
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
  public final static double[] plusEquals(final double[] v1, final double[] v2) {
    assert (v1.length == v2.length) : "Vector dimensions must agree.";
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
   * @param s2 scalar vor v2
   * @return v1 = v1 + v2 * s2
   */
  public final static double[] plusTimesEquals(final double[] v1, final double[] v2, final double s2) {
    assert (v1.length == v2.length) : "Vector dimensions must agree.";
    for(int i = 0; i < v1.length; i++) {
      v1[i] += s2 * v2[i];
    }
    return v1;
  }

  /**
   * Computes v1 = v1 * s1 + v2, overwriting v1
   * 
   * @param v1 first vector
   * @param s1 scalar for v1
   * @param v2 another vector
   * @return v1 = v1 * s1 + v2
   */
  public final static double[] timesPlusEquals(final double[] v1, final double s1, final double[] v2) {
    assert (v1.length == v2.length) : "Vector dimensions must agree.";
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
  public final static double[] timesPlusTimesEquals(final double[] v1, final double s1, final double[] v2, final double s2) {
    assert (v1.length == v2.length) : "Vector dimensions must agree.";
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
  public final static double[] plus(final double[] v1, final double d) {
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
  public final static double[] plusEquals(final double[] v1, final double d) {
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
  public final static double[] minus(final double[] v1, final double[] v2) {
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
  public final static double[] minusTimes(final double[] v1, final double[] v2, final double s2) {
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
  public final static double[] timesMinus(final double[] v1, final double s1, final double[] v2) {
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
  public final static double[] timesMinusTimes(final double[] v1, final double s1, final double[] v2, final double s2) {
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
  public final static double[] minusEquals(final double[] v1, final double[] v2) {
    assert (v1.length == v2.length) : "Vector dimensions must agree.";
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
  public final static double[] minusTimesEquals(final double[] v1, final double[] v2, final double s2) {
    assert (v1.length == v2.length) : "Vector dimensions must agree.";
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
  public final static double[] timesMinusEquals(final double[] v1, final double s1, final double[] v2) {
    assert (v1.length == v2.length) : "Vector dimensions must agree.";
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
  public final static double[] timesMinusTimesEquals(final double[] v1, final double s1, final double[] v2, final double s2) {
    assert (v1.length == v2.length) : "Vector dimensions must agree.";
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
  public final static double[] minus(final double[] v1, final double d) {
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
  public final static double[] minusEquals(final double[] v1, final double d) {
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
  public final static double[] times(final double[] v1, final double s1) {
    final double[] v = new double[v1.length];
    for(int i = 0; i < v1.length; i++) {
      v[i] = v1[i] * s1;
    }
    return v;
  }

  /**
   * Computes v1 = v1 * s1, overwritings v1
   * 
   * @param v1 original vector
   * @param s scalar
   * @return v1 = v1 * s1
   */
  public final static double[] timesEquals(final double[] v1, final double s) {
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
  public final static double[][] times(final double[] v1, final double[][] m2) {
    assert (m2.length == 1) : "Matrix inner dimensions must agree.";
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
   * Linear algebraic matrix multiplication, v1<sup>T</sup> * m2
   * 
   * @param v1 vector
   * @param m2 other matrix
   * @return Matrix product, v1<sup>T</sup> * m2
   */
  public final static double[][] transposeTimes(final double[] v1, final double[][] m2) {
    assert (m2.length == v1.length) : "Matrix inner dimensions must agree.";
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
   * Linear algebraic matrix multiplication, v1<sup>T</sup> * v2
   * 
   * @param v1 vector
   * @param v2 other vector
   * @return Matrix product, v1<sup>T</sup> * v2
   */
  public final static double transposeTimes(final double[] v1, final double[] v2) {
    assert (v2.length == v1.length) : "Matrix inner dimensions must agree.";
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
   * @param v2 other matrix
   * @return Matrix product, v1 * m2^T
   */
  public final static double[][] timesTranspose(final double[] v1, final double[][] m2) {
    assert (m2[0].length == 1) : "Matrix inner dimensions must agree.";

    final double[][] re = new double[v1.length][m2.length];
    for(int j = 0; j < m2.length; j++) {
      for(int i = 0; i < v1.length; i++) {
        re[i][j] = v1[i] * m2[j][0];
      }
    }
    return re;
  }

  /**
   * Linear algebraic matrix multiplication, v1 * v2^T
   * 
   * @param v1 vector
   * @param v2 other vector
   * @return Matrix product, v1 * v2^T
   */
  public final static double[][] timesTranspose(final double[] v1, final double[] v2) {
    final double[][] re = new double[v1.length][v2.length];
    for(int j = 0; j < v2.length; j++) {
      for(int i = 0; i < v1.length; i++) {
        re[i][j] = v1[i] * v2[j];
      }
    }
    return re;
  }

  /**
   * Returns the scalar product of this vector and the specified vector v.
   * 
   * This is the same as transposeTimes.
   * 
   * @param v1 vector
   * @param v2 other vector
   * @return double the scalar product of vectors v1 and v2
   */
  public final static double scalarProduct(final double[] v1, final double[] v2) {
    assert (v1.length == v2.length) : "Vector dimensions must agree.";
    double scalarProduct = 0.0;
    for(int row = 0; row < v1.length; row++) {
      scalarProduct += v1[row] * v2[row];
    }
    return scalarProduct;
  }

  /**
   * Euclidean length of the vector
   * 
   * @param v1 vector
   * @return euclidean length of this vector
   */
  public final static double euclideanLength(final double[] v1) {
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
   * @param normalized copy of v1
   */
  public final static double[] normalize(final double[] v1) {
    double norm = euclideanLength(v1);
    double[] re = new double[v1.length];
    if(norm != 0) {
      for(int row = 0; row < v1.length; row++) {
        re[row] = v1[row] / norm;
      }
    }
    return re;
  }

  /**
   * Normalizes v1 to the length of 1.0.
   * 
   * @param v1 vector
   * @param normalized v1
   */
  public final static double[] normalizeEquals(final double[] v1) {
    double norm = euclideanLength(v1);
    if(norm != 0) {
      for(int row = 0; row < v1.length; row++) {
        v1[row] /= norm;
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
  public final static double[] project(final double[] v1, final double[][] m2) {
    assert (v1.length == m2.length) : "v1 and m2 differ in dimensionality!";
    final int columndimension = m2[0].length;

    double[] sum = new double[v1.length];
    for(int i = 0; i < columndimension; i++) {
      // TODO: optimize - copy less.
      double[] v_i = getColumnVector(m2, i);
      plusEquals(sum, times(v_i, scalarProduct(v1, v_i)));
    }
    return sum;
  }

  /**
   * Get a column from a matrix as vector.
   * 
   * @param m1 Matrix to extract the column from
   * @param col Column number
   * @return Column
   */
  public final static double[] getColumnVector(double[][] m1, int col) {
    double[] ret = new double[m1.length];
    for(int i = 0; i < ret.length; i++) {
      ret[i] = m1[i][col];
    }
    return ret;
  }

  /**
   * Compute the hash code for the vector
   * 
   * @param v1 elements
   * @return hash code
   */
  public final static int hashCode(final double[] v1) {
    return Arrays.hashCode(v1);
  }

  /**
   * Compare for equality.
   * 
   * @param v1 first vector
   * @param v2 second vector
   * @return comparison result
   */
  public final static boolean equals(final double[] v1, final double[] v2) {
    return Arrays.equals(v1, v2);
  }

  /**
   * Reset the Vector to 0.
   * 
   * @param v1 vector
   */
  public final static void clear(final double[] v1) {
    Arrays.fill(v1, 0.0);
  }

  /**
   * Rotate vector by 90 degrees.
   * 
   * @param v1 first vector
   * @return modified v1, rotated by 90 degrees
   */
  public final static double[] rotate90Equals(final double[] v1) {
    assert (v1.length == 2) : "rotate90Equals is only valid for 2d vectors.";
    double temp = v1[0];
    v1[0] = v1[1];
    v1[1] = -temp;
    return v1;
  }
}