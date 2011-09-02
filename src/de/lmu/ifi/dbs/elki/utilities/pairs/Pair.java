package de.lmu.ifi.dbs.elki.utilities.pairs;

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

import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;

/**
 * Generic SimplePair<FIRST,SECOND> class.
 * 
 * Does not implement any "special" interfaces such as Comparable, use
 * {@link CPair} if you want comparable pairs.
 * 
 * @author Erich Schubert
 * 
 * @param <FIRST> first type
 * @param <SECOND> second type
 */
public class Pair<FIRST, SECOND> implements PairInterface<FIRST, SECOND> {
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
  @Override
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
  @Override
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
   * Create a new array of the given size (for generics)
   * 
   * @param <F> First class
   * @param <S> Second class
   * @param size array size
   * @return empty array of the new type.
   */
  public static final <F, S> Pair<F, S>[] newPairArray(int size) {
    Class<Pair<F, S>> paircls = ClassGenericsUtil.uglyCastIntoSubclass(Pair.class);
    return ClassGenericsUtil.newArrayOfNull(size, paircls);
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
  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null) {
      return false;
    }
    if(!(obj instanceof Pair)) {
      return false;
    }
    Pair<FIRST, SECOND> other = (Pair<FIRST, SECOND>) obj;
    // Handle "null" values appropriately
    if(this.first == null) {
      if(other.first != null) {
        return false;
      }
    } else {
      if (!this.first.equals(other.first)) {
        return false;
      }
    }
    if(this.second == null) {
      if(other.second != null) {
        return false;
      }
    } else {
      if (!this.second.equals(other.second)) {
        return false;
      }
    }
    return true;
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