package experimentalcode.erich.diss;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.DistributionEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.GammaDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.GeneralizedExtremeValueDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Estimate the L-Moments of a sample.
 * 
 * Reference:
 * <p>
 * J. R. M. Hosking, J. R. Wallis, and E. F. Wood<br />
 * Estimation of the generalized extreme-value distribution by the method of
 * probability-weighted moments.<br />
 * Technometrics 27.3
 * </p>
 * 
 * Also based on:
 * <p>
 * J. R. M. Hosking<br />
 * Fortran routines for use with the method of L-moments Version 3.03<br />
 * IBM Research.
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "J.R.M. Hosking, J. R. Wallis, and E. F. Wood", title = "Estimation of the generalized extreme-value distribution by the method of probability-weighted moments.", booktitle = "Technometrics 27.3", url = "http://dx.doi.org/10.1080/00401706.1985.10488049")
public class ProbabilityWeightedMoments implements DistributionEstimator<GeneralizedExtremeValueDistribution> {
  /**
   * Compute the alpha_r factors using the method of probability-weighted
   * moments.
   * 
   * @param sorted <b>Presorted</b> data array.
   * @param nmom Number of moments to compute
   * @return Alpha moments (0-indexed)
   */
  public static <A> double[] alphaPWM(A data, NumberArrayAdapter<?, A> adapter, final int nmom) {
    final int n = adapter.size(data);
    final double[] xmom = new double[nmom];
    double weight = 1. / n;
    for (int i = 0; i < n; i++) {
      final double val = adapter.getDouble(data, i);
      xmom[0] += weight * val;
      for (int j = 1; j < nmom; j++) {
        weight *= (n - i - j + 1) / (n - j + 1);
        xmom[j] += weight * val;
      }
    }
    return xmom;
  }

  /**
   * Compute the beta_r factors using the method of probability-weighted
   * moments.
   * 
   * @param sorted <b>Presorted</b> data array.
   * @param nmom Number of moments to compute
   * @return Beta moments (0-indexed)
   */
  public static <A> double[] betaPWM(A data, NumberArrayAdapter<?, A> adapter, final int nmom) {
    final int n = adapter.size(data);
    final double[] xmom = new double[nmom];
    double weight = 1. / n;
    for (int i = 0; i < n; i++) {
      final double val = adapter.getDouble(data, i);
      xmom[0] += weight * val;
      for (int j = 1; j < nmom; j++) {
        weight *= (i - j + 1) / (n - j + 1);
        xmom[j] += weight * val;
      }
    }
    return xmom;
  }

  /**
   * Compute the alpha_r and beta_r factors in parallel using the method of
   * probability-weighted moments. Usually cheaper than computing them
   * separately.
   * 
   * @param sorted <b>Presorted</b> data array.
   * @param nmom Number of moments to compute
   * @return Alpha and Beta moments (0-indexed, interleaved)
   */
  public static <A> double[] alphaBetaPWM(A data, NumberArrayAdapter<?, A> adapter, final int nmom) {
    final int n = adapter.size(data);
    final double[] xmom = new double[nmom << 1];
    double aweight = 1. / n, bweight = aweight;
    for (int i = 0; i < n; i++) {
      final double val = adapter.getDouble(data, i);
      xmom[0] += aweight * val;
      xmom[1] += bweight * val;
      for (int j = 1, k = 2; j < nmom; j++, k += 2) {
        aweight *= (n - i - j + 1) / (n - j + 1);
        bweight *= (i - j + 1) / (n - j + 1);
        xmom[k + 1] += aweight * val;
        xmom[k + 1] += bweight * val;
      }
    }
    return xmom;
  }

