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

import static java.lang.Math.abs;
import static java.lang.Math.max;

import java.util.Arrays;

import net.jafama.FastMath;

/**
 * Eigenvalues and eigenvectors of a real matrix.
 * <p>
 * If A is symmetric, then A = V*D*V' where the eigenvalue matrix D is diagonal
 * and the eigenvector matrix V is orthogonal. I.e. A =
 * V.times(D.timesTranspose(V)) and V.timesTranspose(V) equals the identity
 * matrix.
 * <p>
 * If A is not symmetric, then the eigenvalue matrix D is block diagonal with
 * the real eigenvalues in 1-by-1 blocks and any complex eigenvalues, lambda +
 * i*mu, in 2-by-2 blocks, [lambda, mu; -mu, lambda]. The columns of V represent
 * the eigenvectors in the sense that A*V = V*D, i.e. A.times(V) equals
 * V.times(D). The matrix V may be badly conditioned, or even singular, so the
 * validity of the equation A = V*D*inverse(V) depends upon V.cond().
 *
 * @author Arthur Zimek
 * @since 0.1
 */
public class EigenvalueDecomposition {
  /**
   * Row and column dimension (square matrix).
   *
   * @serial matrix dimension.
   */
  private final int n;

  /**
   * Arrays for internal storage of eigenvalues.
   *
   * @serial internal storage of eigenvalues.
   */
  private double[] d, e;

  /**
   * Array for internal storage of eigenvectors.
   *
   * @serial internal storage of eigenvectors.
   */
  private double[][] V;

  /**
   * Array for internal storage of nonsymmetric Hessenberg form.
   *
   * @serial internal storage of nonsymmetric Hessenberg form.
   */
  private double[][] H;

  /**
   * Working storage for nonsymmetric algorithm.
   *
   * @serial working storage for nonsymmetric algorithm.
   */
  private double[] ort;

  /**
   * Check for symmetry, then construct the eigenvalue decomposition
   *
   * @param A Square matrix
   */
  public EigenvalueDecomposition(double[][] A) {
    n = A.length;
    d = new double[n];
    e = new double[n];

    boolean issymmetric = true;
    for(int j = 0; (j < n) && issymmetric; j++) {
      for(int i = 0; (i < n) && issymmetric; i++) {
        issymmetric = (A[i][j] == A[j][i]);
        if(Double.isNaN(A[i][j])) {
          throw new IllegalArgumentException("NaN in EigenvalueDecomposition!");
        }
        if(Double.isInfinite(A[i][j])) {
          throw new IllegalArgumentException("+-inf in EigenvalueDecomposition!");
        }
      }
    }

    if(issymmetric) {
      V = VMath.copy(A);
      // Tridiagonalize.
      tred2();
      // Diagonalize.
      tql2();
    }
    else {
      V = new double[n][n];
      H = VMath.copy(A);
      ort = new double[n];
      // Reduce to Hessenberg form.
      orthes();
      // Reduce Hessenberg to real Schur form.
      hqr2();
    }
    // Sort eigenvalues and corresponding vectors.
    sortEigen();
  }

