/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.*;

import java.util.Arrays;

import net.jafama.FastMath;

/**
 * Cholesky Decomposition.
 *
 * For a symmetric, positive definite matrix A, the Cholesky decomposition is an
 * lower triangular matrix L so that A = L*L^T.
 *
 * If the matrix is not symmetric or positive definite, the constructor returns
 * a partial decomposition and sets an internal flag that may be queried by the
 * isSPD() method.
 * 
 * @author Arthur Zimek
 * @since 0.1
 * 
 * @assoc - transforms - Matrix
 */
public class CholeskyDecomposition {
  /**
   * Array for internal storage of decomposition.
   */
  private double[][] L;

  /**
   * Symmetric and positive definite flag.
   */
  private boolean isspd;

  /**
   * Cholesky algorithm for symmetric and positive definite matrix.
   * 
   * @param A Square, symmetric matrix.
   */
  public CholeskyDecomposition(double[][] A) {
    final int n = A.length;
    L = new double[n][n];
    isspd = A[0].length == n;
    { // First iteration
      double d = A[0][0];
      isspd &= d > 0.0;
      L[0][0] = d > 0 ? FastMath.sqrt(d) : 0;
      Arrays.fill(L[0], 1, n, 0.0);
    }
    // Main loop.
    for(int j = 1; j < n; j++) {
      final double[] Lj = L[j], Aj = A[j];
      double d = 0.0;
      for(int k = 0; k < j; k++) {
        final double[] Lk = L[k];
        double s = 0.0;
        for(int i = 0; i < k; i++) {
          s += Lk[i] * Lj[i];
        }
        Lj[k] = s = (Aj[k] - s) / Lk[k];
        d += s * s;
        isspd &= (A[k][j] == Aj[k]);
      }
      d = Aj[j] - d;
      isspd &= d > 0.0;
      Lj[j] = d > 0 ? FastMath.sqrt(d) : 0;
      Arrays.fill(Lj, j + 1, n, 0.0);
    }
  }

  /**
   * Is the matrix symmetric and positive definite?
   * 
   * @return true if A is symmetric and positive definite.
   */
  public boolean isSPD() {
    return isspd;
  }

  /**
   * Return triangular factor.
   * 
   * @return L
   */
  public double[][] getL() {
    return L;
  }

  /**
   * Solve A*X = B
   * 
   * @param B A Matrix with as many rows as A and any number of columns.
   * @return X so that L*L^T*X = B
   * @exception IllegalArgumentException Matrix row dimensions must agree.
   * @exception RuntimeException Matrix is not symmetric positive definite.
   */
  public double[][] solve(double[][] B) {
    if(B.length != L.length) {
      throw new IllegalArgumentException(ERR_MATRIX_DIMENSIONS);
    }
    if(!isspd) {
      throw new ArithmeticException(ERR_MATRIX_NOT_SPD);
    }
    // Work on a copy!
    return solveLtransposed(solveL(copy(B)));
  }

  /**
   * Solve L*Y = B
   * 
   * @param X Copy of B.
   * @return X
   */
  private double[][] solveL(double[][] X) {
    final int n = L.length, m = X[0].length;
    { // First iteration, simplified.
      final double[] X0 = X[0], L0 = L[0];
      for(int j = 0; j < m; j++) {
        X0[j] /= L0[0];
      }
    }
    for(int k = 1; k < n; k++) {
      final double[] Xk = X[k], Lk = L[k];
      final double iLkk = 1. / Lk[k];
      for(int j = 0; j < m; j++) {
        double Xkj = Xk[j];
        for(int i = 0; i < k; i++) {
          Xkj -= X[i][j] * Lk[i];
        }
        Xk[j] = Xkj * iLkk;
      }
    }
    return X;
  }

  /**
   * Solve L^T*X = Y
   *
   * @param X Solution of L*Y=B
   * @return X
   */
  private double[][] solveLtransposed(double[][] X) {
    for(int k = L.length - 1; k >= 0; k--) {
      final double[] Lk = L[k], Xk = X[k];
      timesEquals(Xk, 1. / Lk[k]);
      for(int i = 0; i < k; i++) {
        minusTimesEquals(X[i], Xk, Lk[i]);
      }
    }
    return X;
  }

  /**
   * Solve A*X = b
   * 
   * @param b A column vector with as many rows as A.
   * @return X so that L*L^T*X = b
   * @exception IllegalArgumentException Matrix row dimensions must agree.
   * @exception RuntimeException Matrix is not symmetric positive definite.
   */
  public double[] solve(double[] b) {
    if(b.length != L.length) {
      throw new IllegalArgumentException(ERR_MATRIX_DIMENSIONS);
    }
    if(!isspd) {
      throw new ArithmeticException(ERR_MATRIX_NOT_SPD);
    }
    // Work on a copy!
    return solveLtransposed(solveLInplace(copy(b)));
  }

  /**
   * Solve L*X = b, <b>modifying X</b>.
   * 
   * @param X Copy of b.
   * @return X
   */
  public double[] solveLInplace(double[] X) {
    final int n = L.length;
    X[0] /= L[0][0]; // First iteration, simplified.
    for(int k = 1; k < n; k++) {
      final double[] Lk = L[k];
      for(int i = 0; i < k; i++) {
        X[k] -= X[i] * Lk[i];
      }
      X[k] /= Lk[k];
    }
    return X;
  }

  /**
   * Solve L^T*X = Y
   *
   * @param X Solution of L*Y=b
   * @return X
   */
  private double[] solveLtransposed(double[] X) {
    for(int k = L.length - 1; k >= 0; k--) {
      final double[] Lk = L[k];
      X[k] /= Lk[k];
      for(int i = 0; i < k; i++) {
        X[i] -= X[k] * Lk[i];
      }
    }
    return X;
  }
}
