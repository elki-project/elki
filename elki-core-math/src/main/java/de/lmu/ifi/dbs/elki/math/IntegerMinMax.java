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
 * Class to find the minimum and maximum int values in data.
 * 
 * @author Erich Schubert
 * @author Arthur Zimek
 * @since 0.5.5
 */
public class IntegerMinMax {
  /**
   * Minimum and maximum.
   */
  private int min, max;

  /**
   * Constructor without starting values.
   * 
   * The minimum will be initialized to {@link Integer#MAX_VALUE}.
   * 
   * The maximum will be initialized to {@link Integer#MIN_VALUE}.
   * 
   * So that the first data added will replace both.
   */
  public IntegerMinMax() {
    super();
    min = Integer.MAX_VALUE;
    max = Integer.MIN_VALUE;
  }

  /**
   * Constructor with predefined minimum and maximum values.
   * 
   * @param min Minimum value
   * @param max Maximum value
   */
  public IntegerMinMax(int min, int max) {
    super();
    this.min = min;
    this.max = max;
  }

  /**
   * Process a single int value.
   * 
   * If the new value is smaller than the current minimum, it will become the
   * new minimum.
   * 
   * If the new value is larger than the current maximum, it will become the new
   * maximum.
   * 
   * @param val New value
   */
  public void put(int val) {
    min = val < min ? val : min;
    max = val > max ? val : max;
  }

  /**
   * Process a whole array of int values.
   * 
   * If any of the values is smaller than the current minimum, it will become
   * the new minimum.
   * 
   * If any of the values is larger than the current maximum, it will become the
   * new maximum.
   * 
   * @param data Data to process
   */
  public void put(int[] data) {
    final int l = data.length;
    for(int i = 0; i < l; i++) {
      put(data[i]);
    }
  }

  /**
   * Process a MinMax pair.
   * 
   * @param val New value
   */
  public void put(IntegerMinMax val) {
    min = val.min < min ? val.min : min;
    max = val.max > max ? val.max : max;
  }

  /**
   * Get the current minimum.
   * 
   * @return current minimum.
   */
  public int getMin() {
    return this.min;
  }

  /**
   * Get the current maximum.
   * 
   * @return current maximum.
   */
  public int getMax() {
    return this.max;
  }

  /**
   * Return the difference between minimum and maximum.
   * 
   * Returns a long, as the difference may exceed integer bounds.
   * 
   * @return Difference of current Minimum and Maximum.
   */
  public long getDiff() {
    return this.max - (long) this.min;
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
  public int[] asIntArray() {
    return new int[] { this.min, this.max };
  }

  /**
   * Generate a new array of initialized IntegerMinMax objects (with default
   * constructor)
   * 
   * @param size Array size
   * @return initialized array
   */
  public static IntegerMinMax[] newArray(int size) {
    IntegerMinMax[] ret = new IntegerMinMax[size];
    for(int i = 0; i < size; i++) {
      ret[i] = new IntegerMinMax();
    }
    return ret;
  }

  /**
   * Reset statistics.
   */
  public void reset() {
    min = Integer.MAX_VALUE;
    max = Integer.MIN_VALUE;
  }
}
