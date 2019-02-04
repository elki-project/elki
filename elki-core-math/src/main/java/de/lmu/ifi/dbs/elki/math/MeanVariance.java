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
package de.lmu.ifi.dbs.elki.math;

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

import net.jafama.FastMath;

/**
 * Do some simple statistics (mean, variance) using a numerically stable online
 * algorithm.
 * <p>
 * This class can repeatedly be fed with data using the add() methods, the
 * resulting values for mean and average can be queried at any time using
 * {@link #getMean()} and {@link #getSampleVariance()}.
 * <p>
 * Make sure you have understood variance correctly when using
 * {@link #getNaiveVariance()} - since this class is fed with samples and
 * estimates the mean from the samples, {@link #getSampleVariance()} is often
 * the more appropriate version.
 * <p>
 * As experimentally studied in
 * <p>
 * Erich Schubert, Michael Gertz<br>
 * Numerically Stable Parallel Computation of (Co-)Variance<br>
 * Proc. 30th Int. Conf. Scientific and Statistical Database Management
 * (SSDBM 2018)
 * <p>
 * the current approach is based on:
 * <p>
 * E. A. Youngs and E. M. Cramer<br>
 * Some Results Relevant to Choice of Sum and Sum-of-Product Algorithms<br>
 * Technometrics 13(3), 1971
 * <p>
 * We have originally experimented with:
 * <p>
 * B. P. Welford<br>
 * Note on a method for calculating corrected sums of squares and products<br>
 * Technometrics 4(3), 1962
 * <p>
 * D. H. D. West<br>
 * Updating Mean and Variance Estimates: An Improved Method<br>
 * Communications of the ACM 22(9)
 * 
 * @author Erich Schubert
 * @since 0.2
 */
@Reference(authors = "Erich Schubert, Michael Gertz", //
    title = "Numerically Stable Parallel Computation of (Co-)Variance", //
    booktitle = "Proc. 30th Int. Conf. Scientific and Statistical Database Management (SSDBM 2018)", //
    url = "https://doi.org/10.1145/3221269.3223036", //
    bibkey = "DBLP:conf/ssdbm/SchubertG18")
@Reference(authors = "E. A. Youngs, E. M. Cramer", //
    title = "Some Results Relevant to Choice of Sum and Sum-of-Product Algorithms", //
    booktitle = "Technometrics 13(3)", //
    url = "https://doi.org/10.1080/00401706.1971.10488826", //
    bibkey = "doi:10.1080/00401706.1971.10488826")
@Reference(authors = "B. P. Welford", //
    title = "Note on a method for calculating corrected sums of squares and products", //
    booktitle = "Technometrics 4(3)", //
    url = "https://doi.org/10.2307/1266577", //
    bibkey = "doi:10.2307/1266577")
@Reference(authors = "D. H. D. West", //
    title = "Updating Mean and Variance Estimates: An Improved Method", //
    booktitle = "Communications of the ACM 22(9)", //
    url = "https://doi.org/10.1145/359146.359153", //
    bibkey = "DBLP:journals/cacm/West79")
public class MeanVariance extends Mean {
  /**
   * n times Variance
   */
  protected double m2;

  /**
   * Empty constructor
   */
  public MeanVariance() {
    m2 = 0.;
  }

  /**
   * Constructor from other instance
   * 
   * @param other other instance to copy data from.
   */
  public MeanVariance(MeanVariance other) {
    this.sum = other.sum;
    this.m2 = other.m2;
    this.n = other.n;
  }

  /**
   * Add a single value with weight 1.0
   * 
   * @param val Value
   */
  @Override
  public void put(double val) {
    if(n <= 0) {
      n = 1;
      sum = val;
      m2 = 0;
      return;
    }
    final double tmp = n * val - sum;
    final double oldn = n; // tmp copy
    n += 1.0;
    sum += val;
    m2 += tmp * tmp / (n * oldn);
  }

