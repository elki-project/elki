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

import java.util.Collection;

import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;

/**
 * Class to find the minimum and maximum double values in data.
 * 
 * @author Erich Schubert
 */
public class DoubleMinMax extends DoubleDoublePair {
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
    super(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
  }

  /**
   * Constructor with predefined minimum and maximum values.
   * 
   * @param min Minimum value
   * @param max Maximum value
   */
  public DoubleMinMax(double min, double max) {
    super(min, max);
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
   * @param data New value
   */
  public void put(double data) {
    this.first = Math.min(this.first, data);
    this.second = Math.max(this.second, data);
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
    for(double value : data) {
      this.put(value);
    }
  }

  /**
   * Process a whole collection of double values.
   * 
   * If any of the values is smaller than the current minimum, it will become
   * the new minimum.
   * 
   * If any of the values is larger than the current maximum, it will become the
   * new maximum.
   * 
   * @param data Data to process
   */
  public void put(Collection<Double> data) {
    for(Double value : data) {
      this.put(value);
    }
  }

  /**
   * Get the current minimum.
   * 
   * @return current minimum.
   */
  public double getMin() {
    return this.first;
  }

  /**
   * Get the current maximum.
   * 
   * @return current maximum.
   */
  public double getMax() {
    return this.second;
  }

  /**
   * Return the difference between minimum and maximum.
   * 
   * @return Difference of current Minimum and Maximum.
   */
  public double getDiff() {
    return this.getMax() - this.getMin();
  }

  /**
   * Test whether the result is defined.
   * 
   * @return true when at least one value has been added
   */
  public boolean isValid() {
    return (first <= second);
  }

  /**
   * Return minimum and maximum as array.
   * 
   * @return Minimum, Maximum
   */
  public double[] asDoubleArray() {
    return new double[] { this.getMin(), this.getMax() };
  }

  /**
   * Generate a new array of initialized DoubleMinMax objects (with default
   * constructor)
   * 
   * @param size Array size
   * @return initialized array
   */
  public static DoubleMinMax[] newArray(int size) {
    DoubleMinMax ret[] = new DoubleMinMax[size];
    for(int i = 0; i < size; i++) {
      ret[i] = new DoubleMinMax();
    }
    return ret;
  }
}
