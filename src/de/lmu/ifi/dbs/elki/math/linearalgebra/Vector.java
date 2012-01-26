package de.lmu.ifi.dbs.elki.math.linearalgebra;

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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;

/**
 * Provides a vector object that encapsulates an m x 1 - matrix object.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.landmark
 */
public class Vector implements NumberVector<Vector, Double> {
  /**
   * Array for internal storage of elements.
   * 
   * @serial internal array storage.
   */
  protected final double[] elements;

  /**
   * Construct a vector from a given array.
   * 
   * @param values array of doubles
   */
  public Vector(final double... values) {
    elements = values;
  }

  /**
   * Provides an m x 1 vector.
   * 
   * @param m the number of rows
   */
  public Vector(final int m) {
    elements = new double[m];
  }

  /**
   * Returns a randomly created vector of length 1.0
   * 
   * @param dimensionality dimensionality
   * @return the dimensionality of the vector
   */
  // FIXME: may also return null vector by chance.
  public static final Vector randomNormalizedVector(final int dimensionality) {
    final Vector v = new Vector(dimensionality);
    for(int i = 0; i < dimensionality; i++) {
      v.elements[i] = Math.random();
    }
    v.normalize();
    return v;
  }

  /**
   * Returns the ith unit vector of the specified dimensionality.
   * 
   * @param dimensionality the dimensionality of the vector
   * @param i the index
   * @return the ith unit vector of the specified dimensionality
   */
  public static final Vector unitVector(final int dimensionality, final int i) {
    final Vector v = new Vector(dimensionality);
    v.elements[i] = 1;
    return v;
  }

  /**
   * Returns a copy of this vector.
   * 
   * @return a copy of this vector
   */
  public final Vector copy() {
    return new Vector(elements.clone());
  }

  /**
   * Clone the Vector object.
   */
  @Override
  public Vector clone() {
    return this.copy();
  }

  /**
   * Access the internal two-dimensional array.
   * 
   * @return Pointer to the two-dimensional array of matrix elements.
   */
  public final double[] getArrayRef() {
    return elements;
  }

  /**
   * Copy the internal two-dimensional array.
   * 
   * @return Two-dimensional array copy of matrix elements.
   */
  public final double[] getArrayCopy() {
    return elements.clone();
  }

  /**
   * Returns the dimensionality of this vector.
   * 
   * @return the dimensionality of this vector
   */
  public final int getDimensionality() {
    return elements.length;
  }

  /**
   * Returns the value at the specified row.
   * 
   * @param i the row index
   * @return the value at row i
   */
  public final double get(final int i) {
    return elements[i];
  }

  /**
   * Sets the value at the specified row.
   * 
   * @param i the row index
   * @param value the value to be set
   * 
   * @return the modified vector
   */
  public final Vector set(final int i, final double value) {
    elements[i] = value;
    return this;
  }

  /**
   * Returns a new vector which is the result of this vector plus the specified
   * vector.
   * 
   * @param v the vector to be added
   * @return the resulting vector
   */
  public final Vector plus(final Vector v) {
    assert (this.elements.length == v.elements.length) : "Vector dimensions must agree.";
    final Vector result = new Vector(elements.length);
    for(int i = 0; i < elements.length; i++) {
      result.elements[i] = elements[i] + v.elements[i];
    }
    return result;
  }

  /**
   * Returns a new vector which is the result of this vector plus the specified
   * vector times the given factor.
   * 
   * @param v the vector to be added
   * @param s the scalar
   * @return the resulting vector
   */
  public final Vector plusTimes(final Vector v, final double s) {
    assert (this.elements.length == v.elements.length) : "Vector dimensions must agree.";
    final Vector result = new Vector(elements.length);
    for(int i = 0; i < elements.length; i++) {
      result.elements[i] = elements[i] + v.elements[i] * s;
    }
    return result;
  }

