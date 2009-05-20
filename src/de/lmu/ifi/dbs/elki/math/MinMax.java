package de.lmu.ifi.dbs.elki.math;

import java.util.SortedSet;

import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Class to find the minimum and maximum double values in data.
 * 
 * @author Erich Schubert
 */
public class MinMax<T extends Comparable<? super T>> extends Pair<T,T> {
  /**
   * Constructor without starting values.
   * 
   * The minimum will be initialized to {@code null}.
   * 
   * The maximum will be initialized to {@code null}.
   */
  public MinMax() {
    super(null, null);
  }

  /**
   * Constructor with predefined minimum and maximum values.
   * 
   * @param min Minimum value
   * @param max Maximum value
   */
  public MinMax(T min, T max) {
    super(min, max);
  }

  /**
   * Process a single value.
   * 
   * If the new value is smaller than the current minimum, it will become the
   * new minimum.
   * 
   * If the new value is larger than the current maximum, it will become the new
   * maximum.
   * 
   * @param data New value
   */
  public void put(T data) {
    if (this.first == null || this.first.compareTo(data) > 0) {
      this.first = data;
    }
    if (this.second == null || this.second.compareTo(data) < 0) {
      this.second = data;
    }
  }

  /**
   * Process a whole array of values.
   * 
   * If any of the values is smaller than the current minimum, it will become
   * the new minimum.
   * 
   * If any of the values is larger than the current maximum, it will become the
   * new maximum.
   * 
   * @param data Data to process
   */
  public void put(T[] data) {
    for(T value : data) {
      this.put(value);
    }
  }

  /**
   * Process a whole collection of values.
   * 
   * If any of the values is smaller than the current minimum, it will become
   * the new minimum.
   * 
   * If any of the values is larger than the current maximum, it will become the
   * new maximum.
   * 
   * @param data Data to process
   */
  public void put(Iterable<T> data) {
    for(T value : data) {
      this.put(value);
    }
  }

  /**
   * Process a whole collection of values.
   * 
   * If any of the values is smaller than the current minimum, it will become
   * the new minimum.
   * 
   * If any of the values is larger than the current maximum, it will become the
   * new maximum.
   * 
   * @param data Data to process
   */
  public void put(SortedSet<T> data) {
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
  public T getMin() {
    return this.getFirst();
  }

  /**
   * Get the current maximum.
   * 
   * @return current maximum.
   */
  public T getMax() {
    return this.getSecond();
  }

  /**
   * Test if we have seen any data (and thus have a useful minimum and maximum).
   * 
   * @return {@code true} iff min != null and max != null.
   */
  public boolean isValid() {
    return (this.getMin() != null) && (this.getMax() != null);
  }
}
