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
package de.lmu.ifi.dbs.elki.database.ids.integer;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;

/**
 * DBID pair using two ints for storage.
 *
 * @author Erich Schubert
 * @since 0.4.0
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

  @Deprecated
  @Override
  public final IntegerDBID getFirst() {
    return new IntegerDBID(first);
  }

  @Deprecated
  @Override
  public final IntegerDBID getSecond() {
    return new IntegerDBID(second);
  }

  @Deprecated
  @Override
  public DBID get(int i) {
    if(i < 0 || i > 1) {
      throw new ArrayIndexOutOfBoundsException();
    }
    return i == 0 ? getFirst() : getSecond();
  }

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
  public DBIDVar assignVar(int index, DBIDVar var) {
    if(index == 0) {
      if(var instanceof IntegerDBIDVar) {
        ((IntegerDBIDVar) var).internalSetIndex(first);
        return var;
      }
      else {
        // Much less efficient:
        var.set(new IntegerDBID(first));
        return var;
      }
    }
    if(index == 1) {
      if(var instanceof IntegerDBIDVar) {
        ((IntegerDBIDVar) var).internalSetIndex(second);
        return var;
      }
      else {
        // Much less efficient:
        var.set(new IntegerDBID(second));
        return var;
      }
    }
    throw new ArrayIndexOutOfBoundsException();
  }

  @Override
  public ArrayDBIDs slice(int begin, int end) {
    return new Slice(begin, end);
  }

  @Override
  public int binarySearch(DBIDRef key) {
    int v = key.internalGetIndex();
    return (v == first) ? 0 //
        : (v == second) ? 1 //
            : (v < first) ? -1 //
                : (v < second) ? -2 : -3;
  }

  @Override
  public IntegerDBIDArrayIter iter() {
    return new Itr(first, second);
  }

  /**
   * Iterator.
   *
   * @author Erich Schubert
   */
  private static class Itr implements IntegerDBIDArrayIter {
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
    public Itr advance() {
      ++pos;
      return this;
    }

    @Override
    public int internalGetIndex() {
      return (pos == 0) ? first : second;
    }

    @Override
    public int getOffset() {
      return pos;
    }

    @Override
    public DBIDArrayIter advance(int count) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public DBIDArrayIter retract() {
      --pos;
      return this;
    }

    @Override
    public DBIDArrayIter seek(int off) {
      pos = off;
      return this;
    }
  }

  /**
   * Slice of an array.
   *
   * @author Erich Schubert
   */
  private class Slice implements IntegerArrayDBIDs {
    /**
     * Slice positions.
     */
    final int begin, end;

    /**
     * Constructor.
     *
     * @param begin Begin, inclusive
     * @param end End, exclusive
     */
    public Slice(int begin, int end) {
      super();
      this.begin = begin;
      this.end = end;
    }

    @Override
    public int size() {
      return end - begin;
    }

    @Override
    public boolean contains(DBIDRef o) {
      int oid = o.internalGetIndex();
      return (begin == 0 && end > 0 && first == oid) //
          || (begin <= 1 && end > 1 && second == oid);
    }

    @Override
    public boolean isEmpty() {
      return begin == end;
    }

    @Override
    public DBID get(int i) {
      return IntegerDBIDPair.this.get(begin + i);
    }

    @Override
    public DBIDVar assignVar(int index, DBIDVar var) {
      return IntegerDBIDPair.this.assignVar(begin + index, var);
    }

    @Override
    public int binarySearch(DBIDRef key) {
      return IntegerDBIDPair.this.binarySearch(key) - begin;
    }

    @Override
    public IntegerDBIDArrayIter iter() {
      return new SliceItr();
    }

    @Override
    public Slice slice(int begin, int end) {
      return new Slice(this.begin + begin, this.begin + end);
    }

    /**
     * Iterator class.
     *
     * @author Erich Schubert
     */
    private class SliceItr implements IntegerDBIDArrayIter {
      /**
       * Iterator position.
       */
      int pos = begin;

      @Override
      public int internalGetIndex() {
        if(pos < 0 || pos > 1) {
          throw new ArrayIndexOutOfBoundsException();
        }
        return pos == 0 ? first : second;
      }

      @Override
      public boolean valid() {
        return pos < end && pos >= begin;
      }

      @Override
      public SliceItr advance() {
        ++pos;
        return this;
      }

      @Override
      public int getOffset() {
        return pos - begin;
      }

      @Override
      public SliceItr advance(int count) {
        pos += count;
        return this;
      }

      @Override
      public SliceItr retract() {
        --pos;
        return this;
      }

      @Override
      public SliceItr seek(int off) {
        pos = begin + off;
        return this;
      }

      @Override
      public String toString() {
        return Integer.toString(internalGetIndex()) + "@" + pos;
      }
    }
  }
}