  /**
   * A = A + B
   * 
   * @param B another matrix
   * @return A + B in this Matrix
   */
  public final Vector plusEquals(final Vector B) {
    assert (this.elements.length == B.elements.length) : "Vector dimensions must agree.";
    for(int i = 0; i < elements.length; i++) {
      elements[i] += B.elements[i];
    }
    return this;
  }

  /**
   * A = A + s * B
   * 
   * @param B another matrix
   * @param s Scalar
   * @return A + s * B in this Matrix
   */
  public final Vector plusTimesEquals(final Vector B, final double s) {
    assert (this.elements.length == B.elements.length) : "Vector dimensions must agree.";
    for(int i = 0; i < elements.length; i++) {
      elements[i] += s * B.elements[i];
    }
    return this;
  }

  /**
   * Add a constant value to all dimensions.
   * 
   * @param d Value to add
   * @return Modified vector
   */
  public final Vector plusEquals(final double d) {
    for(int i = 0; i < elements.length; i++) {
      elements[i] += d;
    }
    return this;
  }

  /**
   * Returns this vector minus the specified vector v.
   * 
   * @param v the vector to be subtracted from this vector
   * @return this vector minus the specified vector v
   */
  public final Vector minus(final Vector v) {
    final Vector sub = new Vector(elements.length);
    for(int i = 0; i < elements.length; i++) {
      sub.elements[i] = elements[i] - v.elements[i];
    }
    return sub;
  }

  /**
   * Returns this vector minus the specified vector v times s.
   * 
   * @param v the vector to be subtracted from this vector
   * @param s the scaling factor
   * @return this vector minus the specified vector v
   */
  public final Vector minusTimes(final Vector v, final double s) {
    final Vector sub = new Vector(elements.length);
    for(int i = 0; i < elements.length; i++) {
      sub.elements[i] = elements[i] - v.elements[i] * s;
    }
    return sub;
  }

  /**
   * A = A - B
   * 
   * @param B another matrix
   * @return A - B in this Matrix
   */
  public final Vector minusEquals(final Vector B) {
    assert (this.elements.length == B.elements.length) : "Vector dimensions must agree.";
    for(int i = 0; i < elements.length; i++) {
      elements[i] -= B.elements[i];
    }
    return this;
  }

  /**
   * A = A - s * B
   * 
   * @param B another matrix
   * @param s Scalar
   * @return A - s * B in this Matrix
   */
  public final Vector minusTimesEquals(final Vector B, final double s) {
    assert (this.elements.length == B.elements.length) : "Vector dimensions must agree.";
    for(int i = 0; i < elements.length; i++) {
      elements[i] -= s * B.elements[i];
    }
    return this;
  }

  /**
   * Subtract a constant value from all dimensions.
   * 
   * @param d Value to subtract
   * @return Modified vector
   */
  public final Vector minusEquals(final double d) {
    for(int i = 0; i < elements.length; i++) {
      elements[i] -= d;
    }
    return this;
  }

  /**
   * Returns a new vector which is the result of this vector multiplied by the
   * specified scalar.
   * 
   * @param s the scalar to be multiplied
   * @return the resulting vector
   */
  public final Vector times(final double s) {
    final Vector v = new Vector(elements.length);
    for(int i = 0; i < elements.length; i++) {
      v.elements[i] = elements[i] * s;
    }
    return v;
  }

  /**
   * Multiply a matrix by a scalar in place, A = s*A
   * 
   * @param s scalar
   * @return replace A by s*A
   */
  public final Vector timesEquals(final double s) {
    for(int i = 0; i < elements.length; i++) {
      elements[i] *= s;
    }
    return this;
  }

  /**
   * Linear algebraic matrix multiplication, A * B
   * 
   * @param B another matrix
   * @return Matrix product, A * B
   */
  public final Matrix times(final Matrix B) {
    assert (B.elements.length == 1) : "Matrix inner dimensions must agree.";
    final Matrix X = new Matrix(this.elements.length, B.columndimension);
    for(int j = 0; j < B.columndimension; j++) {
      for(int i = 0; i < this.elements.length; i++) {
        X.elements[i][j] = elements[i] * B.elements[0][j];
      }
    }
    return X;
  }

