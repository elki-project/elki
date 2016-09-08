package de.lmu.ifi.dbs.elki.math.linearalgebra;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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

/**
 * LU Decomposition.
 * <P>
 * For an m-by-n matrix A with m >= n, the LU decomposition is an m-by-n unit
 * lower triangular matrix L, an n-by-n upper triangular matrix U, and a
 * permutation vector piv of length m so that A(piv,:) = L*U. If m &lt; n, then
 * L is m-by-m and U is m-by-n.
 * <P>
 * The LU decompostion with pivoting always exists, even if the matrix is
 * singular, so the constructor will never fail. The primary use of the LU
 * decomposition is in the solution of square systems of simultaneous linear
 * equations. This will fail if isNonsingular() returns false.
 * 
 * @author Arthur Zimek
 * @since 0.2
 * 
 * @apiviz.uses Matrix - - transforms
 */
public class LUDecomposition implements java.io.Serializable {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Array for internal storage of decomposition.
   * 
   * @serial internal array storage.
   */
  private double[][] LU;

  /**
   * Row and column dimensions, and pivot sign.
   * 
   * @serial column dimension.
   * @serial row dimension.
   * @serial pivot sign.
   */
  private int m, n, pivsign;

  /**
   * Internal storage of pivot vector.
   * 
   * @serial pivot vector.
   */
  private int[] piv;

  /*
   * ------------------------ Constructor ------------------------
   */

  /**
   * LU Decomposition
   * 
   * @param LU Rectangular matrix
   */
  public LUDecomposition(double[][] LU) {
    this(LU, LU.length, LU[0].length);
  }

  /**
   * LU Decomposition
   * 
   * @param LU Rectangular matrix
   * @param m row dimensionality
   * @param n column dimensionality
   */
  public LUDecomposition(double[][] LU, int m, int n) {
    LU = VMath.copy(LU);
    this.LU = LU;
    this.m = m;
    this.n = n;
    // Use a "left-looking", dot-product, Crout/Doolittle algorithm.
    piv = new int[m];
    for (int i = 0; i < m; i++) {
      piv[i] = i;
    }
    pivsign = 1;
    double[] LUrowi;
    double[] LUcolj = new double[m];

    // Outer loop.

    for (int j = 0; j < n; j++) {
      // Make a copy of the j-th column to localize references.

      for (int i = 0; i < m; i++) {
        LUcolj[i] = LU[i][j];
      }

      // Apply previous transformations.

      for (int i = 0; i < m; i++) {
        LUrowi = LU[i];

        // Most of the time is spent in the following dot product.

        int kmax = Math.min(i, j);
        double s = 0.0;
        for (int k = 0; k < kmax; k++) {
          s += LUrowi[k] * LUcolj[k];
        }

        LUrowi[j] = LUcolj[i] -= s;
      }

      // Find pivot and exchange if necessary.

      int p = j;
      for (int i = j + 1; i < m; i++) {
        if (Math.abs(LUcolj[i]) > Math.abs(LUcolj[p])) {
          p = i;
        }
      }
      if (p != j) {
        for (int k = 0; k < n; k++) {
          double t = LU[p][k];
          LU[p][k] = LU[j][k];
          LU[j][k] = t;
        }
        int k = piv[p];
        piv[p] = piv[j];
        piv[j] = k;
        pivsign = -pivsign;
      }

      // Compute multipliers.

      if (j < m && LU[j][j] != 0.0) {
        for (int i = j + 1; i < m; i++) {
          LU[i][j] /= LU[j][j];
        }
      }
    }
  }

  /*
   * ------------------------ Public Methods ------------------------
   */

  /**
   * Is the matrix nonsingular?
   * 
   * @return true if U, and hence A, is nonsingular.
   */
  public boolean isNonsingular() {
    for (int j = 0; j < n; j++) {
      if (LU[j][j] == 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * Return lower triangular factor
   * 
   * @return L
   */
  public double[][] getL() {
    double[][] L = new double[m][n];
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        if (i > j) {
          L[i][j] = LU[i][j];
        } else if (i == j) {
          L[i][j] = 1.0;
        } else {
          L[i][j] = 0.0;
        }
      }
    }
    return L;
  }

  /**
   * Return upper triangular factor
   * 
   * @return U
   */
  public double[][] getU() {
    double[][] U = new double[n][n];
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        if (i <= j) {
          U[i][j] = LU[i][j];
        } else {
          U[i][j] = 0.0;
        }
      }
    }
    return U;
  }

  /**
   * Return pivot permutation vector
   * 
   * @return piv
   */
  public int[] getPivot() {
    int[] p = new int[m];
    System.arraycopy(piv, 0, p, 0, m);
    return p;
  }

  /**
   * Return pivot permutation vector as a one-dimensional double array
   * 
   * @return (double) piv
   */
  public double[] getDoublePivot() {
    double[] vals = new double[m];
    for (int i = 0; i < m; i++) {
      vals[i] = (double) piv[i];
    }
    return vals;
  }

  /**
   * Determinant
   * 
   * @return det(A)
   * @exception IllegalArgumentException Matrix must be square
   */
  public double det() {
    if (m != n) {
      throw new IllegalArgumentException("Matrix must be square.");
    }
    double d = pivsign;
    for (int j = 0; j < n; j++) {
      d *= LU[j][j];
    }
    return d;
  }

  /**
   * Solve A*X = B
   * 
   * @param B A Matrix with as many rows as A and any number of columns.
   * @return X so that L*U*X = B(piv,:)
   * @exception IllegalArgumentException Matrix row dimensions must agree.
   * @exception RuntimeException Matrix is singular.
   */
  public double[][] solve(double[][] B) {
    int mx = B.length;
    int nx = B[0].length;
    if (mx != m) {
      throw new IllegalArgumentException("Matrix row dimensions must agree.");
    }
    if (!this.isNonsingular()) {
      throw new RuntimeException("Matrix is singular.");
    }
    double[][] Xmat = VMath.getMatrix(B, piv, 0, nx - 1);
    solveInplace(Xmat, nx);
    return Xmat;
  }

  /**
   * Solve A*X = B
   * 
   * @param B A Matrix with as many rows as A and any number of columns.
   * @param nx Number of columns
   */
  private void solveInplace(double[][] B, int nx) {
    // Solve L*Y = B(piv,:)
    for (int k = 0; k < n; k++) {
      for (int i = k + 1; i < n; i++) {
        for (int j = 0; j < nx; j++) {
          B[i][j] -= B[k][j] * LU[i][k];
        }
      }
    }
    // Solve U*X = Y;
    for (int k = n - 1; k >= 0; k--) {
      for (int j = 0; j < nx; j++) {
        B[k][j] /= LU[k][k];
      }
      for (int i = 0; i < k; i++) {
        for (int j = 0; j < nx; j++) {
          B[i][j] -= B[k][j] * LU[i][k];
        }
      }
    }
  }
}
