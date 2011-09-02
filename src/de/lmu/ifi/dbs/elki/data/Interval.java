package de.lmu.ifi.dbs.elki.data;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.utilities.FormatUtil;

/**
 * Represents an interval in a certain dimension of the data space.
 * 
 * @author Elke Achtert
 */
public class Interval implements Comparable<Interval> {
  /**
   * The dimension of this interval in the (original) data space.
   */
  private int dimension;

  /**
   * The minimum (left) value of this interval.
   */
  private double min;

  /**
   * The maximum (right) value of this interval.
   */
  private double max;

  /**
   * Creates a new interval with the specified parameters.
   * 
   * @param dimension the dimension of the interval in the original data space
   * @param min the minimum (left) value of the interval
   * @param max the maximum (right) value of the interval
   */
  public Interval(int dimension, double min, double max) {
    this.dimension = dimension;
    this.min = min;
    this.max = max;
  }

  /**
   * Returns the dimension of the interval in the original data space
   * 
   * @return the dimension of the interval in the original data space
   */
  public int getDimension() {
    return dimension;
  }

  /**
   * Returns the minimum (left) value of the interval.
   * 
   * @return the minimum (left) value of the interval
   */
  public double getMin() {
    return min;
  }

  /**
   * Returns the maximum (right) value of the interval.
   * 
   * @return the maximum (right) value of the interval
   */
  public double getMax() {
    return max;
  }

  /**
   * Returns a string representation of this interval. The string representation
   * consists of the dimension and the min and max values of this interval.
   * 
   * @return a string representation of this interval
   */
  @Override
  public String toString() {
    return "d" + (dimension + 1) + "-[" + FormatUtil.format(min, 2) + "; " + FormatUtil.format(max, 2) + "[";
  }

  /**
   * Compares this interval with the specified interval for order. Returns a
   * negative integer, zero, or a positive integer as this interval is less
   * than, equal to, or greater than the specified interval. First the
   * dimensions of the intervals are compared. In case of equality the min
   * (left) values are compared.
   * 
   * @param other the interval to be compared
   * @return a negative integer, zero, or a positive integer as this object is
   *         less than, equal to, or greater than the specified object.
   */
  @Override
  public int compareTo(Interval other) {
    if(dimension < other.dimension) {
      return -1;
    }
    if(dimension > other.dimension) {
      return 1;
    }

    if(min < other.min) {
      return -1;
    }
    if(min > other.min) {
      return 1;
    }

    if(max != other.max) {
      throw new RuntimeException("Should never happen!");
    }
    return 0;
  }
}