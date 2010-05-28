package de.lmu.ifi.dbs.elki.math.linearalgebra;

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;

/**
 * The Matrix Class represents real-valued matrices.
 * <p/>
 * For a Matrix {@code M} we have therefore {@code M &isin;$real;<sup>m &times;
 * n</sup>}, where {@code m} and {@code n} are the number of rows and columns,
 * respectively.
 */
public abstract class AbstractMatrixLike<M extends AbstractMatrixLike<M>> implements MatrixLike<M>, Cloneable, java.io.Serializable {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 2L;

  /**
   * A small number to handle numbers near 0 as 0.
   */
  public static final double DELTA = 1E-3;
  
  /**
   * Clone the Matrix object.
   */
  @Override
  public Object clone() {
    return this.copy();
  }

  /**
   * One norm
   * 
   * @return maximum column sum.
   */
  public double norm1() {
    double f = 0;
    for(int j = 0; j < getColumnDimensionality(); j++) {
      double s = 0;
      for(int i = 0; i < getRowDimensionality(); i++) {
        s += Math.abs(get(i, j));
      }
      f = Math.max(f, s);
    }
    return f;
  }

  /**
   * Infinity norm
   * 
   * @return maximum row sum.
   */
  public double normInf() {
    double f = 0;
    for(int i = 0; i < getRowDimensionality(); i++) {
      double s = 0;
      for(int j = 0; j < getColumnDimensionality(); j++) {
        s += Math.abs(get(i, j));
      }
      f = Math.max(f, s);
    }
    return f;
  }

  /**
   * Frobenius norm
   * 
   * @return sqrt of sum of squares of all elements.
   */
  public double normF() {
    double f = 0;
    for(int i = 0; i < getRowDimensionality(); i++) {
      for(int j = 0; j < getColumnDimensionality(); j++) {
        f = MathUtil.hypotenuse(f, get(i, j));
      }
    }
    return f;
  }

  /**
   * C = A + B
   * 
   * @param B another matrix
   * @return A + B in a new Matrix
   */
  public M plus(M B) {
    return copy().plusEquals(B);
  }

  /**
   * C = A - B
   * 
   * @param B another matrix
   * @return A - B in a new Matrix
   */
  public M minus(M B) {
    return copy().minusEquals(B);
  }

  /**
   * Element-by-element multiplication, C = A.*B
   * 
   * @param B another matrix
   * @return A.*B
   */
  public M arrayTimes(M B) {
    return copy().arrayTimesEquals(B);
  }

  /**
   * Element-by-element right division, C = A./B
   * 
   * @param B another matrix
   * @return A./B
   */
  public M arrayRightDivide(M B) {
    return copy().arrayRightDivide(B);
  }

  /**
   * Element-by-element left division, C = A.\B
   * 
   * @param B another matrix
   * @return A.\B
   */
  public M arrayLeftDivide(M B) {
    return copy().arrayLeftDivide(B);
  }

  /**
   * Multiply a matrix by a scalar, C = s*A
   * 
   * @param s scalar
   * @return s*A
   */
  public M times(double s) {
    return copy().timesEquals(s);
  }

  /**
   * Check if size(A) == size(B) *
   */
  protected void checkMatrixDimensions(MatrixLike<?> B) {
    if(B.getRowDimensionality() != getRowDimensionality() || B.getColumnDimensionality() != getColumnDimensionality()) {
      throw new IllegalArgumentException("Matrix dimensions must agree.");
    }
  }

  /**
   * toString returns String-representation of Matrix.
   */
  @Override
  public String toString() {
    return FormatUtil.format(this);
  }

  /**
   * Returns the dimensionality of this matrix as a string.
   * 
   * @return the dimensionality of this matrix as a string
   */
  public String dimensionInfo() {
    return getRowDimensionality() + " x " + getColumnDimensionality();
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
    final MatrixLike<?> other = (MatrixLike<?>) obj;
    if(this.getRowDimensionality() != other.getRowDimensionality()) {
      return false;
    }
    if(this.getColumnDimensionality() != other.getColumnDimensionality()) {
      return false;
    }
    for(int i = 0; i < this.getRowDimensionality(); i++) {
      for(int j = 0; j < this.getColumnDimensionality(); j++) {
        if(this.get(i, j) != other.get(i, j)) {
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
   * @param obj other object to compare with
   * @param maxdelta maximum delta allowed
   * @return true if delta smaller than maximum
   */
  public boolean almostEquals(Object obj, double maxdelta) {
    if(this == obj) {
      return true;
    }
    if(obj == null) {
      return false;
    }
    if(getClass() != obj.getClass()) {
      return false;
    }
    final MatrixLike<?> other = (MatrixLike<?>) obj;
    if(this.getRowDimensionality() != other.getRowDimensionality()) {
      return false;
    }
    if(this.getColumnDimensionality() != other.getColumnDimensionality()) {
      return false;
    }
    for(int i = 0; i < this.getRowDimensionality(); i++) {
      for(int j = 0; j < this.getColumnDimensionality(); j++) {
        if(Math.abs(this.get(i, j) - other.get(i, j)) > maxdelta) {
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
   * @param obj other object to compare with
   * @return almost equals with delta {@link #DELTA}
   */
  public boolean almostEquals(Object obj) {
    return almostEquals(obj, DELTA);
  }
}