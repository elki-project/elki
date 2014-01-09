package de.lmu.ifi.dbs.elki.database.ids.integer;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;

/**
 * Static (no modifications allowed) set of Database Object IDs.
 * 
 * @author Erich Schubert
 */
public class ArrayStaticIntegerDBIDs implements IntegerArrayStaticDBIDs {
  /**
   * The actual storage.
   */
  protected int[] store;

  /**
   * Constructor.
   * 
   * @param ids Array of ids.
   */
  public ArrayStaticIntegerDBIDs(int... ids) {
    super();
    this.store = ids;
  }

  @Override
  public int size() {
    return store.length;
  }

  @Override
  public boolean isEmpty() {
    return store.length == 0;
  }

  @Override
  public boolean contains(DBIDRef o) {
    final int oid = DBIDUtil.asInteger(o);
    for (int i = 0; i < store.length; i++) {
      if (store[i] == oid) {
        return true;
      }
    }
    return false;
  }

  @Override
  public DBID get(int i) {
    return DBIDFactory.FACTORY.importInteger(store[i]);
  }

  @Override
  public void assignVar(int i, DBIDVar var) {
    if (var instanceof IntegerDBIDVar) {
      ((IntegerDBIDVar) var).internalSetIndex(store[i]);
    } else {
      // Much less efficient:
      var.set(get(i));
    }
  }

  @Override
  public int binarySearch(DBIDRef key) {
    return Arrays.binarySearch(store, DBIDUtil.asInteger(key));
  }

  @Override
  public Itr iter() {
    return new Itr();
  }

  @Override
  public Slice slice(int begin, int end) {
    return new Slice(begin, end);
  }

  /**
   * DBID iterator in ELKI/C style.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected class Itr implements IntegerDBIDArrayIter {
    /**
     * Position within array.
     */
    int pos = 0;

    @Override
    public boolean valid() {
      return pos < store.length && pos >= 0;
    }

    @Override
    public void advance() {
      pos++;
    }

    @Override
    public void advance(int count) {
      pos += 0;
    }

    @Override
    public void retract() {
      pos--;
    }

    @Override
    public void seek(int off) {
      pos = off;
    }

    @Override
    public int getOffset() {
      return pos;
    }

    @Override
    public int internalGetIndex() {
      return store[pos];
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof DBID) {
        LoggingUtil.warning("Programming error detected: DBIDItr.equals(DBID). Use sameDBID()!", new Throwable());
      }
      return super.equals(other);
    }

    @Override
    public String toString() {
      return Integer.toString(internalGetIndex()) + "@" + pos;
    }
  }

  /**
   * Slice of an array.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
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
      // TODO: recognize sorted arrays, then use binary search?
      int oid = o.internalGetIndex();
      for (int i = begin; i < end; i++) {
        if (store[i] == oid) {
          return true;
        }
      }
      return false;
    }

    @Override
    public boolean isEmpty() {
      return begin == end;
    }

    @Override
    public DBID get(int i) {
      return ArrayStaticIntegerDBIDs.this.get(begin + i);
    }

    @Override
    public void assignVar(int index, DBIDVar var) {
      ArrayStaticIntegerDBIDs.this.assignVar(begin + index, var);
    }

    @Override
    public int binarySearch(DBIDRef key) {
      return Arrays.binarySearch(store, begin, end, key.internalGetIndex()) - begin;
    }

    @Override
    public SliceItr iter() {
      return new SliceItr();
    }

    @Override
    public Slice slice(int begin, int end) {
      return new Slice(begin + begin, begin + end);
    }

    /**
     * Iterator class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    private class SliceItr implements IntegerDBIDArrayIter {
      /**
       * Iterator position.
       */
      int pos = begin;

      @Override
      public int internalGetIndex() {
        return store[pos];
      }

      @Override
      public boolean valid() {
        return pos < end && pos >= begin;
      }

      @Override
      public void advance() {
        ++pos;
      }

      @Override
      public int getOffset() {
        return pos;
      }

      @Override
      public void advance(int count) {
        pos += count;
      }

      @Override
      public void retract() {
        --pos;
      }

      @Override
      public void seek(int off) {
        pos = begin + off;
      }

      @Override
      public String toString() {
        return Integer.toString(internalGetIndex()) + "@" + pos;
      }
    }
  }
}