  /**
   * Symmetric Householder reduction to tridiagonal form.
   */
  private void tred2() {
    // This is derived from the Algol procedures tred2 by
    // Bowdler, Martin, Reinsch, and Wilkinson, Handbook for
    // Auto. Comp., Vol.ii-Linear Algebra, and the corresponding
    // Fortran subroutine in EISPACK.
    System.arraycopy(V[n - 1], 0, d, 0, n);

    // Householder reduction to tridiagonal form.
    for(int i = n - 1; i > 0; i--) {
      double[] V_i = V[i], V_im1 = V[i - 1];
      // Scale to avoid under/overflow.
      double scale = 0.;
      for(int k = 0; k < i; k++) {
        scale += abs(d[k]);
      }
      if(scale < Double.MIN_NORMAL) {
        e[i] = d[i - 1];
        for(int j = 0; j < i; j++) {
          d[j] = V_im1[j];
          V_i[j] = V[j][i] = 0.;
        }
        d[i] = 0;
        continue;
      }
      // Generate Householder vector.
      double h = 0.;
      for(int k = 0; k < i; k++) {
        double dk = d[k] /= scale;
        h += dk * dk;
      }
      {
        double f = d[i - 1], g = FastMath.sqrt(h);
        g = (f > 0) ? -g : g;
        e[i] = scale * g;
        h -= f * g;
        d[i - 1] = f - g;
        Arrays.fill(e, 0, i, 0.);
      }

      // Apply similarity transformation to remaining columns.
      for(int j = 0; j < i; j++) {
        final double[] Vj = V[j];
        double dj = Vj[i] = d[j], ej = e[j] + Vj[j] * dj;
        for(int k = j + 1; k <= i - 1; k++) {
          final double Vkj = V[k][j];
          ej += Vkj * d[k];
          e[k] += Vkj * dj;
        }
        e[j] = ej;
      }
      double sum = 0.;
      for(int j = 0; j < i; j++) {
        sum += (e[j] /= h) * d[j];
      }
      double hh = sum / (h + h);
      for(int j = 0; j < i; j++) {
        e[j] -= hh * d[j];
      }
      for(int j = 0; j < i; j++) {
        double dj = d[j], ej = e[j];
        for(int k = j; k <= i - 1; k++) {
          V[k][j] -= (dj * e[k] + ej * d[k]);
        }
        d[j] = V_im1[j];
        V_i[j] = 0.;
      }
      d[i] = h;
    }

    // Accumulate transformations.
    tred2AccumulateTransformations();
    System.arraycopy(V[n - 1], 0, d, 0, n);
    Arrays.fill(V[n - 1], 0.);
    V[n - 1][n - 1] = 1.;
    e[0] = 0.;
  }

  private void tred2AccumulateTransformations() {
    for(int i = 0; i < n - 1; i++) {
      final double[] Vi = V[i];
      V[n - 1][i] = Vi[i];
      Vi[i] = 1.;
      final double h = d[i + 1];
      if(h > 0.0 || h < 0.0) {
        for(int k = 0; k <= i; k++) {
          d[k] = V[k][i + 1] / h;
        }
        for(int j = 0; j <= i; j++) {
          double g = 0.0;
          for(int k = 0; k <= i; k++) {
            final double[] Vk = V[k];
            g += Vk[i + 1] * Vk[j];
          }
          for(int k = 0; k <= i; k++) {
            V[k][j] -= g * d[k];
          }
        }
      }
      for(int k = 0; k <= i; k++) {
        V[k][i + 1] = 0.0;
      }
    }
  }

  /**
   * Symmetric tridiagonal QL algorithm.
   */
  private void tql2() {
    // This is derived from the Algol procedures tql2, by
    // Bowdler, Martin, Reinsch, and Wilkinson, Handbook for
    // Auto. Comp., Vol.ii-Linear Algebra, and the corresponding
    // Fortran subroutine in EISPACK.

    System.arraycopy(e, 1, e, 0, n - 1);
    e[n - 1] = 0.;

    double f = 0., tst1 = 0.;
    for(int l = 0; l < n; l++) {
      // Find small subdiagonal element
      tst1 = max(tst1, abs(d[l]) + abs(e[l]));
      int m = l;
      for(; m < n; ++m) {
        if(abs(e[m]) <= 0x1P-52 * tst1) {
          break;
        }
      }

      // If m == l, d[l] is an eigenvalue,
      // otherwise, iterate.
      if(m > l) {
        do {
          // Compute implicit shift
          f += tql2ComputeImplicitShift(l);
          // Implicit QL transformation.
          tql2ImplicitQL(l, m, d[l + 1]);
          // Check for convergence.
          // TODO: Iteration limit?
        }
        while(abs(e[l]) > 0x1P-52 * tst1);
      }
      d[l] += f;
      e[l] = 0.;
    }
  }

