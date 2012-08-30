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

import java.io.BufferedReader;
import java.io.StreamTokenizer;
import java.util.Arrays;
import java.util.logging.Logger;

import de.lmu.ifi.dbs.elki.data.RationalNumber;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;

/**
 * A two-dimensional matrix class, where the data is stored as two-dimensional
 * array.
 * 
 * Implementation note: this class contains various optimizations that
 * theoretically the java hotspot compiler should optimize on its own. However,
 * they do show up a hotspots in the profiler (in cpu=times mode), so it does
 * make a difference at least when optimizing other parts of ELKI.
 * 
 * @author Elke Achtert
 * @author Erich Schubert
 * 
 * @apiviz.uses Vector
 * @apiviz.landmark
 */
public class Matrix {
  /**
   * A small number to handle numbers near 0 as 0.
   */
  public static final double DELTA = 1E-3;

  /**
   * Error: matrix not square.
   */
  public static final String ERR_NOTSQUARE = "All rows must have the same length.";

  /**
   * Error: matrix indexes incorrect
   */
  public static final String ERR_REINDEX = "Submatrix indices incorrect.";

  /**
   * Error when matrix dimensions do not agree.
   */
  public static final String ERR_MATRIX_DIMENSIONS = "Matrix must consist of the same no of rows!";

  /**
   * Error when matrix inner dimensions do not agree.
   */
  private static final String ERR_MATRIX_INNERDIM = "Matrix inner dimensions must agree.";

  /**
   * Array for internal storage of elements.
   * 
   * @serial internal array storage.
   */
  protected final double[][] elements;

  // row dimensionality == elements.length!

  /**
   * Column dimension.
   */
  final int columndimension;

  /**
   * Constructs an m-by-n matrix of zeros.
   * 
   * @param m number of rows
   * @param n number of columns
   */
  public Matrix(final int m, final int n) {
    this.columndimension = n;
    elements = new double[m][n];
  }

  /**
   * Constructs an m-by-n constant matrix.
   * 
   * @param m number of rows
   * @param n number of columns
   * @param s A scalar value defining the constant value in the matrix
   */
  public Matrix(final int m, final int n, final double s) {
    this.columndimension = n;
    elements = new double[m][n];
    for(int i = 0; i < m; i++) {
      for(int j = 0; j < n; j++) {
        elements[i][j] = s;
      }
    }
  }

  /**
   * Constructs a matrix from a 2-D array.
   * 
   * @param elements an array of arrays of doubles defining the values of the
   *        matrix
   * @throws IllegalArgumentException if not all rows conform in the same length
   */
  public Matrix(final double[][] elements) {
    columndimension = elements[0].length;
    for(int i = 0; i < elements.length; i++) {
      if(elements[i].length != columndimension) {
        throw new IllegalArgumentException(ERR_NOTSQUARE);
      }
    }
    this.elements = elements;
  }

  /**
   * Constructs a Matrix for a given array of arrays of {@link RationalNumber}s.
   * 
   * @param q an array of arrays of RationalNumbers. q is not checked for
   *        consistency (i.e. whether all rows are of equal length)
   */
  public Matrix(final RationalNumber[][] q) {
    columndimension = q[0].length;
    elements = new double[q.length][columndimension];
    for(int row = 0; row < q.length; row++) {
      for(int col = 0; col < q[row].length; col++) {
        elements[row][col] = q[row][col].doubleValue();
      }
    }
  }

  /**
   * Construct a matrix from a one-dimensional packed array
   * 
   * @param values One-dimensional array of doubles, packed by columns (ala
   *        Fortran).
   * @param m Number of rows.
   * @throws IllegalArgumentException Array length must be a multiple of m.
   */
  public Matrix(final double values[], final int m) {
    columndimension = (m != 0 ? values.length / m : 0);
    if(m * columndimension != values.length) {
      throw new IllegalArgumentException("Array length must be a multiple of m.");
    }
    elements = new double[m][columndimension];
    for(int i = 0; i < m; i++) {
      for(int j = 0; j < columndimension; j++) {
        elements[i][j] = values[i + j * m];
      }
    }
  }

  /**
   * Constructor, cloning an existing matrix.
   * 
   * @param mat Matrix to clone
   */
  public Matrix(Matrix mat) {
    this(mat.getArrayCopy());
  }

  /**
   * Construct a matrix from a copy of a 2-D array.
   * 
   * @param A Two-dimensional array of doubles.
   * @return new matrix
   * @throws IllegalArgumentException All rows must have the same length
   */
  public static final Matrix constructWithCopy(final double[][] A) {
    final int m = A.length;
    final int n = A[0].length;
    final Matrix X = new Matrix(m, n);
    for(int i = 0; i < m; i++) {
      if(A[i].length != n) {
        throw new IllegalArgumentException(ERR_NOTSQUARE);
      }
      System.arraycopy(A[i], 0, X.elements[i], 0, n);
    }
    return X;
  }

