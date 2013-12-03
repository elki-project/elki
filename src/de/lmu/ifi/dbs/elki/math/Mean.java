package de.lmu.ifi.dbs.elki.math;

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

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Compute the mean using a numerically stable online algorithm.
 * 
 * This class can repeatedly be fed with data using the add() methods, the
 * resulting values for mean can be queried at any time using getMean().
 * 
 * Trivial code, but replicated a lot. The class is final so it should come at
 * low cost.
 * 
 * Related Literature:
 * 
 * <p>
 * B. P. Welford<br />
 * Note on a method for calculating corrected sums of squares and products<br />
 * in: Technometrics 4(3)
 * </p>
 * 
 * <p>
 * D.H.D. West<br />
 * Updating Mean and Variance Estimates: An Improved Method<br />
 * In: Communications of the ACM, Volume 22 Issue 9
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "B. P. Welford", title = "Note on a method for calculating corrected sums of squares and products", booktitle = "Technometrics 4(3)")
public class Mean {
  /**
   * Mean of values - first moment.
   */
  protected double m1 = 0.;

  /**
   * Weight sum (number of samples).
   */
  protected double n = 0;

  /**
   * Empty constructor
   */
  public Mean() {
    // nothing to do here, initialization done above.
  }

  /**
   * Constructor from other instance
   * 
   * @param other other instance to copy data from.
   */
  public Mean(Mean other) {
    this.m1 = other.m1;
    this.n = other.n;
  }

  /**
   * Add a single value with weight 1.0
   * 
   * @param val Value
   */
  public void put(double val) {
    n += 1.0;
    final double delta = val - m1;
    m1 += delta / n;
  }

  /**
   * Add data with a given weight.
   * 
   * See also: D.H.D. West<br />
   * Updating Mean and Variance Estimates: An Improved Method
   * 
   * @param val data
   * @param weight weight
   */
  public void put(double val, double weight) {
    final double nwsum = weight + n;
    final double delta = val - m1;
    final double rval = delta * weight / nwsum;
    m1 += rval;
    n = nwsum;
  }

  /**
   * Join the data of another MeanVariance instance.
   * 
   * @param other Data to join with
   */
  public void put(Mean other) {
    final double nwsum = other.n + this.n;

    // this.mean += rval;
    // This supposedly is more numerically stable:
    this.m1 = (this.n * this.m1 + other.n * other.m1) / nwsum;
    this.n = nwsum;
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
    return m1;
  }

  /**
   * Create and initialize a new array of MeanVariance
   * 
   * @param dimensionality Dimensionality
   * @return New and initialized Array
   */
  public static Mean[] newArray(int dimensionality) {
    Mean[] arr = new Mean[dimensionality];
    for (int i = 0; i < dimensionality; i++) {
      arr[i] = new Mean();
    }
    return arr;
  }

  @Override
  public String toString() {
    return "Mean(" + getMean() + ")";
  }

  /**
   * Reset the value.
   */
  public void reset() {
    m1 = 0;
    n = 0;
  }

  /**
   * Static helper function.
   * 
   * @param data Data to compute the mean for.
   * @return Mean
   */
  public static double of(double[] data) {
    // FIXME: what is numerically best. Kahan summation?
    double sum = 0.;
    for (double v : data) {
      sum += v;
    }
    return sum / data.length;
  }
}