  private double tql2ComputeImplicitShift(int l) {
    final double el = e[l], g = d[l], p = (d[l + 1] - g) / (2. * el);
    final double r = FastMath.sqrt(p * p + 1.);
    final double pr = (p >= 0) ? p + r : p - r;
    final double dl = d[l] = el / pr;
    d[l + 1] = el * pr;
    final double h = g - dl;
    for(int i = l + 2; i < n; i++) {
      d[i] -= h;
    }
    return h;
  }

  private void tql2ImplicitQL(int l, int m, double dl1) {
    double p = d[m], c = 1., c2 = 1., c3 = 1., s = 0., s2 = 0.;
    final double el1 = e[l + 1];
    for(int i = m - 1; i >= l; i--) {
      c3 = c2;
      c2 = c;
      s2 = s;
      final double di = d[i], ei = e[i];
      double g = c * ei, h = c * p, r = FastMath.hypot(p, ei);
      e[i + 1] = s * r;
      s = ei / r;
      c = p / r;
      p = c * di - s * g;
      d[i + 1] = h + s * (c * g + s * di);

      // Accumulate transformation.
      for(int k = 0; k < n; k++) {
        final double[] Vk = V[k];
        h = Vk[i + 1];
        Vk[i + 1] = s * Vk[i] + c * h;
        Vk[i] = c * Vk[i] - s * h;
      }
    }
    p = -s * s2 * c3 * el1 * e[l] / dl1;
    e[l] = s * p;
    d[l] = c * p;
  }

  private void sortEigen() {
    for(int i = 0; i < n - 1; i++) {
      // Find maximum:
      int k = i;
      double p = abs(d[i]);
      for(int j = i + 1; j < n; j++) {
        final double d_j = abs(d[j]);
        if(d_j > p) {
          k = j;
          p = d_j;
        }
      }
      if(k == i) {
        continue;
      }
      // Swap Eigenvalues
      double tmp = d[k]; // p, or -p...
      d[k] = d[i];
      d[i] = tmp;
      // Swap columns of V
      for(int j = 0; j < n; j++) {
        final double[] Vj = V[j];
        final double swap = Vj[i];
        Vj[i] = Vj[k];
        Vj[k] = swap;
      }
    }
  }

  /**
   * Nonsymmetric reduction to Hessenberg form.
   */
  private void orthes() {
    // FIXME: does this fail on NaN/inf values?

    // This is derived from the Algol procedures orthes and ortran,
    // by Martin and Wilkinson, Handbook for Auto. Comp.,
    // Vol.ii-Linear Algebra, and the corresponding
    // Fortran subroutines in EISPACK.

    final int low = 0, high = n - 1;
    for(int m = low + 1; m <= high - 1; m++) {
      // Scale column.
      double scale = 0.0;
      for(int i = m; i <= high; i++) {
        scale += abs(H[i][m - 1]);
      }
      if(scale > 0.0 || scale < 0.0) {
        // Compute Householder transformation.
        double h = 0.0;
        for(int i = high; i >= m; i--) {
          double oi = ort[i] = H[i][m - 1] / scale;
          h += oi * oi;
        }
        double g = FastMath.sqrt(h), om = ort[m];
        g = (om > 0) ? -g : g;
        h -= om * g;
        ort[m] = om - g;

        // Apply Householder similarity transformation
        // H = (I-u*u'/h)*H*(I-u*u')/h)

        for(int j = m; j < n; j++) {
          double f = 0.0;
          for(int i = high; i >= m; i--) {
            f += ort[i] * H[i][j];
          }
          f /= h;
          for(int i = m; i <= high; i++) {
            H[i][j] -= f * ort[i];
          }
        }

        for(int i = 0; i <= high; i++) {
          final double[] Hi = H[i];
          double f = 0.0;
          for(int j = high; j >= m; j--) {
            f += ort[j] * Hi[j];
          }
          f /= h;
          for(int j = m; j <= high; j++) {
            Hi[j] -= f * ort[j];
          }
        }
        ort[m] *= scale;
        H[m][m - 1] = scale * g;
      }
    }

    // Accumulate transformations (Algol's ortran).
    for(int i = 0; i < n; i++) {
      Arrays.fill(V[i], 0.);
      V[i][i] = 1.;
    }

    for(int m = high - 1; m >= low + 1; m--) {
      final double[] H_m = H[m];
      if(H_m[m - 1] != 0.0) {
        for(int i = m + 1; i <= high; i++) {
          ort[i] = H[i][m - 1];
        }
        final double ort_m = ort[m];
        for(int j = m; j <= high; j++) {
          double g = 0.0;
          for(int i = m; i <= high; i++) {
            g += ort[i] * V[i][j];
          }
          // Double division avoids possible underflow
          g = (g / ort_m) / H_m[m - 1];
          for(int i = m; i <= high; i++) {
            V[i][j] += g * ort[i];
          }
        }
      }
    }
  }

