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

/**
 * Compute the mean using a numerically stable online algorithm.
 * <p>
 * This class can repeatedly be fed with data using the put() methods, the
 * resulting values for mean can be queried at any time using getMean().
 * <p>
 * The high-precision function is based on:
 * <p>
 * P. M. Neely<br>
 * Comparison of Several Algorithms for Computation of Means, Standard
 * Deviations and Correlation Coefficients<br>
 * Communications of the ACM 9(7), 1966
 *
 * @author Erich Schubert
 * @since 0.2
 */
public class Mean {
  /**
   * Sum of all values.
   */
  protected double sum;

  /**
   * Weight sum (number of samples).
   */
  protected double n;

  /**
   * Empty constructor
   */
  public Mean() {
    sum = 0.;
    n = 0;
  }

  /**
   * Constructor from other instance
   *
   * @param other other instance to copy data from.
   */
  public Mean(Mean other) {
    this.sum = other.sum;
    this.n = other.n;
  }

  /**
   * Add a single value with weight 1.0
   *
   * @param val Value
   */
  public void put(double val) {
    n += 1.;
    sum += val;
  }

  /**
   * Add data with a given weight.
   *
   * @param val data
   * @param weight weight
   */
  public void put(double val, double weight) {
    if(weight == 0.) {
      return;
    }
    sum += val * weight;
    n += weight;
  }

  /**
   * Join the data of another MeanVariance instance.
   *
   * @param other Data to join with
   */
  public void put(Mean other) {
    if(other.n == 0) {
      return;
    }
    this.sum += other.sum;
    this.n = other.n + this.n;
  }

  /**
   * Add values with weight 1.0
   *
   * @param vals Values
   * @return this
   */
  public Mean put(double[] vals) {
    for(double v : vals) {
      put(v);
    }
    return this;
  }

  /**
   * Add values with weight 1.0
   *
   * @param vals Values
   * @return this
   */
  public Mean put(double[] vals, double[] weights) {
    assert (vals.length == weights.length);
    for(int i = 0, end = vals.length; i < end; i++) {
      put(vals[i], weights[i]);
    }
    return this;
  }

  /**
   * Get the number of points the average is based on.
   *
   * @return number of data points
   */
  public double getCount() {
    return n;
  }

  /**
   * Return mean
   *
   * @return mean
   */
  public double getMean() {
    return sum / n;
  }

  /**
   * Create and initialize a new array of MeanVariance
   *
   * @param dimensionality Dimensionality
   * @return New and initialized Array
   */
  public static Mean[] newArray(int dimensionality) {
    Mean[] arr = new Mean[dimensionality];
    for(int i = 0; i < dimensionality; i++) {
      arr[i] = new Mean();
    }
    return arr;
  }

  @Override
  public String toString() {
    return "Mean(" + getMean() + ",weight=" + getCount() + ")";
  }

  /**
   * Reset the value.
   */
  public void reset() {
    sum = 0;
    n = 0;
  }

  /**
   * Static helper function.
   *
   * @param data Data to compute the mean for.
   * @return Mean
   */
  public static double of(double... data) {
    double sum = 0.;
    for(double v : data) {
      sum += v;
    }
    return sum / data.length;
  }

  /**
   * Static helper function, with extra precision
   *
   * @param data Data to compute the mean for.
   * @return Mean
   */
  @Reference(authors = "P. M. Neely", //
      title = "Comparison of Several Algorithms for Computation of Means, Standard Deviations and Correlation Coefficients", //
      booktitle = "Communications of the ACM 9(7), 1966", //
      url = "https://doi.org/10.1145/365719.365958", //
      bibkey = "doi:10.1145/365719.365958")
  public static double highPrecision(double... data) {
    double sum = 0.;
    for(double v : data) {
      sum += v;
    }
    sum /= data.length;
    // Perform a second pass to increase precision
    // In ideal math, this would sum to 0.
    double err = 0;
    for(double v : data) {
      err += v - sum;
    }
    return sum + err / data.length;
  }
}