  /**
   * Linear algebraic matrix multiplication, A<sup>T</sup> * B
   * 
   * @param B another matrix
   * @return Matrix product, A<sup>T</sup> * B
   */
  public final Matrix transposeTimes(final Matrix B) {
    assert (B.elements.length == this.elements.length) : "Matrix inner dimensions must agree.";
    final Matrix X = new Matrix(1, B.columndimension);
    for(int j = 0; j < B.columndimension; j++) {
      // multiply it with each row from A
      double s = 0;
      for(int k = 0; k < this.elements.length; k++) {
        s += this.elements[k] * B.elements[k][j];
      }
      X.elements[0][j] = s;
    }
    return X;
  }

  /**
   * Linear algebraic matrix multiplication, a<sup>T</sup> * B * c
   * 
   * @param B matrix
   * @param c vector on the right
   * @return Matrix product, a<sup>T</sup> * B
   */
  public final double transposeTimesTimes(final Matrix B, final Vector c) {
    assert (B.elements.length == this.elements.length) : "Matrix inner dimensions must agree.";
    double sum = 0.0;
    for(int j = 0; j < B.columndimension; j++) {
      // multiply it with each row from A
      double s = 0;
      for(int k = 0; k < this.elements.length; k++) {
        s += this.elements[k] * B.elements[k][j];
      }
      sum += s * c.elements[j];
    }
    return sum;
  }

  /**
   * Linear algebraic matrix multiplication, A<sup>T</sup> * B
   * 
   * @param B another vector
   * @return Matrix product, A<sup>T</sup> * B
   */
  public final double transposeTimes(final Vector B) {
    assert (B.elements.length == this.elements.length) : "Matrix inner dimensions must agree.";
    double s = 0;
    for(int k = 0; k < this.elements.length; k++) {
      s += this.elements[k] * B.elements[k];
    }
    return s;
  }

  /**
   * Linear algebraic matrix multiplication, A * B^T
   * 
   * @param B another matrix
   * @return Matrix product, A * B^T
   */
  public final Matrix timesTranspose(final Matrix B) {
    assert (B.columndimension == 1) : "Matrix inner dimensions must agree.";
    final Matrix X = new Matrix(this.elements.length, B.elements.length);
    for(int j = 0; j < B.elements.length; j++) {
      for(int i = 0; i < this.elements.length; i++) {
        X.elements[i][j] = elements[i] * B.elements[j][0];
      }
    }
    return X;
  }

  /**
   * Linear algebraic matrix multiplication, A * B^T
   * 
   * @param B another matrix
   * @return Matrix product, A * B^T
   */
  public final Matrix timesTranspose(final Vector B) {
    final Matrix X = new Matrix(this.elements.length, B.elements.length);
    for(int j = 0; j < B.elements.length; j++) {
      for(int i = 0; i < this.elements.length; i++) {
        X.elements[i][j] = elements[i] * B.elements[j];
      }
    }
    return X;
  }

  /**
   * Returns the scalar product of this vector and the specified vector v.
   * 
   * @param v the vector
   * @return double the scalar product of this vector and v
   */
  public final double scalarProduct(final Vector v) {
    assert (this.elements.length == v.elements.length) : "Vector dimensions must agree.";
    double scalarProduct = 0.0;
    for(int row = 0; row < elements.length; row++) {
      scalarProduct += elements[row] * v.elements[row];
    }
    return scalarProduct;
  }

  /**
   * Returns the length of this vector.
   * 
   * @return the length of this vector
   */
  public final double euclideanLength() {
    // Find maximum
    double max = 0.0;
    for(int row = 0; row < elements.length; row++) {
      if(elements[row] < 0) {
        if(max < -elements[row]) {
          max = -elements[row];
        }
      }
      else {
        if(max < elements[row]) {
          max = elements[row];
        }
      }
    }
    if(max <= 0.0) {
      return 0.0;
    }
    // Accumulate scaled values, for a reduced loss in precision
    double acc = 0.0;
    for(int row = 0; row < elements.length; row++) {
      final double v = elements[row] / max;
      acc += v * v;
    }
    return max * Math.sqrt(acc);
  }