  /**
   * Returns the unit matrix of the specified dimension.
   * 
   * @param dim the dimensionality of the unit matrix
   * @return the unit matrix of the specified dimension
   */
  public static final Matrix unitMatrix(final int dim) {
    final double[][] e = new double[dim][dim];
    for(int i = 0; i < dim; i++) {
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
  public static final Matrix zeroMatrix(final int dim) {
    final double[][] z = new double[dim][dim];
    return new Matrix(z);
  }

  /**
   * Generate matrix with random elements
   * 
   * @param m Number of rows.
   * @param n Number of columns.
   * @return An m-by-n matrix with uniformly distributed random elements.
   */
  public static final Matrix random(final int m, final int n) {
    final Matrix A = new Matrix(m, n);
    for(int i = 0; i < m; i++) {
      for(int j = 0; j < n; j++) {
        A.elements[i][j] = Math.random();
      }
    }
    return A;
  }

  /**
   * Generate identity matrix
   * 
   * @param m Number of rows.
   * @param n Number of columns.
   * @return An m-by-n matrix with ones on the diagonal and zeros elsewhere.
   */
  public static final Matrix identity(final int m, final int n) {
    final Matrix A = new Matrix(m, n);
    for(int i = 0; i < Math.min(m, n); i++) {
      A.elements[i][i] = 1.0;
    }
    return A;
  }

  /**
   * Returns a quadratic Matrix consisting of zeros and of the given values on
   * the diagonal.
   * 
   * @param diagonal the values on the diagonal
   * @return the resulting matrix
   */
  public static final Matrix diagonal(final double[] diagonal) {
    final Matrix result = new Matrix(diagonal.length, diagonal.length);
    for(int i = 0; i < diagonal.length; i++) {
      result.elements[i][i] = diagonal[i];
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
  public static final Matrix diagonal(final Vector diagonal) {
    final Matrix result = new Matrix(diagonal.elements.length, diagonal.elements.length);
    for(int i = 0; i < diagonal.elements.length; i++) {
      result.elements[i][i] = diagonal.elements[i];
    }
    return result;
  }

  /**
   * Make a deep copy of a matrix.
   * 
   * @return a new matrix containing the same values as this matrix
   */
  public final Matrix copy() {
    final Matrix X = new Matrix(elements.length, columndimension);
    for(int i = 0; i < elements.length; i++) {
      System.arraycopy(elements[i], 0, X.elements[i], 0, columndimension);
    }
    return X;
  }

  /**
   * Clone the Matrix object.
   */
  @Override
  public Matrix clone() {
    return this.copy();
  }

  /**
   * Access the internal two-dimensional array.
   * 
   * @return Pointer to the two-dimensional array of matrix elements.
   */
  public final double[][] getArrayRef() {
    return elements;
  }

  /**
   * Copy the internal two-dimensional array.
   * 
   * @return Two-dimensional array copy of matrix elements.
   */
  public final double[][] getArrayCopy() {
    final double[][] C = new double[elements.length][columndimension];
    for(int i = 0; i < elements.length; i++) {
      for(int j = 0; j < columndimension; j++) {
        C[i][j] = elements[i][j];
      }
    }
    return C;
  }

  /**
   * Returns the dimensionality of the rows of this matrix.
   * 
   * @return m, the number of rows.
   */
  public final int getRowDimensionality() {
    return elements.length;
  }

  /**
   * Returns the dimensionality of the columns of this matrix.
   * 
   * @return n, the number of columns.
   */
  public final int getColumnDimensionality() {
    return columndimension;
  }

  /**
   * Get a single element.
   * 
   * @param i Row index.
   * @param j Column index.
   * @return A(i,j)
   * @throws ArrayIndexOutOfBoundsException on bounds error
   */
  public final double get(final int i, final int j) {
    return elements[i][j];
  }

  /**
   * Set a single element.
   * 
   * @param i Row index.
   * @param j Column index.
   * @param s A(i,j).
   * @return modified matrix
   * @throws ArrayIndexOutOfBoundsException on bounds error
   */
  public final Matrix set(final int i, final int j, final double s) {
    elements[i][j] = s;
    return this;
  }

  /**
   * Increments a single element.
   * 
   * @param i the row index
   * @param j the column index
   * @param s the increment value: A(i,j) = A(i.j) + s.
   * @return modified matrix
   * @throws ArrayIndexOutOfBoundsException on bounds error
   */
  public final Matrix increment(final int i, final int j, final double s) {
    elements[i][j] += s;
    return this;
  }

  /**
   * Make a one-dimensional row packed copy of the internal array.
   * 
   * @return Matrix elements packed in a one-dimensional array by rows.
   */
  public final double[] getRowPackedCopy() {
    double[] vals = new double[elements.length * columndimension];
    for(int i = 0; i < elements.length; i++) {
      for(int j = 0; j < columndimension; j++) {
        vals[i * columndimension + j] = elements[i][j];
      }
    }
    return vals;
  }

  /**
   * Make a one-dimensional column packed copy of the internal array.
   * 
   * @return Matrix elements packed in a one-dimensional array by columns.
   */
  public final double[] getColumnPackedCopy() {
    final double[] vals = new double[elements.length * columndimension];
    for(int i = 0; i < elements.length; i++) {
      for(int j = 0; j < columndimension; j++) {
        vals[i + j * elements.length] = elements[i][j];
      }
    }
    return vals;
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
  public final Matrix getMatrix(final int i0, final int i1, final int j0, final int j1) {
    final Matrix X = new Matrix(i1 - i0 + 1, j1 - j0 + 1);
    try {
      for(int i = i0; i <= i1; i++) {
        for(int j = j0; j <= j1; j++) {
          X.elements[i - i0][j - j0] = elements[i][j];
        }
      }
    }
    catch(ArrayIndexOutOfBoundsException e) {
      throw new ArrayIndexOutOfBoundsException(ERR_REINDEX);
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
  public final Matrix getMatrix(final int[] r, final int[] c) {
    final Matrix X = new Matrix(r.length, c.length);
    try {
      for(int i = 0; i < r.length; i++) {
        for(int j = 0; j < c.length; j++) {
          X.elements[i][j] = elements[r[i]][c[j]];
        }
      }
    }
    catch(ArrayIndexOutOfBoundsException e) {
      throw new ArrayIndexOutOfBoundsException(ERR_REINDEX);
    }
    return X;
  }

  /**
   * Get a submatrix.
   * 
   * @param r Array of row indices.
   * @param j0 Initial column index
   * @param j1 Final column index
   * @return A(r(:),j0:j1)
   * @throws ArrayIndexOutOfBoundsException Submatrix indices
   */
  public final Matrix getMatrix(final int[] r, final int j0, final int j1) {
    final Matrix X = new Matrix(r.length, j1 - j0 + 1);
    try {
      for(int i = 0; i < r.length; i++) {
        for(int j = j0; j <= j1; j++) {
          X.elements[i][j - j0] = elements[r[i]][j];
        }
      }
    }
    catch(ArrayIndexOutOfBoundsException e) {
      throw new ArrayIndexOutOfBoundsException(ERR_REINDEX);
    }
    return X;
  }

  /**
   * Get a submatrix.
   * 
   * @param i0 Initial row index
   * @param i1 Final row index
   * @param c Array of column indices.
   * @return A(i0:i1,c(:))
   * @throws ArrayIndexOutOfBoundsException Submatrix indices
   */
  public final Matrix getMatrix(final int i0, final int i1, final int[] c) {
    final Matrix X = new Matrix(i1 - i0 + 1, c.length);
    try {
      for(int i = i0; i <= i1; i++) {
        for(int j = 0; j < c.length; j++) {
          X.elements[i - i0][j] = elements[i][c[j]];
        }
      }
    }
    catch(ArrayIndexOutOfBoundsException e) {
      throw new ArrayIndexOutOfBoundsException(ERR_REINDEX);
    }
    return X;
  }

  /**
   * Set a submatrix.
   * 
   * @param i0 Initial row index
   * @param i1 Final row index
   * @param j0 Initial column index
   * @param j1 Final column index
   * @param X A(i0:i1,j0:j1)
   * @throws ArrayIndexOutOfBoundsException Submatrix indices
   */
  public final void setMatrix(final int i0, final int i1, final int j0, final int j1, final Matrix X) {
    try {
      for(int i = i0; i <= i1; i++) {
        for(int j = j0; j <= j1; j++) {
          elements[i][j] = X.elements[i - i0][j - j0];
        }
      }
    }
    catch(ArrayIndexOutOfBoundsException e) {
      throw new ArrayIndexOutOfBoundsException(ERR_REINDEX);
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
  public final void setMatrix(final int[] r, final int[] c, final Matrix X) {
    try {
      for(int i = 0; i < r.length; i++) {
        for(int j = 0; j < c.length; j++) {
          elements[r[i]][c[j]] = X.elements[i][j];
        }
      }
    }
    catch(ArrayIndexOutOfBoundsException e) {
      throw new ArrayIndexOutOfBoundsException(ERR_REINDEX);
    }
  }

  /**
   * Set a submatrix.
   * 
   * @param r Array of row indices.
   * @param j0 Initial column index
   * @param j1 Final column index
   * @param X A(r(:),j0:j1)
   * @throws ArrayIndexOutOfBoundsException Submatrix indices
   */
  public final void setMatrix(final int[] r, final int j0, final int j1, final Matrix X) {
    try {
      for(int i = 0; i < r.length; i++) {
        for(int j = j0; j <= j1; j++) {
          elements[r[i]][j] = X.elements[i][j - j0];
        }
      }
    }
    catch(ArrayIndexOutOfBoundsException e) {
      throw new ArrayIndexOutOfBoundsException(ERR_REINDEX);
    }
  }

  /**
   * Set a submatrix.
   * 
   * @param i0 Initial row index
   * @param i1 Final row index
   * @param c Array of column indices.
   * @param X A(i0:i1,c(:))
   * @throws ArrayIndexOutOfBoundsException Submatrix indices
   */
  public final void setMatrix(final int i0, final int i1, final int[] c, final Matrix X) {
    try {
      for(int i = i0; i <= i1; i++) {
        for(int j = 0; j < c.length; j++) {
          elements[i][c[j]] = X.elements[i - i0][j];
        }
      }
    }
    catch(ArrayIndexOutOfBoundsException e) {
      throw new ArrayIndexOutOfBoundsException(ERR_REINDEX);
    }
  }

  /**
   * Returns the <code>i</code>th row of this matrix as vector.
   * 
   * @param i the index of the row to be returned
   * @return the <code>i</code>th row of this matrix
   */
  public final Vector getRow(final int i) {
    double[] row = elements[i].clone();
    return new Vector(row);
  }

  /**
   * Sets the <code>j</code>th row of this matrix to the specified vector.
   * 
   * @param j the index of the column to be set
   * @param row the value of the column to be set
   */
  public final void setRow(final int j, final Vector row) {
    if(row.elements.length != columndimension) {
      throw new IllegalArgumentException(ERR_MATRIX_DIMENSIONS);
    }
    for(int i = 0; i < columndimension; i++) {
      elements[j][i] = row.elements[i];
    }
  }

  /**
   * Returns the <code>j</code>th column of this matrix as vector.
   * 
   * @param j the index of the column to be returned
   * @return the <code>j</code>th column of this matrix
   */
  public final Vector getCol(final int j) {
    final Vector v = new Vector(elements.length);
    for(int i = 0; i < elements.length; i++) {
      v.elements[i] = elements[i][j];
    }
    return v;
  }

  /**
   * Sets the <code>j</code>th column of this matrix to the specified column.
   * 
   * @param j the index of the column to be set
   * @param column the value of the column to be set
   */
  public final void setCol(final int j, final Vector column) {
    if(column.elements.length != elements.length) {
      throw new IllegalArgumentException(ERR_MATRIX_DIMENSIONS);
    }
    for(int i = 0; i < elements.length; i++) {
      elements[i][j] = column.elements[i];
    }
  }

  /**
   * Matrix transpose.
   * 
   * @return A<sup>T</sup>
   */
  public final Matrix transpose() {
    final Matrix X = new Matrix(columndimension, elements.length);
    for(int i = 0; i < elements.length; i++) {
      for(int j = 0; j < columndimension; j++) {
        X.elements[j][i] = elements[i][j];
      }
    }
    return X;
  }

  /**
   * C = A + B
   * 
   * @param B another matrix
   * @return A + B in a new Matrix
   */
  public final Matrix plus(final Matrix B) {
    return copy().plusEquals(B);
  }

  /**
   * C = A + s * B
   * 
   * @param B another matrix
   * @param s scalar
   * @return A + s * B in a new Matrix
   */
  public final Matrix plusTimes(final Matrix B, final double s) {
    return copy().plusTimesEquals(B, s);
  }

  /**
   * A = A + B
   * 
   * @param B another matrix
   * @return A + B in this Matrix
   */
  public final Matrix plusEquals(final Matrix B) {
    checkMatrixDimensions(B);
    for(int i = 0; i < elements.length; i++) {
      for(int j = 0; j < columndimension; j++) {
        elements[i][j] += B.elements[i][j];
      }
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
  public final Matrix plusTimesEquals(final Matrix B, final double s) {
    checkMatrixDimensions(B);
    for(int i = 0; i < elements.length; i++) {
      for(int j = 0; j < columndimension; j++) {
        elements[i][j] += s * B.elements[i][j];
      }
    }
    return this;
  }

  /**
   * C = A - B
   * 
   * @param B another matrix
   * @return A - B in a new Matrix
   */
  public final Matrix minus(final Matrix B) {
    return copy().minusEquals(B);
  }

  /**
   * C = A - s * B
   * 
   * @param B another matrix
   * @param s Scalar
   * @return A - s * B in a new Matrix
   */
  public final Matrix minusTimes(final Matrix B, final double s) {
    return copy().minusTimesEquals(B, s);
  }

  /**
   * A = A - B
   * 
   * @param B another matrix
   * @return A - B in this Matrix
   */
  public final Matrix minusEquals(final Matrix B) {
    checkMatrixDimensions(B);
    for(int i = 0; i < elements.length; i++) {
      for(int j = 0; j < columndimension; j++) {
        elements[i][j] -= B.elements[i][j];
      }
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
  public final Matrix minusTimesEquals(final Matrix B, final double s) {
    checkMatrixDimensions(B);
    for(int i = 0; i < elements.length; i++) {
      for(int j = 0; j < columndimension; j++) {
        elements[i][j] -= s * B.elements[i][j];
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
  public final Matrix times(final double s) {
    return copy().timesEquals(s);
  }

  /**
   * Multiply a matrix by a scalar in place, A = s*A
   * 
   * @param s scalar
   * @return replace A by s*A
   */
  public final Matrix timesEquals(final double s) {
    for(int i = 0; i < elements.length; i++) {
      for(int j = 0; j < columndimension; j++) {
        elements[i][j] *= s;
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
  public final Matrix times(final Matrix B) {
    // Optimized implementation, exploiting the storage layout
    if(B.elements.length != this.columndimension) {
      throw new IllegalArgumentException(ERR_MATRIX_INNERDIM);
    }
    final Matrix X = new Matrix(this.elements.length, B.columndimension);
    // Optimized ala Jama. jik order.
    final double[] Bcolj = new double[this.columndimension];
    for(int j = 0; j < X.columndimension; j++) {
      // Make a linear copy of column j from B
      for(int k = 0; k < this.columndimension; k++) {
        Bcolj[k] = B.elements[k][j];
      }
      // multiply it with each row from A
      for(int i = 0; i < this.elements.length; i++) {
        final double[] Arowi = this.elements[i];
        double s = 0;
        for(int k = 0; k < this.columndimension; k++) {
          s += Arowi[k] * Bcolj[k];
        }
        X.elements[i][j] = s;
      }
    }
    return X;
  }

  /**
   * Linear algebraic matrix multiplication, A * B
   * 
   * @param B a vector
   * @return Matrix product, A * B
   * @throws IllegalArgumentException Matrix inner dimensions must agree.
   */
  public final Vector times(final Vector B) {
    if(B.elements.length != this.columndimension) {
      throw new IllegalArgumentException(ERR_MATRIX_INNERDIM);
    }
    final Vector X = new Vector(this.elements.length);
    // multiply it with each row from A
    for(int i = 0; i < this.elements.length; i++) {
      final double[] Arowi = this.elements[i];
      double s = 0;
      for(int k = 0; k < this.columndimension; k++) {
        s += Arowi[k] * B.elements[k];
      }
      X.elements[i] = s;
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
  public final Vector transposeTimes(final Vector B) {
    if(B.elements.length != elements.length) {
      throw new IllegalArgumentException(ERR_MATRIX_INNERDIM);
    }
    final Vector X = new Vector(this.columndimension);
    // multiply it with each row from A
    for(int i = 0; i < this.columndimension; i++) {
      double s = 0;
      for(int k = 0; k < elements.length; k++) {
        s += elements[k][i] * B.elements[k];
      }
      X.elements[i] = s;
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
    if(B.elements.length != elements.length) {
      throw new IllegalArgumentException(ERR_MATRIX_INNERDIM);
    }
    final Matrix X = new Matrix(this.columndimension, B.columndimension);
    final double[] Bcolj = new double[elements.length];
    for(int j = 0; j < X.columndimension; j++) {
      // Make a linear copy of column j from B
      for(int k = 0; k < elements.length; k++) {
        Bcolj[k] = B.elements[k][j];
      }
      // multiply it with each row from A
      for(int i = 0; i < this.columndimension; i++) {
        double s = 0;
        for(int k = 0; k < elements.length; k++) {
          s += elements[k][i] * Bcolj[k];
        }
        X.elements[i][j] = s;
      }
    }
    return X;
  }

  /**
   * Linear algebraic matrix multiplication, A * B^T
   * 
   * @param B another matrix
   * @return Matrix product, A * B^T
   * @throws IllegalArgumentException Matrix inner dimensions must agree.
   */
  public final Matrix timesTranspose(final Matrix B) {
    if(B.columndimension != this.columndimension) {
      throw new IllegalArgumentException(ERR_MATRIX_INNERDIM);
    }
    final Matrix X = new Matrix(this.elements.length, B.elements.length);
    for(int j = 0; j < X.elements.length; j++) {
      final double[] Browj = B.elements[j];
      // multiply it with each row from A
      for(int i = 0; i < this.elements.length; i++) {
        final double[] Arowi = this.elements[i];
        double s = 0;
        for(int k = 0; k < this.columndimension; k++) {
          s += Arowi[k] * Browj[k];
        }
        X.elements[i][j] = s;
      }
    }
    return X;
  }

  /**
   * Linear algebraic matrix multiplication, A^T * B^T. Computed as (B*A)^T
   * 
   * @param B another matrix
   * @return Matrix product, A^T * B^T
   * @throws IllegalArgumentException Matrix inner dimensions must agree.
   */
  public final Matrix transposeTimesTranspose(Matrix B) {
    // Optimized implementation, exploiting the storage layout
    if(this.elements.length != B.columndimension) {
      throw new IllegalArgumentException("Matrix inner dimensions must agree: " + getRowDimensionality() + "," + getColumnDimensionality() + " * " + B.getRowDimensionality() + "," + B.getColumnDimensionality());
    }
    final Matrix X = new Matrix(this.columndimension, B.elements.length);
    // Optimized ala Jama. jik order.
    final double[] Acolj = new double[this.elements.length];
    for(int j = 0; j < X.elements.length; j++) {
      // Make a linear copy of column j from B
      for(int k = 0; k < this.elements.length; k++) {
        Acolj[k] = this.elements[k][j];
      }
      final double[] Xrow = X.elements[j];
      // multiply it with each row from A
      for(int i = 0; i < B.elements.length; i++) {
        final double[] Browi = B.elements[i];
        double s = 0;
        for(int k = 0; k < B.columndimension; k++) {
          s += Browi[k] * Acolj[k];
        }
        Xrow[i] = s;
      }
    }
    return X;
  }

  /**
   * Solve A*X = B
   * 
   * @param B right hand side
   * @return solution if A is square, least squares solution otherwise
   */
  public final Matrix solve(final Matrix B) {
    return (elements.length == columndimension ? (new LUDecomposition(this)).solve(B) : (new QRDecomposition(this)).solve(B));
  }

  /**
   * Matrix inverse or pseudoinverse
   * 
   * @return inverse(A) if A is square, pseudoinverse otherwise.
   */
  public final Matrix inverse() {
    return solve(identity(elements.length, elements.length));
  }

  /**
   * Matrix determinant
   * 
   * @return determinant
   */
  public final double det() {
    return new LUDecomposition(this).det();
  }

  /**
   * Matrix rank
   * 
   * @return effective numerical rank, obtained from SVD.
   */
  public final int rank() {
    return new SingularValueDecomposition(this).rank();
  }

  /**
   * Matrix condition (2 norm)
   * 
   * @return ratio of largest to smallest singular value.
   */
  public final double cond() {
    return new SingularValueDecomposition(this).cond();
  }

  /**
   * Matrix trace.
   * 
   * @return sum of the diagonal elements.
   */
  public final double trace() {
    double t = 0;
    for(int i = 0; i < Math.min(elements.length, columndimension); i++) {
      t += elements[i][i];
    }
    return t;
  }

  /**
   * One norm
   * 
   * @return maximum column sum.
   */
  public final double norm1() {
    double f = 0;
    for(int j = 0; j < columndimension; j++) {
      double s = 0;
      for(int i = 0; i < elements.length; i++) {
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
  public final double norm2() {
    return (new SingularValueDecomposition(this).norm2());
  }

  /**
   * Infinity norm
   * 
   * @return maximum row sum.
   */
  public final double normInf() {
    double f = 0;
    for(int i = 0; i < elements.length; i++) {
      double s = 0;
      for(int j = 0; j < columndimension; j++) {
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
  public final double normF() {
    double f = 0;
    for(int i = 0; i < elements.length; i++) {
      for(int j = 0; j < columndimension; j++) {
        f = MathUtil.fastHypot(f, elements[i][j]);
      }
    }
    return f;
  }

  /**
   * Normalizes the columns of this matrix to length of 1.0.
   */
  public final void normalizeColumns() {
    for(int col = 0; col < columndimension; col++) {
      double norm = 0.0;
      for(int row = 0; row < elements.length; row++) {
        norm = norm + (elements[row][col] * elements[row][col]);
      }
      norm = Math.sqrt(norm);
      if(norm != 0) {
        for(int row = 0; row < elements.length; row++) {
          elements[row][col] /= norm;
        }
      }
      // TODO: else: throw an exception?
    }
  }

  /**
   * Returns true if the specified column matrix <code>a</code> is linearly
   * independent to the columns of this matrix. Linearly independence is given,
   * if the matrix resulting from appending <code>a</code> to this matrix has
   * full rank.
   * 
   * @param columnMatrix the column matrix to be tested for linear independence
   * @return true if the specified column matrix is linearly independent to the
   *         columns of this matrix
   */
  public final boolean linearlyIndependent(final Matrix columnMatrix) {
    if(columnMatrix.columndimension != 1) {
      throw new IllegalArgumentException("a.getColumnDimension() != 1");
    }
    if(this.elements.length != columnMatrix.elements.length) {
      throw new IllegalArgumentException(ERR_MATRIX_DIMENSIONS);
    }
    if(this.columndimension + columnMatrix.columndimension > this.elements.length) {
      return false;
    }
    final StringBuffer msg = LoggingConfiguration.DEBUG ? new StringBuffer() : null;

    final double[][] a = new double[columndimension + 1][elements.length - 1];
    final double[] b = new double[columndimension + 1];

    for(int i = 0; i < a.length; i++) {
      for(int j = 0; j < a[i].length; j++) {
        if(i < columndimension) {
          a[i][j] = elements[j][i];
        }
        else {
          a[i][j] = columnMatrix.elements[j][0];
        }
      }
    }

    for(int i = 0; i < b.length; i++) {
      if(i < columndimension) {
        b[i] = elements[elements.length - 1][i];
      }
      else {
        b[i] = columnMatrix.elements[i][0];
      }
    }

    final LinearEquationSystem les = new LinearEquationSystem(a, b);
    les.solveByTotalPivotSearch();

    final double[][] coefficients = les.getCoefficents();
    final double[] rhs = les.getRHS();

    if(msg != null) {
      msg.append("\na' ").append(FormatUtil.format(this.getArrayRef()));
      msg.append("\nb' ").append(FormatUtil.format(columnMatrix.getColumnPackedCopy()));

      msg.append("\na ").append(FormatUtil.format(a));
      msg.append("\nb ").append(FormatUtil.format(b));
      msg.append("\nleq ").append(les.equationsToString(4));
    }

    for(int i = 0; i < coefficients.length; i++) {
      boolean allCoefficientsZero = true;
      for(int j = 0; j < coefficients[i].length; j++) {
        final double value = coefficients[i][j];
        if(Math.abs(value) > DELTA) {
          allCoefficientsZero = false;
          break;
        }
      }
      // allCoefficients=0 && rhs=0 -> linearly dependent
      if(allCoefficientsZero) {
        final double value = rhs[i];
        if(Math.abs(value) < DELTA) {
          if(msg != null) {
            msg.append("\nvalue ").append(value).append("[").append(i).append("]");
            msg.append("\nlinearly independent ").append(false);
            Logger.getLogger(this.getClass().getName()).fine(msg.toString());
          }
          return false;
        }
      }
    }

    if(msg != null) {
      msg.append("\nlinearly independent ").append(true);
      Logger.getLogger(this.getClass().getName()).fine(msg.toString());
    }
    return true;
  }

  /**
   * Returns a matrix derived by Gauss-Jordan-elimination using RationalNumbers
   * for the transformations.
   * 
   * @return a matrix derived by Gauss-Jordan-elimination using RationalNumbers
   *         for the transformations
   */
  public final Matrix exactGaussJordanElimination() {
    final RationalNumber[][] gauss = exactGaussElimination();

    // reduced form
    for(int row = gauss.length - 1; row > 0; row--) {
      int firstCol = -1;
      for(int col = 0; col < gauss[row].length && firstCol == -1; col++) {
        // if(gauss.get(row, col) != 0.0) // i.e. == 1
        if(gauss[row][col].equals(RationalNumber.ONE)) {
          firstCol = col;
        }
      }
      if(firstCol > -1) {
        for(int currentRow = row - 1; currentRow >= 0; currentRow--) {
          RationalNumber multiplier = gauss[currentRow][firstCol].copy();
          for(int col = firstCol; col < gauss[currentRow].length; col++) {
            RationalNumber subtrahent = gauss[row][col].times(multiplier);
            gauss[currentRow][col] = gauss[currentRow][col].minus(subtrahent);
          }
        }
      }
    }
    return new Matrix(gauss);
  }

  /**
   * Perform an exact Gauss-elimination of this Matrix using RationalNumbers to
   * yield highest possible accuracy.
   * 
   * @return an array of arrays of RationalNumbers representing the
   *         Gauss-eliminated form of this Matrix
   */
  private final RationalNumber[][] exactGaussElimination() {
    final RationalNumber[][] gauss = new RationalNumber[elements.length][this.columndimension];
    for(int row = 0; row < elements.length; row++) {
      for(int col = 0; col < this.columndimension; col++) {
        gauss[row][col] = new RationalNumber(elements[row][col]);
      }
    }
    return exactGaussElimination(gauss);
  }

  /**
   * Perform recursive Gauss-elimination on the given matrix of RationalNumbers.
   * 
   * @param gauss an array of arrays of RationalNumber
   * @return recursive derived Gauss-elimination-form of the given matrix of
   *         RationalNumbers
   */
  private static final RationalNumber[][] exactGaussElimination(final RationalNumber[][] gauss) {
    int firstCol = -1;
    int firstRow = -1;

    // 1. find first column unequal to zero
    for(int col = 0; col < gauss[0].length && firstCol == -1; col++) {
      for(int row = 0; row < gauss.length && firstCol == -1; row++) {
        // if(gauss.get(row, col) != 0.0)
        if(!gauss[row][col].equals(RationalNumber.ZERO)) {
          firstCol = col;
          firstRow = row;
        }
      }
    }

    // 2. set row as first row
    if(firstCol != -1) {
      if(firstRow != 0) {
        final RationalNumber[] row = new RationalNumber[gauss[firstRow].length];
        System.arraycopy(gauss[firstRow], 0, row, 0, gauss[firstRow].length);
        System.arraycopy(gauss[0], 0, gauss[firstRow], 0, gauss[firstRow].length);
        System.arraycopy(row, 0, gauss[0], 0, row.length);
      }

      // 3. create leading 1
      if(!gauss[0][firstCol].equals(RationalNumber.ONE)) {
        final RationalNumber inverse = gauss[0][firstCol].multiplicativeInverse();
        for(int col = 0; col < gauss[0].length; col++) {
          gauss[0][col] = gauss[0][col].times(inverse);
        }
      }

      // 4. eliminate values unequal to zero below leading 1
      for(int row = 1; row < gauss.length; row++) {
        final RationalNumber multiplier = gauss[row][firstCol].copy();
        // if(multiplier != 0.0)
        if(!multiplier.equals(RationalNumber.ZERO)) {
          for(int col = firstCol; col < gauss[row].length; col++) {
            final RationalNumber subtrahent = gauss[0][col].times(multiplier);
            gauss[row][col] = gauss[row][col].minus(subtrahent);
          }
        }
      }

      // 5. recursion
      if(gauss.length > 1) {
        final RationalNumber[][] subMatrix = new RationalNumber[gauss.length - 1][gauss[1].length];
        System.arraycopy(gauss, 1, subMatrix, 0, gauss.length - 1);
        final RationalNumber[][] eliminatedSubMatrix = exactGaussElimination(subMatrix);
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
  public final boolean isSymmetric() {
    if(elements.length != columndimension) {
      return false;
    }
    for(int i = 0; i < elements.length; i++) {
      for(int j = i + 1; j < columndimension; j++) {
        if(elements[i][j] != elements[j][i]) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Completes this d x c basis of a subspace of R^d to a d x d basis of R^d,
   * i.e. appends c-d columns to this basis.
   * 
   * @return the appended columns
   */
  public final Matrix completeBasis() {
    Matrix basis = copy();
    Matrix result = null;
    for(int i = 0; i < elements.length; i++) {
      final Matrix e_i = new Matrix(elements.length, 1);
      e_i.elements[0][i] = 1.0;
      final boolean li = basis.linearlyIndependent(e_i);

      // TODO: efficiency - appendColumns is expensive.
      if(li) {
        if(result == null) {
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
  public final Matrix completeToOrthonormalBasis() {
    Matrix basis = copy();
    Matrix result = null;
    for(int i = 0; i < elements.length; i++) {
      final Matrix e_i = new Matrix(elements.length, 1);
      e_i.elements[i][0] = 1.0;
      final boolean li = basis.linearlyIndependent(e_i);

      // TODO: efficiency - appendColumns is expensive.
      if(li) {
        if(result == null) {
          result = e_i.copy();
        }
        else {
          result = result.appendColumns(e_i);
        }
        basis = basis.appendColumns(e_i);
      }
    }
    basis = basis.orthonormalize();
    return basis.getMatrix(0, basis.elements.length - 1, columndimension, basis.columndimension - 1);
  }

  /**
   * Returns a matrix which consists of this matrix and the specified columns.
   * 
   * @param columns the columns to be appended
   * @return the new matrix with the appended columns
   */
  public final Matrix appendColumns(final Matrix columns) {
    if(elements.length != columns.elements.length) {
      throw new IllegalArgumentException(ERR_MATRIX_DIMENSIONS);
    }

    final Matrix result = new Matrix(elements.length, columndimension + columns.columndimension);
    for(int i = 0; i < result.columndimension; i++) {
      // FIXME: optimize - excess copying!
      if(i < columndimension) {
        result.setCol(i, getCol(i));
      }
      else {
        result.setCol(i, columns.getCol(i - columndimension));
      }
    }
    return result;
  }

  /**
   * Returns an orthonormalization of this matrix.
   * 
   * @return the orthonormalized matrix
   */
  public final Matrix orthonormalize() {
    Matrix v = copy();

    // FIXME: optimize - excess copying!
    for(int i = 1; i < columndimension; i++) {
      final Vector u_i = getCol(i);
      final Vector sum = new Vector(elements.length);
      for(int j = 0; j < i; j++) {
        final Vector v_j = v.getCol(j);
        double scalar = u_i.transposeTimes(v_j) / v_j.transposeTimes(v_j);
        sum.plusTimesEquals(v_j, scalar);
      }
      final Vector v_i = u_i.minus(sum);
      v.setCol(i, v_i);
    }

    v.normalizeColumns();
    return v;
  }

  /**
   * Adds a given value to the diagonal entries if the entry is smaller than the
   * constant.
   * 
   * @param constant value to add to the diagonal entries
   * @return a new Matrix differing from this Matrix by the given value added to
   *         the diagonal entries
   */
  public final Matrix cheatToAvoidSingularity(final double constant) {
    final Matrix a = this.copy();
    for(int i = 0; i < a.columndimension && i < a.elements.length; i++) {
      // if(a.get(i, i) < constant)
      {
        a.elements[i][i] += constant;
      }
    }
    return a;
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
  public static final Matrix read(final BufferedReader input) throws java.io.IOException {
    final StreamTokenizer tokenizer = new StreamTokenizer(input);

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
    while(tokenizer.nextToken() == StreamTokenizer.TT_EOL) {
      // ignore initial empty lines
    }
    if(tokenizer.ttype == StreamTokenizer.TT_EOF) {
      throw new java.io.IOException("Unexpected EOF on matrix read.");
    }
    do {
      v.addElement(Double.valueOf(tokenizer.sval)); // Read & store 1st
      // row.
    }
    while(tokenizer.nextToken() == StreamTokenizer.TT_WORD);

    int n = v.size(); // Now we've got the number of columns!
    double row[] = new double[n];
    for(int j = 0; j < n; j++) {
      // extract the elements of the 1st row.
      row[j] = v.elementAt(j);
    }
    // v.removeAllElements();
    java.util.Vector<double[]> rowV = new java.util.Vector<double[]>();
    rowV.addElement(row); // Start storing rows instead of columns.
    while(tokenizer.nextToken() == StreamTokenizer.TT_WORD) {
      // While non-empty lines
      rowV.addElement(row = new double[n]);
      int j = 0;
      do {
        if(j >= n) {
          throw new java.io.IOException("Row " + v.size() + " is too long.");
        }
        row[j++] = (Double.valueOf(tokenizer.sval));
      }
      while(tokenizer.nextToken() == StreamTokenizer.TT_WORD);
      if(j < n) {
        throw new java.io.IOException("Row " + v.size() + " is too short.");
      }
    }
    int m = rowV.size(); // Now we've got the number of rows.
    double[][] A = new double[m][];
    rowV.copyInto(A); // copy the rows out of the vector
    return new Matrix(A);
  }

  /**
   * Check if size(A) == size(B)
   */
  protected void checkMatrixDimensions(Matrix B) {
    if(B.getRowDimensionality() != getRowDimensionality() || B.getColumnDimensionality() != getColumnDimensionality()) {
      throw new IllegalArgumentException("Matrix dimensions must agree.");
    }
  }

  @Override
  public int hashCode() {
    final int PRIME = 31;
    int result = 1;
    result = PRIME * result + Arrays.hashCode(this.elements);
    result = PRIME * result + this.elements.length;
    result = PRIME * result + this.columndimension;
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
    final Matrix other = (Matrix) obj;
    if(this.elements.length != other.elements.length) {
      return false;
    }
    if(this.columndimension != other.columndimension) {
      return false;
    }
    for(int i = 0; i < this.elements.length; i++) {
      for(int j = 0; j < this.columndimension; j++) {
        if(this.elements[i][j] != other.elements[i][j]) {
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
    final Matrix other = (Matrix) obj;
    if(this.elements.length != other.elements.length) {
      return false;
    }
    if(this.columndimension != other.columndimension) {
      return false;
    }
    for(int i = 0; i < this.elements.length; i++) {
      for(int j = 0; j < this.columndimension; j++) {
        if(Math.abs(this.elements[i][j] - other.elements[i][j]) > maxdelta) {
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

  /**
   * toString returns String-representation of Matrix.
   */
  @Override
  public String toString() {
    return FormatUtil.format(this);
  }
}