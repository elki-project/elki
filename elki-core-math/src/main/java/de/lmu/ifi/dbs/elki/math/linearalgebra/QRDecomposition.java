/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.math.linearalgebra;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.copy;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.unitMatrix;

import net.jafama.FastMath;

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
    this.QR = copy(A);
    if(m < n) {
      throw new IllegalArgumentException("Matrix does not satisfy rows >= columns!");
    }
    this.m = m;
    this.n = n;
    Rdiag = new double[n];

    // Main loop.
    for(int k = 0; k < n; k++) {
      // Compute 2-norm of k-th column without under/overflow.
      double nrm = 0;
      for(int i = k; i < m; i++) {
        nrm = FastMath.hypot(nrm, QR[i][k]);
      }

      if(nrm != 0.0) {
        final double[] QRk = QR[k];
        // Form k-th Householder vector.
        nrm = (QRk[k] < 0) ? -nrm : nrm;
        for(int i = k; i < m; i++) {
          QR[i][k] /= nrm;
        }
        QRk[k] += 1.0;

        // Apply transformation to remaining columns.
        for(int j = k + 1; j < n; j++) {
          double s = 0.0;
          for(int i = k; i < m; i++) {
            final double[] QRi = QR[i];
            s += QRi[k] * QRi[j];
          }
          s /= QRk[k];
          for(int i = k; i < m; i++) {
            final double[] QRi = QR[i];
            QRi[j] -= s * QRi[k];
          }
        }
      }
      Rdiag[k] = -nrm;
    }
  }

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
   * Get the matrix rank?
   * 
   * @param t Tolerance threshold
   * @return Rank of R
   */
  public int rank(double t) {
    int rank = n;
    for(int j = 0; j < n; j++) {
      if(Math.abs(Rdiag[j]) <= t) {
        --rank;
      }
    }
    return rank;
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
      System.arraycopy(QR[i], i + 1, R[i], i + 1, n - i - 1);
      R[i][i] = Rdiag[i];
    }
    return R;
  }

  /**
   * Generate and return the (economy-sized, m by n) orthogonal factor
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
          s /= QR[k][k];
          for(int i = k; i < m; i++) {
            Q[i][j] -= s * QR[i][k];
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
   * @throws IllegalArgumentException Matrix row dimensions must agree.
   * @throws ArithmeticException Matrix is rank deficient.
   */
  public double[][] solve(double[][] B) {
    return solveInplace(copy(B));
  }

  /**
   * Least squares solution of A*X = B
   * 
   * @param B A Matrix with as many rows as A and any number of columns.
   * @return X that minimizes the two norm of Q*R*X-B.
   * @throws IllegalArgumentException Matrix row dimensions must agree.
   * @throws ArithmeticException Matrix is rank deficient.
   */
  private double[][] solveInplace(double[][] X) {
    int rows = X.length, nx = X[0].length;
    if(rows != m) {
      throw new IllegalArgumentException("Matrix row dimensions must agree.");
    }
    if(!this.isFullRank()) {
      throw new ArithmeticException("Matrix is rank deficient.");
    }
    // Compute Y = transpose(Q)*B
    for(int k = 0; k < n; k++) {
      for(int j = 0; j < nx; j++) {
        double s = 0.0;
        for(int i = k; i < m; i++) {
          s += QR[i][k] * X[i][j];
        }
        s /= QR[k][k];
        for(int i = k; i < m; i++) {
          X[i][j] -= s * QR[i][k];
        }
      }
    }
    // Solve R*X = Y;
    for(int k = n - 1; k >= 0; k--) {
      final double[] Xk = X[k];
      for(int j = 0; j < nx; j++) {
        Xk[j] /= Rdiag[k];
      }
      for(int i = 0; i < k; i++) {
        final double[] Xi = X[i];
        final double QRik = QR[i][k];
        for(int j = 0; j < nx; j++) {
          Xi[j] -= Xk[j] * QRik;
        }
      }
    }
    return X;
  }

  /**
   * Least squares solution of A*X = b
   * 
   * @param b A column vector with as many rows as A.
   * @return X that minimizes the two norm of Q*R*X-b.
   * @throws IllegalArgumentException Matrix row dimensions must agree.
   * @throws ArithmeticException Matrix is rank deficient.
   */
  public double[] solve(double[] b) {
    return solveInplace(copy(b));
  }

  /**
   * Least squares solution of A*X = b
   * 
   * @param b A column vector with as many rows as A.
   * @return X that minimizes the two norm of Q*R*X-b.
   * @throws IllegalArgumentException Matrix row dimensions must agree.
   * @throws ArithmeticException Matrix is rank deficient.
   */
  public double[] solveInplace(double[] x) {
    if(x.length != m) {
      throw new IllegalArgumentException("Matrix row dimensions must agree.");
    }
    if(!this.isFullRank()) {
      throw new ArithmeticException("Matrix is rank deficient.");
    }
    // Compute Y = transpose(Q)*B
    for(int k = 0; k < n; k++) {
      double s = 0.0;
      for(int i = k; i < m; i++) {
        s += QR[i][k] * x[i];
      }
      s /= QR[k][k];
      for(int i = k; i < m; i++) {
        x[i] -= s * QR[i][k];
      }
    }
    // Solve R*X = Y;
    for(int k = n - 1; k >= 0; k--) {
      x[k] /= Rdiag[k];
      for(int i = 0; i < k; i++) {
        final double QRik = QR[i][k];
        x[i] -= x[k] * QRik;
      }
    }
    return x;
  }

  /**
   * Find the inverse matrix.
   *
   * @return Inverse matrix
   * @throws ArithmeticException Matrix is rank deficient.
   */
  public double[][] inverse() {
    return solveInplace(unitMatrix(m));
  }
}
