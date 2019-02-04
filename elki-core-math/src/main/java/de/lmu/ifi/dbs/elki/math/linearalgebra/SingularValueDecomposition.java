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

import net.jafama.FastMath;

/**
 * Singular Value Decomposition.
 * <p>
 * For an m-by-n matrix A with m &gt;= n, the singular value decomposition is an
 * m-by-n orthogonal matrix U, an n-by-n diagonal matrix S, and an n-by-n
 * ozthogonal matrix V so that A = U*S*V'.
 * <p>
 * The singular values, sigma[k] = S[k][k], are ordered so that sigma[0] &gt;=
 * sigma[1] &gt;= ... &gt;= sigma[n-1].
 * <p>
 * The singular value decomposition always exists, so the constructor will never
 * fail. The matrix condition number and the effective numerical rank can be
 * computed from this decomposition.
 *
 * @author Arthur Zimek
 * @since 0.1
 *
 * @assoc - transforms - Matrix
 */
public class SingularValueDecomposition {
  /**
   * Arrays for internal storage of U and V.
   *
   * @serial internal storage of U.
   * @serial internal storage of V.
   */
  private double[][] U, V;

  /**
   * Array for internal storage of singular values.
   * 
   * @serial internal storage of singular values.
   */
  private double[] s;

  /**
   * Row and column dimensions.
   * 
   * @serial row dimension.
   * @serial column dimension.
   */
  private int m, n;

  /**
   * Constructor.
   *
   * @param Arg Rectangular input matrix
   */
  public SingularValueDecomposition(double[][] Arg) {
    double[][] A = VMath.copy(Arg);
    this.m = A.length;
    this.n = A[0].length;
    // Derived from LINPACK code.
    // Initialize.
    final int nu = Math.min(m, n);
    s = new double[Math.min(m + 1, n)];
    U = new double[m][nu];
    V = new double[n][n];
    double[] e = new double[n], work = new double[m];
    boolean wantu = true, wantv = true;

    // Reduce A to bidiagonal form, storing the diagonal elements
    // in s and the super-diagonal elements in e.

    final int nct = Math.min(m - 1, n);
    final int nrt = Math.max(0, Math.min(n - 2, m));
    for(int k = 0; k < Math.max(nct, nrt); k++) {
      if(k < nct) {
        // Compute the transformation for the k-th column and
        // place the k-th diagonal in s[k].
        // Compute 2-norm of k-th column without under/overflow.
        double sk = A[k][k];
        for(int i = k + 1; i < m; i++) {
          sk = FastMath.hypot(sk, A[i][k]);
        }
        if(sk != 0.0) {
          sk = (A[k][k] < 0.0) ? -sk : sk;
          for(int i = k; i < m; i++) {
            A[i][k] /= sk;
          }
          A[k][k] += 1.0;
        }
        s[k] = -sk;
      }
      for(int j = k + 1; j < n; j++) {
        if((k < nct) && (s[k] != 0.0)) {
          // Apply the transformation.
          double t = 0;
          for(int i = k; i < m; i++) {
            t += A[i][k] * A[i][j];
          }
          t /= -A[k][k];
          for(int i = k; i < m; i++) {
            A[i][j] += t * A[i][k];
          }
        }

        // Place the k-th row of A into e for the
        // subsequent calculation of the row transformation.
        e[j] = A[k][j];
      }
      if(wantu && (k < nct)) {
        // Place the transformation in U for subsequent back
        // multiplication.
        for(int i = k; i < m; i++) {
          U[i][k] = A[i][k];
        }
      }
      if(k < nrt) {
        // Compute the k-th row transformation and place the
        // k-th super-diagonal in e[k].
        // Compute 2-norm without under/overflow.
        double ek = e[k + 1];
        for(int i = k + 2; i < n; i++) {
          ek = FastMath.hypot(ek, e[i]);
        }
        if(ek != 0.0) {
          ek = (e[k + 1] < 0.0) ? -ek : ek;
          for(int i = k + 1; i < n; i++) {
            e[i] /= ek;
          }
          e[k + 1] += 1.0;
        }
        e[k] = -ek;
        if((k + 1 < m) && (ek != 0.0)) {
          // Apply the transformation.
          for(int i = k + 1; i < m; i++) {
            work[i] = 0.0;
          }
          for(int j = k + 1; j < n; j++) {
            for(int i = k + 1; i < m; i++) {
              work[i] += e[j] * A[i][j];
            }
          }
          for(int j = k + 1; j < n; j++) {
            double t = -e[j] / e[k + 1];
            for(int i = k + 1; i < m; i++) {
              A[i][j] += t * work[i];
            }
          }
        }
        if(wantv) {
          // Place the transformation in V for subsequent
          // back multiplication.
          for(int i = k + 1; i < n; i++) {
            V[i][k] = e[i];
          }
        }
      }
    }

    // Set up the final bidiagonal matrix or order p.
    int p = Math.min(n, m + 1);
    if(nct < n) {
      s[nct] = A[nct][nct];
    }
    if(m < p) {
      s[p - 1] = 0.0;
    }
    if(nrt + 1 < p) {
      e[nrt] = A[nrt][p - 1];
    }
    e[p - 1] = 0.0;

    // If required, generate U.
    if(wantu) {
      generateU(nu, nct);
    }

    // If required, generate V.
    if(wantv) {
      generateV(nu, e, nrt);
    }

    // Main iteration loop for the singular values.

    final int pp = p - 1;
    final double eps = 0x1p-52, tiny = 0x1p-966;
    while(p > 0) {
      // TODO: add an iteration limit.

      // This section of the program inspects for
      // negligible elements in the s and e arrays.
      int k;
      for(k = p - 2; k > -1; k--) {
        if(Math.abs(e[k]) <= tiny + eps * (Math.abs(s[k]) + Math.abs(s[k + 1]))) {
          e[k] = 0.0;
          break;
        }
      }
      if(k == p - 2) {
        // e(p-1) is negligible (convergence).
        k = convergence(++k, pp, wantu, wantv);
        --p;
        continue;
      }
      int ks;
      for(ks = p - 1; ks > k; ks--) {
        double t = (ks != p ? Math.abs(e[ks]) : 0.) + (ks != k + 1 ? Math.abs(e[ks - 1]) : 0.);
        if(Math.abs(s[ks]) <= tiny + eps * t) {
          s[ks] = 0.0;
          break;
        }
      }
      if(ks == k) {
        // e[k-1] is negligible, k<p, and
        // s(k), ..., s(p) are not negligible (qr step).
        qrStep(e, p, ++k, wantu, wantv);
      }
      else if(ks == p - 1) {
        // s(p) and e[k-1] are negligible and k<p
        deflate(e, p, ++k, wantv);
      }
      else {
        // s(k) is negligible and k<p
        k = ks;
        split(e, p, ++k, wantu);
      }
    }
  }

