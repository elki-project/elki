package de.lmu.ifi.dbs.elki.math.linearalgebra;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.io.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;

/**
 * A mathematical vector object, along with mathematical operations.
 *
 * @author Elke Achtert
 *
 * @apiviz.landmark
 */
public class Vector implements NumberVector {
  /**
   * Static vector factory.
   */
  public static final Factory FACTORY = new Factory();

  /**
   * Serializer for up to 127 dimensions.
   */
  public static final ByteBufferSerializer<Vector> BYTE_SERIALIZER = new SmallSerializer();

  /**
   * Serializer for up to 2^15-1 dimensions.
   */
  public static final ByteBufferSerializer<Vector> SHORT_SERIALIZER = new ShortSerializer();

  /**
   * Serializer using varint encoding.
   */
  public static final ByteBufferSerializer<Vector> VARIABLE_SERIALIZER = new VariableSerializer();

  /**
   * Array for internal storage of elements.
   *
   * @serial internal array storage.
   */
  protected final double[] elements;

  /**
   * Error message (in assertions!) when vector dimensionalities do not agree.
   */
  public static final String ERR_VEC_DIMENSIONS = "Vector dimensions do not agree.";

  /**
   * Error message (in assertions!) when matrix dimensionalities do not agree.
   */
  public static final String ERR_MATRIX_INNERDIM = "Matrix inner dimensions do not agree.";

  /**
   * Error message (in assertions!) when dimensionalities do not agree.
   */
  private static final String ERR_DIMENSIONS = "Dimensionalities do not agree.";

  /**
   * Construct a vector from a given array.
   *
   * @param values array of doubles
   */
  public Vector(final double... values) {
    elements = values;
  }

  /**
   * Constructor
   *
   * @param m the number of rows
   */
  public Vector(final int m) {
    elements = new double[m];
  }