  /** Complex scalar division, writing into buf[off++] */
  private static void cdiv(double xr, double xi, double yr, double yi, double[] buf, int off) {
    if(abs(yr) > abs(yi)) {
      final double r = yi / yr, d = yr + r * yi;
      buf[off] = (xr + r * xi) / d;
      buf[off + 1] = (xi - r * xr) / d;
    }
    else {
      final double r = yr / yi, d = yi + r * yr;
      buf[off] = (r * xr + xi) / d;
      buf[off + 1] = (r * xi - xr) / d;
    }
  }

  /** Nonsymmetric reduction from Hessenberg to real Schur form. */
  private void hqr2() {
    // FIXME: does this fail on NaN/inf values?

    // This is derived from the Algol procedure hqr2,
    // by Martin and Wilkinson, Handbook for Auto. Comp.,
    // Vol.ii-Linear Algebra, and the corresponding
    // Fortran subroutine in EISPACK.

    // Initialize
    final int nn = this.n, low = 0, high = nn - 1;

    // Store roots isolated by balanc and compute matrix norm
    double norm = 0.;
    for(int i = 0; i < nn; i++) {
      // If eventually allowing low,high to be set, use this:
      // if(i < low || i > high) { d[i] = H[i][i]; e[i] = 0.; }
      for(int j = (i > 0 ? i - 1 : 0); j < nn; j++) {
        norm += abs(H[i][j]);
      }
    }

    // Outer loop over eigenvalue index
    {
      double exshift = 0.0;
      // double p = 0, q = 0, r = 0; // , s = 0, z = 0;
      int iter = 0;
      for(int n = nn - 1; n >= low;) {
        // Look for single small sub-diagonal element
        int l = n;
        for(; l > low; --l) {
          double s = abs(H[l - 1][l - 1]) + abs(H[l][l]);
          if(abs(H[l][l - 1]) < 0x1P-52 * (s == 0. ? norm : s)) {
            break;
          }
        }

        // Check for convergence
        if(l == n) {
          // One root found
          d[n] = (H[n][n] += exshift);
          e[n] = 0.;
          n--;
          iter = 0;
          continue;
        }
        final double[] Hn = H[n], Hnm1 = H[n - 1];
        if(l == n - 1) {
          // Two roots found
          double w = Hn[n - 1] * Hnm1[n];
          double p = (Hnm1[n - 1] - Hn[n]) * 0.5, q = p * p + w;
          double x = (Hn[n] += exshift), z = FastMath.sqrt(abs(q));
          Hnm1[n - 1] += exshift;

          if(q >= 0) {
            // Real pair
            z = (p >= 0) ? p + z : p - z;
            d[n - 1] = x + z;
            d[n] = (z != 0.) ? x - w / z : d[n - 1];
            e[n] = e[n - 1] = 0.;
            x = Hn[n - 1];
            double s = abs(x) + abs(z);
            p = x / s;
            q = z / s;
            double r = FastMath.hypot(p, q);
            p /= r;
            q /= r;

            // Row modification
            for(int j = n - 1; j < nn; j++) {
              double tmp = Hnm1[j];
              Hnm1[j] = q * tmp + p * Hn[j];
              Hn[j] = q * Hn[j] - p * tmp;
            }

            // Column modification
            for(int i = 0; i <= n; i++) {
              modifyQP(H[i], n, p, q);
            }

            // Accumulate transformations
            for(int i = low; i <= high; i++) {
              modifyQP(V[i], n, p, q);
            }
          }
          else {
            // Complex pair
            d[n] = d[n - 1] = x + p;
            e[n] = -(e[n - 1] = z);
          }
          n -= 2;
          iter = 0;
          continue;
        }
        // No convergence yet

        // Form shift
        double x = Hn[n], y = 0., w = 0.;
        y = Hnm1[n - 1];
        w = Hn[n - 1] * Hnm1[n];

        // Wilkinson's original ad hoc shift
        if(iter == 10) {
          exshift += x;
          for(int i = low; i <= n; i++) {
            H[i][i] -= x;
          }
          double s = abs(Hn[n - 1]) + abs(Hnm1[n - 2]);
          x = y = 0.75 * s;
          w = -0.4375 * s * s;
        }

        // MATLAB's new ad hoc shift
        if(iter == 30) {
          double s = (y - x) * 0.5;
          s = s * s + w;
          if(s > 0) {
            s = FastMath.sqrt(s);
            s = (y < x) ? -s : s;
            s = x - w / ((y - x) * 0.5 + s);
            for(int i = low; i <= n; i++) {
              H[i][i] -= s;
            }
            exshift += s;
            x = y = w = 0.964;
          }
        }

        ++iter; // (Could check iteration count here.)

        double p = 0., q = 0., r = 0.;
        // Look for two consecutive small sub-diagonal elements
        int m = n - 2;
        for(; m >= l; m--) {
          final double[] Hm = H[m], Hmp1 = H[m + 1];
          final double z = Hm[m], xz = x - z;
          p = (xz * (y - z) - w) / Hmp1[m] + Hm[m + 1];
          q = Hmp1[m + 1] - xz - y;
          r = H[m + 2][m + 1];
          double tmp = abs(p) + abs(q) + abs(r);
          p /= tmp;
          q /= tmp;
          r /= tmp;
          if(m == l || abs(Hm[m - 1]) * (abs(q) + abs(r)) < 0x1P-52 * (abs(p) * (abs(H[m - 1][m - 1]) + abs(z) + abs(Hmp1[m + 1])))) {
            break;
          }
        }

        for(int i = m + 2; i <= n; i++) {
          final double[] Hi = H[i];
          Hi[i - 2] = 0.;
          if(i > m + 2) {
            Hi[i - 3] = 0.;
          }
        }

        // Double QR step involving rows l:n and columns m:n
        for(int k = m, last = n - 1; k <= last; k++) {
          boolean notlast = (k != last);
          final double[] Hk = H[k], Hkp1 = H[k + 1],
              Hkp2 = notlast ? H[k + 2] : null;
          if(k != m) {
            p = Hk[k - 1];
            q = Hkp1[k - 1];
            r = notlast ? Hkp2[k - 1] : 0.;
            x = abs(p) + abs(q) + abs(r);
            if(x == 0.0) {
              continue; // Jama 1.0.3 fix
            }
            p /= x;
            q /= x;
            r /= x;
          }
          double s = FastMath.hypot(p, q, r);
          s = (p < 0) ? -s : s;
          if(s != 0) {
            if(k != m) {
              Hk[k - 1] = -s * x;
            }
            else if(l != m) {
              Hk[k - 1] = -Hk[k - 1];
            }
            p += s;
            x = p / s;
            y = q / s;
            double z = r / s;
            q /= p;
            r /= p;

            // Row modification
            for(int j = k; j < nn; j++) {
              double tmp = Hk[j] + q * Hkp1[j];
              if(notlast) {
                tmp += r * Hkp2[j];
                Hkp2[j] -= tmp * z;
              }
              Hk[j] -= tmp * x;
              Hkp1[j] -= tmp * y;
            }

            // Column modification
            for(int i = 0; i <= Math.min(n, k + 3); i++) {
              modifyQR(H[i], k, notlast, q, r, x, y, z);
            }

            // Accumulate transformations
            for(int i = low; i <= high; i++) {
              modifyQR(V[i], k, notlast, q, r, x, y, z);
            }
          } // (s != 0)
        } // k loop
        // check convergence
      } // while (n >= low)
    }

    if(norm == 0.0) {
      return;
    }

    // Backsubstitute to find vectors of upper triangular form
    for(int n = nn - 1; n >= 0; n--) {
      double p = d[n], q = e[n];
      if(q == 0) {
        hqr2BacksubstituteReal(n, p, norm);
      }
      else if(q < 0) {
        hqr2BacksubstituteComplex(n, p, q, norm);
      }
    }

    // Vectors of isolated roots
    // Only matters if the user can modify low, high:
    // for(int i = 0; i < nn; i++) {
    // if(i < low || i > high) { System.arraycopy(H[i], i, V[i], i, nn - i); }
    // }

    // Back transformation to get eigenvectors of original matrix
    hqr2BackTransformation(nn, low, high);
  }

