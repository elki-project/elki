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

/**
 * Simple class wrapping two objects.
 *
 * <b>Do not use this for primitive types such as {@code Integer} and
 * {@code Double} - avoid the memory waste and garbage collection overhead!</b>
 *
 * Does not implement any "special" interfaces such as Comparable. If you need
 * more complicated pairs, please use <em>domain specific</em> code, with more
 * meaningful field names and comparators.
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @param <FIRST> first type
 * @param <SECOND> second type
 */
public class Pair<FIRST, SECOND> {
  /**
   * First value in pair
   */
  public FIRST first;

  /**
   * Second value in pair
   */
  public SECOND second;

  /**
   * Initialize pair
   *
   * @param first first parameter
   * @param second second parameter
   */
  public Pair(FIRST first, SECOND second) {
    this.first = first;
    this.second = second;
  }

  /**
   * Canonical toString operator
   */
  @Override
  public String toString() {
    return "Pair(" + (first != null ? first.toString() : "null") + ", " + (second != null ? second.toString() : "null") + ")";
  }

  /**
   * Getter for first
   *
   * @return first element in pair
   */
  public final FIRST getFirst() {
    return first;
  }

  /**
   * Setter for first
   *
   * @param first new value for first element
   */
  public final void setFirst(FIRST first) {
    this.first = first;
  }

  /**
   * Getter for second element in pair
   *
   * @return second element in pair
   */
  public final SECOND getSecond() {
    return second;
  }

  /**
   * Setter for second
   *
   * @param second new value for second element
   */
  public final void setSecond(SECOND second) {
    this.second = second;
  }

  /**
   * Simple equals statement.
   *
   * This Pair equals another Object if they are identical or if the other
   * Object is also a Pair and the {@link #first} and {@link #second} element of
   * this Pair equal the {@link #first} and {@link #second} element,
   * respectively, of the other Pair.
   *
   * @param obj Object to compare to
   */
  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || this.getClass() != obj.getClass()) {
      return false;
    }
    Pair<?, ?> other = (Pair<?, ?>) obj;
    // Handle "null" values appropriately
    return (this.first == other.first || (this.first != null && this.first.equals(other.first))) //
        && (this.second == other.second || (this.second != null && this.second.equals(other.second)));
  }

  /**
   * Canonical hash function, mixing the two hash values.
   */
  @Override
  public final int hashCode() {
    // primitive hash function mixing the two integers.
    // this number does supposedly not have any factors in common with 2^32
    final long prime = 2654435761L;
    long result = 1;
    result = prime * result + ((first == null) ? 0 : first.hashCode());
    result = prime * result + ((second == null) ? 0 : second.hashCode());
    return (int) result;
  }
}
