package de.lmu.ifi.dbs.elki.math.linearalgebra;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Logger;

import de.lmu.ifi.dbs.elki.data.RationalNumber;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.output.FormatUtil;

/**
 * The Matrix Class represents real-valued matrices.
 * <p/>
 * For a Matrix {@code M} we have therefore {@code M &isin;$real;<sup>m &times; n</sup>},
 * where {@code m} and {@code n} are the number of rows and columns, respectively.
 */
public class Matrix implements Cloneable, java.io.Serializable {
    /**
     * Serial version
     */
    private static final long serialVersionUID = -8331523628259600919L;

    /**
     * A small number to handle numbers near 0 as 0.
     */
    public static final double DELTA = 1E-3;

    /**
     * Array for internal storage of elements.
     *
     * @serial internal array storage.
     */
    private double[][] elements;

    /**
     * Row dimension.
     */
    private int rowdimension;

    /**
     * Column dimension.
     */
    private int columndimension;

    /**
     * Basic-constructor for use in complex constructors only.
     */
    private Matrix() {
      // Nothing to do
    }

    /**
     * Constructs an m-by-n matrix of zeros.
     *
     * @param m number of rows
     * @param n number of colums
     */
    public Matrix(int m, int n) {
        this();
        this.rowdimension = m;
        this.columndimension = n;
        elements = new double[m][n];
    }