  private static void modifyQP(final double[] Hi, int n, double p, double q) {
    double tmp = Hi[n - 1];
    Hi[n - 1] = q * tmp + p * Hi[n];
    Hi[n] = q * Hi[n] - p * tmp;
  }

  private static void modifyQR(final double[] Hi, int k, boolean notlast, double q, double r, double x, double y, double z) {
    double tmp = x * Hi[k] + y * Hi[k + 1];
    if(notlast) {
      tmp += z * Hi[k + 2];
      Hi[k + 2] -= tmp * r;
    }
    Hi[k] -= tmp;
    Hi[k + 1] -= tmp * q;
  }

  // Real vector
  private void hqr2BacksubstituteReal(int n, double p, double norm) {
    H[n][n] = 1.0;
    double s = 0, z = 0;
    for(int i = n - 1, l = n; i >= 0; i--) {
      final double[] Hi = H[i];
      double w = Hi[i] - p, r = 0.;
      for(int j = l; j <= n; j++) {
        r += Hi[j] * H[j][n];
      }
      if(e[i] < 0.0) {
        z = w;
        s = r;
        continue;
      }
      l = i;
      if(!(e[i] > 0.0)) {
        Hi[n] = -r / ((w > 0.0 || w < 0.0) ? w : (0x1P-52 * norm));
      }
      else {
        // Solve real equations
        final double[] Hip1 = H[i + 1];
        final double x = Hi[i + 1], y = Hip1[i], dip = d[i] - p, ei = e[i];
        final double t = Hi[n] = (x * s - z * r) / (dip * dip + ei * ei);
        Hip1[n] = (abs(x) > abs(z)) ? (-r - w * t) / x : (-s - y * t) / z;
      }

      // Overflow control
      final double t = abs(Hi[n]);
      if((0x1P-52 * t) * t > 1) {
        for(int j = i; j <= n; j++) {
          H[j][n] /= t;
        }
      }
    }
  }

