package de.lmu.ifi.dbs.elki.math;

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

import java.util.SortedSet;

import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Class to find the minimum and maximum double values in data.
 * 
 * @author Erich Schubert
 * 
 * Note: Unused, see {@link DoubleMinMax} specialization.
 * 
 * @param <T> Data type
 */
public class MinMax<T extends Comparable<? super T>> extends Pair<T, T> {
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
    if(this.first == null || this.first.compareTo(data) > 0) {
      this.first = data;
    }
    if(this.second == null || this.second.compareTo(data) < 0) {
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
    if(!data.isEmpty()) {
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

  /**
   * Return minimum and maximum as array.
   * 
   * @return Minimum, Maximum
   */
  public Object[] asArray() {
    return new Object[] { this.getMin(), this.getMax() };
  }

  /**
   * New array of MinMax objects for a given type.
   * 
   * @param <N> Number type.
   * @param size Size.
   * @return Initialized array.
   */
  public static <N extends Comparable<N>> MinMax<N>[] newArray(int size) {
    Class<MinMax<N>> mmcls = ClassGenericsUtil.uglyCastIntoSubclass(MinMax.class);
    MinMax<N>[] mms = ClassGenericsUtil.newArrayOfNull(size, mmcls);
    for(int i = 0; i < size; i++) {
      mms[i] = new MinMax<N>();
    }
    return mms;
  }
}