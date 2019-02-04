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
package de.lmu.ifi.dbs.elki.math.linearalgebra;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import net.jafama.FastMath;

/**
 * Class providing basic vector mathematics, for low-level vectors stored as
 * {@code double[]}. While this is less nice syntactically, it reduces memory
 * usage and VM overhead.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @opt nodefillcolor LemonChiffon
 */
@Title("Vector and Matrix Math Library")
public final class VMath {
  /**
   * A small number to handle numbers near 0 as 0.
   */
  private static final double DELTA = 1E-5;

  /**
   * Error message when vector dimensionalities do not agree.
   */
  protected static final String ERR_VEC_DIMENSIONS = "Vector dimensions do not agree.";

  /**
   * Error message when matrix dimensionalities do not agree.
   */
  protected static final String ERR_MATRIX_DIMENSIONS = "Matrix dimensions do not agree.";

  /**
   * Error message when matrix dimensionalities do not agree.
   */
  protected static final String ERR_MATRIX_INNERDIM = "Matrix inner dimensions do not agree.";

  /**
   * Error message when dimensionalities do not agree.
   */
  protected static final String ERR_DIMENSIONS = "Dimensionalities do not agree.";

  /**
   * Error message when min &gt; max is given as a range.
   */
  protected static final String ERR_INVALID_RANGE = "Invalid range parameters.";

  /**
   * Error when a non-square matrix is used with determinant.
   */
  protected static final String ERR_MATRIX_NONSQUARE = "Matrix must be square.";

  /**
   * Error with a singular matrix.
   */
  protected static final String ERR_SINGULAR = "Matrix is singular.";

  /**
   * When a symmetric positive definite matrix is required.
   */
  protected static final String ERR_MATRIX_NOT_SPD = "Matrix is not symmetric positive definite.";

  /**
   * When a matrix is rank deficient.
   */
  protected static final String ERR_MATRIX_RANK_DEFICIENT = "Matrix is rank deficient.";

  /**
   * Fake constructor. Static class.
   */
  private VMath() {
    // Cannot be instantiated
  }