  // Complex vector
  private void hqr2BacksubstituteComplex(int n, double p, double q, double norm) {
    final double[] Hn = H[n], Hnm1 = H[n - 1];
    // Last vector component imaginary so matrix is triangular
    final double tmp = Hn[n - 1];
    if(abs(tmp) > abs(Hnm1[n])) {
      Hnm1[n - 1] = q / tmp;
      Hnm1[n] = -(Hn[n] - p) / tmp;
    }
    else {
      cdiv(0., -Hnm1[n], Hnm1[n - 1] - p, q, Hnm1, n - 1);
    }
    Hn[n - 1] = 0.;
    Hn[n] = 1.;
    double r = 0., s = 0., z = 0.;
    for(int i = n - 2, l = n - 1; i >= 0; i--) {
      final double[] Hi = H[i], Hip1 = H[i + 1];
      double w = Hi[i] - p, ra = 0., sa = 0.;
      for(int j = l; j <= n; j++) {
        final double Hij = Hi[j];
        final double[] Hj = H[j];
        ra += Hij * Hj[n - 1];
        sa += Hij * Hj[n];
      }

      if(e[i] < 0.) {
        z = w;
        r = ra;
        s = sa;
        continue;
      }
      l = i;
      if(!(e[i] > 0.0)) {
        cdiv(-ra, -sa, w, q, Hi, n - 1);
      }
      else {
        // Solve complex equations
        final double x = Hi[i + 1], y = Hip1[i], dip = d[i] - p;
        double vr = dip * dip + e[i] * e[i] - q * q, vi = dip * 2. * q;
        if(vr == 0. && vi == 0.) {
          vr = 0x1P-52 * norm * (abs(w) + abs(q) + abs(x) + abs(y) + abs(z));
        }
        cdiv(x * r - z * ra + q * sa, x * s - z * sa - q * ra, vr, vi, Hi, n - 1);
        if(abs(x) > (abs(z) + abs(q))) {
          Hip1[n - 1] = (-ra - w * Hi[n - 1] + q * Hi[n]) / x;
          Hip1[n] = (-sa - w * Hi[n] - q * Hi[n - 1]) / x;
        }
        else {
          cdiv(-r - y * Hi[n - 1], -s - y * Hi[n], z, q, Hip1, n - 1);
        }
      }

      // Overflow control
      double t = max(abs(Hi[n - 1]), abs(Hi[n]));
      if((0x1P-52 * t) * t > 1) {
        for(int j = i; j <= n; j++) {
          final double[] Hj = H[j];
          Hj[n - 1] /= t;
          Hj[n] /= t;
        }
      }
    }
  }