    /**
     * Constructs an m-by-n constant matrix.
     *
     * @param m number of rows
     * @param n number of colums
     * @param s A scalar value defining the constant value in the matrix
     */
    public Matrix(int m, int n, double s) {
        this();
        this.rowdimension = m;
        this.columndimension = n;
        elements = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                elements[i][j] = s;
            }
        }
    }

    /**
     * Constructs a matrix from a 2-D array.
     *
     * @param elements an array of arrays of doubles defining the values of the matrix
     * @throws IllegalArgumentException if not all rows conform in the same length
     */
    public Matrix(double[][] elements) {
        this();
        rowdimension = elements.length;
        columndimension = elements[0].length;
        for (int i = 0; i < rowdimension; i++) {
            if (elements[i].length != columndimension) {
                throw new IllegalArgumentException("All rows must have the same length.");
            }
        }
        this.elements = elements;
    }

    /**
     * Constructs a Matrix for a given array of arrays of {@link RationalNumber}s.
     *
     * @param q an array of arrays of RationalNumbers. q is not checked for
     *          consistency (i.e. whether all rows are of equal length)
     */
    public Matrix(RationalNumber[][] q) {
        this();
        rowdimension = q.length;
        columndimension = q[0].length;
        elements = new double[rowdimension][columndimension];
        for (int row = 0; row < q.length; row++) {
            for (int col = 0; col < q[row].length; col++) {
                elements[row][col] = q[row][col].doubleValue();
            }
        }
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
        this.rowdimension = m;
        columndimension = (m != 0 ? values.length / m : 0);
        if (m * columndimension != values.length) {
            throw new IllegalArgumentException("Array length must be a multiple of m.");
        }
        elements = new double[m][columndimension];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < columndimension; j++) {
                elements[i][j] = values[i + j * m];
            }
        }
    }

    /**
     * Construct a matrix from a copy of a 2-D array.
     *
     * @param A Two-dimensional array of doubles.
     * @return new matrix
     * @throws IllegalArgumentException All rows must have the same length
     */
    public static Matrix constructWithCopy(double[][] A) {
        int m = A.length;
        int n = A[0].length;
        Matrix X = new Matrix(m, n);
        double[][] C = X.getArray();
        for (int i = 0; i < m; i++) {
            if (A[i].length != n) {
                throw new IllegalArgumentException("All rows must have the same length.");
            }
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j];
            }
        }
        return X;
    }

    /**
     * Make a deep copy of a matrix.
     * @return a new matrix containing the same values as this matrix
     */
    public Matrix copy() {
        Matrix X = new Matrix(rowdimension, columndimension);
        double[][] C = X.getArray();
        for (int i = 0; i < rowdimension; i++) {
            System.arraycopy(elements[i], 0, C[i], 0, columndimension);
        }
        return X;
    }

    /**
     * Clone the Matrix object.
     */
    @Override
    public Object clone() {
        return this.copy();
    }

    /**
     * Access the internal two-dimensional array.
     *
     * @return Pointer to the two-dimensional array of matrix elements.
     */
    public double[][] getArray() {
        return elements;
    }

    /**
     * Copy the internal two-dimensional array.
     *
     * @return Two-dimensional array copy of matrix elements.
     */
    public double[][] getArrayCopy() {
        double[][] C = new double[rowdimension][columndimension];
        for (int i = 0; i < rowdimension; i++) {
            for (int j = 0; j < columndimension; j++) {
                C[i][j] = elements[i][j];
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
        double[] vals = new double[rowdimension * columndimension];
        for (int i = 0; i < rowdimension; i++) {
            for (int j = 0; j < columndimension; j++) {
                vals[i + j * rowdimension] = elements[i][j];
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
        double[] vals = new double[rowdimension * columndimension];
        for (int i = 0; i < rowdimension; i++) {
            for (int j = 0; j < columndimension; j++) {
                vals[i * columndimension + j] = elements[i][j];
            }
        }
        return vals;
    }

    /**
     * Returns the dimensionality of the rows of this matrix.
     *
     * @return m, the number of rows.
     */
    public int getRowDimensionality() {
        return rowdimension;
    }

    /**
     * Returns the dimensionality of the columns of this matrix.
     *
     * @return n, the number of columns.
     */
    public int getColumnDimensionality() {
        return columndimension;
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
        return elements[i][j];
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
                    B[i - i0][j - j0] = elements[i][j];
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
                    B[i][j] = elements[r[i]][c[j]];
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
                    B[i - i0][j] = elements[i][c[j]];
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
                    B[i][j - j0] = elements[r[i]][j];
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
        elements[i][j] = s;
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
        elements[i][j] += s;
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
                    elements[i][j] = X.get(i - i0, j - j0);
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
                    elements[r[i]][c[j]] = X.get(i, j);
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
                    elements[r[i]][j] = X.get(i, j - j0);
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
                    elements[i][c[j]] = X.get(i - i0, j);
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
        for (int i = 0; i < rowdimension; i++) {
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
     * Returns the <code>i</code>th row of this matrix as vector.
     *
     * @param i the index of the row to be returned
     * @return the <code>i</code>th row of this matrix
     */
    public Vector getRowVector(int i) {
        double[] row = elements[i].clone();
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
            throw new IllegalArgumentException("Matrix must consist of one column!");
        if (column.getRowDimensionality() != getRowDimensionality())
            throw new IllegalArgumentException("Matrix must consist of the same no of rows!");

        setMatrix(0, getRowDimensionality() - 1, j, j, column);
    }

    /**
     * Matrix transpose.
     *
     * @return A'
     */
    public Matrix transpose() {
        Matrix X = new Matrix(columndimension, rowdimension);
        double[][] C = X.getArray();
        for (int i = 0; i < rowdimension; i++) {
            for (int j = 0; j < columndimension; j++) {
                C[j][i] = elements[i][j];
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
        for (int j = 0; j < columndimension; j++) {
            double s = 0;
            for (int i = 0; i < rowdimension; i++) {
                s += Math.abs(elements[i][j]);
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
        for (int i = 0; i < rowdimension; i++) {
            double s = 0;
            for (int j = 0; j < columndimension; j++) {
                s += Math.abs(elements[i][j]);
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
        for (int i = 0; i < rowdimension; i++) {
            for (int j = 0; j < columndimension; j++) {
                f = MathUtil.hypotenuse(f, elements[i][j]);
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
        Matrix X = new Matrix(rowdimension, columndimension);
        double[][] C = X.getArray();
        for (int i = 0; i < rowdimension; i++) {
            for (int j = 0; j < columndimension; j++) {
                C[i][j] = -elements[i][j];
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
        Matrix X = new Matrix(rowdimension, columndimension);
        double[][] C = X.getArray();
        for (int i = 0; i < rowdimension; i++) {
            for (int j = 0; j < columndimension; j++) {
                C[i][j] = elements[i][j] + B.elements[i][j];
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
        for (int i = 0; i < rowdimension; i++) {
            for (int j = 0; j < columndimension; j++) {
                elements[i][j] = elements[i][j] + B.elements[i][j];
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
        Matrix X = new Matrix(rowdimension, columndimension);
        double[][] C = X.getArray();
        for (int i = 0; i < rowdimension; i++) {
            for (int j = 0; j < columndimension; j++) {
                C[i][j] = elements[i][j] - B.elements[i][j];
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
        for (int i = 0; i < rowdimension; i++) {
            for (int j = 0; j < columndimension; j++) {
                elements[i][j] = elements[i][j] - B.elements[i][j];
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
        Matrix X = new Matrix(rowdimension, columndimension);
        double[][] C = X.getArray();
        for (int i = 0; i < rowdimension; i++) {
            for (int j = 0; j < columndimension; j++) {
                C[i][j] = elements[i][j] * B.elements[i][j];
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
        for (int i = 0; i < rowdimension; i++) {
            for (int j = 0; j < columndimension; j++) {
                elements[i][j] = elements[i][j] * B.elements[i][j];
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
        Matrix X = new Matrix(rowdimension, columndimension);
        double[][] C = X.getArray();
        for (int i = 0; i < rowdimension; i++) {
            for (int j = 0; j < columndimension; j++) {
                C[i][j] = elements[i][j] / B.elements[i][j];
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
        for (int i = 0; i < rowdimension; i++) {
            for (int j = 0; j < columndimension; j++) {
                elements[i][j] = elements[i][j] / B.elements[i][j];
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
        Matrix X = new Matrix(rowdimension, columndimension);
        double[][] C = X.getArray();
        for (int i = 0; i < rowdimension; i++) {
            for (int j = 0; j < columndimension; j++) {
                C[i][j] = B.elements[i][j] / elements[i][j];
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
        for (int i = 0; i < rowdimension; i++) {
            for (int j = 0; j < columndimension; j++) {
                elements[i][j] = B.elements[i][j] / elements[i][j];
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
        Matrix X = new Matrix(rowdimension, columndimension);
        double[][] C = X.getArray();
        for (int i = 0; i < rowdimension; i++) {
            for (int j = 0; j < columndimension; j++) {
                C[i][j] = s * elements[i][j];
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
        for (int i = 0; i < rowdimension; i++) {
            for (int j = 0; j < columndimension; j++) {
                elements[i][j] = s * elements[i][j];
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
        if (B.rowdimension != columndimension) {
            throw new IllegalArgumentException("Matrix inner dimensions must agree.");
        }
        Matrix X = new Matrix(rowdimension, B.columndimension);
        double[][] C = X.getArray();
        double[] Bcolj = new double[columndimension];
        for (int j = 0; j < B.columndimension; j++) {
            for (int k = 0; k < columndimension; k++) {
                Bcolj[k] = B.elements[k][j];
            }
            for (int i = 0; i < rowdimension; i++) {
                double[] Arowi = elements[i];
                double s = 0;
                for (int k = 0; k < columndimension; k++) {
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
        return (rowdimension == columndimension ? (new LUDecomposition(this)).solve(B) : (new QRDecomposition(this)).solve(B));
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
        return solve(identity(rowdimension, rowdimension));
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
        for (int i = 0; i < Math.min(rowdimension, columndimension); i++) {
            t += elements[i][i];
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
        for (int i = 0; i < rowdimension; i++) {
            for (int j = 0; j < columndimension; j++) {
                String s = format.format(elements[i][j]); // format the number
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
     * @return New matrix
     * @throws java.io.IOException on input error 
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
        while (tokenizer.nextToken() == StreamTokenizer.TT_EOL) {
            // ignore initial empty lines
        }
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
                    throw new java.io.IOException("Row " + v.size() + " is too long.");
                row[j++] = (Double.valueOf(tokenizer.sval));
            }
            while (tokenizer.nextToken() == StreamTokenizer.TT_WORD);
            if (j < n)
                throw new java.io.IOException("Row " + v.size() + " is too short.");
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
        if (B.rowdimension != rowdimension || B.columndimension != columndimension) {
            throw new IllegalArgumentException("Matrix dimensions must agree.");
        }
    }

    /*
     * ##########################################################################################
     * additional methods by AZ
     * ##########################################################################################
     */

    /**
     * Returns a string representation of this matrix.
     *
     * @param w column width
     * @param d number of digits after the decimal
     * @return a string representation of this matrix
     */
    public String toString(int w, int d) {
        DecimalFormat format = new DecimalFormat();
        format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
        format.setMinimumIntegerDigits(1);
        format.setMaximumFractionDigits(d);
        format.setMinimumFractionDigits(d);
        format.setGroupingUsed(false);

        int width = w + 1;
        StringBuffer msg = new StringBuffer();
        msg.append("\n"); // start on new line.
        for (int i = 0; i < rowdimension; i++) {
            for (int j = 0; j < columndimension; j++) {
                String s = format.format(elements[i][j]); // format the number
                int padding = Math.max(1, width - s.length()); // At _least_ 1
                // space
                for (int k = 0; k < padding; k++)
                    msg.append(' ');
                msg.append(s);
            }
            msg.append("\n");
        }
        // msg.append("\n");

        return msg.toString();
    }

    /**
     * toString returns String-representation of Matrix.
     */
    @Override
    public String toString() {
        StringBuffer output = new StringBuffer();
        output.append("[\n");
        for (int i = 0; i < rowdimension; i++) {
            output.append(" [");
            for (int j = 0; j < columndimension; j++) {
                output.append(" ").append(elements[i][j]);
                if (j < columndimension - 1) {
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
    public String toString(String pre) {
        StringBuffer output = new StringBuffer();
        output.append(pre).append("[\n").append(pre);
        for (int i = 0; i < rowdimension; i++) {
            output.append(" [");
            for (int j = 0; j < columndimension; j++) {
                output.append(" ").append(elements[i][j]);
                if (j < columndimension - 1) {
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
        String[][] entries = new String[rowdimension][columndimension];
        for (int i = 0; i < rowdimension; i++) {
            for (int j = 0; j < columndimension; j++) {
                entries[i][j] = nf.format(elements[i][j]);
                if (entries[i][j].length() > colMax[j]) {
                    colMax[j] = entries[i][j].length();
                }
            }
        }
        StringBuffer output = new StringBuffer();
        output.append("[\n");
        for (int i = 0; i < rowdimension; i++) {
            output.append(" [");
            for (int j = 0; j < columndimension; j++) {
                output.append(" ");
                int space = colMax[j] - entries[i][j].length();
                for (int s = 0; s < space; s++) {
                    output.append(" ");
                }
                output.append(entries[i][j]);
                if (j < columndimension - 1) {
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
        String[][] entries = new String[rowdimension][columndimension];
        for (int i = 0; i < rowdimension; i++) {
            for (int j = 0; j < columndimension; j++) {
                entries[i][j] = nf.format(elements[i][j]);
                if (entries[i][j].length() > colMax[j]) {
                    colMax[j] = entries[i][j].length();
                }
            }
        }
        StringBuffer output = new StringBuffer();
        output.append(pre).append("[\n").append(pre);
        for (int i = 0; i < rowdimension; i++) {
            output.append(" [");
            for (int j = 0; j < columndimension; j++) {
                output.append(" ");
                int space = colMax[j] - entries[i][j].length();
                for (int s = 0; s < space; s++) {
                    output.append(" ");
                }
                output.append(entries[i][j]);
                if (j < columndimension - 1) {
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
     * @return double[] the values on the diagonal of the Matrix
     */
    public double[] getDiagonal() {
        double[] diagonal = new double[rowdimension];
        for (int i = 0; i < rowdimension; i++) {
            diagonal[i] = elements[i][i];
        }
        return diagonal;
    }

    /**
     * Scales the columns of this matrix with the specified factor.
     *
     * @param scale the factor to scale the column
     */
    public void scaleColumns(double scale) {
        for (int j = 0; j < columndimension; j++) {
            scaleColumn(j, scale);
        }
    }

    /**
     * Normalizes the columns of this matrix to length of 1.0.
     */
    public void normalizeColumns() {
        for (int col = 0; col < columndimension; col++) {
            double norm = 0.0;
            for (int row = 0; row < rowdimension; row++) {
                norm = norm + (elements[row][col] * elements[row][col]);
            }
            norm = Math.sqrt(norm);
            if (norm != 0) {
                for (int row = 0; row < rowdimension; row++) {
                    elements[row][col] = (elements[row][col] / norm);
                }
            }
        }
    }

    /**
     * Scales the specified column with the specified factor.
     *
     * @param j     the index of the column to be scaled
     * @param scale the factor to scale the column
     */
    public void scaleColumn(int j, double scale) {
        for (int row = 0; row < rowdimension; row++) {
            elements[row][j] = (elements[row][j] * scale);
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
        for (int col = 0; col < columndimension; col++) {
            for (int row = 0; row < rowdimension; row++) {
                double distIJ = elements[row][col] - arrayB[row][col];
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
        return Math.acos(this.scalarProduct(colA, B, colB) / (this.euclideanNorm(colA) * B.euclideanNorm(colB)));
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
        for (int row = 0; row < rowdimension; row++) {
            double prod = elements[row][colA] * arrayB[row][colB];
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
     * @throws IllegalArgumentException if this matrix is no row vector, i.e.
     *                                  this matrix has more than one column or this matrix and v have
     *                                  different length of rows
     */
    public Matrix projection(Matrix v) {
        if (getColumnDimensionality() != 1) {
            throw new IllegalArgumentException("The column dimension of p must be one!");
        }
        if (getRowDimensionality() != v.getRowDimensionality()) {
            throw new IllegalArgumentException("p and v differ in row dimensionality!");
        }
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
     * Returns the zero matrix of the specified dimension.
     *
     * @param dim the dimensionality of the unit matrix
     * @return the zero matrix of the specified dimension
     */
    public static Matrix zeroMatrix(int dim) {
        double[][] z = new double[dim][dim];
        return new Matrix(z);
    }

    /**
     * Returns true if the specified column matrix <code>a</code> is linearly
     * independent to the columns of this matrix. Linearly independence is
     * given, if the matrix resulting from appending <code>a</code> to this
     * matrix has full rank.
     *
     * @param columnMatrix the column matrix to be tested for linear
     *                     independence
     * @return true if the specified column matrix is linearly independent to
     *         the columns of this matrix
     */
    public boolean linearlyIndependent(Matrix columnMatrix) {
        if (columnMatrix.getColumnDimensionality() != 1) {
            throw new IllegalArgumentException("a.getColumnDimension() != 1");
        }
        if (this.getRowDimensionality() != columnMatrix.getRowDimensionality()) {
            throw new IllegalArgumentException("a.getRowDimension() != b.getRowDimension()");
        }
        if (this.getColumnDimensionality() + columnMatrix.getColumnDimensionality() > this.getRowDimensionality()) {
            return false;
        }
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

        if (LoggingConfiguration.DEBUG) {
            msg.append("\na' " + FormatUtil.format(this.getArrayCopy()));
            msg.append("\nb' " + FormatUtil.format(columnMatrix.getColumnPackedCopy()));

            msg.append("\na " + FormatUtil.format(a));
            msg.append("\nb " + FormatUtil.format(b));
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
                    if (LoggingConfiguration.DEBUG) {
                        msg.append("\nvalue " + value + "[" + i + "]");
                        msg.append("\nlinearly independent " + false);
                        Logger.getLogger(this.getClass().getName()).fine(msg.toString());
                    }
                    return false;
                }
            }
        }

        if (LoggingConfiguration.DEBUG) {
            msg.append("\nlinearly independent " + true);
            Logger.getLogger(this.getClass().getName()).fine(msg.toString());
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
                    RationalNumber multiplier = gauss[currentRow][firstCol].copy();
                    for (int col = firstCol; col < gauss[currentRow].length; col++) {
                        RationalNumber subtrahent = gauss[row][col].times(multiplier);
                        gauss[currentRow][col] = gauss[currentRow][col].minus(subtrahent);
                    }
                }
            }
        }
        return new Matrix(gauss);
    }

    /**
     * Perform an exact Gauss-elimination of this Matrix using RationalNumbers
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
     * Perform recursive Gauss-elimination on the given matrix of
     * RationalNumbers.
     *
     * @param gauss an array of arrays of RationalNumber
     * @return recursive derived Gauss-elimination-form of the given matrix of
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
                System.arraycopy(gauss[firstRow], 0, row, 0, gauss[firstRow].length);
                System.arraycopy(gauss[0], 0, gauss[firstRow], 0, gauss[firstRow].length);
                System.arraycopy(row, 0, gauss[0], 0, row.length);

            }

            // 3. create leading 1
            if (!gauss[0][firstCol].equals(RationalNumber.ONE)) {
                RationalNumber inverse = gauss[0][firstCol].multiplicativeInverse();
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
                        RationalNumber subtrahent = gauss[0][col].times(multiplier);
                        gauss[row][col] = gauss[row][col].minus(subtrahent);
                    }
                }
            }

            // 5. recursion
            if (gauss.length > 1) {
                RationalNumber[][] subMatrix = new RationalNumber[gauss.length - 1][gauss[1].length];
                System.arraycopy(gauss, 1, subMatrix, 0, gauss.length - 1);
                RationalNumber[][] eliminatedSubMatrix = exactGaussElimination(subMatrix);
                System.arraycopy(eliminatedSubMatrix, 0, gauss, 1, eliminatedSubMatrix.length);
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
        if (rowdimension != columndimension)
            return false;
        for (int i = 0; i < rowdimension; i++) {
            for (int j = i; j < columndimension; j++) {
                if (elements[i][j] != elements[j][i])
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

    /**
     * Completes this d x c basis of a subspace of R^d to a d x d basis of R^d,
     * i.e. appends c-d columns to this basis.
     *
     * @return the appended columns
     */
    public Matrix completeBasis() {
        Matrix e = Matrix.unitMatrix(getRowDimensionality());
        Matrix basis = copy();
        Matrix result = null;
        for (int i = 0; i < e.getColumnDimensionality(); i++) {
            Matrix e_i = e.getColumn(i);
            boolean li = basis.linearlyIndependent(e_i);

            if (li) {
                if (result == null) {
                    result = e_i.copy();
                }
                else {
                    result = result.appendColumns(e_i);
                }
                basis = basis.appendColumns(e_i);
            }
        }
        return result;
    }

    /**
     * Completes this d x c basis of a subspace of R^d to a d x d basis of R^d,
     * i.e. appends c-d columns to this basis.
     *
     * @return the appended columns
     */
    public Matrix completeToOrthonormalBasis() {
        Matrix e = Matrix.unitMatrix(getRowDimensionality());
        Matrix basis = copy();
        Matrix result = null;
        for (int i = 0; i < e.getColumnDimensionality(); i++) {
            Matrix e_i = e.getColumn(i);
            boolean li = basis.linearlyIndependent(e_i);

            if (li) {
                if (result == null) {
                    result = e_i.copy();
                }
                else {
                    result = result.appendColumns(e_i);
                }
                basis = basis.appendColumns(e_i);
            }
        }
        basis = basis.orthonormalize();
        return basis.getMatrix(0, basis.getRowDimensionality() - 1, getColumnDimensionality(), basis.getColumnDimensionality() - 1);
    }

    /**
     * Returns a matrix which consists of this matrix and the specified columns.
     *
     * @param columns the columns to be appended
     * @return the new matrix with the appended columns
     */
    public Matrix appendColumns(Matrix columns) {
        if (getRowDimensionality() != columns.getRowDimensionality())
            throw new IllegalArgumentException("m.getRowDimension() != column.getRowDimension()");

        Matrix result = new Matrix(getRowDimensionality(), getColumnDimensionality() + columns.getColumnDimensionality());
        for (int i = 0; i < result.getColumnDimensionality(); i++) {
            if (i < getColumnDimensionality()) {
                result.setColumn(i, getColumn(i));
            }
            else {
                result.setColumn(i, columns.getColumn(i - getColumnDimensionality()));
            }
        }
        return result;
    }

    /**
     * Returns an orthonormalization of this matrix.
     *
     * @return the orthonormalized matrix
     */
    public Matrix orthonormalize() {
        Matrix v = getColumn(0).copy();

        for (int i = 1; i < getColumnDimensionality(); i++) {
            Matrix u_i = getColumn(i);
            Matrix sum = new Matrix(getRowDimensionality(), 1);
            for (int j = 0; j < i; j++) {
                Matrix v_j = v.getColumn(j);
                double scalar = u_i.scalarProduct(0, v_j, 0) / v_j.scalarProduct(0, v_j, 0);
                sum = sum.plus(v_j.times(scalar));
            }
            Matrix v_i = u_i.minus(sum);
            v = v.appendColumns(v_i);
        }

        v.normalizeColumns();
        return v;
    }

    /**
     * Adds a given value to the diagonal entries if the entry is smaller than
     * the constant.
     *
     * @param constant value to add to the diagonal entries
     * @return a new Matrix differing from this Matrix by the given value added
     *         to the diagonal entries
     */
    public Matrix cheatToAvoidSingularity(double constant) {
        Matrix a = this.copy();
        for (int i = 0; i < a.getColumnDimensionality() && i < a.getRowDimensionality(); i++) {
            // if(a.get(i, i) < constant)
            {
                a.increment(i, i, constant);
            }
        }
        return a;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + Arrays.hashCode(this.elements);
        result = PRIME * result + this.rowdimension;
        result = PRIME * result + this.columndimension;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Matrix other = (Matrix) obj;
        if (this.rowdimension != other.rowdimension)
            return false;
        if (this.columndimension != other.columndimension)
            return false;
        for (int i=0; i < this.rowdimension; i++)
          for (int j=0; j < this.columndimension; j++)
            if (this.elements[i][j] != other.elements[i][j])
               return false;
        return true;
    }

    /**
     * Compare two matrices with a delta parameter to take numerical
     * errors into account.
     * 
     * @param obj other object to compare with
     * @param maxdelta maximum delta allowed
     * @return true if delta smaller than maximum
     */
    public boolean almostEquals(Object obj, double maxdelta) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Matrix other = (Matrix) obj;
        if (this.rowdimension != other.rowdimension)
            return false;
        if (this.columndimension != other.columndimension)
            return false;
        for (int i=0; i < this.rowdimension; i++)
          for (int j=0; j < this.columndimension; j++)
            if (Math.abs(this.elements[i][j] - other.elements[i][j]) > maxdelta)
               return false;
        return true;
    }

    /**
     * Compare two matrices with a delta parameter to take numerical
     * errors into account.
     * 
     * @param obj other object to compare with
     * @return almost equals with delta {@link #DELTA}
     */
    public boolean almostEquals(Object obj) {
      return almostEquals(obj, DELTA);
    }
}
