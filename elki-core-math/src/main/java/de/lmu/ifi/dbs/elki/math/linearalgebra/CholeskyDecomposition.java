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
 * @since 0.2
 * 
 * @apiviz.uses Matrix - - transforms
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
    isspd = (A[0].length == n);
    // Main loop.
    for(int j = 0; j < n; j++) {
      final double[] Lj = L[j], Aj = A[j];
      double d = 0.0;
      for(int k = 0; k < j; k++) {
        final double[] Lk = L[k];
        double s = transposeTimes(Lk, Lj);
        Lj[k] = s = (Aj[k] - s) / Lk[k];
        d += s * s;
        isspd &= (A[k][j] == Aj[k]);
      }
      d = Aj[j] - d;
      isspd &= (d > 0.0);
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
      throw new IllegalArgumentException("Matrix row dimensions must agree.");
    }
    if(!isspd) {
      throw new ArithmeticException("Matrix is not symmetric positive definite.");
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
    final int n = L.length;
    for(int k = 0; k < n; k++) {
      final double[] Xk = X[k];
      for(int i = k + 1; i < n; i++) {
        plusTimesEquals(X[i], Xk, -L[i][k]);
      }
      timesEquals(Xk, 1. / L[k][k]);
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
        plusTimesEquals(X[i], Xk, -Lk[i]);
      }
    }
    return X;
  }
}