  /**
   * Back transformation to get eigenvectors of original matrix.
   */
  private void hqr2BackTransformation(int nn, int low, int high) {
    for(int j = nn - 1; j >= low; j--) {
      final int last = j < high ? j : high;
      for(int i = low; i <= high; i++) {
        final double[] Vi = V[i];
        double sum = 0.;
        for(int k = low; k <= last; k++) {
          sum += Vi[k] * H[k][j];
        }
        Vi[j] = sum;
      }
    }
  }

  /**
   * Return the eigenvector matrix
   *
   * @return V
   */
  public double[][] getV() {
    return V;
  }

  /**
   * Return the real parts of the eigenvalues
   *
   * @return real(diag(D))
   */
  public double[] getRealEigenvalues() {
    return d;
  }

  /**
   * Return the imaginary parts of the eigenvalues
   *
   * @return imag(diag(D))
   */
  public double[] getImagEigenvalues() {
    return e;
  }

  /**
   * Return the block diagonal eigenvalue matrix
   *
   * @return D
   */
  public double[][] getD() {
    double[][] D = new double[n][n];
    for(int i = 0; i < n; i++) {
      final double e_i = e[i];
      final double[] D_i = D[i];
      D_i[i] = d[i];
      if(e_i > 0) {
        D_i[i + 1] = e_i;
      }
      else if(e_i < 0) {
        D_i[i - 1] = e_i;
      } // else: e_i = 0
    }
    return D;
  }
}
