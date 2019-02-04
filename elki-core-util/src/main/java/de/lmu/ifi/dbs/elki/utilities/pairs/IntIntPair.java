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
package de.lmu.ifi.dbs.elki.utilities.pairs;

import java.util.Comparator;

/**
 * Pair storing two integers.
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @has - - - Comparator
 */
public class IntIntPair implements Comparable<IntIntPair> {
  /**
   * first value
   */
  public int first;

  /**
   * second value
   */
  public int second;

  /**
   * Constructor
   * 
   * @param first First value
   * @param second Second value
   */
  public IntIntPair(int first, int second) {
    super();
    this.first = first;
    this.second = second;
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

    IntIntPair other = (IntIntPair) obj;
    return (this.first == other.first) && (this.second == other.second);
  }

  /**
   * Trivial hashCode implementation mixing the two integers.
   */
  @Override
  public final int hashCode() {
    // primitive hash function mixing the two integers.
    // this number does supposedly not have any factors in common with 2^32
    return (int) (first * 0x9e3779b1 + second);
  }

  /**
   * Implementation of comparable interface, sorting by first then second.
   * 
   * @param other Object to compare to
   * @return comparison result
   */
  @Override
  public int compareTo(IntIntPair other) {
    int fdiff = this.first - other.first;
    if(fdiff != 0) {
      return fdiff;
    }
    return this.second - other.second;
  }

  /**
   * Implementation of comparableSwapped interface, sorting by second then
   * first.
   * 
   * @param other Object to compare to
   * @return comparison result
   */
  public int compareSwappedTo(IntIntPair other) {
    int fdiff = this.second - other.second;
    if(fdiff != 0) {
      return fdiff;
    }
    return this.first - other.first;
  }

  /**
   * Set first value
   * 
   * @param first new value
   */
  public final void setFirst(int first) {
    this.first = first;
  }

  /**
   * Set second value
   * 
   * @param second new value
   */
  public final void setSecond(int second) {
    this.second = second;
  }

  @Override
  public String toString() {
    return "(" + first + ", " + second + ")";
  }

  /**
   * Comparator to compare by second component only
   */
  public static final Comparator<IntIntPair> BYFIRST_COMPARATOR = new Comparator<IntIntPair>() {
    @Override
    public int compare(IntIntPair o1, IntIntPair o2) {
      return o1.first - o2.first;
    }
  };

  /**
   * Comparator to compare by second component only
   */
  public static final Comparator<IntIntPair> BYSECOND_COMPARATOR = new Comparator<IntIntPair>() {
    @Override
    public int compare(IntIntPair o1, IntIntPair o2) {
      return o1.second - o2.second;
    }
  };

  /**
   * Comparator to compare by swapped components
   */
  public static final Comparator<IntIntPair> SWAPPED_COMPARATOR = new Comparator<IntIntPair>() {
    @Override
    public int compare(IntIntPair o1, IntIntPair o2) {
      return o1.compareSwappedTo(o2);
    }
  };
}