  /**
   * Returns the ith unit vector of the specified dimensionality.
   *
   * @param dimensionality the dimensionality of the vector
   * @param i the index
   * @return the ith unit vector of the specified dimensionality
   */
  public static double[] unitVector(final int dimensionality, final int i) {
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
  public static double[] copy(final double[] v) {
    return v.clone();
  }

  /**
   * Transpose vector to a matrix <em>without copying</em>.
   *
   * @param v Vector
   * @return Matrix
   */
  public static double[][] transpose(final double[] v) {
    return new double[][] { v };
  }

  /**
   * Computes component-wise v1 + v2 for vectors.
   *
   * @param v1 first vector
   * @param v2 second vector
   * @return the sum v1 + v2
   */
  public static double[] plus(final double[] v1, final double[] v2) {
    assert v1.length == v2.length : ERR_VEC_DIMENSIONS;
    final double[] result = new double[v1.length];
    for(int i = 0; i < result.length; i++) {
      result[i] = v1[i] + v2[i];
    }
    return result;
  }

  /**
   * Computes component-wise v1 + v2 * s2.
   *
   * @param v1 first vector
   * @param v2 second vector
   * @param s2 the scalar
   * @return the result of v1 + v2 * s2
   */
  public static double[] plusTimes(final double[] v1, final double[] v2, final double s2) {
    assert v1.length == v2.length : ERR_VEC_DIMENSIONS;
    final double[] result = new double[v1.length];
    for(int i = 0; i < result.length; i++) {
      result[i] = v1[i] + v2[i] * s2;
    }
    return result;
  }

  /**
   * Computes component-wise v1 * s1 + v2.
   *
   * @param v1 first vector
   * @param s1 the scalar for v1
   * @param v2 second vector
   * @return the result of v1 * s1 + v2
   */
  public static double[] timesPlus(final double[] v1, final double s1, final double[] v2) {
    assert v1.length == v2.length : ERR_VEC_DIMENSIONS;
    final double[] result = new double[v1.length];
    for(int i = 0; i < result.length; i++) {
      result[i] = v1[i] * s1 + v2[i];
    }
    return result;
  }

  /**
   * Computes component-wise v1 * s1 + v2 * s2.
   *
   * @param v1 first vector
   * @param s1 the scalar for v1
   * @param v2 second vector
   * @param s2 the scalar for v2
   * @return the result of v1 * s1 + v2 * s2
   */
  public static double[] timesPlusTimes(final double[] v1, final double s1, final double[] v2, final double s2) {
    assert v1.length == v2.length : ERR_VEC_DIMENSIONS;
    final double[] result = new double[v1.length];
    for(int i = 0; i < result.length; i++) {
      result[i] = v1[i] * s1 + v2[i] * s2;
    }
    return result;
  }

  /**
   * Computes component-wise v1 = v1 + v2,
   * overwriting the vector v1.
   *
   * @param v1 first vector (overwritten)
   * @param v2 second vector
   * @return v1 = v1 + v2
   */
  public static double[] plusEquals(final double[] v1, final double[] v2) {
    assert v1.length == v2.length : ERR_VEC_DIMENSIONS;
    for(int i = 0; i < v1.length; i++) {
      v1[i] += v2[i];
    }
    return v1;
  }

  /**
   * Computes component-wise v1 = v1 + v2 * s2,
   * overwriting the vector v1.
   *
   * @param v1 first vector (overwritten)
   * @param v2 another vector
   * @param s2 scalar factor for v2
   * @return v1 = v1 + v2 * s2
   */
  public static double[] plusTimesEquals(final double[] v1, final double[] v2, final double s2) {
    assert v1.length == v2.length : ERR_VEC_DIMENSIONS;
    for(int i = 0; i < v1.length; i++) {
      v1[i] += s2 * v2[i];
    }
    return v1;
  }

  /**
   * Computes component-wise v1 = v1 * s1 + v2,
   * overwriting the vector v1.
   *
   * @param v1 first vector (overwritten)
   * @param s1 scalar factor for v1
   * @param v2 another vector
   * @return v1 = v1 * s1 + v2
   */
  public static double[] timesPlusEquals(final double[] v1, final double s1, final double[] v2) {
    assert v1.length == v2.length : ERR_VEC_DIMENSIONS;
    for(int i = 0; i < v1.length; i++) {
      v1[i] = v1[i] * s1 + v2[i];
    }
    return v1;
  }

  /**
   * Computes component-wise v1 = v1 * s1 + v2 * s2,
   * overwriting the vector v1.
   *
   * @param v1 first vector (overwritten)
   * @param s1 scalar for v1
   * @param v2 another vector
   * @param s2 scalar for v2
   * @return v1 = v1 * s1 + v2 * s2
   */
  public static double[] timesPlusTimesEquals(final double[] v1, final double s1, final double[] v2, final double s2) {
    assert v1.length == v2.length : ERR_VEC_DIMENSIONS;
    for(int i = 0; i < v1.length; i++) {
      v1[i] = v1[i] * s1 + v2[i] * s2;
    }
    return v1;
  }

  /**
   * Computes component-wise v1 + s1.
   *
   * @param v1 vector to add to
   * @param s1 constant value to add
   * @return v1 + s1
   */
  public static double[] plus(final double[] v1, final double s1) {
    final double[] result = new double[v1.length];
    for(int i = 0; i < result.length; i++) {
      result[i] = v1[i] + s1;
    }
    return result;
  }

  /**
   * Computes component-wise v1 = v1 + s1,
   * overwriting the vector v1.
   *
   * @param v1 vector to add to (overwritten)
   * @param s1 constant value to add
   * @return Modified vector
   */
  public static double[] plusEquals(final double[] v1, final double s1) {
    for(int i = 0; i < v1.length; i++) {
      v1[i] += s1;
    }
    return v1;
  }

  /**
   * Computes component-wise v1 - v2.
   *
   * @param v1 first vector
   * @param v2 the vector to be subtracted from this vector
   * @return v1 - v2
   */
  public static double[] minus(final double[] v1, final double[] v2) {
    assert v1.length == v2.length : ERR_VEC_DIMENSIONS;
    final double[] sub = new double[v1.length];
    for(int i = 0; i < v1.length; i++) {
      sub[i] = v1[i] - v2[i];
    }
    return sub;
  }

  /**
   * Computes component-wise v1 - v2 * s2.
   *
   * @param v1 first vector
   * @param v2 the vector to be subtracted from this vector
   * @param s2 the scaling factor for v2
   * @return v1 - v2 * s2
   */
  public static double[] minusTimes(final double[] v1, final double[] v2, final double s2) {
    assert v1.length == v2.length : ERR_VEC_DIMENSIONS;
    final double[] sub = new double[v1.length];
    for(int i = 0; i < v1.length; i++) {
      sub[i] = v1[i] - v2[i] * s2;
    }
    return sub;
  }

  /**
   * Computes component-wise v1 * s1 - v2.
   *
   * @param v1 first vector
   * @param s1 the scaling factor for v1
   * @param v2 the vector to be subtracted from this vector
   * @return v1 * s1 - v2
   */
  public static double[] timesMinus(final double[] v1, final double s1, final double[] v2) {
    assert v1.length == v2.length : ERR_VEC_DIMENSIONS;
    final double[] sub = new double[v1.length];
    for(int i = 0; i < v1.length; i++) {
      sub[i] = v1[i] * s1 - v2[i];
    }
    return sub;
  }

  /**
   * Computes component-wise v1 * s1 - v2 * s2.
   *
   * @param v1 first vector
   * @param s1 the scaling factor for v1
   * @param v2 the vector to be subtracted from this vector
   * @param s2 the scaling factor for v2
   * @return v1 * s1 - v2 * s2
   */
  public static double[] timesMinusTimes(final double[] v1, final double s1, final double[] v2, final double s2) {
    assert v1.length == v2.length : ERR_VEC_DIMENSIONS;
    final double[] sub = new double[v1.length];
    for(int i = 0; i < v1.length; i++) {
      sub[i] = v1[i] * s1 - v2[i] * s2;
    }
    return sub;
  }

  /**
   * Computes component-wise v1 = v1 - v2,
   * overwriting the vector v1.
   *
   * @param v1 vector
   * @param v2 another vector
   * @return v1 = v1 - v2
   */
  public static double[] minusEquals(final double[] v1, final double[] v2) {
    assert v1.length == v2.length : ERR_VEC_DIMENSIONS;
    for(int i = 0; i < v1.length; i++) {
      v1[i] -= v2[i];
    }
    return v1;
  }

  /**
   * Computes component-wise v1 = v1 - v2 * s2,
   * overwriting the vector v1.
   *
   * @param v1 vector
   * @param v2 another vector
   * @param s2 scalar for v2
   * @return v1 = v1 - v2 * s2
   */
  public static double[] minusTimesEquals(final double[] v1, final double[] v2, final double s2) {
    assert v1.length == v2.length : ERR_VEC_DIMENSIONS;
    for(int i = 0; i < v1.length; i++) {
      v1[i] -= v2[i] * s2;
    }
    return v1;
  }

  /**
   * Computes component-wise v1 = v1 * s1 - v2,
   * overwriting the vector v1.
   *
   * @param v1 vector
   * @param s1 scalar for v1
   * @param v2 another vector
   * @return v1 = v1 * s1 - v2
   */
  public static double[] timesMinusEquals(final double[] v1, final double s1, final double[] v2) {
    assert v1.length == v2.length : ERR_VEC_DIMENSIONS;
    for(int i = 0; i < v1.length; i++) {
      v1[i] = v1[i] * s1 - v2[i];
    }
    return v1;
  }

  /**
   * Computes component-wise v1 = v1 * s1 - v2 * s2,
   * overwriting the vector v1.
   *
   * @param v1 vector
   * @param s1 scalar for v1
   * @param v2 another vector
   * @param s2 Scalar
   * @return v1 = v1 * s1 - v2 * s2
   */
  public static double[] timesMinusTimesEquals(final double[] v1, final double s1, final double[] v2, final double s2) {
    assert v1.length == v2.length : ERR_VEC_DIMENSIONS;
    for(int i = 0; i < v1.length; i++) {
      v1[i] = v1[i] * s1 - v2[i] * s2;
    }
    return v1;
  }

  /**
   * Subtract component-wise v1 - s1.
   *
   * @param v1 original vector
   * @param s1 Value to subtract
   * @return v1 - s1
   */
  public static double[] minus(final double[] v1, final double s1) {
    final double[] result = new double[v1.length];
    for(int i = 0; i < v1.length; i++) {
      result[i] = v1[i] - s1;
    }
    return result;
  }

  /**
   * Subtract component-wise in-place v1 = v1 - s1,
   * overwriting the vector v1.
   *
   * @param v1 original vector
   * @param s1 Value to subtract
   * @return v1 = v1 - s1
   */
  public static double[] minusEquals(final double[] v1, final double s1) {
    for(int i = 0; i < v1.length; i++) {
      v1[i] -= s1;
    }
    return v1;
  }

  /**
   * Multiply component-wise v1 * s1.
   *
   * @param v1 original vector
   * @param s1 the scalar to be multiplied
   * @return v1 * s1
   */
  public static double[] times(final double[] v1, final double s1) {
    final double[] v = new double[v1.length];
    for(int i = 0; i < v1.length; i++) {
      v[i] = v1[i] * s1;
    }
    return v;
  }

  /**
   * Multiply component-wise v1 = v1 * s1,
   * overwriting the vector v1.
   *
   * @param v1 original vector
   * @param s scalar
   * @return v1 = v1 * s1
   */
  public static double[] timesEquals(final double[] v1, final double s) {
    for(int i = 0; i < v1.length; i++) {
      v1[i] *= s;
    }
    return v1;
  }

  /**
   * Multiply component-wise v1 = v2 * s,
   * overwriting the vector v1.
   *
   * @param v1 output vector
   * @param v2 input vector
   * @param s scalar
   * @return v1 with new values
   */
  public static double[] overwriteTimes(final double[] v1, final double[] v2, final double s) {
    for(int i = 0; i < v1.length; i++) {
      v1[i] = v2[i] * s;
    }
    return v1;
  }

  /**
   * Matrix multiplication: v1 * m2 (m2 <em>must have one row only</em>).
   * <p>
   * Note: this is an unusual operation, m2 must be a costly column matrix.
   * <p>
   * This method is equivalent to the
   * {@link #timesTranspose(double[], double[])} method
   * with m2 being the second vector as matrix, but transposed.
   *
   * @param v1 vector
   * @param m2 other matrix, must have one row.
   * @return Matrix product, v1 * m2
   * @deprecated this is fairly inefficient memory layout, rewriting your code
   */
  @Deprecated
  public static double[][] times(final double[] v1, final double[][] m2) {
    assert m2.length == 1 : ERR_MATRIX_INNERDIM;
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
   * Vector to matrix multiplication, v1<sup>T</sup> m2.
   *
   * @param v1 vector
   * @param m2 other matrix
   * @return Matrix product, v1<sup>T</sup> * m2
   */
  public static double[][] transposeTimes(final double[] v1, final double[][] m2) {
    assert m2.length == v1.length : ERR_MATRIX_INNERDIM;
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
   * Vector scalar product (dot product),
   * v1<sup>T</sup> v2 = v1·v2 = &lt;v1,v2&gt;.
   *
   * @param v1 vector
   * @param v2 other vector
   * @return scalar result of matrix product, v1<sup>T</sup> * v2
   */
  public static double transposeTimes(final double[] v1, final double[] v2) {
    assert v2.length == v1.length : ERR_VEC_DIMENSIONS;
    double s = 0;
    for(int k = 0; k < v1.length; k++) {
      s += v1[k] * v2[k];
    }
    return s;
  }

  /**
   * Vector with transposed matrix multiplication, v1 * m2<sup>T</sup>
   * (m2 <em>must have one row only</em>).
   * <p>
   * Note: this is an unusual operation, m2 must be a costly column matrix.
   * <p>
   * This method is equivalent to the
   * {@link #timesTranspose(double[], double[])} method
   * with m2 being the second vector as matrix.
   *
   * @param v1 vector
   * @param m2 other matrix
   * @return Matrix product, v1 * m2<sup>T</sup>
   * @deprecated this is fairly inefficient memory layout, rewriting your code
   */
  @Deprecated
  public static double[][] timesTranspose(final double[] v1, final double[][] m2) {
    assert m2[0].length == 1 : ERR_MATRIX_INNERDIM;

    final double[][] re = new double[v1.length][m2.length];
    for(int j = 0; j < m2.length; j++) {
      for(int i = 0; i < v1.length; i++) {
        re[i][j] = v1[i] * m2[j][0];
      }
    }
    return re;
  }

  /**
   * Vectors to matrix multiplication, v1 * v2<sup>T</sup>.
   *
   * @param v1 vector
   * @param v2 other vector
   * @return Matrix product, v1 * v2<sup>T</sup>
   */
  public static double[][] timesTranspose(final double[] v1, final double[] v2) {
    final double[][] re = new double[v1.length][v2.length];
    for(int j = 0; j < v2.length; j++) {
      for(int i = 0; i < v1.length; i++) {
        re[i][j] = v1[i] * v2[j];
      }
    }
    return re;
  }

  /**
   * Returns the scalar product (dot product) of two vectors,
   * &lt;v1,v2&gt; = v1<sup>T</sup> v2.
   * <p>
   * This is the same as {@link #transposeTimes(double[], double[])}.
   *
   * @param v1 vector
   * @param v2 other vector
   * @return double the scalar product of vectors v1 and v2
   */
  public static double scalarProduct(final double[] v1, final double[] v2) {
    return transposeTimes(v1, v2);
  }

  /**
   * Returns the dot product (scalar product) of two vectors,
   * v1·v2 = v1<sup>T</sup> v2.
   * <p>
   * This is the same as {@link #transposeTimes(double[], double[])}.
   *
   * @param v1 vector
   * @param v2 other vector
   * @return double the scalar product of vectors v1 and v2
   */
  public static double dot(final double[] v1, final double[] v2) {
    return transposeTimes(v1, v2);
  }

  /**
   * Sum of the vector components.
   *
   * @param v1 vector
   * @return sum of this vector
   */
  public static double sum(final double[] v1) {
    double acc = 0.;
    for(int row = 0; row < v1.length; row++) {
      acc += v1[row];
    }
    return acc;
  }

  /**
   * Squared Euclidean length of the vector v1<sup>T</sup> v1 = v1·v1.
   *
   * @param v1 vector
   * @return squared Euclidean length of this vector
   */
  public static double squareSum(final double[] v1) {
    double acc = 0.0;
    for(int row = 0; row < v1.length; row++) {
      final double v = v1[row];
      acc += v * v;
    }
    return acc;
  }

  /**
   * Euclidean length of the vector sqrt(v1<sup>T</sup> v1).
   *
   * @param v1 vector
   * @return Euclidean length of this vector
   */
  public static double euclideanLength(final double[] v1) {
    return FastMath.sqrt(squareSum(v1));
  }

  /**
   * Find the maximum value.
   *
   * @param v Vector
   * @return Position of maximum
   */
  public static int argmax(double[] v) {
    assert (v.length > 0);
    int maxIndex = 0;
    double currentMax = v[0];
    for(int i = 1; i < v.length; i++) {
      final double x = v[i];
      if(x > currentMax) {
        maxIndex = i;
        currentMax = x;
      }
    }
    return maxIndex;
  }

  /**
   * Normalizes v1 to the length of 1.0.
   *
   * @param v1 vector
   * @return normalized copy of v1
   */
  public static double[] normalize(final double[] v1) {
    final double norm = 1. / euclideanLength(v1);
    double[] re = new double[v1.length];
    if(norm < Double.POSITIVE_INFINITY) {
      for(int row = 0; row < v1.length; row++) {
        re[row] = v1[row] * norm;
      }
    }
    return re;
  }

  /**
   * Normalizes v1 to the length of 1.0 in place.
   *
   * @param v1 vector (overwritten)
   * @return normalized v1
   */
  public static double[] normalizeEquals(final double[] v1) {
    final double norm = 1. / euclideanLength(v1);
    if(norm < Double.POSITIVE_INFINITY) {
      for(int row = 0; row < v1.length; row++) {
        v1[row] *= norm;
      }
    }
    return v1;
  }

  /**
   * Compute the hash code for the vector.
   *
   * @param v1 elements
   * @return hash code
   */
  public static int hashCode(final double[] v1) {
    return Arrays.hashCode(v1);
  }

  /**
   * Compare for equality.
   *
   * @param v1 first vector
   * @param v2 second vector
   * @return comparison result
   */
  public static boolean equals(final double[] v1, final double[] v2) {
    return Arrays.equals(v1, v2);
  }

  /**
   * Reset the vector to 0.
   *
   * @param v1 vector
   */
  public static void clear(final double[] v1) {
    Arrays.fill(v1, 0.0);
  }

  /**
   * Reset the matrix to 0.
   *
   * @param m Matrix
   */
  public static void clear(final double[][] m) {
    for(int i = 0; i < m.length; i++) {
      Arrays.fill(m[i], 0.0);
    }
  }

  /**
   * Rotate the two-dimensional vector by 90 degrees.
   *
   * @param v1 first vector
   * @return modified v1, rotated by 90 degrees
   */
  public static double[] rotate90Equals(final double[] v1) {
    assert v1.length == 2 : "rotate90Equals is only valid for 2d vectors.";
    final double temp = v1[0];
    v1[0] = v1[1];
    v1[1] = -temp;
    return v1;
  }

  // *********** MATRIX operations

  /**
   * Returns the unit / identity / "eye" matrix of the specified dimension.
   *
   * @param dim the dimensionality of the unit matrix
   * @return the unit matrix of the specified dimension
   */
  public static double[][] unitMatrix(final int dim) {
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
  public static double[][] zeroMatrix(final int dim) {
    return new double[dim][dim];
  }

  /**
   * Generate unit / identity / "eye" matrix.
   *
   * @param m Number of rows.
   * @param n Number of columns.
   * @return An m-by-n matrix with ones on the diagonal and zeros elsewhere.
   */
  public static double[][] identity(final int m, final int n) {
    final double[][] A = new double[m][n];
    final int dim = m < n ? m : n;
    for(int i = 0; i < dim; i++) {
      A[i][i] = 1.0;
    }
    return A;
  }

  /**
   * Returns a quadratic matrix consisting of zeros and of the given values on
   * the diagonal.
   *
   * @param v1 the values on the diagonal
   * @return the resulting matrix
   */
  public static double[][] diagonal(final double[] v1) {
    final int dim = v1.length;
    final double[][] result = new double[dim][dim];
    for(int i = 0; i < dim; i++) {
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
  public static double[][] copy(final double[][] m1) {
    final int rowdim = m1.length, coldim = getColumnDimensionality(m1);
    final double[][] X = new double[rowdim][coldim];
    for(int i = 0; i < rowdim; i++) {
      System.arraycopy(m1[i], 0, X[i], 0, coldim);
    }
    return X;
  }

  /**
   * Make a one-dimensional row packed copy of the internal array.
   *
   * @param m1 Input matrix
   * @return Matrix elements packed in a one-dimensional array by rows.
   */
  public static double[] rowPackedCopy(final double[][] m1) {
    final int rowdim = m1.length, coldim = getColumnDimensionality(m1);
    final double[] vals = new double[rowdim * coldim];
    for(int i = 0, j = 0; i < rowdim; i++, j += coldim) {
      // assert m1[i].length == coldim : ERR_MATRIX_RAGGED;
      System.arraycopy(m1[i], 0, vals, j, coldim);
    }
    return vals;
  }

  /**
   * Make a one-dimensional column packed copy of the internal array.
   *
   * @param m1 Input matrix
   * @return Matrix elements packed in a one-dimensional array by columns.
   */
  public static double[] columnPackedCopy(final double[][] m1) {
    final int rowdim = m1.length, coldim = getColumnDimensionality(m1);
    final double[] vals = new double[m1.length * coldim];
    for(int i = 0; i < rowdim; i++) {
      final double[] rowM = m1[i];
      // assert rowM.length == coldim : ERR_MATRIX_RAGGED;
      for(int j = 0, k = i; j < coldim; j++, k += rowdim) {
        vals[k] = rowM[j];
      }
    }
    return vals;
  }

  /**
   * Get a submatrix.
   *
   * @param m1 Input matrix
   * @param r0 Initial row index
   * @param r1 Final row index (exclusive)
   * @param c0 Initial column index
   * @param c1 Final column index (exclusive)
   * @return m1(r0:r1-1,c0:c1-1)
   */
  public static double[][] getMatrix(final double[][] m1, final int r0, final int r1, final int c0, final int c1) {
    assert r0 <= r1 && c0 <= c1 : ERR_INVALID_RANGE;
    assert r1 <= m1.length && c1 <= m1[0].length : ERR_MATRIX_DIMENSIONS;
    final int rowdim = r1 - r0, coldim = c1 - c0;
    final double[][] X = new double[rowdim][coldim];
    for(int i = r0; i < r1; i++) {
      System.arraycopy(m1[i], c0, X[i - r0], 0, coldim);
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
  public static double[][] getMatrix(final double[][] m1, final int[] r, final int[] c) {
    final int rowdim = r.length, coldim = c.length;
    final double[][] X = new double[rowdim][coldim];
    for(int i = 0; i < rowdim; i++) {
      final double[] rowM = m1[r[i]], rowX = X[i];
      for(int j = 0; j < coldim; j++) {
        rowX[j] = rowM[c[j]];
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
   * @param c1 Final column index (exclusive)
   * @return m1(r(:),c0:c1-1)
   */
  public static double[][] getMatrix(final double[][] m1, final int[] r, final int c0, final int c1) {
    assert c0 <= c1 : ERR_INVALID_RANGE;
    assert c1 <= m1[0].length : ERR_MATRIX_DIMENSIONS;
    final int rowdim = r.length, coldim = c1 - c0;
    final double[][] X = new double[rowdim][coldim];
    for(int i = 0; i < rowdim; i++) {
      System.arraycopy(m1[r[i]], c0, X[i], 0, coldim);
    }
    return X;
  }

  /**
   * Get a submatrix.
   *
   * @param m1 Input matrix
   * @param r0 Initial row index
   * @param r1 Final row index (exclusive)
   * @param c Array of column indices.
   * @return m1(r0:r1-1,c(:))
   */
  public static double[][] getMatrix(final double[][] m1, final int r0, final int r1, final int[] c) {
    assert r0 <= r1 : ERR_INVALID_RANGE;
    assert r1 <= m1.length : ERR_MATRIX_DIMENSIONS;
    final int rowdim = r1 - r0, coldim = c.length;
    final double[][] X = new double[rowdim][coldim];
    for(int i = r0; i < r1; i++) {
      final double[] row = m1[i];
      final double[] Xi = X[i - r0];
      for(int j = 0; j < coldim; j++) {
        Xi[j] = row[c[j]];
      }
    }
    return X;
  }

  /**
   * Set a submatrix.
   *
   * @param m1 Original matrix
   * @param r0 Initial row index
   * @param r1 Final row index (exclusive)
   * @param c0 Initial column index
   * @param c1 Final column index (exclusive)
   * @param m2 New values for m1(r0:r1-1,c0:c1-1)
   */
  public static void setMatrix(final double[][] m1, final int r0, final int r1, final int c0, final int c1, final double[][] m2) {
    assert r0 <= r1 && c0 <= c1 : ERR_INVALID_RANGE;
    assert r1 <= m1.length && c1 <= m1[0].length : ERR_MATRIX_DIMENSIONS;
    final int coldim = c1 - c0;
    for(int i = r0; i < r1; i++) {
      System.arraycopy(m2[i - r0], 0, m1[i], c0, coldim);
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
  public static void setMatrix(final double[][] m1, final int[] r, final int[] c, final double[][] m2) {
    for(int i = 0; i < r.length; i++) {
      final double[] row1 = m1[r[i]], row2 = m2[i];
      for(int j = 0; j < c.length; j++) {
        row1[c[j]] = row2[j];
      }
    }
  }

  /**
   * Set a submatrix.
   *
   * @param m1 Input matrix
   * @param r Array of row indices.
   * @param c0 Initial column index
   * @param c1 Final column index (exclusive)
   * @param m2 New values for m1(r(:),c0:c1-1)
   */
  public static void setMatrix(final double[][] m1, final int[] r, final int c0, final int c1, final double[][] m2) {
    assert c0 <= c1 : ERR_INVALID_RANGE;
    assert c1 <= m1[0].length : ERR_MATRIX_DIMENSIONS;
    for(int i = 0; i < r.length; i++) {
      System.arraycopy(m2[i], 0, m1[r[i]], c0, c1 - c0);
    }
  }

  /**
   * Set a submatrix.
   *
   * @param m1 Input matrix
   * @param r0 Initial row index
   * @param r1 Final row index
   * @param c Array of column indices.
   * @param m2 New values for m1(r0:r1-1,c(:))
   */
  public static void setMatrix(final double[][] m1, final int r0, final int r1, final int[] c, final double[][] m2) {
    assert r0 <= r1 : ERR_INVALID_RANGE;
    assert r1 <= m1.length : ERR_MATRIX_DIMENSIONS;
    for(int i = r0; i < r1; i++) {
      final double[] row1 = m1[i], row2 = m2[i - r0];
      for(int j = 0; j < c.length; j++) {
        row1[c[j]] = row2[j];
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
  public static double[] getRow(final double[][] m1, final int r) {
    return m1[r].clone();
  }

  /**
   * Sets the <code>r</code>th row of this matrix to the specified vector.
   *
   * @param m1 Original matrix
   * @param r the index of the column to be set
   * @param row the value of the column to be set
   */
  public static void setRow(final double[][] m1, final int r, final double[] row) {
    final int columndimension = getColumnDimensionality(m1);
    assert row.length == columndimension : ERR_DIMENSIONS;
    System.arraycopy(row, 0, m1[r], 0, columndimension);
  }

  /**
   * Get a column from a matrix as vector.
   *
   * @param m1 Matrix to extract the column from
   * @param col Column number
   * @return Column
   */
  public static double[] getCol(double[][] m1, int col) {
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
  public static void setCol(final double[][] m1, final int c, final double[] column) {
    assert column.length == m1.length : ERR_DIMENSIONS;
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
  public static double[][] transpose(final double[][] m1) {
    final int rowdim = m1.length, coldim = getColumnDimensionality(m1);
    final double[][] re = new double[coldim][rowdim];
    for(int i = 0; i < rowdim; i++) {
      final double[] row = m1[i];
      for(int j = 0; j < coldim; j++) {
        re[j][i] = row[j];
      }
    }
    return re;
  }

  /**
   * Component-wise matrix sum: m3 = m1 + m2.
   *
   * @param m1 Input matrix
   * @param m2 another matrix
   * @return m1 + m1 in a new Matrix
   */
  public static double[][] plus(final double[][] m1, final double[][] m2) {
    return plusEquals(copy(m1), m2);
  }

  /**
   * Component-wise matrix operation: m3 = m1 + m2 * s2.
   *
   * @param m1 Input matrix
   * @param m2 another matrix
   * @param s2 scalar
   * @return m1 + m2 * s2 in a new matrix
   */
  public static double[][] plusTimes(final double[][] m1, final double[][] m2, final double s2) {
    return plusTimesEquals(copy(m1), m2, s2);
  }

  /**
   * Component-wise matrix operation: m1 = m1 + m2,
   * overwriting the existing matrix m1.
   *
   * @param m1 input matrix (overwritten)
   * @param m2 another matrix
   * @return m1 = m1 + m2
   */
  public static double[][] plusEquals(final double[][] m1, final double[][] m2) {
    final int rowdim = m1.length, coldim = getColumnDimensionality(m1);
    assert getRowDimensionality(m1) == getRowDimensionality(m2) && coldim == getColumnDimensionality(m2) : ERR_MATRIX_DIMENSIONS;
    for(int i = 0; i < rowdim; i++) {
      final double[] row1 = m1[i], row2 = m2[i];
      for(int j = 0; j < coldim; j++) {
        row1[j] += row2[j];
      }
    }
    return m1;
  }

  /**
   * Component-wise matrix operation: m1 = m1 + m2 * s2,
   * overwriting the existing matrix m1.
   *
   * @param m1 input matrix (overwritten)
   * @param m2 another matrix
   * @param s2 scalar for s2
   * @return m1 = m1 + m2 * s2, overwriting m1
   */
  public static double[][] plusTimesEquals(final double[][] m1, final double[][] m2, final double s2) {
    final int rowdim = m1.length, coldim = getColumnDimensionality(m1);
    assert getRowDimensionality(m1) == getRowDimensionality(m2) && coldim == getColumnDimensionality(m2) : ERR_MATRIX_DIMENSIONS;
    for(int i = 0; i < rowdim; i++) {
      final double[] row1 = m1[i], row2 = m2[i];
      for(int j = 0; j < coldim; j++) {
        row1[j] += s2 * row2[j];
      }
    }
    return m1;
  }

  /**
   * Component-wise matrix subtraction m3 = m1 - m2.
   *
   * @param m1 Input matrix
   * @param m2 another matrix
   * @return m1 - m2 in a new matrix
   */
  public static double[][] minus(final double[][] m1, final double[][] m2) {
    return minusEquals(copy(m1), m2);
  }

  /**
   * Component-wise matrix operation: m3 = m1 - m2 * s2
   *
   * @param m1 Input matrix
   * @param m2 another matrix
   * @param s2 Scalar
   * @return m1 - m2 * s2 in a new Matrix
   */
  public static double[][] minusTimes(final double[][] m1, final double[][] m2, final double s2) {
    return minusTimesEquals(copy(m1), m2, s2);
  }

  /**
   * Component-wise matrix subtraction: m1 = m1 - m2,
   * overwriting the existing matrix m1.
   *
   * @param m1 Input matrix
   * @param m2 another matrix
   * @return m1 - m2, overwriting m1
   */
  public static double[][] minusEquals(final double[][] m1, final double[][] m2) {
    final int columndimension = getColumnDimensionality(m1);
    assert getRowDimensionality(m1) == getRowDimensionality(m2) && columndimension == getColumnDimensionality(m2) : ERR_MATRIX_DIMENSIONS;
    for(int i = 0; i < m1.length; i++) {
      final double[] row1 = m1[i], row2 = m2[i];
      for(int j = 0; j < columndimension; j++) {
        row1[j] -= row2[j];
      }
    }
    return m1;
  }

  /**
   * Component-wise matrix operation: m1 = m1 - m2 * s2,
   * overwriting the existing matrix m1.
   *
   * @param m1 Input matrix
   * @param m2 another matrix
   * @param s2 Scalar
   * @return m1 = m1 - s2 * m2, overwriting m1
   */
  public static double[][] minusTimesEquals(final double[][] m1, final double[][] m2, final double s2) {
    assert getRowDimensionality(m1) == getRowDimensionality(m2) && getColumnDimensionality(m1) == getColumnDimensionality(m2) : ERR_MATRIX_DIMENSIONS;
    for(int i = 0; i < m1.length; i++) {
      final double[] row1 = m1[i], row2 = m2[i];
      for(int j = 0; j < row1.length; j++) {
        row1[j] -= s2 * row2[j];
      }
    }
    return m1;
  }

  /**
   * Multiply a matrix by a scalar component-wise, m3 = m1 * s1.
   *
   * @param m1 Input matrix
   * @param s1 scalar
   * @return m1 * s1, in a new matrix
   */
  public static double[][] times(final double[][] m1, final double s1) {
    return timesEquals(copy(m1), s1);
  }

  /**
   * Multiply a matrix by a scalar component-wise in place, m1 = m1 * s1,
   * overwriting the existing matrix m1.
   *
   * @param m1 Input matrix
   * @param s1 scalar
   * @return m1 = m1 * s1, overwriting m1
   */
  public static double[][] timesEquals(final double[][] m1, final double s1) {
    final int rowdim = m1.length;
    for(int i = 0; i < rowdim; i++) {
      final double[] row = m1[i];
      for(int j = 0; j < row.length; j++) {
        row[j] *= s1;
      }
    }
    return m1;
  }

  /**
   * Matrix multiplication, m1 * m2.
   *
   * @param m1 Input matrix
   * @param m2 another matrix
   * @return Matrix product, m1 * m2
   */
  public static double[][] times(final double[][] m1, final double[][] m2) {
    final int rowdim1 = m1.length, coldim1 = getColumnDimensionality(m1);
    final int coldim2 = getColumnDimensionality(m2);
    // Optimized implementation, exploiting the storage layout
    assert m2.length == coldim1 : ERR_MATRIX_INNERDIM;
    final double[][] r2 = new double[rowdim1][coldim2];
    // Optimized ala Jama. jik order.
    final double[] Bcolj = new double[coldim1];
    for(int j = 0; j < coldim2; j++) {
      // Make a linear copy of column j from B
      for(int k = 0; k < coldim1; k++) {
        Bcolj[k] = m2[k][j];
      }
      // multiply it with each row from A
      for(int i = 0; i < rowdim1; i++) {
        final double[] Arowi = m1[i];
        double s = 0;
        for(int k = 0; k < coldim1; k++) {
          s += Arowi[k] * Bcolj[k];
        }
        r2[i][j] = s;
      }
    }
    return r2;
  }

  /**
   * Matrix with vector multiplication, m1 * v2.
   *
   * @param m1 Input matrix
   * @param v2 a vector
   * @return Matrix product, m1 * v2
   */
  public static double[] times(final double[][] m1, final double[] v2) {
    final int rowdim = m1.length, coldim = getColumnDimensionality(m1);
    assert v2.length == coldim : ERR_MATRIX_INNERDIM;
    final double[] re = new double[rowdim];
    // multiply it with each row from A
    for(int i = 0; i < rowdim; i++) {
      final double[] Arowi = m1[i];
      // assert Arowi.length == coldim : ERR_MATRIX_RAGGED;
      double s = 0;
      for(int k = 0; k < coldim; k++) {
        s += Arowi[k] * v2[k];
      }
      re[i] = s;
    }
    return re;
  }

  /**
   * Transposed matrix with vector multiplication, m1<sup>T</sup> * v2
   *
   * @param m1 Input matrix
   * @param v2 another matrix
   * @return Matrix product, m1<sup>T</sup> * v2
   */
  public static double[] transposeTimes(final double[][] m1, final double[] v2) {
    final int rowdim = m1.length, coldim = getColumnDimensionality(m1);
    assert v2.length == rowdim : ERR_MATRIX_INNERDIM;
    final double[] re = new double[coldim];
    // multiply it with each row from A
    for(int i = 0; i < coldim; i++) {
      double s = 0;
      for(int k = 0; k < rowdim; k++) {
        s += m1[k][i] * v2[k];
      }
      re[i] = s;
    }
    return re;
  }

  /**
   * Matrix multiplication, m1<sup>T</sup> * m2
   *
   * @param m1 Input matrix
   * @param m2 another matrix
   * @return Matrix product, m1<sup>T</sup> * m2
   */
  public static double[][] transposeTimes(final double[][] m1, final double[][] m2) {
    final int rowdim1 = m1.length, coldim1 = getColumnDimensionality(m1);
    final int coldim2 = getColumnDimensionality(m2);
    assert m2.length == rowdim1 : ERR_MATRIX_INNERDIM;
    final double[][] re = new double[coldim1][coldim2];
    final double[] Bcolj = new double[rowdim1];
    for(int j = 0; j < coldim2; j++) {
      // Make a linear copy of column j from B
      for(int k = 0; k < rowdim1; k++) {
        Bcolj[k] = m2[k][j];
      }
      // multiply it with each row from A
      for(int i = 0; i < coldim1; i++) {
        double s = 0;
        for(int k = 0; k < rowdim1; k++) {
          s += m1[k][i] * Bcolj[k];
        }
        re[i][j] = s;
      }
    }
    return re;
  }

  /**
   * Matrix multiplication, v1<sup>T</sup> * m2 * v3
   *
   * @param v1 vector on the left
   * @param m2 matrix
   * @param v3 vector on the right
   * @return Matrix product, v1<sup>T</sup> * m2 * v3
   */
  public static double transposeTimesTimes(final double[] v1, final double[][] m2, final double[] v3) {
    final int rowdim = m2.length, coldim = getColumnDimensionality(m2);
    assert rowdim == v1.length : ERR_MATRIX_INNERDIM;
    assert coldim == v3.length : ERR_MATRIX_INNERDIM;
    double sum = 0.0;
    for(int k = 0; k < rowdim; k++) {
      final double[] B_k = m2[k];
      double s = 0;
      for(int j = 0; j < coldim; j++) {
        s += v3[j] * B_k[j];
      }
      sum += s * v1[k];
    }
    return sum;
  }

  /**
   * Matrix multiplication, m1 * m2<sup>T</sup>
   *
   * @param m1 Input matrix
   * @param m2 another matrix
   * @return Matrix product, m1 * m2<sup>T</sup>
   */
  public static double[][] timesTranspose(final double[][] m1, final double[][] m2) {
    final int rowdim1 = m1.length, coldim1 = getColumnDimensionality(m1);
    final int rowdim2 = m2.length;
    assert coldim1 == getColumnDimensionality(m2) : ERR_MATRIX_INNERDIM;
    final double[][] re = new double[rowdim1][rowdim2];
    for(int j = 0; j < rowdim2; j++) {
      final double[] Browj = m2[j];
      // multiply it with each row from A
      for(int i = 0; i < rowdim1; i++) {
        final double[] Arowi = m1[i];
        double s = 0;
        // assert Arowi.length == coldim1 : ERR_MATRIX_RAGGED;
        // assert Browj.length == coldim1 : ERR_MATRIX_INNERDIM;
        for(int k = 0; k < coldim1; k++) {
          s += Arowi[k] * Browj[k];
        }
        re[i][j] = s;
      }
    }
    return re;
  }

  /**
   * Matrix multiplication, m1<sup>T</sup> * m2<sup>T</sup>.
   * Computed as (m2*m1)<sup>T</sup>
   *
   * @param m1 Input matrix
   * @param m2 another matrix
   * @return Matrix product, m1<sup>T</sup> * m2<sup>T</sup>
   */
  public static double[][] transposeTimesTranspose(final double[][] m1, final double[][] m2) {
    // Optimized implementation, exploiting the storage layout
    assert m1.length == getColumnDimensionality(m2) : ERR_MATRIX_INNERDIM;
    final double[][] re = new double[getColumnDimensionality(m1)][m2.length];
    // Optimized ala Jama. jik order.
    final double[] Acolj = new double[m1.length];
    for(int j = 0; j < re.length; j++) {
      // Make a linear copy of column j from A
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
   * Matrix multiplication with diagonal, m1^T * d2 * m3
   * 
   * @param m1 Left matrix
   * @param d2 Diagonal entries
   * @param m3 Right matrix
   * @return m1^T * d2 * m3
   */
  public static double[][] transposeDiagonalTimes(double[][] m1, double[] d2, double[][] m3) {
    final int innerdim = d2.length;
    assert m1.length == innerdim : ERR_MATRIX_INNERDIM;
    assert m3.length == innerdim : ERR_MATRIX_INNERDIM;
    final int coldim1 = getColumnDimensionality(m1);
    final int coldim2 = getColumnDimensionality(m3);
    final double[][] r = new double[coldim1][coldim2];
    final double[] Acoli = new double[innerdim]; // Buffer
    // multiply it with each row from A
    for(int i = 0; i < coldim1; i++) {
      final double[] r_i = r[i]; // Output row
      // Make a linear copy of column i from A
      for(int k = 0; k < innerdim; k++) {
        Acoli[k] = m1[k][i] * d2[k];
      }
      for(int j = 0; j < coldim2; j++) {
        double s = 0;
        for(int k = 0; k < innerdim; k++) {
          s += Acoli[k] * m3[k][j];
        }
        r_i[j] = s;
      }
    }
    return r;
  }

  /**
   * Matrix multiplication, (a-c)<sup>T</sup> * B * (a-c)
   * <p>
   * Note: it may (or may not) be more efficient to materialize (a-c), then use
   * {@code transposeTimesTimes(a_minus_c, B, a_minus_c)} instead.
   *
   * @param B matrix
   * @param a First vector
   * @param c Center vector
   * @return Matrix product, (a-c)<sup>T</sup> * B * (a-c)
   */
  @Reference(authors = "P. C. Mahalanobis", //
      title = "On the generalized distance in statistics", //
      booktitle = "Proceedings of the National Institute of Sciences of India. 2 (1)", //
      bibkey = "journals/misc/Mahalanobis36")
  public static double mahalanobisDistance(final double[][] B, final double[] a, final double[] c) {
    final int rowdim = B.length, coldim = getColumnDimensionality(B);
    assert rowdim == a.length : ERR_MATRIX_INNERDIM;
    assert coldim == c.length : ERR_MATRIX_INNERDIM;
    assert a.length == c.length : ERR_VEC_DIMENSIONS;
    double sum = 0.0;
    for(int k = 0; k < rowdim; k++) {
      final double[] B_k = B[k];
      double s = 0;
      for(int j = 0; j < coldim; j++) {
        s += (a[j] - c[j]) * B_k[j];
      }
      sum += (a[k] - c[k]) * s;
    }
    return sum;
  }

  /**
   * getDiagonal returns array of diagonal-elements.
   *
   * @param m1 Input matrix
   * @return values on the diagonal of the Matrix
   */
  public static double[] getDiagonal(final double[][] m1) {
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
   * Note: if a column has length 0, it will remain unmodified.
   *
   * @param m1 Input matrix
   */
  public static void normalizeColumns(final double[][] m1) {
    final int columndimension = getColumnDimensionality(m1);
    for(int col = 0; col < columndimension; col++) {
      double norm = 0.0;
      for(int row = 0; row < m1.length; row++) {
        final double v = m1[row][col];
        norm += v * v;
      }
      if(norm > 0) {
        norm = FastMath.sqrt(norm);
        for(int row = 0; row < m1.length; row++) {
          m1[row][col] /= norm;
        }
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
  public static double[][] appendColumns(final double[][] m1, final double[][] m2) {
    final int columndimension = getColumnDimensionality(m1);
    final int ccolumndimension = getColumnDimensionality(m2);
    assert m1.length == m2.length : "m.getRowDimension() != column.getRowDimension()";

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
  public static double[][] orthonormalize(final double[][] m1) {
    final int columndimension = getColumnDimensionality(m1);
    final double[][] v = copy(m1);

    // FIXME: optimize - excess copying!
    for(int i = 1; i < columndimension; i++) {
      final double[] u_i = getCol(m1, i);
      final double[] sum = new double[m1.length];
      for(int j = 0; j < i; j++) {
        final double[] v_j = getCol(v, j);
        final double scalar = scalarProduct(u_i, v_j) / scalarProduct(v_j, v_j);
        plusEquals(sum, times(v_j, scalar));
      }
      final double[] v_i = minus(u_i, sum);
      setCol(v, i, v_i);
    }

    normalizeColumns(v);
    return v;
  }

  /**
   * Solve A*X = B
   *
   * @param B right hand side
   * @return solution if A is square, least squares solution otherwise
   */
  public static double[][] solve(double[][] A, double[][] B) {
    final int rows = A.length, cols = A[0].length;
    return rows == cols //
        ? (new LUDecomposition(A, rows, cols)).solve(B) //
        : (new QRDecomposition(A, rows, cols)).solve(B);
  }

  /**
   * Solve A*X = b
   *
   * @param b right hand side
   * @return solution if A is square, least squares solution otherwise
   */
  public static double[] solve(double[][] A, double[] b) {
    final int rows = A.length, cols = A[0].length;
    return rows == cols //
        ? (new LUDecomposition(A, rows, cols)).solve(b) //
        : (new QRDecomposition(A, rows, cols)).solve(b);
  }

  /**
   * Matrix inverse or pseudoinverse
   *
   * @param A matrix to invert
   * @return inverse(A) if A is square, pseudoinverse otherwise.
   */
  public static double[][] inverse(double[][] A) {
    final int rows = A.length, cols = A[0].length;
    return rows == cols //
        ? (new LUDecomposition(A, rows, cols)).inverse() //
        : (new QRDecomposition(A, rows, cols)).inverse();
  }

  /**
   * Frobenius norm
   *
   * @param elements Matrix
   * @return sqrt of sum of squares of all elements.
   */
  public static double normF(double[][] elements) {
    double f = 0;
    for(int i = 0; i < elements.length; i++) {
      final double[] row = elements[i];
      for(int j = 0; j < row.length; j++) {
        final double v = row[j];
        f += v * v;
      }
    }
    // TODO: detect overflow, fall back to slower hypot()
    return FastMath.sqrt(f);
  }

  /**
   * Compute hash code
   *
   * @param m1 Input matrix
   * @return Hash code
   */
  public static int hashCode(final double[][] m1) {
    return Arrays.deepHashCode(m1);
  }

  /**
   * Test for equality
   *
   * @param m1 Input matrix
   * @param m2 Other matrix
   * @return Equality
   */
  public static boolean equals(final double[][] m1, final double[][] m2) {
    return Arrays.deepEquals(m1, m2);
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
  public static boolean almostEquals(final double[][] m1, final double[][] m2, final double maxdelta) {
    if(m1 == m2) {
      return true;
    }
    if(m1 == null || m2 == null) {
      return false;
    }
    final int rowdim = m1.length, coldim = getColumnDimensionality(m1);
    if(rowdim != m2.length || coldim != getColumnDimensionality(m2)) {
      return false;
    }
    for(int i = 0; i < rowdim; i++) {
      for(int j = 0; j < coldim; j++) {
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
  public static boolean almostEquals(final double[][] m1, final double[][] m2) {
    return almostEquals(m1, m2, DELTA);
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
  public static boolean almostEquals(final double[] m1, final double[] m2, final double maxdelta) {
    if(m1 == m2) {
      return true;
    }
    if(m1 == null || m2 == null) {
      return false;
    }
    final int rowdim = m1.length;
    if(rowdim != m2.length) {
      return false;
    }
    for(int i = 0; i < rowdim; i++) {
      if(Math.abs(m1[i] - m2[i]) > maxdelta) {
        return false;
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
  public static boolean almostEquals(final double[] m1, final double[] m2) {
    return almostEquals(m1, m2, DELTA);
  }

  /**
   * Returns the dimensionality of the rows of this matrix.
   *
   * @param m1 Input matrix
   * @return the number of rows.
   */
  public static int getRowDimensionality(final double[][] m1) {
    return m1.length;
  }

  /**
   * Returns the dimensionality of the columns of this matrix.
   *
   * @param m1 Input matrix
   * @return the number of columns.
   */
  public static int getColumnDimensionality(final double[][] m1) {
    return m1[0].length;
  }

  /**
   * Compute the cosine of the angle between two vectors,
   * where the smaller angle between those vectors is viewed.
   *
   * @param v1 first vector
   * @param v2 second vector
   * @return cosine of the smaller angle
   */
  public static double angle(double[] v1, double[] v2) {
    final int mindim = (v1.length <= v2.length) ? v1.length : v2.length;
    // Essentially, we want to compute this:
    // v1.transposeTimes(v2) / (v1.euclideanLength() * v2.euclideanLength());
    // We can just compute all three in parallel.
    double s = 0, e1 = 0, e2 = 0;
    for(int k = 0; k < mindim; k++) {
      final double r1 = v1[k], r2 = v2[k];
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
    double a = FastMath.sqrt((s / e1) * (s / e2));
    return (a < 1.) ? a : 1.;
  }

  /**
   * Compute the cosine of the angle between two vectors,
   * where the smaller angle between those vectors is viewed.
   *
   * @param v1 first vector
   * @param v2 second vector
   * @param o Origin
   * @return cosine of the smaller angle
   */
  public static double angle(double[] v1, double[] v2, double[] o) {
    final int mindim = (v1.length <= v2.length) ? v1.length : v2.length;
    // Essentially, we want to compute this:
    // v1' = v1 - o, v2' = v2 - o
    // v1'.transposeTimes(v2') / (v1'.euclideanLength()*v2'.euclideanLength());
    // We can just compute all three in parallel.
    double s = 0, e1 = 0, e2 = 0;
    for(int k = 0; k < mindim; k++) {
      final double ok = (k < o.length) ? o[k] : 0;
      final double r1 = v1[k] - ok, r2 = v2[k] - ok;
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
    double a = FastMath.sqrt((s / e1) * (s / e2));
    return (a < 1.) ? a : 1.;
  }
}
