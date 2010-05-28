package de.lmu.ifi.dbs.elki.math.linearalgebra;


/**
 * Interface for Matrix-Like objects, where M is their own type.
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
  public void set(int i, int j, double s);

  /**
   * Increments a single element.
   * 
   * @param i the row index
   * @param j the column index
   * @param s the increment value: A(i,j) = A(i.j) + s.
   * @throws ArrayIndexOutOfBoundsException on bounds error
   */
  public void increment(int i, int j, double s);

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
  public MatrixLike<?> transpose();

  /**
   * One norm
   * 
   * @return maximum column sum.
   */
  public double norm1();

  /**
   * Infinity norm
   * 
   * @return maximum row sum.
   */
  public double normInf();

  /**
   * Frobenius norm
   * 
   * @return sqrt of sum of squares of all elements.
   */
  public double normF();

  /**
   * C = A + B
   * 
   * @param B another matrix
   * @return A + B in a new Matrix
   */
  public M plus(M B);

  /**
   * A = A + B
   * 
   * @param B another matrix
   * @return A + B in this Matrix
   */
  public M plusEquals(M B);

  /**
   * C = A - B
   * 
   * @param B another matrix
   * @return A - B in a new Matrix
   */
  public M minus(M B);

  /**
   * A = A - B
   * 
   * @param B another matrix
   * @return A - B in this Matrix
   */
  public M minusEquals(M B);

  /**
   * Element-by-element multiplication, C = A.*B
   * 
   * @param B another matrix
   * @return A.*B
   */
  public M arrayTimes(M B);

  /**
   * Element-by-element multiplication in place, A = A.*B
   * 
   * @param B another matrix
   * @return A.*B
   */
  public M arrayTimesEquals(M B);

  /**
   * Element-by-element right division, C = A./B
   * 
   * @param B another matrix
   * @return A./B
   */
  public M arrayRightDivide(M B);

  /**
   * Element-by-element right division in place, A = A./B
   * 
   * @param B another matrix
   * @return A./B
   */
  public M arrayRightDivideEquals(M B);

  /**
   * Element-by-element left division, C = A.\B
   * 
   * @param B another matrix
   * @return A.\B
   */
  public M arrayLeftDivide(M B);

  /**
   * Element-by-element left division in place, A = A.\B
   * 
   * @param B another matrix
   * @return A.\B
   */
  public M arrayLeftDivideEquals(M B);

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

  /**
   * Returns the dimensionality of this matrix as a string.
   * 
   * @return the dimensionality of this matrix as a string
   */
  public String dimensionInfo();

  /**
   * Compare two matrices with a delta parameter to take numerical errors into
   * account.
   * 
   * @param obj other object to compare with
   * @param maxdelta maximum delta allowed
   * @return true if delta smaller than maximum
   */
  public boolean almostEquals(Object obj, double maxdelta);

  /**
   * Compare two matrices with a delta parameter to take numerical errors into
   * account.
   * 
   * @param obj other object to compare with
   * @return almost equals with delta {@link #DELTA}
   */
  public boolean almostEquals(Object obj);
}