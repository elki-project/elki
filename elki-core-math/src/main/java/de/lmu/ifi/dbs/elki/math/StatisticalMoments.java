package de.lmu.ifi.dbs.elki.math;

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

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Track various statistical moments, including mean, variance, skewness and
 * kurtosis.
 * 
 * References:
 * <p>
 * T. B. Terriberry<br />
 * Computing Higher-Order Moments Online<br/>
 * http://people.xiph.org/~tterribe/notes/homs.html
 * </p>
 * 
 * General recurrence, for higher order moments, can be found in:
 * <p>
 * Philippe Pébay<br />
 * Formulas for Robust, One-Pass Parallel Computation of Covariances and
 * Arbitrary-Order Statistical Moments<br />
 * Sandia Report SAND2008-6212, Sandia National Laboratories
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "T. B. Terriberry", title = "Computing Higher-Order Moments Online", booktitle = "Online - Technical Note", url = "http://people.xiph.org/~tterribe/notes/homs.html")
public class StatisticalMoments extends MeanVarianceMinMax {
  /**
   * Third moment.
   */
  double m3;

  /**
   * Fourth moment.
   */
  double m4;

  /**
   * Empty constructor
   */
  public StatisticalMoments() {
    // nothing to do here, initialization done above.
  }

  /**
   * Constructor from other instance
   * 
   * @param other other instance to copy data from.
   */
  public StatisticalMoments(StatisticalMoments other) {
    this.m1 = other.m1;
    this.m2 = other.m2;
    this.n = other.n;
    this.m3 = other.m3;
    this.m4 = other.m4;
  }

  /**
   * Add a single value with weight 1.0
   * 
   * @param val Value
   */
  @Override
  public void put(double val) {
    final double nn = this.n + 1.0;
    final double delta = val - m1;
    final double delta_nn = delta / nn;
    final double delta_nn2 = delta_nn * delta_nn;
    final double inc = delta * delta_nn * this.n;

    // Update values:
    m4 += inc * delta_nn2 * (nn * nn - 3. * nn + 3.) + 6. * delta_nn2 * m2 - 4. * delta_nn * m3;
    m3 += inc * delta_nn * (nn - 2) - 3. * delta_nn * m2;
    m2 += inc;
    m1 += delta_nn;
    n = nn;
  
    min = Math.min(min, val);
    max = Math.max(max, val);
  }

  /**
   * Add data with a given weight.
   * 
   * @param val data
   * @param weight weight
   */
  @Override
  public void put(double val, double weight) {
    // TODO: any way of further simplifying this?
    // Right now it is copy & paste from the merge formula.
    final double nn = weight + this.n;
    final double delta = val - this.m1;

    // Some factors used below:
    final double otherm2 = val * val;
    final double otherm3 = otherm2 * val;
    final double otherm4 = otherm3 * val;

    final double delta_nn = delta / nn;
    final double delta_nn2 = delta_nn * delta_nn;
    final double delta_nn3 = delta_nn2 * delta_nn;
    final double na2 = this.n * this.n;
    final double nb2 = weight * weight;
    final double ntn = this.n * weight;

    this.m4 += otherm4 + delta * delta_nn3 * ntn * (na2 - ntn + nb2) + 6. * (na2 * otherm2 + nb2 * this.m2) * delta_nn2 + 4. * (this.n * otherm3 - weight * this.m3) * delta_nn;
    this.m3 += otherm3 + delta * delta_nn2 * ntn * (this.n - weight) + 3. * (this.n * otherm2 - weight * this.m2) * delta_nn;
    this.m2 += otherm2 + delta * delta_nn * this.n * weight;
    this.m1 += weight * delta_nn;
    this.n = nn;

    min = Math.min(min, val);
    max = Math.max(max, val);
  }

