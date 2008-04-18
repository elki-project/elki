package de.lmu.ifi.dbs.algorithm.clustering.clique;

import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.data.RealVector;

/**
 * Represents an one-dimensional interval.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class CLIQUEInterval {
  /**
   * The dimension of this interval in the original data space.
   */
  private int dimension;

  /**
   * The minimum value of this interval.
   */
  private double min;

  /**
   * The maximum value of this interval.
   */
  private double max;


  /**
   * Creates a new interval with the specified parameters.
   * @param dimension the dimension of the interval in the original data space
   * @param min the minimum (left) value of the interval
   * @param max the maximum (right) value of the interval
   */
  public CLIQUEInterval(int dimension, double min, double max) {
    this.dimension = dimension;
    this.min = min;
    this.max = max;
  }


  /**
   * Returns the dimension of the interval in the original data space
   * @return the dimension of the interval in the original data space
   */
  public int getDimension() {
    return dimension;
  }

  /**
   * Returns the minimum (left) value of the interval.
   * @return the minimum (left) value of the interval
   */
  public double getMin() {
    return min;
  }

  /**
   * Returns the maximum (right) value of the interval.
   * @return the maximum (right) value of the interval
   */
  public double getMax() {
    return max;
  }

  /**
   * Returns a string representation of this interval.
   * The string representation consists of the dimension and
   * the min and max values of this interval.
   *
   * @return a string representation of this interval
   */
  public String toString() {
    return dimension + "-[" + Util.format(min, 2) + "; " + Util.format(max, 2) + "]";
  }
}