  /**
   * Compute the sample L-Moments using probability weighted moments.
   * 
   * @param sorted <b>Presorted</b> data array.
   * @param nmom Number of moments to compute
   * @return Alpha and Beta moments (0-indexed, interleaved)
   */
  public static <A> double[] samLMR(A sorted, NumberArrayAdapter<?, A> adapter, final int nmom) {
    final int n = adapter.size(sorted);
    final double[] sum = new double[nmom];
    // Estimate probability weighted moments (unbiased)
    for (int i = 0; i < n; i++) {
      double term = adapter.getDouble(sorted, i);
      sum[0] += term;
      for (int j = 1, z = i; j < nmom; j++, z--) {
        term *= z;
        sum[j] += term;
      }
    }
    // Normalize by "n choose (j + 1)"
    sum[0] /= n;
    double z = n;
    for (int j = 1; j < nmom; j++) {
      z *= n - j;
      sum[j] /= z;
    }
    for (int k = nmom - 1; k >= 1; --k) {
      double p = ((k & 1) == 0) ? +1 : -1;
      double temp = p * sum[0];
      for (int i = 0; i < k; i++) {
        double ai = i + 1.;
        p *= -(k + ai) * (k - i) / (ai * ai);
        temp += p * sum[i + 1];
      }
      sum[k] = temp;
    }
    if (nmom > 2 && !(sum[1] > 0)) {
      throw new ArithmeticException("Can't compute higher order moments for constant data.");
    }
    for (int i = 2; i < nmom; i++) {
      sum[i] /= sum[1];
    }
    return sum;
  }

  /**
   * Constants for fast rational approximations.
   */
  private final double //
      A0 = 0.28377530,
      A1 = -1.21096399, A2 = -2.50728214, A3 = -1.13455566, A4 = -0.07138022, //
      B1 = 2.06189696, B2 = 1.31912239, B3 = 0.25077104, //
      C1 = 1.59921491, C2 = -0.48832213, C3 = 0.01573152, //
      D1 = -0.64363929, D2 = 0.08985247;

  /** Maximum number of iterations. */
  private int MAXIT = 20;

  @Override
  public <A> GeneralizedExtremeValueDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
    // Sort:
    final int size = adapter.size(data);
    double[] sorted = new double[size];
    for (int i = 0; i < size; i++) {
      sorted[i] = adapter.getDouble(data, i);
    }
    Arrays.sort(sorted);
    double[] xmom = samLMR(sorted, ArrayLikeUtil.DOUBLEARRAYADAPTER, 3);
    double t3 = xmom[2];
    if (Math.abs(t3) < 1e-50 || (t3 >= 1.)) {
      throw new ArithmeticException("Invalid moment estimation.");
    }
    // Approximation for t3 between 0 and 1:
    double g;
    if (t3 > 0.) {
      double z = 1. - t3;
      g = (-1. + z * (C1 + z * (C2 + z * C3))) / (1. + z * (D1 + z * D2));
      // g: Almost zero?
      if (Math.abs(g) < 1e-50) {
        double k = 0;
        double sigma = xmom[1] / MathUtil.LOG2;
        double mu = xmom[0] - Math.E * sigma;
        return new GeneralizedExtremeValueDistribution(mu, sigma, k);
      }
    } else {
      // Approximation for t3 between -.8 and 0L:
      g = (A0 + t3 * (A1 + t3 * (A2 + t3 * (A3 + t3 * A4)))) / (1. + t3 * (B1 + t3 * (B2 + t3 * B3)));
      if (t3 < -.8) {
        // Newton-Raphson iteration for t3 < -.8
        if (t3 <= -.97) {
          g = 1. - Math.log(1. + t3) / MathUtil.LOG2;
        }
        double t0 = .5 * (t3 + 3.);
        for (int it = 1;; it++) {
          double x2 = Math.pow(2., -g), xx2 = 1. - x2;
          double x3 = Math.pow(3., -g), xx3 = 1. - x3;
          double t = xx3 / xx2;
          double deriv = (xx2 * x3 * MathUtil.LOG3 - xx3 * x2 * MathUtil.LOG2) / (xx2 * x2);
          double oldg = g;
          g -= (t - t0) / deriv;
          if (Math.abs(g - oldg) < 1e-20 * g) {
            break;
          }
          if (it >= MAXIT) {
            throw new ArithmeticException("Newton-Raphson did not converge.");
          }
        }
      }
    }
    double gam = Math.exp(GammaDistribution.logGamma(1. + g));
    final double mu, sigma, k;
    k = g;
    sigma = xmom[1] * g / (gam * (1. - Math.pow(2., -g)));
    mu = xmom[0] - sigma * (1. - gam) / g;
    return new GeneralizedExtremeValueDistribution(mu, sigma, k);
  }

  @Override
  public Class<? super GeneralizedExtremeValueDistribution> getDistributionClass() {
    return GeneralizedExtremeValueDistribution.class;
  }
}