  /**
   * Join the data of another MeanVariance instance.
   * 
   * @param other Data to join with
   */
  @Override
  public void put(Mean other) {
    if (other instanceof StatisticalMoments) {
      StatisticalMoments othe = (StatisticalMoments) other;
      final double nn = othe.n + this.n;
      final double delta = othe.m1 - this.m1;

      // Some factors used below:
      final double delta_nn = delta / nn;
      final double delta_nn2 = delta_nn * delta_nn;
      final double delta_nn3 = delta_nn2 * delta_nn;
      final double na2 = this.n * this.n;
      final double nb2 = othe.n * othe.n;
      final double ntn = this.n * othe.n;

      this.m4 += othe.m4 + delta * delta_nn3 * ntn * (na2 - ntn + nb2) + 6. * (na2 * othe.m2 + nb2 * this.m2) * delta_nn2 + 4. * (this.n * othe.m3 - othe.n * this.m3) * delta_nn;
      this.m3 += othe.m3 + delta * delta_nn2 * ntn * (this.n - othe.n) + 3. * (this.n * othe.m2 - othe.n * this.m2) * delta_nn;
      this.m2 += othe.m2 + delta * delta_nn * this.n * othe.n;
      this.m1 += othe.n * delta_nn;
      this.n = nn;
      
      min = Math.min(min, othe.min);
      max = Math.max(max, othe.max);
    } else {
      throw new AbortException("I cannot combine Mean or MeanVariance into to a StatisticalMoments class.");
    }
  }

  /**
   * Get the skewness using sample variance.
   * 
   * @return Skewness
   */
  public double getSampleSkewness() {
    assert (n > 2.) : "Cannot compute a reasonable sample skewness with weight <= 2.0!";
    double sigma2 = getSampleVariance();
    return (m3 * n / (n - 1) / (n - 2)) / Math.pow(sigma2, 1.5);
  }

  /**
   * Get the skewness using naive variance.
   * 
   * @return Skewness
   */
  public double getNaiveSkewness() {
    double sigma2 = getNaiveVariance();
    return (m3 / n) / Math.pow(sigma2, 1.5);
  }

  /**
   * Get the kurtosis using sample variance.
   * 
   * Note: this formula does <em>not</em> include the correction factor, such
   * that a normal distribution should be 0.
   * 
   * @return Kurtosis
   */
  public double getSampleKurtosis() {
    assert (n > 3.) : "Cannot compute a reasonable sample kurtosis with weight <= 3.0!";
    if (!(m2 > 0)) {
      throw new ArithmeticException("Kurtosis not defined when variance is 0!");
    }
    final double nm1 = n - 1.;
    return (nm1 / ((n - 2.) * (n - 3.))) * (n * (n + 1) * m4 / (m2 * m2) - 3 * nm1) + 3;
  }

  /**
   * Get the kurtosis using naive variance.
   * 
   * Note: this formula does <em>not</em> include the -3 term.
   * 
   * @return Kurtosis
   */
  public double getNaiveKurtosis() {
    if (!(m2 > 0)) {
      throw new ArithmeticException("Kurtosis not defined when variance is 0!");
    }
    return (n * m4) / (m2 * m2);
  }

  /**
   * Get the kurtosis using sample variance.
   * 
   * Note: this formula <em>does</em> include the correction factor, such that a
   * normal distribution should be 0.
   * 
   * @return Kurtosis
   */
  public double getSampleExcessKurtosis() {
    assert (n > 3.) : "Cannot compute a reasonable sample kurtosis with weight <= 3.0!";
    if (!(m2 > 0)) {
      throw new ArithmeticException("Kurtosis not defined when variance is 0!");
    }
    final double nm1 = n - 1.;
    return (nm1 / ((n - 2.) * (n - 3.))) * (n * (n + 1) * m4 / (m2 * m2) - 3 * nm1);
  }

  /**
   * Get the kurtosis using naive variance.
   * 
   * Note: this formula <em>does</em> include the -3 term.
   * 
   * @return Kurtosis
   */
  public double getNaiveExcessKurtosis() {
    if (!(m2 > 0)) {
      throw new ArithmeticException("Kurtosis not defined when variance is 0!");
    }
    return (n * m4) / (m2 * m2) - 3;
  }

  /**
   * Create and initialize a new array of MeanVariance
   * 
   * @param dimensionality Dimensionality
   * @return New and initialized Array
   */
  public static StatisticalMoments[] newArray(int dimensionality) {
    StatisticalMoments[] arr = new StatisticalMoments[dimensionality];
    for (int i = 0; i < dimensionality; i++) {
      arr[i] = new StatisticalMoments();
    }
    return arr;
  }

  @Override
  public String toString() {
    return "StatisticalMoments(mean=" + getMean() + ",m2=" + m2 + ",m3=" + m3 + ",m4=" + m4 + ",n=" + n + ")";
  }

  @Override
  public void reset() {
    super.reset();
    m3 = 0;
    m4 = 0;
  }
}