  /**
   * Normalizes this vector to the length of 1.0.
   */
  public final Vector normalize() {
    double norm = euclideanLength();
    if(norm != 0) {
      for(int row = 0; row < elements.length; row++) {
        elements[row] /= norm;
      }
    }
    return this;
  }

  /**
   * Projects this row vector into the subspace formed by the specified matrix
   * v.
   * 
   * @param v the subspace matrix
   * @return the projection of p into the subspace formed by v
   */
  public final Vector projection(final Matrix v) {
    assert (elements.length == v.elements.length) : "p and v differ in row dimensionality!";
    Vector sum = new Vector(elements.length);
    for(int i = 0; i < v.columndimension; i++) {
      // TODO: optimize - copy less?
      Vector v_i = v.getCol(i);
      sum.plusTimesEquals(v_i, scalarProduct(v_i));
    }
    return sum;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(this.elements);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null) {
      return false;
    }
    if(getClass() != obj.getClass()) {
      return false;
    }
    final Vector other = (Vector) obj;
    if(this.elements.length != other.elements.length) {
      return false;
    }
    return Arrays.equals(this.elements, other.elements);
  }

  /**
   * Returns a string representation of this vector.
   * 
   * @return a string representation of this vector.
   */
  @Override
  public final String toString() {
    return FormatUtil.format(this);
  }

  /**
   * Returns a string representation of this vector without adding extra
   * whitespace
   * 
   * @return a string representation of this vector.
   */
  public final String toStringNoWhitespace() {
    return "[" + FormatUtil.format(elements, ",") + "]";
  }

  /**
   * Reset the Vector to 0.
   */
  public void setZero() {
    Arrays.fill(elements, 0.0);
  }

  /**
   * Rotate vector by 90 degrees.
   * 
   * @return self, for operation chaining.
   */
  public Vector rotate90Equals() {
    assert (elements.length == 2);
    double temp = elements[0];
    elements[0] = elements[1];
    elements[1] = -temp;
    return this;
  }

  // ////// NumberVector API. A bit hackish. :-(

  @Override
  public double getMin(int dimension) {
    return elements[dimension - 1];
  }

  @Override
  public double getMax(int dimension) {
    return elements[dimension - 1];
  }

  @Override
  public Double getValue(int dimension) {
    return elements[dimension - 1];
  }

  @Override
  public double doubleValue(int dimension) {
    return elements[dimension - 1];
  }

  @Override
  public float floatValue(int dimension) {
    return (float) elements[dimension - 1];
  }

  @Override
  public int intValue(int dimension) {
    return (int) elements[dimension - 1];
  }

  @Override
  public long longValue(int dimension) {
    return (long) elements[dimension - 1];
  }

  @Override
  public short shortValue(int dimension) {
    return (short) elements[dimension - 1];
  }

  @Override
  public byte byteValue(int dimension) {
    return (byte) elements[dimension - 1];
  }

  @Override
  public Vector getColumnVector() {
    return this;
  }

  @Override
  public Vector newNumberVector(double[] values) {
    return new Vector(values);
  }

  @Override
  public <A> Vector newNumberVector(A array, NumberArrayAdapter<?, A> adapter) {
    double[] raw = new double[adapter.size(array)];
    for(int i = 0; i < raw.length; i++) {
      raw[i] = adapter.getDouble(array, i);
    }
    return new Vector(raw);
  }

  @Override
  public <A> Vector newFeatureVector(A array, ArrayAdapter<Double, A> adapter) {
    if(adapter instanceof NumberArrayAdapter) {
      return newNumberVector(array, (NumberArrayAdapter<?, A>) adapter);
    }
    double[] raw = new double[adapter.size(array)];
    for(int i = 0; i < raw.length; i++) {
      raw[i] = adapter.get(array, i);
    }
    return new Vector(raw);
  }
}