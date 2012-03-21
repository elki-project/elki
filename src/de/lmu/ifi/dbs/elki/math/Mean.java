package de.lmu.ifi.dbs.elki.math;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
   * Mean of values
   */
  protected double mean = 0.0;

  /**
   * Weight sum (number of samples)
   */
  protected double wsum = 0;

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
    this.mean = other.mean;
    this.wsum = other.wsum;
  }

  /**
   * Add a single value with weight 1.0
   * 
   * @param val Value
   */
  public void put(double val) {
    wsum += 1.0;
    final double delta = val - mean;
    mean += delta / wsum;
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
    final double nwsum = weight + wsum;
    final double delta = val - mean;
    final double rval = delta * weight / nwsum;
    mean += rval;
    wsum = nwsum;
  }

  /**
   * Join the data of another MeanVariance instance.
   * 
   * @param other Data to join with
   */
  public void put(Mean other) {
    final double nwsum = other.wsum + this.wsum;

    // this.mean += rval;
    // This supposedly is more numerically stable:
    this.mean = (this.wsum * this.mean + other.wsum * other.mean) / nwsum;
    this.wsum = nwsum;
  }

  /**
   * Get the number of points the average is based on.
   * 
   * @return number of data points
   */
  public double getCount() {
    return wsum;
  }

  /**
   * Return mean
   * 
   * @return mean
   */
  public double getMean() {
    return mean;
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
    return "Mean(" + getMean() + ")";
  }

  /**
   * Reset the value.
   */
  public void reset() {
    mean = 0;
    wsum = 0;
  }
}