  /**
   * Add data with a given weight.
   * 
   * @param val data
   * @param weight weight
   */
  @Override
  public void put(double val, double weight) {
    if(weight == 0.) {
      return;
    }
    if(n <= 0) {
      n = weight;
      sum = val * weight;
      return;
    }
    val *= weight;
    final double tmp = n * val - sum * weight;
    final double oldn = n; // tmp copy
    n += weight;
    sum += val;
    m2 += tmp * tmp / (weight * n * oldn);
  }

  /**
   * Join the data of another MeanVariance instance.
   * 
   * @param other Data to join with
   */
  @Override
  public void put(Mean other) {
    if(!(other instanceof MeanVariance)) {
      throw new IllegalArgumentException("I cannot combine Mean and MeanVariance to a MeanVariance.");
    }
    final MeanVariance mvo = (MeanVariance) other;
    final double on = mvo.n, osum = mvo.sum;
    final double tmp = n * osum - sum * on;
    final double oldn = n; // tmp copy
    n += on;
    sum += osum;
    m2 += mvo.m2 + tmp * tmp / (on * n * oldn);
  }

  /**
   * Add values with weight 1.0
   * 
   * @param vals Values
   * @return this
   */
  @Override
  public MeanVariance put(double[] vals) {
    final int l = vals.length;
    if(l < 2) {
      if(l == 1) {
        put(vals[0]);
      }
      return this;
    }
    // First pass:
    double s1 = 0.;
    for(int i = 0; i < l; i++) {
      s1 += vals[i];
    }
    final double om1 = s1 / l;
    // Second pass:
    double om2 = 0., err = 0.;
    for(int i = 0; i < l; i++) {
      final double v = vals[i] - om1;
      om2 += v * v;
      err += v;
    }
    s1 += err;
    om2 += err / l;
    if(n <= 0) {
      n = l;
      sum = s1;
      m2 = om2;
      return this;
    }
    final double tmp = n * s1 - sum * l;
    final double oldn = n; // tmp copy
    n += l;
    sum += s1 + err;
    m2 += om2 + tmp * tmp / (l * n * oldn);
    return this;
  }

  @Override
  public MeanVariance put(double[] vals, double[] weights) {
    assert (vals.length == weights.length);
    for(int i = 0, end = vals.length; i < end; i++) {
      // TODO: use a two-pass update as in the other put
      put(vals[i], weights[i]);
    }
    return this;
  }

  /**
   * Return the naive variance (not taking sampling into account)
   * 
   * Note: usually, you should be using {@link #getSampleVariance} instead!
   * 
   * @return variance
   */
  public double getNaiveVariance() {
    return m2 / n;
  }

  /**
   * Return sample variance.
   * 
   * @return sample variance
   */
  public double getSampleVariance() {
    if(!(n > 1.)) {
      throw new ArithmeticException("Cannot compute a reasonable sample variance with weight <= 1.0!");
    }
    return m2 / (n - 1);
  }

  /**
   * Get the sum of squares.
   *
   * @return sum of squared deviations
   */
  public double getSumOfSquares() {
    return m2;
  }

  /**
   * Return standard deviation using the non-sample variance
   * 
   * Note: usually, you should be using {@link #getSampleStddev} instead!
   * 
   * @return stddev
   */
  public double getNaiveStddev() {
    return FastMath.sqrt(getNaiveVariance());
  }

  /**
   * Return standard deviation
   * 
   * @return stddev
   */
  public double getSampleStddev() {
    return FastMath.sqrt(getSampleVariance());
  }

  /**
   * Create and initialize a new array of MeanVariance
   * 
   * @param dimensionality Dimensionality
   * @return New and initialized Array
   */
  public static MeanVariance[] newArray(int dimensionality) {
    MeanVariance[] arr = new MeanVariance[dimensionality];
    for(int i = 0; i < dimensionality; i++) {
      arr[i] = new MeanVariance();
    }
    return arr;
  }

  @Override
  public String toString() {
    return "MeanVariance(mean=" + getMean() + ",var=" + getNaiveVariance() + ",weight=" + n + ")";
  }

  @Override
  public void reset() {
    super.reset();
    m2 = 0;
  }
}
