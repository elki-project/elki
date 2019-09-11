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
package elki.utilities.pairs;

import java.util.Comparator;

/**
 * Pair storing two doubles.
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @has - - - Comparator
 */
public class DoubleDoublePair implements Comparable<DoubleDoublePair> {
  /**
   * first value
   */
  public double first;

  /**
   * second value
   */
  public double second;

  /**
   * Constructor
   * 
   * @param first First value
   * @param second Second value
   */
  public DoubleDoublePair(double first, double second) {
    super();
    this.first = first;
    this.second = second;
  }

  /**
   * Clone constructor.
   *
   * @param other Existing pair.
   */
  public DoubleDoublePair(DoubleDoublePair other) {
    this(other.first, other.second);
  }

  /**
   * Trivial equals implementation
   * 
   * @param obj Object to compare to
   */
  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    DoubleDoublePair other = (DoubleDoublePair) obj;
    return (this.first == other.first) && (this.second == other.second);
  }

  /**
   * Trivial hashCode implementation mixing the two integers.
   */
  @Override
  public final int hashCode() {
    // convert to longs
    long firsthash = Double.doubleToLongBits(first);
    firsthash = firsthash ^ (firsthash >> 32);
    long secondhash = Double.doubleToLongBits(second);
    secondhash = secondhash ^ (secondhash >> 32);
    // primitive hash function mixing the two integers.
    // this number does supposedly not have any factors in common with 2^32
    return (int) (firsthash * 0x9e3779b1 + secondhash);
  }

  /**
   * Implementation of comparable interface, sorting by first then second.
   * 
   * @param other Object to compare to
   * @return comparison result
   */
  @Override
  public int compareTo(DoubleDoublePair other) {
    int fdiff = Double.compare(this.first, other.first);
    if(fdiff != 0) {
      return fdiff;
    }
    return Double.compare(this.second, other.second);
  }

  /**
   * Implementation of comparableSwapped interface, sorting by second then
   * first.
   * 
   * @param other Object to compare to
   * @return comparison result
   */
  public int compareSwappedTo(DoubleDoublePair other) {
    int fdiff = Double.compare(this.second, other.second);
    if(fdiff != 0) {
      return fdiff;
    }
    return Double.compare(this.first, other.first);
  }

  /**
   * Set first value
   * 
   * @param first new value
   */
  public final void setFirst(double first) {
    this.first = first;
  }

  /**
   * Set second value
   * 
   * @param second new value
   */
  public final void setSecond(double second) {
    this.second = second;
  }

  @Override
  public String toString() {
    return "DoubleDoublePair(" + first + "," + second + ")";
  }

  /**
   * Comparator to compare by second component only
   */
  public static final Comparator<DoubleDoublePair> BYFIRST_COMPARATOR = new Comparator<DoubleDoublePair>() {
    @Override
    public int compare(DoubleDoublePair o1, DoubleDoublePair o2) {
      return Double.compare(o1.first, o2.first);
    }
  };

  /**
   * Comparator to compare by second component only
   */
  public static final Comparator<DoubleDoublePair> BYSECOND_COMPARATOR = new Comparator<DoubleDoublePair>() {
    @Override
    public int compare(DoubleDoublePair o1, DoubleDoublePair o2) {
      return Double.compare(o1.second, o2.second);
    }
  };

  /**
   * Comparator to compare by swapped components
   */
  public static final Comparator<DoubleDoublePair> SWAPPED_COMPARATOR = new Comparator<DoubleDoublePair>() {
    @Override
    public int compare(DoubleDoublePair o1, DoubleDoublePair o2) {
      return o1.compareSwappedTo(o2);
    }
  };
}
