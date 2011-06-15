package de.lmu.ifi.dbs.elki.math.linearalgebra;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;

/**
 * Provides a vector object that encapsulates an m x 1 - matrix object.
 * 
 * @author Elke Achtert
 */
public class Vector implements MatrixLike<Vector> {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

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
  @Override
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

  @Override
  public final int getRowDimensionality() {
    return elements.length;
  }

  @Override
  public final int getColumnDimensionality() {
    return 1;
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

  @Override
  public final double get(final int i, final int j) {
    if(j != 0) {
      throw new ArrayIndexOutOfBoundsException();
    }
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

  @Override
  public final Vector set(final int i, final int j, final double s) {
    if(j != 0) {
      throw new ArrayIndexOutOfBoundsException();
    }
    elements[i] = s;
    return this;
  }

  @Override
  public final Vector increment(final int i, final int j, final double s) {
    if(j != 0) {
      throw new ArrayIndexOutOfBoundsException();
    }
    elements[i] += s;
    return this;
  }

  @Override
  public final Vector getColumnVector(final int i) {
    if(i != 0) {
      throw new ArrayIndexOutOfBoundsException();
    }
    return this;
  }

  @Override
  public final Matrix transpose() {
    return new Matrix(this.elements, 1);
  }

  /**
   * Returns a new vector which is the result of this vector plus the specified
   * vector.
   * 
   * @param v the vector to be added
   * @return the resulting vector
   */
  @Override
  public final Vector plus(final Vector v) {
    checkDimensions(v);
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
  @Override
  public final Vector plusTimes(final Vector v, final double s) {
    checkDimensions(v);
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
  @Override
  public final Vector plusEquals(final Vector B) {
    checkDimensions(B);
    for(int i = 0; i < elements.length; i++) {
      elements[i] += B.get(i, 0);
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
  @Override
  public final Vector plusTimesEquals(final Vector B, final double s) {
    checkDimensions(B);
    for(int i = 0; i < elements.length; i++) {
      elements[i] += s * B.get(i, 0);
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
  @Override
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
  @Override
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
  @Override
  public final Vector minusEquals(final Vector B) {
    checkDimensions(B);
    for(int i = 0; i < elements.length; i++) {
      elements[i] -= B.get(i, 0);
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
  @Override
  public final Vector minusTimesEquals(final Vector B, final double s) {
    checkDimensions(B);
    for(int i = 0; i < elements.length; i++) {
      elements[i] -= s * B.get(i, 0);
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
  @Override
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
  @Override
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
   * @throws IllegalArgumentException Matrix inner dimensions must agree.
   */
  public final Matrix times(final Matrix B) {
    if(B.elements.length != 1) {
      throw new IllegalArgumentException("Matrix inner dimensions must agree.");
    }
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
   * @throws IllegalArgumentException Matrix inner dimensions must agree.
   */
  public final Matrix transposeTimes(final Matrix B) {
    if(B.elements.length != this.elements.length) {
      throw new IllegalArgumentException("Matrix inner dimensions must agree.");
    }
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
   * Linear algebraic matrix multiplication, A<sup>T</sup> * B
   * 
   * @param B another vector
   * @return Matrix product, A<sup>T</sup> * B
   * @throws IllegalArgumentException Matrix inner dimensions must agree.
   */
  public final double transposeTimes(final Vector B) {
    if(B.elements.length != this.elements.length) {
      throw new IllegalArgumentException("Matrix inner dimensions must agree.");
    }
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
   * @throws IllegalArgumentException Matrix inner dimensions must agree.
   */
  public final Matrix timesTranspose(final Matrix B) {
    if(B.columndimension != 1) {
      throw new IllegalArgumentException("Matrix inner dimensions must agree.");
    }
    final Matrix X = new Matrix(this.elements.length, B.elements.length);
    for(int j = 0; j < B.elements.length; j++) {
      for(int i = 0; i < this.elements.length; i++) {
        X.elements[i][j] = elements[i] * B.elements[0][j];
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
    checkDimensions(v);
    double scalarProduct = 0.0;
    for(int row = 0; row < elements.length; row++) {
      final double prod = elements[row] * v.elements[row];
      scalarProduct += prod;
    }
    return scalarProduct;
  }

  /**
   * Inverts every element of the vector.
   * 
   * @return the resulting vector
   */
  public final Vector inverseVector() {
    final Vector inv = new Vector(elements.length);
    for(int i = 0; i < elements.length; i++) {
      inv.elements[i] = 1.0 / elements[i];
    }
    return inv;
  }

  /**
   * Square roots every element of the vector.
   * 
   * @return the resulting vector
   */
  public final Vector sqrtVector() {
    final Vector sqrt = new Vector(elements.length);
    for(int i = 0; i < elements.length; i++) {
      sqrt.elements[i] = Math.sqrt(elements[i]);
    }
    return sqrt;
  }

  /**
   * Returns the length of this vector.
   * 
   * @return the length of this vector
   */
  public final double euclideanLength() {
    double sqlen = 0.0;
    for(int row = 0; row < elements.length; row++) {
      sqlen += elements[row] * elements[row];
    }
    return Math.sqrt(sqlen);
  }

  /**
   * Frobenius norm
   * 
   * @return sqrt of sum of squares of all elements.
   */
  public double normF() {
    double f = 0;
    for(int i = 0; i < elements.length; i++) {
      f = MathUtil.hypotenuse(f, elements[i]);
    }
    return f;
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
   * @throws IllegalArgumentException if this matrix is no row vector, i.e. this
   *         matrix has more than one column or this matrix and v have different
   *         length of rows
   */
  public final Vector projection(final Matrix v) {
    if(elements.length != v.elements.length) {
      throw new IllegalArgumentException("p and v differ in row dimensionality!");
    }
    Vector sum = new Vector(elements.length);
    for(int i = 0; i < v.columndimension; i++) {
      // TODO: optimize - copy less.
      Vector v_i = v.getColumnVector(i);
      sum.plusEquals(v_i.times(scalarProduct(v_i)));
    }
    return sum;
  }

  /**
   * Check if this.getDimensionality() == v.getDimensionality().
   * 
   * @throws IllegalArgumentException if the dimensions do not agree
   */
  private final void checkDimensions(final Vector v) {
    if(this.elements.length != v.elements.length) {
      throw new IllegalArgumentException("Vector dimensions must agree.");
    }
  }

  @Override
  public int hashCode() {
    final int PRIME = 31;
    int result = 1;
    result = PRIME * result + Arrays.hashCode(this.elements);
    return result;
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
    for(int i = 0; i < this.elements.length; i++) {
      if(this.elements[i] != other.elements[i]) {
        return false;
      }
    }
    return true;
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
}