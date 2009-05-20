package de.lmu.ifi.dbs.elki.math;

import java.util.SortedSet;

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
  public void put(Double[] data) {
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
  public void put(Iterable<Double> data) {
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
  public void put(SortedSet<Double> data) {
    if (!data.isEmpty()) {
      this.put(data.first());
      this.put(data.last());
    }
  }

  
  /**
   * Get the current minimum.
   * 
   * @return current minimum.
   */
  public double getMin() {
    return this.getFirst();
  }

  /**
   * Get the current maximum.
   * 
   * @return current maximum.
   */
  public double getMax() {
    return this.getSecond();
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
   * Test if we have seen any data (and thus have a useful minimum and maximum).
   * by convention, the values are initialized such that min > max when there was no
   * data yet, ideally min = Datatype.MAX_VALUE and max = Datatype.MIN_VALUE.
   * 
   * @return {@code true} iff min <= max.
   */
  public boolean isValid() {
    return (this.getMin() <= this.getMax());
  }

  /**
   * Return minimum and maximum as array.
   * 
   * @return Minimum, Maximum
   */
  public double[] asArray() {
    return new double[] { this.getMin(), this.getMax() };
  }

  /**
   * Generate a new array of initialized DoubleMinMax objects (with default constructor)
   * 
   * @param size Array size
   * @return initialized array
   */
  public static DoubleMinMax[] newArray(int size) {
    DoubleMinMax ret[] = new DoubleMinMax[size];
    for (int i = 0; i < size; i++) {
      ret[i] = new DoubleMinMax();
    }
    return ret;
  }
}
