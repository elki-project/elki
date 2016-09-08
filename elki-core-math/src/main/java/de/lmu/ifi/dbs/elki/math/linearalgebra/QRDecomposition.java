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

import de.lmu.ifi.dbs.elki.math.MathUtil;

/**
 * QR Decomposition.
 * <P>
 * For an m-by-n matrix A with m >= n, the QR decomposition is an m-by-n
 * orthogonal matrix Q and an n-by-n upper triangular matrix R so that A = Q*R.
 * <P>
 * The QR decompostion always exists, even if the matrix does not have full
 * rank, so the constructor will never fail. The primary use of the QR
 * decomposition is in the least squares solution of nonsquare systems of
 * simultaneous linear equations. This will fail if isFullRank() returns false.
 * 
 * @author Arthur Zimek
 * @since 0.2
 *
 * @apiviz.uses Matrix - - transforms
 */
public class QRDecomposition implements java.io.Serializable {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Array for internal storage of decomposition.
   * 
   * @serial internal array storage.
   */
  private double[][] QR;

  /**
   * Row and column dimensions.
   * 
   * @serial column dimension.
   * @serial row dimension.
   */
  private int m, n;

  /**
   * Array for internal storage of diagonal of R.
   * 
   * @serial diagonal of R.
   */
  private double[] Rdiag;

  /*
   * ------------------------ Constructor ------------------------
   */

  /**
   * QR Decomposition, computed by Householder reflections.
   * 
   * @param A Rectangular matrix
   */
  public QRDecomposition(double[][] A) {
    this(A, A.length, A[0].length);
  }

  /**
   * QR Decomposition, computed by Householder reflections.
   * 
   * @param A Rectangular matrix
   * @param m row dimensionality
   * @param n column dimensionality
   */
  public QRDecomposition(double[][] A, int m, int n) {
    this.QR = VMath.copy(A);
    this.m = QR.length;
    this.n = QR[0].length;
    Rdiag = new double[n];

    // Main loop.
    for(int k = 0; k < n; k++) {
      // Compute 2-norm of k-th column without under/overflow.
      double nrm = 0;
      for(int i = k; i < m; i++) {
        nrm = MathUtil.fastHypot(nrm, QR[i][k]);
      }

      if(nrm != 0.0) {
        // Form k-th Householder vector.
        if(QR[k][k] < 0) {
          nrm = -nrm;
        }
        for(int i = k; i < m; i++) {
          QR[i][k] /= nrm;
        }
        QR[k][k] += 1.0;

        // Apply transformation to remaining columns.
        for(int j = k + 1; j < n; j++) {
          double s = 0.0;
          for(int i = k; i < m; i++) {
            s += QR[i][k] * QR[i][j];
          }
          s = -s / QR[k][k];
          for(int i = k; i < m; i++) {
            QR[i][j] += s * QR[i][k];
          }
        }
      }
      Rdiag[k] = -nrm;
    }
  }

  /*
   * ------------------------ Public Methods ------------------------
   */

  /**
   * Is the matrix full rank?
   * 
   * @return true if R, and hence A, has full rank.
   */
  public boolean isFullRank() {
    for(int j = 0; j < n; j++) {
      if(Rdiag[j] == 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * Return the Householder vectors
   * 
   * @return Lower trapezoidal matrix whose columns define the reflections
   */
  public double[][] getH() {
    double[][] H = new double[m][n];
    for(int i = 0; i < m; i++) {
      System.arraycopy(QR[i], i, H[i], i, n - i);
    }
    return H;
  }

  /**
   * Return the upper triangular factor
   * 
   * @return R
   */
  public double[][] getR() {
    double[][] R = new double[n][n];
    for(int i = 0; i < n; i++) {
      System.arraycopy(QR[i], 0, R[i], 0, i);
      R[i][i] = Rdiag[i];
    }
    return R;
  }

  /**
   * Generate and return the (economy-sized) orthogonal factor
   * 
   * @return Q
   */
  public double[][] getQ() {
    double[][] Q = new double[m][n];
    for(int k = n - 1; k >= 0; k--) {
      for(int i = 0; i < m; i++) {
        Q[i][k] = 0.0;
      }
      Q[k][k] = 1.0;
      for(int j = k; j < n; j++) {
        if(QR[k][k] != 0) {
          double s = 0.0;
          for(int i = k; i < m; i++) {
            s += QR[i][k] * Q[i][j];
          }
          s = -s / QR[k][k];
          for(int i = k; i < m; i++) {
            Q[i][j] += s * QR[i][k];
          }
        }
      }
    }
    return Q;
  }

  /**
   * Least squares solution of A*X = B
   * 
   * @param B A Matrix with as many rows as A and any number of columns.
   * @return X that minimizes the two norm of Q*R*X-B.
   * @exception IllegalArgumentException Matrix row dimensions must agree.
   * @exception RuntimeException Matrix is rank deficient.
   */
  public double[][] solve(double[][] B) {
    int rows = B.length;
    int cols = B[0].length;
    if(rows != m) {
      throw new IllegalArgumentException("Matrix row dimensions must agree.");
    }
    if(!this.isFullRank()) {
      throw new RuntimeException("Matrix is rank deficient.");
    }

    // Copy right hand side
    double[][] X = VMath.copy(B);

    solveInplace(X, cols);
    return VMath.getMatrix(X, 0, n - 1, 0, cols - 1);
  }

  private void solveInplace(double[][] X, int nx) {
    // Compute Y = transpose(Q)*B
    for(int k = 0; k < n; k++) {
      for(int j = 0; j < nx; j++) {
        double s = 0.0;
        for(int i = k; i < m; i++) {
          s += QR[i][k] * X[i][j];
        }
        s = -s / QR[k][k];
        for(int i = k; i < m; i++) {
          X[i][j] += s * QR[i][k];
        }
      }
    }
    // Solve R*X = Y;
    for(int k = n - 1; k >= 0; k--) {
      for(int j = 0; j < nx; j++) {
        X[k][j] /= Rdiag[k];
      }
      for(int i = 0; i < k; i++) {
        for(int j = 0; j < nx; j++) {
          X[i][j] -= X[k][j] * QR[i][k];
        }
      }
    }
  }
}