  private void generateU(int nu, int nct) {
    for(int j = nct; j < nu; j++) {
      for(int i = 0; i < m; i++) {
        U[i][j] = 0.0;
      }
      U[j][j] = 1.0;
    }
    for(int k = nct - 1; k >= 0; k--) {
      if(s[k] != 0.0) {
        for(int j = k + 1; j < nu; j++) {
          double t = 0;
          for(int i = k; i < m; i++) {
            final double[] Ui = U[i];
            t += Ui[k] * Ui[j];
          }
          t /= -U[k][k];
          for(int i = k; i < m; i++) {
            final double[] Ui = U[i];
            Ui[j] += t * Ui[k];
          }
        }
        for(int i = k; i < m; i++) {
          final double[] Ui = U[i];
          Ui[k] = -Ui[k];
        }
        U[k][k] = 1.0 + U[k][k];
        for(int i = 0; i < k - 1; i++) {
          U[i][k] = 0.0;
        }
      }
      else {
        for(int i = 0; i < m; i++) {
          U[i][k] = 0.0;
        }
        U[k][k] = 1.0;
      }
    }
  }

  private void generateV(int nu, double[] e, int nrt) {
    for(int k = n - 1; k >= 0; k--) {
      if((k < nrt) && (e[k] != 0.0)) {
        for(int j = k + 1; j < nu; j++) {
          double t = 0;
          for(int i = k + 1; i < n; i++) {
            final double[] Vi = V[i];
            t += Vi[k] * Vi[j];
          }
          t /= -V[k + 1][k];
          for(int i = k + 1; i < n; i++) {
            final double[] Vi = V[i];
            Vi[j] += t * Vi[k];
          }
        }
      }
      for(int i = 0; i < n; i++) {
        V[i][k] = 0.0;
      }
      V[k][k] = 1.0;
    }
  }

  private void deflate(double[] e, int p, int k, boolean wantv) {
    double f = e[p - 2];
    e[p - 2] = 0.0;
    for(int j = p - 2; j >= k; j--) {
      double t = FastMath.hypot(s[j], f);
      double cs = s[j] / t;
      double sn = f / t;
      s[j] = t;
      if(j != k) {
        f = -sn * e[j - 1];
        e[j - 1] = cs * e[j - 1];
      }
      if(wantv) {
        for(int i = 0; i < n; i++) {
          t = cs * V[i][j] + sn * V[i][p - 1];
          V[i][p - 1] = -sn * V[i][j] + cs * V[i][p - 1];
          V[i][j] = t;
        }
      }
    }
  }

  private void split(double[] e, int p, int k, boolean wantu) {
    double f = e[k - 1];
    e[k - 1] = 0.0;
    for(int j = k; j < p; j++) {
      double t = FastMath.hypot(s[j], f);
      double cs = s[j] / t;
      double sn = f / t;
      s[j] = t;
      f = -sn * e[j];
      e[j] = cs * e[j];
      if(wantu) {
        for(int i = 0; i < m; i++) {
          t = cs * U[i][j] + sn * U[i][k - 1];
          U[i][k - 1] = -sn * U[i][j] + cs * U[i][k - 1];
          U[i][j] = t;
        }
      }
    }
  }

