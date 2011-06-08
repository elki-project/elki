package de.lmu.ifi.dbs.elki.math.linearalgebra;

/**
 * Common Interface for Matrix and Vector objects, where M is the actual type.
 * 
 * The type M guarantees type safety for many operations.
 * 
 * @param M the actual type
 * 
 * @author Elke Achtert
 * @author Erich Schubert
 */
public interface MatrixLike<M extends MatrixLike<M>> extends Cloneable {
  /**
   * Make a deep copy of a matrix.
   * 
   * @return a new matrix containing the same values as this matrix
   */
  public M copy();

  /**
   * Clone the Matrix object.
   */
  public Object clone();

  /**
   * Returns the dimensionality of the rows of this matrix.
   * 
   * @return m, the number of rows.
   */
  public int getRowDimensionality();

  /**
   * Returns the dimensionality of the columns of this matrix.
   * 
   * @return n, the number of columns.
   */
  public int getColumnDimensionality();

  /**
   * Get a single element.
   * 
   * @param i Row index.
   * @param j Column index.
   * @return A(i,j)
   * @throws ArrayIndexOutOfBoundsException on bounds error
   */
  public double get(int i, int j);

  /**
   * Set a single element.
   * 
   * @param i Row index.
   * @param j Column index.
   * @param s A(i,j).
   * @throws ArrayIndexOutOfBoundsException on bounds error
   */
  public M set(int i, int j, double s);

  /**
   * Increments a single element.
   * 
   * @param i the row index
   * @param j the column index
   * @param s the increment value: A(i,j) = A(i.j) + s.
   * @throws ArrayIndexOutOfBoundsException on bounds error
   */
  public M increment(int i, int j, double s);

  /**
   * Returns the <code>i</code>th column of this matrix as vector.
   * 
   * @param i the index of the column to be returned
   * @return the <code>i</code>th column of this matrix
   */
  public Vector getColumnVector(int i);

  /**
   * Matrix transpose.
   * 
   * @return A<sup>T</sup>
   */
  public Matrix transpose();

  /**
   * C = A + B
   * 
   * @param B another matrix
   * @return A + B in a new Matrix
   */
  public M plus(M B);

  /**
   * C = A + s*B
   * 
   * @param B another matrix
   * @param s scalar
   * @return A + s*B in a new Matrix
   */
  public M plusTimes(M B, double s);

  /**
   * A = A + B
   * 
   * @param B another matrix
   * @return A + B in this Matrix
   */
  public M plusEquals(M B);

  /**
   * C = A + s*B
   * 
   * @param B another matrix
   * @param s scalar
   * @return A + s*B in this Matrix
   */
  public M plusTimesEquals(M B, double s);

  /**
   * C = A - B
   * 
   * @param B another matrix
   * @return A - B in a new Matrix
   */
  public M minus(M B);

  /**
   * C = A - s*B
   * 
   * @param B another matrix
   * @param s Scalar
   * @return A - s*B in a new Matrix
   */
  public M minusTimes(M B, double s);

  /**
   * A = A - B
   * 
   * @param B another matrix
   * @return A - B in this Matrix
   */
  public M minusEquals(M B);

  /**
   * C = A - s*B
   * 
   * @param B another matrix
   * @param s Scalar
   * @return A - s*B in a new Matrix
   */
  public M minusTimesEquals(M B, double s);

  /**
   * Multiply a matrix by a scalar, C = s*A
   * 
   * @param s scalar
   * @return s*A
   */
  public M times(double s);

  /**
   * Multiply a matrix by a scalar in place, A = s*A
   * 
   * @param s scalar
   * @return replace A by s*A
   */
  public M timesEquals(double s);
}