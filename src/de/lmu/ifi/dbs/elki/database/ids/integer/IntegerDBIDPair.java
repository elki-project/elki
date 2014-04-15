package de.lmu.ifi.dbs.elki.database.ids.integer;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;

/**
 * DBID pair using two ints for storage.
 * 
 * @author Erich Schubert
 */
class IntegerDBIDPair implements DBIDPair, IntegerDBIDs {
  /**
   * First value in pair
   */
  public int first;

  /**
   * Second value in pair
   */
  public int second;

  /**
   * Initialize pair
   * 
   * @param first first parameter
   * @param second second parameter
   */
  protected IntegerDBIDPair(int first, int second) {
    this.first = first;
    this.second = second;
  }

  /**
   * Canonical toString operator
   */
  @Override
  public String toString() {
    return "Pair(" + first + ", " + second + ")";
  }

  /**
   * Getter for first
   * 
   * @return first element in pair
   */
  @Override
  public final IntegerDBID getFirst() {
    return new IntegerDBID(first);
  }

  /**
   * Getter for second element in pair
   * 
   * @return second element in pair
   */
  @Override
  public final IntegerDBID getSecond() {
    return new IntegerDBID(second);
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
    if(obj == null) {
      return false;
    }
    if(!(obj instanceof IntegerDBIDPair)) {
      return false;
    }
    IntegerDBIDPair other = (IntegerDBIDPair) obj;
    return (this.first == other.first) && (this.second == other.second);
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
    result = prime * result + first;
    result = prime * result + second;
    return (int) result;
  }

  @Override
  public int size() {
    return 2;
  }

  @Override
  public boolean contains(DBIDRef o) {
    final int i = o.internalGetIndex();
    return (i == first) || (i == second);
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public IntegerDBIDIter iter() {
    return new Itr(first, second);
  }

  /**
   * Iterator.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private static class Itr implements IntegerDBIDIter {
    /**
     * State
     */
    int first, second, pos;

    /**
     * Constructor.
     * 
     * @param first First ID
     * @param second Second ID
     */
    public Itr(int first, int second) {
      super();
      this.first = first;
      this.second = second;
      this.pos = 0;
    }

    @Override
    public boolean valid() {
      return pos < 2;
    }

    @Override
    public DBIDIter advance() {
      ++pos;
      return this;
    }

    @Override
    public int internalGetIndex() {
      return (pos == 0) ? first : second;
    }
  }
}