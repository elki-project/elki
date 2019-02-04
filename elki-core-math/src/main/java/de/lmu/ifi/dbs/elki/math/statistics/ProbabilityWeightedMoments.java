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
package de.lmu.ifi.dbs.elki.math.statistics;

import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Estimate the L-Moments of a sample.
 * <p>
 * Reference:
 * <p>
 * J. R. M. Hosking, J. R. Wallis, E. F. Wood<br>
 * Estimation of the generalized extreme-value distribution by the method of
 * probability-weighted moments.<br>
 * Technometrics 27.3
 * <p>
 * Also based on:
 * <p>
 * J. R. M. Hosking<br>
 * Fortran routines for use with the method of L-moments Version 3.03<br>
 * IBM Research.
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "J. R. M. Hosking, J. R. Wallis, E. F. Wood", //
    title = "Estimation of the generalized extreme-value distribution by the method of probability-weighted moments.", //
    booktitle = "Technometrics 27.3", //
    url = "https://doi.org/10.1080/00401706.1985.10488049", //
    bibkey = "doi:10.1080/00401706.1985.10488049")
@Reference(authors = "J. R. M. Hosking", //
    title = "Fortran routines for use with the method of L-moments Version 3.03", //
    booktitle = "IBM Research Technical Report", //
    bibkey = "tr/ibm/Hosking00")
public final class ProbabilityWeightedMoments {
  /**
   * Private constructor, use static methods!
   */
  private ProbabilityWeightedMoments() {
    // Static utility class - do not instantiate!
  }

  /**
   * Compute the alpha_r factors using the method of probability-weighted
   * moments.
   * 
   * @param data <b>Presorted</b> data array.
   * @param adapter Array adapter.
   * @param nmom Number of moments to compute
   * @return Alpha moments (0-indexed)
   */
  public static <A> double[] alphaPWM(A data, NumberArrayAdapter<?, A> adapter, final int nmom) {
    final int n = adapter.size(data);
    final double[] xmom = new double[nmom];
    double weight = 1. / n;
    for(int i = 0; i < n; i++) {
      final double val = adapter.getDouble(data, i);
      xmom[0] += weight * val;
      for(int j = 1; j < nmom; j++) {
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
   * @param data <b>Presorted</b> data array.
   * @param adapter Array adapter.
   * @param nmom Number of moments to compute
   * @return Beta moments (0-indexed)
   */
  public static <A> double[] betaPWM(A data, NumberArrayAdapter<?, A> adapter, final int nmom) {
    final int n = adapter.size(data);
    final double[] xmom = new double[nmom];
    double weight = 1. / n;
    for(int i = 0; i < n; i++) {
      final double val = adapter.getDouble(data, i);
      xmom[0] += weight * val;
      for(int j = 1; j < nmom; j++) {
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
   * @param data <b>Presorted</b> data array.
   * @param adapter Array adapter.
   * @param nmom Number of moments to compute
   * @return Alpha and Beta moments (0-indexed, interleaved)
   */
  public static <A> double[] alphaBetaPWM(A data, NumberArrayAdapter<?, A> adapter, final int nmom) {
    final int n = adapter.size(data);
    final double[] xmom = new double[nmom << 1];
    double aweight = 1. / n, bweight = aweight;
    for(int i = 0; i < n; i++) {
      final double val = adapter.getDouble(data, i);
      xmom[0] += aweight * val;
      xmom[1] += bweight * val;
      for(int j = 1, k = 2; j < nmom; j++, k += 2) {
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
   * @param adapter Array adapter.
   * @param nmom Number of moments to compute
   * @return Array containing Lambda1, Lambda2, Tau3 ... TauN
   */
  public static <A> double[] samLMR(A sorted, NumberArrayAdapter<?, A> adapter, int nmom) {
    final int n = adapter.size(sorted);
    final double[] sum = new double[nmom];
    nmom = n < nmom ? n : nmom;
    // Estimate probability weighted moments (unbiased)
    for(int i = 0; i < n; i++) {
      double term = adapter.getDouble(sorted, i);
      // Robustness: skip bad values
      if(Double.isInfinite(term) || Double.isNaN(term)) {
        continue;
      }
      sum[0] += term;
      for(int j = 1, z = i; j < nmom; j++, z--) {
        term *= z;
        sum[j] += term;
      }
    }
    // Normalize by "n choose (j + 1)"
    sum[0] /= n;
    double z = n;
    for(int j = 1; j < nmom; j++) {
      z *= n - j;
      sum[j] /= z;
    }
    normalizeLMR(sum, nmom);
    // Handle case when lambda2 == 0, by setting tau3...tauN = 0:
    if(sum[1] == 0) {
      for(int i = 2; i < nmom; i++) {
        sum[i] = 0.; // tau3...tauN = 0.
      }
      return sum;
    }
    // Map lambda3...lambdaN to tau3...tauN
    for(int i = 2; i < nmom; i++) {
      sum[i] /= sum[1];
    }
    return sum;
  }

  /**
   * Normalize the moments
   * 
   * @param sum Sums
   * @param nmom Number of moments
   */
  private static void normalizeLMR(double[] sum, int nmom) {
    for(int k = nmom - 1; k >= 1; --k) {
      double p = ((k & 1) == 0) ? +1 : -1;
      double temp = p * sum[0];
      for(int i = 0; i < k; i++) {
        double ai = i + 1.;
        p *= -(k + ai) * (k - i) / (ai * ai);
        temp += p * sum[i + 1];
      }
      sum[k] = temp;
    }
  }
}