  private void qrStep(double[] e, int p, int k, boolean wantu, boolean wantv) {
    // Calculate the shift.
    double scale = Math.max(Math.max(Math.max(Math.max(Math.abs(s[p - 1]), Math.abs(s[p - 2])), Math.abs(e[p - 2])), Math.abs(s[k])), Math.abs(e[k]));
    double sp = s[p - 1] / scale;
    double spm1 = s[p - 2] / scale;
    double epm1 = e[p - 2] / scale;
    double sk = s[k] / scale;
    double ek = e[k] / scale;
    double b = ((spm1 + sp) * (spm1 - sp) + epm1 * epm1) / 2.0;
    double c = (sp * epm1) * (sp * epm1);
    double shift = 0.0;
    if((b != 0.0) || (c != 0.0)) {
      shift = FastMath.sqrt(b * b + c);
      shift = c / (b < 0. ? b - shift : b + shift);
    }
    double f = (sk + sp) * (sk - sp) + shift;
    double g = sk * ek;

    // Chase zeros.
    for(int j = k; j < p - 1; j++) {
      double t = FastMath.hypot(f, g);
      double cs = f / t;
      double sn = g / t;
      if(j != k) {
        e[j - 1] = t;
      }
      f = cs * s[j] + sn * e[j];
      e[j] = cs * e[j] - sn * s[j];
      g = sn * s[j + 1];
      s[j + 1] = cs * s[j + 1];
      if(wantv) {
        for(int i = 0; i < n; i++) {
          final double[] Vi = V[i];
          final double tmp = cs * Vi[j] + sn * Vi[j + 1];
          Vi[j + 1] = -sn * Vi[j] + cs * Vi[j + 1];
          Vi[j] = tmp;
        }
      }
      t = s[j] = FastMath.hypot(f, g);
      cs = f / t;
      sn = g / t;
      f = cs * e[j] + sn * s[j + 1];
      s[j + 1] = -sn * e[j] + cs * s[j + 1];
      g = sn * e[j + 1];
      e[j + 1] = cs * e[j + 1];
      if(wantu && (j < m - 1)) {
        for(int i = 0; i < m; i++) {
          final double[] Ui = U[i];
          final double tmp = cs * Ui[j] + sn * Ui[j + 1];
          Ui[j + 1] = -sn * Ui[j] + cs * Ui[j + 1];
          Ui[j] = tmp;
        }
      }
    }
    e[p - 2] = f;
  }

  private int convergence(int k, int pp, boolean wantu, boolean wantv) {
    // Make the singular values positive.
    if(s[k] <= 0.0) {
      s[k] = (s[k] < 0.0 ? -s[k] : 0.0);
      if(wantv) {
        for(int i = 0; i <= pp; i++) {
          V[i][k] = -V[i][k];
        }
      }
    }

    // Order the singular values.
    while(k < pp) {
      if(s[k] >= s[k + 1]) {
        break;
      }
      final double t = s[k];
      s[k] = s[k + 1];
      s[k + 1] = t;
      if(wantv && (k < n - 1)) {
        for(int i = 0; i < n; i++) {
          final double[] Vi = V[i];
          final double swap = Vi[k + 1];
          Vi[k + 1] = Vi[k];
          Vi[k] = swap;
        }
      }
      if(wantu && (k < m - 1)) {
        for(int i = 0; i < m; i++) {
          final double[] Ui = U[i];
          final double swap = Ui[k + 1];
          Ui[k + 1] = Ui[k];
          Ui[k] = swap;
        }
      }
      k++;
    }
    return k;
  }

  /**
   * Return the left singular vectors
   * 
   * @return U
   */
  public double[][] getU() {
    return U;
  }

  /**
   * Return the right singular vectors
   * 
   * @return V
   */
  public double[][] getV() {
    return V;
  }

  /**
   * Return the one-dimensional array of singular values
   * 
   * @return diagonal of S.
   */
  public double[] getSingularValues() {
    return s;
  }

  /**
   * Return the diagonal matrix of singular values
   * 
   * @return S
   */
  public double[][] getS() {
    double[][] S = new double[n][n];
    for(int i = 0; i < n; i++) {
      S[i][i] = this.s[i];
    }
    return S;
  }

  /**
   * Two norm
   * 
   * @return max(S)
   */
  public double norm2() {
    return s[0];
  }

  /**
   * Two norm condition number
   * 
   * @return max(S)/min(S)
   */
  public double cond() {
    return s[0] / s[Math.min(m, n) - 1];
  }

  /**
   * Effective numerical matrix rank
   * 
   * @return Number of non-negligible singular values.
   */
  public int rank() {
    final double eps = 0x1p-52, tol = (m > n ? m : n) * s[0] * eps;
    int r = 0;
    for(int i = 0; i < s.length; i++) {
      if(s[i] > tol) {
        r++;
      }
    }
    return r;
  }
}
