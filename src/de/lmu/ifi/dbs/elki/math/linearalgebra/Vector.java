package de.lmu.ifi.dbs.elki.math.linearalgebra;

import java.text.NumberFormat;

import de.lmu.ifi.dbs.elki.utilities.FormatUtil;

/**
 * Provides a vector object that encapsulates an m x 1 - matrix object.
 *
 * @author Elke Achtert 
 */
@SuppressWarnings("serial")
public class Vector extends Matrix {
  /**
   * Construct a vector from a given array.
   *
   * @param values array of doubles
   */
  public Vector(double[] values) {
    super(values, values.length);
  }

  /**
   * Provides an m x 1 vector.
   *
   * @param m the number of rows
   */
  public Vector(int m) {
    super(m, 1);
  }

  /**
   * Returns the value at the specified row.
   *
   * @param i the row index
   * @return the value at row i
   */
  public double get(int i) {
    return get(i, 0);
  }

  /**
   * Sets the value at the specified row.
   *
   * @param i     the row index
   * @param value the value to be set
   */
  public void set(int i, double value) {
    super.set(i, 0, value);
  }

  /**
   * Inverts every element of the vector.
   *
   * @return the resulting vector
   */
  public Vector inverseVector() {
    Vector inv = new Vector(getRowDimensionality());
    for (int i = 0; i < getRowDimensionality(); i++) {
      inv.set(i, 1.0 / get(i));
    }
    return inv;
  }

  /**
   * Square roots every element of the vector.
   *
   * @return the resulting vector
   */
  public Vector sqrtVector() {
    Vector sqrt = new Vector(getRowDimensionality());
    for (int i = 0; i < getRowDimensionality(); i++) {
      sqrt.set(i, Math.sqrt(get(i)));
    }
    return sqrt;
  }

  /**
   * Returns this vector minus the specified vector v.
   *
   * @param v the vector to be subtracted from this vector
   * @return this vector minus the specified vector v
   */
  public Vector minus(Vector v) {
    Vector sub = new Vector(getRowDimensionality());
    for (int i = 0; i < getRowDimensionality(); i++) {
      sub.set(i, get(i) - v.get(i));
    }
    return sub;
  }

  /**
   * Returns the scalar product of this vector
   * and the specified vector v.
   *
   * @param v the vector
   * @return double the scalar product of this vector and v
   */
  public double scalarProduct(Vector v) {
    return super.scalarProduct(0, v, 0);
  }

  /**
   * Returns the length of this vector.
   *
   * @return the length of this vector
   */
  public double length() {
    return Math.sqrt(scalarProduct(this));
  }

  /**
   * Returns the dimensionality of this vector.
   *
   * @return the dimensionality of this vector
   */
  public int getDimensionality() {
    return getRowDimensionality();
  }

  /**
   * Normalizes this vector to the length of 1.0.
   */
  public void normalize() {
    super.normalizeColumns();
  }

  /**
   * Returns a new vector which is the result of this vector
   * plus the specified vector.
   *
   * @param v the vector to be added
   * @return the resulting vector
   */
  public Vector plus(Vector v) {
    checkDimensions(v);
    Vector result = new Vector(getDimensionality());
    for (int i = 0; i < getDimensionality(); i++) {
      result.set(i, get(i) + v.get(i));
    }
    return result;
  }



  /**
   * Returns a new vector which is the result of this vector
   * multiplied by the specified scalar.
   *
   * @param s the scalar to be multiplied
   * @return the resulting vector
   */
  @Override
  public Vector times(double s) {
    Vector v = new Vector(getDimensionality());
    for (int i = 0; i < getDimensionality(); i++) {
      v.set(i, get(i)*s);
    }
    return v;
  }


  /**
   * Returns a randomly created vector of length 1.0
   *
   * @param dimensionality
   * @return the dimensionality of the vector
   */
  public static Vector randomNormalizedVector(int dimensionality) {
    Vector v = new Vector(dimensionality);
    for (int i = 0; i < dimensionality; i++) {
      v.set(i, Math.random());
    }
    v.normalize();
    return v;
  }

  /**
   * Returns the ith unit vector of the specified dimensionality.
   *
   * @param dimensionality the dimensionality of the vector
   * @param i              the index
   * @return the ith unit vector of the specified dimensionality
   */
  public static Vector unitVector(int dimensionality, int i) {
    Vector v = new Vector(dimensionality);
    v.set(i, 1);
    return v;
  }

  /**
   * Returns a copy of this vector.
   *
   * @return a copy of this vector
   */
  @Override
  public Vector copy() {
    return new Vector(this.getRowPackedCopy().clone());
  }

  /**
   * Returns a string representation of this vector.
   *
   * @return a string representation of this vector.
   */
  @Override
  public String toString() {
    return "[" + FormatUtil.format(getColumnPackedCopy()) + "]";
  }

  /**
   * Returns a string representation of this vector.
   *
   * @param nf a NumberFormat to specify the output precision
   * @return a string representation of this vector.
   */
  @Override
  public String toString(NumberFormat nf) {
    return "[" + FormatUtil.format(getColumnPackedCopy(), nf) + "]";
  }

  /**
   * Check if this.getDimensionality() == v.getDimensionality().
   *
   * @throws IllegalArgumentException if the dimensions do not agree
   */
  private void checkDimensions(Vector v) {
    if (this.getDimensionality() != v.getDimensionality())
      throw new IllegalArgumentException("Vector dimensions must agree.");
  }
}
