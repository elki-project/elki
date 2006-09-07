package de.lmu.ifi.dbs.math.linearalgebra;

import de.lmu.ifi.dbs.data.RationalNumber;
import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.Util;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * The Matrix Class provides the fundamental operations of numerical linear
 * algebra. Various constructors create Matrices from two dimensional arrays of
 * double precision floating point numbers. Various "gets" and "sets" provide
 * access to submatrices and matrix elements. Several methods implement basic
 * matrix arithmetic, including matrix addition and multiplication, matrix
 * norms, and element-by-element array operations. Methods for reading and
 * printing matrices are also included. All the operations in this version of
 * the Matrix Class involve real matrices. Complex matrices may be handled in a
 * future version. <p/> Five fundamental matrix decompositions, which consist of
 * pairs or triples of matrices, permutation vectors, and the like, produce
 * results in five decomposition classes. These decompositions are accessed by
 * the Matrix class to compute solutions of simultaneous linear equations,
 * determinants, inverses and other matrix functions. The five decompositions
 * are: <p/>
 * <UL>
 * <LI>Cholesky Decomposition of symmetric, positive definite matrices.
 * <LI>LU Decomposition of rectangular matrices.
 * <LI>QR Decomposition of rectangular matrices.
 * <LI>Singular Value Decomposition of rectangular matrices.
 * <LI>Eigenvalue Decomposition of both symmetric and nonsymmetric square
 * matrices.
 * </UL>
 * <DL>
 * <DT><B>Example of use:</B></DT>
 * <p/>
 * <DD>Solve a linear system A x = b and compute the residual norm, ||b - A
 * x||. <p/> <p/> <p/>
 * <p/>
 * <PRE>
 * <p/>
 * <p/> <p/> double[][] vals = {{1.,2.,3},{4.,5.,6.},{7.,8.,10.}}; Matrix A =
 * new Matrix(vals); Matrix b = Matrix.random(3,1); Matrix x = A.solve(b);
 * Matrix r = A.times(x).minus(b); double rnorm = r.normInf(); <p/> <p/>
 * <p/>
 * </PRE>
 * <p/>
 * <p/> <p/> </DD>
 * </DL>
 * <p/> <p/> The original package is gotten from <a
 * href="http://math.nist.gov/javanumerics/jama/">http://math.nist.gov/javanumerics/jama/</a>.
 * </p>
 *
 * @author The MathWorks, Inc. and the National Institute of Standards and
 *         Technology.
 * @author Arthur Zimek.
 * @version 5 August 1998/6 July 2003 and ongoing (AZ)
 */
@SuppressWarnings("serial")
public class Matrix extends AbstractLoggable implements Cloneable, java.io.Serializable {

  /**
   * A small number to handle numbers near 0 as 0.
   */
  public static final double DELTA = 1E-3;

  /**
   * Array for internal storage of elements.
   *
   * @serial internal array storage.
   */
  private double[][] A;

  /**
   * Row dimension.
   */
  private int m;

  /**
   * Column dimension.
   */
  private int n;

  public Matrix() {
    super(LoggingConfiguration.DEBUG);
  }

  /**
   * Construct an m-by-n matrix of zeros.
   *
   * @param m Number of rows.
   * @param n Number of colums.
   */

  public Matrix(int m, int n) {
    this();
    this.m = m;
    this.n = n;
    A = new double[m][n];
  }

  /**
   * Construct an m-by-n constant matrix.
   *
   * @param m Number of rows.
   * @param n Number of colums.
   * @param s Fill the matrix with this scalar value.
   */