  /**
   * Returns a randomly created vector of length 1.0.
   *
   * @param dimensionality dimensionality
   * @return the dimensionality of the vector
   */
  public static final Vector randomNormalizedVector(final int dimensionality) {
    final Vector v = new Vector(dimensionality);
    double norm = 0;
    while(norm <= 0) {
      for(int i = 0; i < dimensionality; i++) {
        v.elements[i] = Math.random();
      }
      norm = v.euclideanLength();
    }
    for(int row = 0; row < dimensionality; row++) {
      v.elements[row] /= norm;
    }
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
  @Override
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
   * Copy the values of another vector into the current vector.
   *
   * @param v Other vector
   * @return This vector
   */
  public final Vector set(final Vector v) {
    assert (this.elements.length == v.elements.length) : ERR_VEC_DIMENSIONS;
    for(int i = 0; i < this.elements.length; i++) {
      this.elements[i] = v.elements[i];
    }
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
    assert (this.elements.length == v.elements.length) : ERR_VEC_DIMENSIONS;
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
    assert (this.elements.length == v.elements.length) : ERR_VEC_DIMENSIONS;
    final Vector result = new Vector(elements.length);
    for(int i = 0; i < elements.length; i++) {
      result.elements[i] = elements[i] + v.elements[i] * s;
    }
    return result;
  }

  /**
   * a = a + b.
   *
   * @param b another vector
   * @return a + b in this vector
   */
  public final Vector plusEquals(final Vector b) {
    assert (this.elements.length == b.elements.length) : ERR_VEC_DIMENSIONS;
    for(int i = 0; i < elements.length; i++) {
      elements[i] += b.elements[i];
    }
    return this;
  }

  /**
   * a = a + s * b.
   *
   * @param b another vector
   * @param s Scalar
   * @return a + s * b in this vector
   */
  public final Vector plusTimesEquals(final Vector b, final double s) {
    assert (this.elements.length == b.elements.length) : ERR_VEC_DIMENSIONS;
    for(int i = 0; i < elements.length; i++) {
      elements[i] += s * b.elements[i];
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
   * a = a - b.
   *
   * @param b another vector
   * @return a - b in this vector
   */
  public final Vector minusEquals(final Vector b) {
    assert (this.elements.length == b.elements.length) : ERR_VEC_DIMENSIONS;
    for(int i = 0; i < elements.length; i++) {
      elements[i] -= b.elements[i];
    }
    return this;
  }

  /**
   * a = a - s * b.
   *
   * @param b another vector
   * @param s Scalar
   * @return a - s * b in this vector
   */
  public final Vector minusTimesEquals(final Vector b, final double s) {
    assert (this.elements.length == b.elements.length) : ERR_VEC_DIMENSIONS;
    for(int i = 0; i < elements.length; i++) {
      elements[i] -= s * b.elements[i];
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
   * Multiply a matrix by a scalar in place, A = s*A.
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
   * Linear algebraic matrix multiplication, A * B.
   *
   * @param B another matrix
   * @return Matrix product, A * B
   */
  public final Matrix times(final Matrix B) {
    assert (B.elements.length == 1) : ERR_MATRIX_INNERDIM;
    final Matrix X = new Matrix(this.elements.length, B.columndimension);
    for(int j = 0; j < B.columndimension; j++) {
      for(int i = 0; i < this.elements.length; i++) {
        X.elements[i][j] = elements[i] * B.elements[0][j];
      }
    }
    return X;
  }

  /**
   * Linear algebraic matrix multiplication, A<sup>T</sup> * B.
   *
   * @param B another matrix
   * @return Matrix product, A<sup>T</sup> * B
   */
  public final Matrix transposeTimes(final Matrix B) {
    assert (B.elements.length == this.elements.length) : ERR_MATRIX_INNERDIM;
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
   * Linear algebraic matrix multiplication, a<sup>T</sup> * B * c.
   *
   * @param B matrix
   * @param c vector on the right
   * @return Matrix product, a<sup>T</sup> * B * c
   */
  public final double transposeTimesTimes(final Matrix B, final Vector c) {
    assert (B.elements.length == this.elements.length) : ERR_MATRIX_INNERDIM;
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
   * Linear algebraic matrix multiplication, A<sup>T</sup> * B.
   *
   * @param B another vector
   * @return Matrix product, A<sup>T</sup> * B
   */
  public final double transposeTimes(final Vector B) {
    assert (B.elements.length == this.elements.length) : ERR_MATRIX_INNERDIM;
    double s = 0;
    for(int k = 0; k < this.elements.length; k++) {
      s += this.elements[k] * B.elements[k];
    }
    return s;
  }

  /**
   * Linear algebraic matrix multiplication, A * B^T.
   *
   * @param B another matrix
   * @return Matrix product, A * B^T
   */
  public final Matrix timesTranspose(final Matrix B) {
    assert (B.columndimension == 1) : ERR_MATRIX_INNERDIM;
    final Matrix X = new Matrix(this.elements.length, B.elements.length);
    for(int j = 0; j < B.elements.length; j++) {
      for(int i = 0; i < this.elements.length; i++) {
        X.elements[i][j] = elements[i] * B.elements[j][0];
      }
    }
    return X;
  }

  /**
   * Linear algebraic matrix multiplication, A * B^T.
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
   * Returns the squared length of this vector: v^T * v.
   *
   * @return the squared length of this vector
   */
  public final double squaredEuclideanLength() {
    double acc = 0.0;
    for(int row = 0; row < elements.length; row++) {
      final double v = elements[row];
      acc += v * v;
    }
    return acc;
  }

  /**
   * Returns the length of this vector.
   *
   * @return the length of this vector
   */
  public final double euclideanLength() {
    double acc = 0.0;
    for(int row = 0; row < elements.length; row++) {
      final double v = elements[row];
      acc += v * v;
    }
    return Math.sqrt(acc);
  }

  /**
   * Normalizes this vector to the length of 1.0.
   *
   * @return this vector
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
    assert (elements.length == v.elements.length) : ERR_DIMENSIONS;
    Vector sum = new Vector(elements.length);
    for(int i = 0; i < v.columndimension; i++) {
      // TODO: optimize - copy less?
      Vector v_i = v.getCol(i);
      sum.plusTimesEquals(v_i, this.transposeTimes(v_i));
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
   * whitespace.
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

  /**
   * Cross product for 3d vectors, i.e. <code>this x other</code>
   *
   * @param other Other vector
   * @return Cross product of this vector and the other vector
   */
  public Vector cross3D(Vector other) {
    assert (elements.length == 3 && other.elements.length == 3);
    Vector out = new Vector(3);
    out.elements[0] = (elements[1] * other.elements[2]) - (elements[2] * other.elements[1]);
    out.elements[1] = (elements[2] * other.elements[0]) - (elements[0] * other.elements[2]);
    out.elements[2] = (elements[0] * other.elements[1]) - (elements[1] * other.elements[0]);
    return out;
  }

  // ////// NumberVector API. A bit hackish. :-(

  @Override
  public double getMin(int dimension) {
    return elements[dimension];
  }

  @Override
  public double getMax(int dimension) {
    return elements[dimension];
  }

  @Override
  @Deprecated
  public Double getValue(int dimension) {
    return Double.valueOf(elements[dimension]);
  }

  @Override
  public double doubleValue(int dimension) {
    return elements[dimension];
  }

  @Override
  public float floatValue(int dimension) {
    return (float) elements[dimension];
  }

  @Override
  public int intValue(int dimension) {
    return (int) elements[dimension];
  }

  @Override
  public long longValue(int dimension) {
    return (long) elements[dimension];
  }

  @Override
  public short shortValue(int dimension) {
    return (short) elements[dimension];
  }

  @Override
  public byte byteValue(int dimension) {
    return (byte) elements[dimension];
  }

  @Override
  public Vector getColumnVector() {
    return copy();
  }

  /**
   * Vector factory for Vectors.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  private static class Factory implements NumberVector.Factory<Vector> {
    @Override
    public <A> Vector newFeatureVector(A array, ArrayAdapter<? extends Number, A> adapter) {
      if(adapter instanceof NumberArrayAdapter) {
        return new Vector(ArrayLikeUtil.toPrimitiveDoubleArray(array, (NumberArrayAdapter<?, ? super A>) adapter));
      }
      double[] data = new double[adapter.size(array)];
      for(int i = 0; i < data.length; i++) {
        data[i] = adapter.get(array, i).doubleValue();
      }
      return new Vector(data);
    }

    @Override
    public ByteBufferSerializer<Vector> getDefaultSerializer() {
      return VARIABLE_SERIALIZER;
    }

    @Override
    public Class<? super Vector> getRestrictionClass() {
      return Vector.class;
    }

    @Override
    public Vector newNumberVector(double[] values) {
      return new Vector(values.clone());
    }

    @Override
    public <A> Vector newNumberVector(A array, NumberArrayAdapter<?, ? super A> adapter) {
      return new Vector(ArrayLikeUtil.toPrimitiveDoubleArray(array, adapter));
    }

    @Override
    public Vector newNumberVector(NumberVector values) {
      return new Vector(ArrayLikeUtil.toPrimitiveDoubleArray(values));
    }
  }

  /**
   * Serialization class for dense double vectors with up to 127 dimensions, by
   * using a byte for storing the dimensionality.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class SmallSerializer implements ByteBufferSerializer<Vector> {
    @Override
    public Vector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final byte dimensionality = buffer.get();
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_DOUBLE * dimensionality);
      final double[] values = new double[dimensionality];
      for(int i = 0; i < dimensionality; i++) {
        values[i] = buffer.getDouble();
      }
      return new Vector(values);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, Vector vec) throws IOException {
      assert (vec.elements.length < Byte.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Byte.MAX_VALUE + "!";
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_DOUBLE * vec.elements.length);
      buffer.put((byte) vec.elements.length);
      for(int i = 0; i < vec.elements.length; i++) {
        buffer.putDouble(vec.elements[i]);
      }
    }

    @Override
    public int getByteSize(Vector vec) {
      assert (vec.elements.length < Byte.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Byte.MAX_VALUE + "!";
      return ByteArrayUtil.SIZE_BYTE + ByteArrayUtil.SIZE_DOUBLE * vec.getDimensionality();
    }
  }

  /**
   * Serialization class for dense double vectors with up to
   * {@link Short#MAX_VALUE} dimensions, by using a short for storing the
   * dimensionality.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class ShortSerializer implements ByteBufferSerializer<Vector> {
    @Override
    public Vector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final short dimensionality = buffer.getShort();
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_DOUBLE * dimensionality);
      final double[] values = new double[dimensionality];
      for(int i = 0; i < dimensionality; i++) {
        values[i] = buffer.getDouble();
      }
      return new Vector(values);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, Vector vec) throws IOException {
      assert (vec.elements.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_DOUBLE * vec.elements.length);
      buffer.putShort((short) vec.elements.length);
      for(int i = 0; i < vec.elements.length; i++) {
        buffer.putDouble(vec.elements[i]);
      }
    }

    @Override
    public int getByteSize(Vector vec) {
      assert (vec.elements.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      return ByteArrayUtil.SIZE_SHORT + ByteArrayUtil.SIZE_DOUBLE * vec.getDimensionality();
    }
  }

  /**
   * Serialization class for variable dimensionality by using VarInt encoding.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  protected static class VariableSerializer implements ByteBufferSerializer<Vector> {
    @Override
    public Vector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final int dimensionality = ByteArrayUtil.readUnsignedVarint(buffer);
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_DOUBLE * dimensionality);
      final double[] values = new double[dimensionality];
      for(int i = 0; i < dimensionality; i++) {
        values[i] = buffer.getDouble();
      }
      return new Vector(values);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, Vector vec) throws IOException {
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_DOUBLE * vec.elements.length);
      ByteArrayUtil.writeUnsignedVarint(buffer, vec.elements.length);
      for(int i = 0; i < vec.elements.length; i++) {
        buffer.putDouble(vec.elements[i]);
      }
    }

    @Override
    public int getByteSize(Vector vec) {
      return ByteArrayUtil.getUnsignedVarintSize(vec.elements.length) + ByteArrayUtil.SIZE_DOUBLE * vec.elements.length;
    }
  }
}
