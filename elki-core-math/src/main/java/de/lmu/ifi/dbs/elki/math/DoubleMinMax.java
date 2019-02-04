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

/**
 * Class to find the minimum and maximum double values in data.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public class DoubleMinMax {
  /**
   * Minimum and maximum.
   */
  private double min, max;

  /**
   * Constructor without starting values.
   * 
   * The minimum will be initialized to {@link Double#POSITIVE_INFINITY}.
   * 
   * The maximum will be initialized to {@link Double#NEGATIVE_INFINITY}.
   * 
   * So that the first data added will replace both.
   */
  public DoubleMinMax() {
    super();
    this.min = Double.POSITIVE_INFINITY;
    this.max = Double.NEGATIVE_INFINITY;
  }

  /**
   * Constructor with predefined minimum and maximum values.
   * 
   * @param min Minimum value
   * @param max Maximum value
   */
  public DoubleMinMax(double min, double max) {
    super();
    this.min = min;
    this.max = max;
  }

  /**
   * Process a single double value.
   * 
   * If the new value is smaller than the current minimum, it will become the
   * new minimum.
   * 
   * If the new value is larger than the current maximum, it will become the new
   * maximum.
   * 
   * @param val New value
   */
  public void put(double val) {
    min = val < min ? val : min;
    max = val > max ? val : max;
  }

  /**
   * Process a whole array of double values.
   * 
   * If any of the values is smaller than the current minimum, it will become
   * the new minimum.
   * 
   * If any of the values is larger than the current maximum, it will become the
   * new maximum.
   * 
   * @param data Data to process
   */
  public void put(double[] data) {
    final int l = data.length;
    for (int i = 0; i < l; i++) {
      put(data[i]);
    }
  }

  /**
   * Process a MinMax pair.
   * 
   * @param val New value
   */
  public void put(DoubleMinMax val) {
    min = val.min < min ? val.min : min;
    max = val.max > max ? val.max : max;
  }

  /**
   * Get the current minimum.
   * 
   * @return current minimum.
   */
  public double getMin() {
    return this.min;
  }

  /**
   * Get the current maximum.
   * 
   * @return current maximum.
   */
  public double getMax() {
    return this.max;
  }

  /**
   * Return the difference between minimum and maximum.
   * 
   * @return Difference of current Minimum and Maximum.
   */
  public double getDiff() {
    return this.max - this.min;
  }

  /**
   * Test whether the result is defined.
   * 
   * @return true when at least one value has been added
   */
  public boolean isValid() {
    return (min <= max);
  }

  /**
   * Return minimum and maximum as array.
   * 
   * @return Minimum, Maximum
   */
  public double[] asDoubleArray() {
    return new double[] { this.min, this.max };
  }

  /**
   * Generate a new array of initialized DoubleMinMax objects (with default
   * constructor)
   * 
   * @param size Array size
   * @return initialized array
   */
  public static DoubleMinMax[] newArray(int size) {
    DoubleMinMax[] ret = new DoubleMinMax[size];
    for(int i = 0; i < size; i++) {
      ret[i] = new DoubleMinMax();
    }
    return ret;
  }

  /**
   * Reset statistics.
   */
  public void reset() {
    min = Double.POSITIVE_INFINITY;
    max = Double.NEGATIVE_INFINITY;
  }

  @Override
  public String toString() {
    return "DoubleMinMax[min=" + min + ", max=" + max + "]";
  }
}