  public Matrix(int m, int n, double s) {
    this();
    this.m = m;
    this.n = n;
    A = new double[m][n];
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        A[i][j] = s;
      }
    }
  }

  /**
   * Construct a matrix from a 2-D array.
   *
   * @param A Two-dimensional array of doubles.
   * @throws IllegalArgumentException All rows must have the same length
   * @see #constructWithCopy
   */

  public Matrix(double[][] A) {
    this();
    m = A.length;
    n = A[0].length;
    for (int i = 0; i < m; i++) {
      if (A[i].length != n) {
        throw new IllegalArgumentException(
            "All rows must have the same length.");
      }
    }
    this.A = A;
  }

  /**
   * Constructs a Matrix for a given matrix of RationalNumber.
   *
   * @param q an array of arrays of RationalNumbers. q is not checked for
   *          consistency (i.e. whether all rows are of equal length)
   */
  public Matrix(RationalNumber[][] q) {
    this();
    m = q.length;
    n = q[0].length;
    A = new double[m][n];
    for (int row = 0; row < q.length; row++) {
      for (int col = 0; col < q[row].length; col++) {
        A[row][col] = q[row][col].doubleValue();
      }
    }
  }

  /**
   * Construct a matrix quickly without checking arguments.
   *
   * @param A Two-dimensional array of doubles.
   * @param m Number of rows.
   * @param n Number of colums.
   */

  public Matrix(double[][] A, int m, int n) {
    this();
    this.A = A;
    this.m = m;
    this.n = n;
  }

  /**
   * Construct a matrix from a one-dimensional packed array
   *
   * @param values One-dimensional array of doubles, packed by columns (ala
   *               Fortran).
   * @param m      Number of rows.
   * @throws IllegalArgumentException Array length must be a multiple of m.
   */

  public Matrix(double values[], int m) {
    this();
    this.m = m;
    n = (m != 0 ? values.length / m : 0);
    if (m * n != values.length) {
      throw new IllegalArgumentException(
          "Array length must be a multiple of m.");
    }
    A = new double[m][n];
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        A[i][j] = values[i + j * m];
      }
    }
  }

  /**
   * Construct a matrix from a copy of a 2-D array.
   *
   * @param A Two-dimensional array of doubles.
   * @throws IllegalArgumentException All rows must have the same length
   */

  public static Matrix constructWithCopy(double[][] A) {
    int m = A.length;
    int n = A[0].length;
    Matrix X = new Matrix(m, n);
    double[][] C = X.getArray();
    for (int i = 0; i < m; i++) {
      if (A[i].length != n) {
        throw new IllegalArgumentException(
            "All rows must have the same length.");
      }
      for (int j = 0; j < n; j++) {
        C[i][j] = A[i][j];
      }
    }
    return X;
  }

  /**
   * Make a deep copy of a matrix
   */

  public Matrix copy() {
    Matrix X = new Matrix(m, n);
    double[][] C = X.getArray();
    for (int i = 0; i < m; i++) {
      System.arraycopy(A[i], 0, C[i], 0, n);
    }
    return X;
  }

  /**
   * Clone the Matrix object.
   */

  public Object clone() {
    return this.copy();
  }

  /**
   * Access the internal two-dimensional array.
   *
   * @return Pointer to the two-dimensional array of matrix elements.
   */

  public double[][] getArray() {
    return A;
  }

  /**
   * Copy the internal two-dimensional array.
   *
   * @return Two-dimensional array copy of matrix elements.
   */

  public double[][] getArrayCopy() {
    double[][] C = new double[m][n];
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        C[i][j] = A[i][j];
      }
    }
    return C;
  }

  /**
   * Make a one-dimensional column packed copy of the internal array.
   *
   * @return Matrix elements packed in a one-dimensional array by columns.
   */

  public double[] getColumnPackedCopy() {
    double[] vals = new double[m * n];
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        vals[i + j * m] = A[i][j];
      }
    }
    return vals;
  }

  /**
   * Make a one-dimensional row packed copy of the internal array.
   *
   * @return Matrix elements packed in a one-dimensional array by rows.
   */

  public double[] getRowPackedCopy() {
    double[] vals = new double[m * n];
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        vals[i * n + j] = A[i][j];
      }
    }
    return vals;
  }

  /**
   * Returns the dimensionality of the rows
   * of this matrix.
   *
   * @return m, the number of rows.
   */

  public int getRowDimensionality() {
    return m;
  }

  /**
   * Returns the dimensionality of the columns
   * of this matrix.
   *
   * @return n, the number of columns.
   */

  public int getColumnDimensionality() {
    return n;
  }

  /**
   * Get a single element.
   *
   * @param i Row index.
   * @param j Column index.
   * @return A(i,j)
   * @throws ArrayIndexOutOfBoundsException
   */

  public double get(int i, int j) {
    return A[i][j];
  }

  /**
   * Get a submatrix.
   *
   * @param i0 Initial row index
   * @param i1 Final row index
   * @param j0 Initial column index
   * @param j1 Final column index
   * @return A(i0:i1,j0:j1)
   * @throws ArrayIndexOutOfBoundsException Submatrix indices
   */
  public Matrix getMatrix(int i0, int i1, int j0, int j1) {
    Matrix X = new Matrix(i1 - i0 + 1, j1 - j0 + 1);
    double[][] B = X.getArray();
    try {
      for (int i = i0; i <= i1; i++) {
        for (int j = j0; j <= j1; j++) {
          B[i - i0][j - j0] = A[i][j];
        }
      }
    }
    catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace();
      throw new ArrayIndexOutOfBoundsException("Submatrix indices");
    }
    return X;
  }

  /**
   * Get a submatrix.
   *
   * @param r Array of row indices.
   * @param c Array of column indices.
   * @return A(r(:),c(:))
   * @throws ArrayIndexOutOfBoundsException Submatrix indices
   */

  public Matrix getMatrix(int[] r, int[] c) {
    Matrix X = new Matrix(r.length, c.length);
    double[][] B = X.getArray();
    try {
      for (int i = 0; i < r.length; i++) {
        for (int j = 0; j < c.length; j++) {
          B[i][j] = A[r[i]][c[j]];
        }
      }
    }
    catch (ArrayIndexOutOfBoundsException e) {
      throw new ArrayIndexOutOfBoundsException("Submatrix indices");
    }
    return X;
  }

  /**
   * Get a submatrix.
   *
   * @param i0 Initial row index
   * @param i1 Final row index
   * @param c  Array of column indices.
   * @return A(i0:i1,c(:))
   * @throws ArrayIndexOutOfBoundsException Submatrix indices
   */

  public Matrix getMatrix(int i0, int i1, int[] c) {
    Matrix X = new Matrix(i1 - i0 + 1, c.length);
    double[][] B = X.getArray();
    try {
      for (int i = i0; i <= i1; i++) {
        for (int j = 0; j < c.length; j++) {
          B[i - i0][j] = A[i][c[j]];
        }
      }
    }
    catch (ArrayIndexOutOfBoundsException e) {
      throw new ArrayIndexOutOfBoundsException("Submatrix indices");
    }
    return X;
  }

  /**
   * Get a submatrix.
   *
   * @param r  Array of row indices.
   * @param j0 Initial column index
   * @param j1 Final column index
   * @return A(r(:),j0:j1)
   * @throws ArrayIndexOutOfBoundsException Submatrix indices
   */

  public Matrix getMatrix(int[] r, int j0, int j1) {
    Matrix X = new Matrix(r.length, j1 - j0 + 1);
    double[][] B = X.getArray();
    try {
      for (int i = 0; i < r.length; i++) {
        for (int j = j0; j <= j1; j++) {
          B[i][j - j0] = A[r[i]][j];
        }
      }
    }
    catch (ArrayIndexOutOfBoundsException e) {
      throw new ArrayIndexOutOfBoundsException("Submatrix indices");
    }
    return X;
  }

  /**
   * Set a single element.
   *
   * @param i Row index.
   * @param j Column index.
   * @param s A(i,j).
   * @throws ArrayIndexOutOfBoundsException
   */

  public void set(int i, int j, double s) {
    A[i][j] = s;
  }

  /**
   * Increments a single element.
   *
   * @param i the row index
   * @param j the column index
   * @param s the increment value: A(i,j) = A(i.j) + s.
   * @throws ArrayIndexOutOfBoundsException
   */
  public void increment(int i, int j, double s) {
    A[i][j] += s;
  }

  /**
   * Set a submatrix.
   *
   * @param i0 Initial row index
   * @param i1 Final row index
   * @param j0 Initial column index
   * @param j1 Final column index
   * @param X  A(i0:i1,j0:j1)
   * @throws ArrayIndexOutOfBoundsException Submatrix indices
   */

  public void setMatrix(int i0, int i1, int j0, int j1, Matrix X) {
    try {
      for (int i = i0; i <= i1; i++) {
        for (int j = j0; j <= j1; j++) {
          A[i][j] = X.get(i - i0, j - j0);
        }
      }
    }
    catch (ArrayIndexOutOfBoundsException e) {
      throw new ArrayIndexOutOfBoundsException("Submatrix indices: " + e);
    }
  }

  /**
   * Set a submatrix.
   *
   * @param r Array of row indices.
   * @param c Array of column indices.
   * @param X A(r(:),c(:))
   * @throws ArrayIndexOutOfBoundsException Submatrix indices
   */

  public void setMatrix(int[] r, int[] c, Matrix X) {
    try {
      for (int i = 0; i < r.length; i++) {
        for (int j = 0; j < c.length; j++) {
          A[r[i]][c[j]] = X.get(i, j);
        }
      }
    }
    catch (ArrayIndexOutOfBoundsException e) {
      throw new ArrayIndexOutOfBoundsException("Submatrix indices");
    }
  }

  /**
   * Set a submatrix.
   *
   * @param r  Array of row indices.
   * @param j0 Initial column index
   * @param j1 Final column index
   * @param X  A(r(:),j0:j1)
   * @throws ArrayIndexOutOfBoundsException Submatrix indices
   */

  public void setMatrix(int[] r, int j0, int j1, Matrix X) {
    try {
      for (int i = 0; i < r.length; i++) {
        for (int j = j0; j <= j1; j++) {
          A[r[i]][j] = X.get(i, j - j0);
        }
      }
    }
    catch (ArrayIndexOutOfBoundsException e) {
      throw new ArrayIndexOutOfBoundsException("Submatrix indices");
    }
  }

  /**
   * Set a submatrix.
   *
   * @param i0 Initial row index
   * @param i1 Final row index
   * @param c  Array of column indices.
   * @param X  A(i0:i1,c(:))
   * @throws ArrayIndexOutOfBoundsException Submatrix indices
   */

  public void setMatrix(int i0, int i1, int[] c, Matrix X) {
    try {
      for (int i = i0; i <= i1; i++) {
        for (int j = 0; j < c.length; j++) {
          A[i][c[j]] = X.get(i - i0, j);
        }
      }
    }
    catch (ArrayIndexOutOfBoundsException e) {
      throw new ArrayIndexOutOfBoundsException("Submatrix indices");
    }
  }

  /**
   * Returns the <code>j</code>th column of this matrix.
   *
   * @param j the index of the column to be returned
   * @return the <code>j</code>th column of this matrix
   */
  public Matrix getColumn(int j) {
    return getMatrix(0, getRowDimensionality() - 1, j, j);
  }

  /**
   * Returns the <code>j</code>th column of this matrix as vector.
   *
   * @param j the index of the column to be returned
   * @return the <code>j</code>th column of this matrix
   */
  public Vector getColumnVector(int j) {
    Vector v = new Vector(this.getRowDimensionality());
    for (int i = 0; i < m; i++) {
      v.set(i, get(i, j));
    }
    return v;
  }

  /**
   * Returns the <code>i</code>th row of this matrix.
   *
   * @param i the index of the row to be returned
   * @return the <code>i</code>th row of this matrix
   */
  public Matrix getRow(int i) {
    return getMatrix(i, i, 0, getColumnDimensionality() - 1);
  }

  /**
   * Returns the <code>i</code>th row of this matrix
   * as vector.
   *
   * @param i the index of the row to be returned
   * @return the <code>i</code>th row of this matrix
   */
  public Vector getRowVector(int i) {
    double[] row = A[i].clone();
    return new Vector(row);
  }

  /**
   * Sets the <code>j</code>th column of this matrix to the specified
   * column.
   *
   * @param j      the index of the column to be set
   * @param column the value of the column to be set
   */
  public void setColumn(int j, Matrix column) {
    if (column.getColumnDimensionality() != 1)
      throw new IllegalArgumentException(
          "Matrix must consist of one column!");
    if (column.getRowDimensionality() != getRowDimensionality())
      throw new IllegalArgumentException(
          "Matrix must consist of the same no of rows!");

    setMatrix(0, getRowDimensionality() - 1, j, j, column);
  }

  /**
   * Matrix transpose.
   *
   * @return A'
   */

  public Matrix transpose() {
    Matrix X = new Matrix(n, m);
    double[][] C = X.getArray();
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        C[j][i] = A[i][j];
      }
    }
    return X;
  }

  /**
   * One norm
   *
   * @return maximum column sum.
   */
  public double norm1() {
    double f = 0;
    for (int j = 0; j < n; j++) {
      double s = 0;
      for (int i = 0; i < m; i++) {
        s += Math.abs(A[i][j]);
      }
      f = Math.max(f, s);
    }
    return f;
  }

  /**
   * Two norm
   *
   * @return maximum singular value.
   */

  public double norm2() {
    return (new SingularValueDecomposition(this).norm2());
  }

  /**
   * Infinity norm
   *
   * @return maximum row sum.
   */

  public double normInf() {
    double f = 0;
    for (int i = 0; i < m; i++) {
      double s = 0;
      for (int j = 0; j < n; j++) {
        s += Math.abs(A[i][j]);
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
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        f = Utils.hypot(f, A[i][j]);
      }
    }
    return f;
  }

  /**
   * Unary minus
   *
   * @return -A
   */

  public Matrix uminus() {
    Matrix X = new Matrix(m, n);
    double[][] C = X.getArray();
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        C[i][j] = -A[i][j];
      }
    }
    return X;
  }

  /**
   * C = A + B
   *
   * @param B another matrix
   * @return A + B
   */

  public Matrix plus(Matrix B) {
    checkMatrixDimensions(B);
    Matrix X = new Matrix(m, n);
    double[][] C = X.getArray();
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        C[i][j] = A[i][j] + B.A[i][j];
      }
    }
    return X;
  }

  /**
   * A = A + B
   *
   * @param B another matrix
   * @return A + B
   */

  public Matrix plusEquals(Matrix B) {
    checkMatrixDimensions(B);
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        A[i][j] = A[i][j] + B.A[i][j];
      }
    }
    return this;
  }

  /**
   * C = A - B
   *
   * @param B another matrix
   * @return A - B
   */

  public Matrix minus(Matrix B) {
    checkMatrixDimensions(B);
    Matrix X = new Matrix(m, n);
    double[][] C = X.getArray();
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        C[i][j] = A[i][j] - B.A[i][j];
      }
    }
    return X;
  }

  /**
   * A = A - B
   *
   * @param B another matrix
   * @return A - B
   */

  public Matrix minusEquals(Matrix B) {
    checkMatrixDimensions(B);
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        A[i][j] = A[i][j] - B.A[i][j];
      }
    }
    return this;
  }

  /**
   * Element-by-element multiplication, C = A.*B
   *
   * @param B another matrix
   * @return A.*B
   */

  public Matrix arrayTimes(Matrix B) {
    checkMatrixDimensions(B);
    Matrix X = new Matrix(m, n);
    double[][] C = X.getArray();
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        C[i][j] = A[i][j] * B.A[i][j];
      }
    }
    return X;
  }

  /**
   * Element-by-element multiplication in place, A = A.*B
   *
   * @param B another matrix
   * @return A.*B
   */

  public Matrix arrayTimesEquals(Matrix B) {
    checkMatrixDimensions(B);
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        A[i][j] = A[i][j] * B.A[i][j];
      }
    }
    return this;
  }

  /**
   * Element-by-element right division, C = A./B
   *
   * @param B another matrix
   * @return A./B
   */

  public Matrix arrayRightDivide(Matrix B) {
    checkMatrixDimensions(B);
    Matrix X = new Matrix(m, n);
    double[][] C = X.getArray();
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        C[i][j] = A[i][j] / B.A[i][j];
      }
    }
    return X;
  }

  /**
   * Element-by-element right division in place, A = A./B
   *
   * @param B another matrix
   * @return A./B
   */

  public Matrix arrayRightDivideEquals(Matrix B) {
    checkMatrixDimensions(B);
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        A[i][j] = A[i][j] / B.A[i][j];
      }
    }
    return this;
  }

  /**
   * Element-by-element left division, C = A.\B
   *
   * @param B another matrix
   * @return A.\B
   */

  public Matrix arrayLeftDivide(Matrix B) {
    checkMatrixDimensions(B);
    Matrix X = new Matrix(m, n);
    double[][] C = X.getArray();
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        C[i][j] = B.A[i][j] / A[i][j];
      }
    }
    return X;
  }

  /**
   * Element-by-element left division in place, A = A.\B
   *
   * @param B another matrix
   * @return A.\B
   */

  public Matrix arrayLeftDivideEquals(Matrix B) {
    checkMatrixDimensions(B);
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        A[i][j] = B.A[i][j] / A[i][j];
      }
    }
    return this;
  }

  /**
   * Multiply a matrix by a scalar, C = s*A
   *
   * @param s scalar
   * @return s*A
   */

  public Matrix times(double s) {
    Matrix X = new Matrix(m, n);
    double[][] C = X.getArray();
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        C[i][j] = s * A[i][j];
      }
    }
    return X;
  }

  /**
   * Multiply a matrix by a scalar in place, A = s*A
   *
   * @param s scalar
   * @return replace A by s*A
   */

  public Matrix timesEquals(double s) {
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        A[i][j] = s * A[i][j];
      }
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

  public Matrix times(Matrix B) {
    if (B.m != n) {
      throw new IllegalArgumentException(
          "Matrix inner dimensions must agree.");
    }
    Matrix X = new Matrix(m, B.n);
    double[][] C = X.getArray();
    double[] Bcolj = new double[n];
    for (int j = 0; j < B.n; j++) {
      for (int k = 0; k < n; k++) {
        Bcolj[k] = B.A[k][j];
      }
      for (int i = 0; i < m; i++) {
        double[] Arowi = A[i];
        double s = 0;
        for (int k = 0; k < n; k++) {
          s += Arowi[k] * Bcolj[k];
        }
        C[i][j] = s;
      }
    }
    return X;
  }

  /**
   * LU Decomposition
   *
   * @return LUDecomposition
   * @see LUDecomposition
   */

  public LUDecomposition lu() {
    return new LUDecomposition(this);
  }

  /**
   * QR Decomposition
   *
   * @return QRDecomposition
   * @see QRDecomposition
   */

  public QRDecomposition qr() {
    return new QRDecomposition(this);
  }

  /**
   * Cholesky Decomposition
   *
   * @return CholeskyDecomposition
   * @see CholeskyDecomposition
   */

  public CholeskyDecomposition chol() {
    return new CholeskyDecomposition(this);
  }

  /**
   * Singular Value Decomposition
   *
   * @return SingularValueDecomposition
   * @see SingularValueDecomposition
   */

  public SingularValueDecomposition svd() {
    return new SingularValueDecomposition(this);
  }

  /**
   * Eigenvalue Decomposition
   *
   * @return EigenvalueDecomposition
   * @see EigenvalueDecomposition
   */

  public EigenvalueDecomposition eig() {
    return new EigenvalueDecomposition(this);
  }

  /**
   * Solve A*X = B
   *
   * @param B right hand side
   * @return solution if A is square, least squares solution otherwise
   */

  public Matrix solve(Matrix B) {
    return (m == n ? (new LUDecomposition(this)).solve(B)
            : (new QRDecomposition(this)).solve(B));
  }

  /**
   * Solve X*A = B, which is also A'*X' = B'
   *
   * @param B right hand side
   * @return solution if A is square, least squares solution otherwise.
   */

  public Matrix solveTranspose(Matrix B) {
    return transpose().solve(B.transpose());
  }

  /**
   * Matrix inverse or pseudoinverse
   *
   * @return inverse(A) if A is square, pseudoinverse otherwise.
   */

  public Matrix inverse() {
    return solve(identity(m, m));
  }

  /**
   * Matrix determinant
   *
   * @return determinant
   */

  public double det() {
    return new LUDecomposition(this).det();
  }

  /**
   * Matrix rank
   *
   * @return effective numerical rank, obtained from SVD.
   */

  public int rank() {
    return new SingularValueDecomposition(this).rank();
  }

  /**
   * Matrix condition (2 norm)
   *
   * @return ratio of largest to smallest singular value.
   */

  public double cond() {
    return new SingularValueDecomposition(this).cond();
  }

  /**
   * Matrix trace.
   *
   * @return sum of the diagonal elements.
   */

  public double trace() {
    double t = 0;
    for (int i = 0; i < Math.min(m, n); i++) {
      t += A[i][i];
    }
    return t;
  }

  /**
   * Generate matrix with random elements
   *
   * @param m Number of rows.
   * @param n Number of colums.
   * @return An m-by-n matrix with uniformly distributed random elements.
   */

  public static Matrix random(int m, int n) {
    Matrix A = new Matrix(m, n);
    double[][] X = A.getArray();
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        X[i][j] = Math.random();
      }
    }
    return A;
  }

  /**
   * Generate identity matrix
   *
   * @param m Number of rows.
   * @param n Number of colums.
   * @return An m-by-n matrix with ones on the diagonal and zeros elsewhere.
   */

  public static Matrix identity(int m, int n) {
    Matrix A = new Matrix(m, n);
    double[][] X = A.getArray();
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        X[i][j] = (i == j ? 1.0 : 0.0);
      }
    }
    return A;
  }

  /**
   * Print the matrix to stdout. Line the elements up in columns with a
   * Fortran-like 'Fw.d' style format.
   *
   * @param w Column width.
   * @param d Number of digits after the decimal.
   */

  public void print(int w, int d) {
    print(new PrintWriter(System.out, true), w, d);
  }

  /**
   * Print the matrix to the output stream. Line the elements up in columns
   * with a Fortran-like 'Fw.d' style format.
   *
   * @param output Output stream.
   * @param w      Column width.
   * @param d      Number of digits after the decimal.
   */

  public void print(PrintWriter output, int w, int d) {
    DecimalFormat format = new DecimalFormat();
    format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
    format.setMinimumIntegerDigits(1);
    format.setMaximumFractionDigits(d);
    format.setMinimumFractionDigits(d);
    format.setGroupingUsed(false);
    print(output, format, w + 2);
  }

  /**
   * Print the matrix to stdout. Line the elements up in columns. Use the
   * format object, and right justify within columns of width characters. Note
   * that is the matrix is to be read back in, you probably will want to use a
   * NumberFormat that is set to US Locale.
   *
   * @param format A Formatting object for individual elements.
   * @param width  Field width for each column.
   * @see java.text.DecimalFormat#setDecimalFormatSymbols
   */

  public void print(NumberFormat format, int width) {
    print(new PrintWriter(System.out, true), format, width);
  }

  // DecimalFormat is a little disappointing coming from Fortran or C's
  // printf.
  // Since it doesn't pad on the left, the elements will come out different
  // widths. Consequently, we'll pass the desired column width in as an
  // argument and do the extra padding ourselves.

  /**
   * Print the matrix to the output stream. Line the elements up in columns.
   * Use the format object, and right justify within columns of width
   * characters. Note that is the matrix is to be read back in, you probably
   * will want to use a NumberFormat that is set to US Locale.
   *
   * @param output the output stream.
   * @param format A formatting object to format the matrix elements
   * @param width  Column width.
   * @see java.text.DecimalFormat#setDecimalFormatSymbols
   */

  public void print(PrintWriter output, NumberFormat format, int width) {
    output.println(); // start on new line.
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        String s = format.format(A[i][j]); // format the number
        int padding = Math.max(1, width - s.length()); // At _least_ 1
        // space
        for (int k = 0; k < padding; k++)
          output.print(' ');
        output.print(s);
      }
      output.println();
    }
    output.println(); // end with blank line.
  }

  /**
   * Read a matrix from a stream. The format is the same the print method, so
   * printed matrices can be read back in (provided they were printed using US
   * Locale). Elements are separated by whitespace, all the elements for each
   * row appear on a single line, the last row is followed by a blank line.
   *
   * @param input the input stream.
   */

  public static Matrix read(BufferedReader input) throws java.io.IOException {
    StreamTokenizer tokenizer = new StreamTokenizer(input);

    // Although StreamTokenizer will parse numbers, it doesn't recognize
    // scientific notation (E or D); however, Double.valueOf does.
    // The strategy here is to disable StreamTokenizer's number parsing.
    // We'll only get whitespace delimited words, EOL's and EOF's.
    // These words should all be numbers, for Double.valueOf to parse.

    tokenizer.resetSyntax();
    tokenizer.wordChars(0, 255);
    tokenizer.whitespaceChars(0, ' ');
    tokenizer.eolIsSignificant(true);
    java.util.Vector<Double> v = new java.util.Vector<Double>();

    // Ignore initial empty lines
    while (tokenizer.nextToken() == StreamTokenizer.TT_EOL)
      ;
    if (tokenizer.ttype == StreamTokenizer.TT_EOF)
      throw new java.io.IOException("Unexpected EOF on matrix read.");
    do {
      v.addElement(Double.valueOf(tokenizer.sval)); // Read & store 1st
      // row.
    }
    while (tokenizer.nextToken() == StreamTokenizer.TT_WORD);

    int n = v.size(); // Now we've got the number of columns!
    double row[] = new double[n];
    for (int j = 0; j < n; j++)
      // extract the elements of the 1st row.
      row[j] = v.elementAt(j);
    // v.removeAllElements();
    java.util.Vector<double[]> rowV = new java.util.Vector<double[]>();
    rowV.addElement(row); // Start storing rows instead of columns.
    while (tokenizer.nextToken() == StreamTokenizer.TT_WORD) {
      // While non-empty lines
      rowV.addElement(row = new double[n]);
      int j = 0;
      do {
        if (j >= n)
          throw new java.io.IOException("Row " + v.size()
                                        + " is too long.");
        row[j++] = (Double.valueOf(tokenizer.sval));
      }
      while (tokenizer.nextToken() == StreamTokenizer.TT_WORD);
      if (j < n)
        throw new java.io.IOException("Row " + v.size()
                                      + " is too short.");
    }
    int m = rowV.size(); // Now we've got the number of rows.
    double[][] A = new double[m][];
    rowV.copyInto(A); // copy the rows out of the vector
    return new Matrix(A);
  }

  /*
  * ------------------------ Private Methods ------------------------
  */

  /**
   * Check if size(A) == size(B) *
   */

  private void checkMatrixDimensions(Matrix B) {
    if (B.m != m || B.n != n) {
      throw new IllegalArgumentException("Matrix dimensions must agree.");
    }
  }

  /*
  * ##########################################################################################
  * additional methods by AZ
  * ##########################################################################################
  */

  /**
   * toString returns String-representation of Matrix.
   */
  public String toString() {
    StringBuffer output = new StringBuffer();
    output.append("[\n");
    for (int i = 0; i < m; i++) {
      output.append(" [");
      for (int j = 0; j < n; j++) {
        output.append(" ").append(A[i][j]);
        if (j < n - 1) {
          output.append(",");
        }
      }
      output.append(" ]\n");
    }
    output.append("]\n");

    return (output.toString());
  }

  /**
   * Returns a string representation of this matrix. In each line the
   * specified String <code>pre<\code> is prefixed.
   *
   * @param pre the prefix of each line
   * @return a string representation of this matrix
   */
  private String toString(String pre) {
    StringBuffer output = new StringBuffer();
    output.append(pre).append("[\n").append(pre);
    for (int i = 0; i < m; i++) {
      output.append(" [");
      for (int j = 0; j < n; j++) {
        output.append(" ").append(A[i][j]);
        if (j < n - 1) {
          output.append(",");
        }
      }
      output.append(" ]\n").append(pre);
    }
    output.append("]\n").append(pre);

    return (output.toString());
  }

  /**
   * returns String-representation of Matrix.
   *
   * @param nf NumberFormat to specify output precision
   * @return String representation of this Matrix in precision as specified by
   *         given NumberFormat
   */
  public String toString(NumberFormat nf) {
    int[] colMax = new int[this.getColumnDimensionality()];
    String[][] entries = new String[m][n];
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        entries[i][j] = nf.format(A[i][j]);
        if (entries[i][j].length() > colMax[j]) {
          colMax[j] = entries[i][j].length();
        }
      }
    }
    StringBuffer output = new StringBuffer();
    output.append("[\n");
    for (int i = 0; i < m; i++) {
      output.append(" [");
      for (int j = 0; j < n; j++) {
        output.append(" ");
        int space = colMax[j] - entries[i][j].length();
        for (int s = 0; s < space; s++) {
          output.append(" ");
        }
        output.append(entries[i][j]);
        if (j < n - 1) {
          output.append(",");
        }
      }
      output.append(" ]\n");
    }
    output.append("]\n");

    return (output.toString());
  }

  /**
   * Returns a string representation of this matrix. In each line the
   * specified String <code>pre<\code> is prefixed.
   *
   * @param nf  number format for output accuracy
   * @param pre the prefix of each line
   * @return a string representation of this matrix
   */
  public String toString(String pre, NumberFormat nf) {
    if (nf == null)
      return toString(pre);

    int[] colMax = new int[this.getColumnDimensionality()];
    String[][] entries = new String[m][n];
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        entries[i][j] = nf.format(A[i][j]);
        if (entries[i][j].length() > colMax[j]) {
          colMax[j] = entries[i][j].length();
        }
      }
    }
    StringBuffer output = new StringBuffer();
    output.append(pre).append("[\n").append(pre);
    for (int i = 0; i < m; i++) {
      output.append(" [");
      for (int j = 0; j < n; j++) {
        output.append(" ");
        int space = colMax[j] - entries[i][j].length();
        for (int s = 0; s < space; s++) {
          output.append(" ");
        }
        output.append(entries[i][j]);
        if (j < n - 1) {
          output.append(",");
        }
      }
      output.append(" ]\n").append(pre);
    }
    output.append("]\n").append(pre);

    return (output.toString());
  }

  /**
   * getDiagonal returns array of diagonal-elements.
   *
   * @return double[] the values on the diagonaly of the Matrix
   */
  public double[] getDiagonal() {
    double[] diagonal = new double[m];
    for (int i = 0; i < m; i++) {
      diagonal[i] = A[i][i];
    }
    return diagonal;
  }

  /**
   * Scales the columns of this matrix with the specified factor.
   *
   * @param scale the factor to scale the column
   */
  public void scaleColumns(double scale) {
    for (int j = 0; j < n; j++) {
      scaleColumn(j, scale);
    }
  }

  /**
   * Normalizes the columns of this matrix to length of 1.0.
   */
  public void normalizeColumns() {
    for (int col = 0; col < n; col++) {
      double norm = 0.0;
      for (int row = 0; row < m; row++) {
        norm = norm + (A[row][col] * A[row][col]);
      }
      norm = Math.sqrt(norm);
      if (norm != 0) {
        for (int row = 0; row < m; row++) {
          A[row][col] = (A[row][col] / norm);
        }
      }
    }
  }

  /**
   * Scales the specified column with the specified factor.
   *
   * @param j     the index of the column tp be scaled
   * @param scale the factor to scale the column
   */
  public void scaleColumn(int j, double scale) {
    for (int row = 0; row < m; row++) {
      A[row][j] = (A[row][j] * scale);
    }
  }

  /**
   * distanceCov returns distance of two Matrices A and B, i.e. the root of
   * the sum of the squared distances A<sub>ij</sub>-B<sub>ij</sub>.
   *
   * @param B Matrix to compute distance from this (A)
   * @return distance of Matrices
   */
  public double distanceCov(Matrix B) {
    double distance = 0.0;
    double[][] arrayB = B.getArray();
    for (int col = 0; col < n; col++) {
      for (int row = 0; row < m; row++) {
        double distIJ = A[row][col] - arrayB[row][col];
        distance += (distIJ * distIJ);
      }
    }
    distance = Math.sqrt(distance);
    return distance;
  }

  /**
   * Returns the angle of the colA col of this and the colB col of B.
   *
   * @param colA the column of A to compute angle for
   * @param B    second Matrix
   * @param colB the column of B to compute angle for
   * @return double angle of the first cols of this and B
   */
  public double angle(int colA, Matrix B, int colB) {
    return Math.acos(this.scalarProduct(colA, B, colB)
                     / (this.euclideanNorm(colA) * B.euclideanNorm(colB)));
  }

  /**
   * Returns the scalar product of the colA cols of this and the colB col of
   * B.
   *
   * @param colA The column of A to compute scalar product for
   * @param B    second Matrix
   * @param colB The column of B to compute scalar product for
   * @return double The scalar product of the first cols of this and B
   */
  public double scalarProduct(int colA, Matrix B, int colB) {
    double scalarProduct = 0.0;
    double[][] arrayB = B.getArray();
    for (int row = 0; row < m; row++) {
      double prod = A[row][colA] * arrayB[row][colB];
      scalarProduct += prod;
    }
    return scalarProduct;
  }

  /**
   * Returns the euclidean norm of the col column.
   *
   * @param col The column to compute euclidean norm of
   * @return double
   */
  public double euclideanNorm(int col) {
    return Math.sqrt(this.scalarProduct(col, this, col));
  }

  /**
   * Returns a quadratic Matrix consisting of zeros and of the given values on
   * the diagonal.
   *
   * @param diagonal the values on the diagonal
   * @return the resulting matrix
   */
  public static Matrix diagonal(double[] diagonal) {
    Matrix result = Matrix.identity(diagonal.length, diagonal.length);
    for (int i = 0; i < diagonal.length; i++) {
      result.set(i, i, diagonal[i]);
    }
    return result;
  }

  /**
   * Returns a quadratic Matrix consisting of zeros and of the given values on
   * the diagonal.
   *
   * @param diagonal the values on the diagonal
   * @return the resulting matrix
   */
  public static Matrix diagonal(Vector diagonal) {
    Matrix result = Matrix.identity(diagonal.getDimensionality(), diagonal.getDimensionality());
    for (int i = 0; i < diagonal.getDimensionality(); i++) {
      result.set(i, i, diagonal.get(i));
    }
    return result;
  }

  /**
   * Swaps the specified rows of this matrix.
   *
   * @param i the first row
   * @param j the second row
   */
  public void swapRow(int i, int j) {
    Matrix row_i = getMatrix(i, i, 0, getColumnDimensionality() - 1);
    Matrix row_j = getMatrix(j, j, 0, getColumnDimensionality() - 1);
    setMatrix(i, i, 0, getColumnDimensionality() - 1, row_j);
    setMatrix(j, j, 0, getColumnDimensionality() - 1, row_i);
  }

  /**
   * Swaps the specified columns of this matrix.
   *
   * @param i the first column
   * @param j the second column
   */
  public void swapColumn(int i, int j) {
    Matrix col_i = getColumn(i);
    Matrix col_j = getColumn(j);
    setColumn(i, col_j);
    setColumn(j, col_i);
  }

  /**
   * Projects this row vector into the subspace formed by the specified matrix
   * v.
   *
   * @param v the subspace matrix
   * @return the projection of p into the subspace formed by v
   * @throws IllegalArgumentException if this matrix is no row vector, i.e. this matrix has more
   *                                  than one column or this matrix and v have different length of
   *                                  rows
   */
  public Matrix projection(Matrix v) {
    if (getColumnDimensionality() != 1)
      throw new IllegalArgumentException(
          "The column dimension of p must be one!");

    if (getRowDimensionality() != v.getRowDimensionality())
      throw new IllegalArgumentException(
          "p and v differ in row dimensionality!");

    Matrix sum = new Matrix(getRowDimensionality(), getColumnDimensionality());
    for (int i = 0; i < v.getColumnDimensionality(); i++) {
      Matrix v_i = v.getColumn(i);
      sum = sum.plus(v_i.times(scalarProduct(0, v_i, 0)));
    }
    return sum;
  }

  /**
   * Returns the unit matrix of the specified dimension.
   *
   * @param dim the dimensionality of the unit matrix
   * @return the unit matrix of the specified dimension
   */
  public static Matrix unitMatrix(int dim) {
    double[][] e = new double[dim][dim];
    for (int i = 0; i < dim; i++) {
      e[i][i] = 1;
    }
    return new Matrix(e);
  }

  /**
   * Returns true if the specified column matrix <code>a</code>
   * is linearly independent to the columns of this matrix.
   * Linearly independence is given, if the matrix resulting from appending
   * <code>a</code> to this matrix has full rank.
   *
   * @param columnMatrix the column matrix to be tested for linear independence
   * @return true if the specified column matrix is linearly independent
   *         to the columns of this matrix
   */
  public boolean linearlyIndependent(Matrix columnMatrix) {
    if (columnMatrix.getColumnDimensionality() != 1)
      throw new IllegalArgumentException("a.getColumnDimension() != 1");

    if (this.getRowDimensionality() != columnMatrix.getRowDimensionality())
      throw new IllegalArgumentException("a.getRowDimension() != b.getRowDimension()");

    if (this.getColumnDimensionality() + columnMatrix.getColumnDimensionality() > this.getRowDimensionality())
      return false;

    StringBuffer msg = new StringBuffer();

    double[][] a = new double[getColumnDimensionality() + 1][getRowDimensionality() - 1];
    double[] b = new double[getColumnDimensionality() + 1];

    for (int i = 0; i < a.length; i++) {
      for (int j = 0; j < a[i].length; j++) {
        if (i < getColumnDimensionality()) {
          a[i][j] = get(j, i);
        }
        else {
          a[i][j] = columnMatrix.get(j, 0);
        }
      }
    }

    for (int i = 0; i < b.length; i++) {
      if (i < getColumnDimensionality()) {
        b[i] = get(getRowDimensionality() - 1, i);
      }
      else {
        b[i] = columnMatrix.get(i, 0);
      }
    }

    LinearEquationSystem les = new LinearEquationSystem(a, b);
    les.solveByTotalPivotSearch();

    double[][] coefficients = les.getCoefficents();
    double[] rhs = les.getRHS();

    if (this.debug) {
      msg.append("\na' " + Util.format(this.getArrayCopy()));
      msg.append("\nb' " + Util.format(columnMatrix.getColumnPackedCopy()));

      msg.append("\na " + Util.format(a));
      msg.append("\nb " + Util.format(b));
      msg.append("\nleq " + les.equationsToString(4));
    }

    for (int i = 0; i < coefficients.length; i++) {
      boolean allCoefficientsZero = true;
      for (int j = 0; j < coefficients[i].length; j++) {
        double value = coefficients[i][j];
        if (Math.abs(value) > DELTA) {
          allCoefficientsZero = false;
          break;
        }
      }
      // allCoefficients=0 && rhs=0 -> linearly dependent
      if (allCoefficientsZero) {
        double value = rhs[i];
        if (Math.abs(value) < DELTA) {
          if (this.debug) {
            msg.append("\nvalue " + value + "[" + i + "]");
            msg.append("\nlinearly independent " + false);
            debugFine(msg.toString());
          }
          return false;
        }
      }
    }

    if (this.debug) {
      msg.append("\nlinearly independent " + true);
      debugFine(msg.toString());
    }
    return true;
  }

  /**
   * Returns a matrix derived by Gauss-Jordan-elimination using
   * RationalNumbers for the transformations.
   *
   * @return a matrix derived by Gauss-Jordan-elimination using
   *         RationalNumbers for the transformations
   */
  public Matrix exactGaussJordanElimination() {
    RationalNumber[][] gauss = exactGaussElimination();

    // reduced form
    for (int row = gauss.length - 1; row > 0; row--) {
      int firstCol = -1;
      for (int col = 0; col < gauss[row].length && firstCol == -1; col++) {
        // if(gauss.get(row, col) != 0.0) // i.e. == 1
        if (gauss[row][col].equals(RationalNumber.ONE)) {
          firstCol = col;
        }
      }
      if (firstCol > -1) {
        for (int currentRow = row - 1; currentRow >= 0; currentRow--) {
          RationalNumber multiplier = gauss[currentRow][firstCol]
              .copy();
          for (int col = firstCol; col < gauss[currentRow].length; col++) {
            RationalNumber subtrahent = gauss[row][col]
                .times(multiplier);
            gauss[currentRow][col] = gauss[currentRow][col]
                .minus(subtrahent);
          }
        }
      }
    }
    return new Matrix(gauss);
  }

  /**
   * Returns a matrix derived by Gauss-Jordan-elimination.
   *
   * @return a new Matrix that is the result of Gauss-Jordan-elimination
   * @deprecated use LinearEquationSystem instead
   */
  public Matrix gaussJordanElimination() {
    Matrix gauss = this.gaussElimination();

    // reduced form
    for (int row = gauss.getRowDimensionality() - 1; row > 0; row--) {
      int firstCol = -1;
      for (int col = 0; col < gauss.getColumnDimensionality()
                        && firstCol == -1; col++) {
        // if(gauss.get(row, col) != 0.0) // i.e. == 1
        if (gauss.get(row, col) == 1.0) {
          // if (gauss.get(row, col) < DELTA * -1 || gauss.get(row,
          // col) > DELTA) {
          firstCol = col;
        }
      }
      if (firstCol > -1) {
        for (int currentRow = row - 1; currentRow >= 0; currentRow--) {
          double multiplier = gauss.get(currentRow, firstCol);
          for (int col = firstCol; col < gauss.getColumnDimensionality(); col++) {
            gauss.set(currentRow, col, gauss.get(currentRow, col)
                                       - gauss.get(row, col) * multiplier);
          }
        }
      }
    }

    return gauss;
  }

  /**
   * Performes an exact Gauss-elimination of this Matrix using RationalNumbers
   * to yield highest possible accuracy.
   *
   * @return an array of arrays of RationalNumbers representing the
   *         Gauss-eliminated form of this Matrix
   */
  private RationalNumber[][] exactGaussElimination() {
    RationalNumber[][] gauss = new RationalNumber[this.getRowDimensionality()][this.getColumnDimensionality()];
    for (int row = 0; row < this.getRowDimensionality(); row++) {
      for (int col = 0; col < this.getColumnDimensionality(); col++) {
        gauss[row][col] = new RationalNumber(this.get(row, col));
      }
    }
    return exactGaussElimination(gauss);
  }

  /**
   * Performes recursivly Gauss-elimination on the given matrix of
   * RationalNumbers.
   *
   * @param gauss an array of arrays of RationalNumber
   * @return recursivly derived Gauss-elimination-form of the given matrix of
   *         RationalNumbers
   */
  private static RationalNumber[][] exactGaussElimination(RationalNumber[][] gauss) {
    int firstCol = -1;
    int firstRow = -1;

    // 1. find first column unequal to zero
    for (int col = 0; col < gauss[0].length && firstCol == -1; col++) {
      for (int row = 0; row < gauss.length && firstCol == -1; row++) {
        // if(gauss.get(row, col) != 0.0)
        if (!gauss[row][col].equals(RationalNumber.ZERO)) {
          firstCol = col;
          firstRow = row;
        }
      }
    }

    // 2. set row as first row
    if (firstCol != -1) {
      if (firstRow != 0) {
        RationalNumber[] row = new RationalNumber[gauss[firstRow].length];
        System.arraycopy(gauss[firstRow], 0, row, 0,
                         gauss[firstRow].length);
        System.arraycopy(gauss[0], 0, gauss[firstRow], 0,
                         gauss[firstRow].length);
        System.arraycopy(row, 0, gauss[0], 0, row.length);

      }

      // 3. create leading 1
      if (!gauss[0][firstCol].equals(RationalNumber.ONE)) {
        RationalNumber inverse = gauss[0][firstCol]
            .multiplicativeInverse();
        for (int col = 0; col < gauss[0].length; col++) {
          gauss[0][col] = gauss[0][col].times(inverse);
        }
      }

      // 4. eliminate values unequal to zero below leading 1
      for (int row = 1; row < gauss.length; row++) {
        RationalNumber multiplier = gauss[row][firstCol].copy();
        // if(multiplier != 0.0)
        if (!multiplier.equals(RationalNumber.ZERO)) {
          for (int col = firstCol; col < gauss[row].length; col++) {
            RationalNumber subtrahent = gauss[0][col]
                .times(multiplier);
            gauss[row][col] = gauss[row][col].minus(subtrahent);
          }
        }
      }

      // 5. recursion
      if (gauss.length > 1) {
        RationalNumber[][] subMatrix = new RationalNumber[gauss.length - 1][gauss[1].length];
        System.arraycopy(gauss, 1, subMatrix, 0, gauss.length - 1);
        RationalNumber[][] eliminatedSubMatrix = exactGaussElimination(subMatrix);
        System.arraycopy(eliminatedSubMatrix, 0, gauss, 1,
                         eliminatedSubMatrix.length);
      }
    }
    return gauss;
  }

  /**
   * Recursive gauss-elimination (non-reduced form).
   *
   * @return a new Matrix that is the result of Gauss-elimination
   */
  private Matrix gaussElimination() {
    Matrix gauss = this.copy();
    int firstCol = -1;
    int firstRow = -1;

    // 1. find first column unequal to zero
    for (int col = 0; col < gauss.getColumnDimensionality() && firstCol == -1; col++) {
      for (int row = 0; row < gauss.getRowDimensionality() && firstCol == -1; row++) {
        if (gauss.get(row, col) < DELTA * -1
            || gauss.get(row, col) > DELTA) {
          firstCol = col;
          firstRow = row;
        }
      }
    }

    // 2. set row as first row
    if (firstCol != -1) {
      if (firstRow != 0) {
        Matrix row = gauss.getMatrix(firstRow, firstRow, 0, gauss
            .getColumnDimensionality() - 1);
        gauss.setMatrix(firstRow, firstRow, 0, gauss
            .getColumnDimensionality() - 1, gauss.getMatrix(0, 0, 0,
                                                       gauss.getColumnDimensionality() - 1));
        gauss.setMatrix(0, 0, 0, gauss.getColumnDimensionality() - 1, row);
      }

      // 3. create leading 1
      double a = gauss.get(0, firstCol);
      if (a != 1) {
        for (int col = 0; col < gauss.getColumnDimensionality(); col++) {
          gauss.set(0, col, gauss.get(0, col) / a);
        }
      }

      // 4. eliminate values unequal to zero below leading 1
      for (int row = 1; row < gauss.getRowDimensionality(); row++) {
        double multiplier = gauss.get(row, firstCol);
        // if(multiplier != 0.0)
        if (multiplier < DELTA * -1 || multiplier > DELTA) {
          for (int col = firstCol; col < gauss.getColumnDimensionality(); col++) {
            gauss.set(row, col, gauss.get(row, col)
                                - gauss.get(0, col) * multiplier);
          }
        }
      }

      // 5. recursion
      if (gauss.getRowDimensionality() > 1) {
        Matrix subMatrix = gauss.getMatrix(1, gauss.getRowDimensionality() - 1, 0, gauss.getColumnDimensionality() - 1);
        gauss.setMatrix(1, gauss.getRowDimensionality() - 1, 0, gauss.getColumnDimensionality() - 1, subMatrix.gaussElimination());
      }
    }
    return gauss;
  }

  /**
   * Returns true, if this matrix is symmetric, false otherwise.
   *
   * @return true, if this matrix is symmetric, false otherwise
   */
  public boolean isSymmetric() {
    if (m != n) return false;
    for (int i = 0; i < m; i++) {
      for (int j = i; j < n; j++) {
        if (A[i][j] != A[j][i])
          return false;
      }
    }
    return true;
  }

  /**
   * Returns the dimensionality of this matrix as a string.
   *
   * @return the dimensionality of this matrix as a string
   */
  public String dimensionInfo() {
    return getRowDimensionality() + " x " + getColumnDimensionality();
  }


}
