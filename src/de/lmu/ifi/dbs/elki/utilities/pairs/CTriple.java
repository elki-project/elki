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
 * Triple with canonical comparison function.
 * 
 * @author Erich Schubert
 * 
 * @param <FIRST> first type
 * @param <SECOND> second type
 * @param <THIRD> second type
 */
public final class CTriple<FIRST extends Comparable<FIRST>, SECOND extends Comparable<SECOND>, THIRD extends Comparable<THIRD>> extends Triple<FIRST, SECOND, THIRD> implements Comparable<CTriple<FIRST, SECOND, THIRD>> {
  /**
   * Constructor with fields
   * 
   * @param first Value of first component
   * @param second Value of second component
   * @param third Value of third component
   */
  public CTriple(FIRST first, SECOND second, THIRD third) {
    super(first, second, third);
  }

  /**
   * Canonical toString operator
   */
  @Override
  public String toString() {
    return "Triple(" + first.toString() + ", " + second.toString() + ", " + third.toString() + ")";
  }

  /**
   * Generic derived compare function.
   * 
   * @param other Object to compare to
   * @return comparison result
   */
  @Override
  public int compareTo(CTriple<FIRST, SECOND, THIRD> other) {
    // try comparing by first
    if(this.first != null) {
      if(other.first == null) {
        return -1;
      }
      int delta1 = this.first.compareTo(other.first);
      if(delta1 != 0) {
        return delta1;
      }
    }
    else if(other.first != null) {
      return +1;
    }
    // try comparing by second
    if(this.second != null) {
      if(other.second == null) {
        return -1;
      }
      int delta2 = this.second.compareTo(other.second);
      if(delta2 != 0) {
        return delta2;
      }
    }
    else if(other.second != null) {
      return +1;
    }
    // try comparing by third
    if(this.third != null) {
      if(other.third == null) {
        return -1;
      }
      int delta3 = this.third.compareTo(other.third);
      if(delta3 != 0) {
        return delta3;
      }
    }
    else if(other.third != null) {
      return +1;
    }
    return 0;
  }

  /**
   * Array constructor for generics
   * 
   * @param <F> First type
   * @param <S> Second type
   * @param <T> Third type
   * @param size Size of array to be constructed.
   * @return New array of requested size
   */
  public static final <F extends Comparable<F>, S extends Comparable<S>, T extends Comparable<T>> CTriple<F, S, T>[] newArray(int size) {
    Class<CTriple<F, S, T>> tripcls = ClassGenericsUtil.uglyCastIntoSubclass(CTriple.class);
    return ClassGenericsUtil.newArrayOfNull(size, tripcls);
  }
}