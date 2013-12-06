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
import java.util.Comparator;

import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Class using a primitive int[] array as storage.
 * 
 * @author Erich Schubert
 */
public class ArrayModifiableIntegerDBIDs implements ArrayModifiableDBIDs, IntegerArrayDBIDs {
  /**
   * The actual Trove array list.
   */
  private int[] store;

  /**
   * Occupied size.
   */
  private int size;

  /**
   * Initial size.
   */
  public static final int INITIAL_SIZE = 21;

  /**
   * Constructor.
   * 
   * @param isize Initial size
   */
  protected ArrayModifiableIntegerDBIDs(int isize) {
    super();
    this.store = new int[isize < 3 ? 3 : isize];
    // default this.size = 0;
  }

  /**
   * Constructor.
   */
  protected ArrayModifiableIntegerDBIDs() {
    super();
    this.store = new int[INITIAL_SIZE];
    // default: this.size = 0;
  }

  /**
   * Constructor.
   * 
   * @param existing Existing ids
   */
  protected ArrayModifiableIntegerDBIDs(DBIDs existing) {
    this(existing.size());
    if (existing instanceof IntegerDBIDRange) {
      IntegerDBIDRange range = (IntegerDBIDRange) existing;
      for (int i = 0; i < range.len; i++) {
        store[i] = range.start + i;
      }
      size = range.len;
    } else {
      this.addDBIDs(existing);
    }
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public DBID get(int i) {
    return new IntegerDBID(store[i]);
  }

  @Override
  public void assignVar(int index, DBIDVar var) {
    if (var instanceof IntegerDBIDVar) {
      ((IntegerDBIDVar) var).internalSetIndex(store[index]);
    } else {
      // less efficient, involves object creation.
      var.set(get(index));
    }
  }

  /**
   * Resize as desired.
   * 
   * @param minsize Desired size
   */
  private void ensureSize(int minsize) {
    if (minsize <= store.length) {
      return;
    }
    int asize = store.length;
    while (asize < minsize) {
      asize = (asize >>> 1) + asize;
    }
    final int[] prev = store;
    store = new int[asize];
    System.arraycopy(prev, 0, store, 0, size);
  }

  /**
   * Grow array by 50%.
   */
  private void grow() {
    final int newsize = store.length + (store.length >>> 1);
    final int[] prev = store;
    store = new int[newsize];
    System.arraycopy(prev, 0, store, 0, size);
  }

  @Override
  public boolean addDBIDs(DBIDs ids) {
    ensureSize(size + ids.size());
    for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      store[size] = iter.internalGetIndex();
      ++size;
    }
    return true;
  }

  @Override
  public boolean removeDBIDs(DBIDs ids) {
    boolean success = false;
    for (DBIDIter id = ids.iter(); id.valid(); id.advance()) {
      int rm = id.internalGetIndex();
      // TODO: when sorted, use binary search!
      for (int i = 0; i < size; i++) {
        if (store[i] == rm) {
          --size;
          store[i] = store[size];
          success = true;
          break;
        }
      }
    }
    return success;
  }

  @Override
  public boolean add(DBIDRef e) {
    if (size == store.length) {
      grow();
    }
    store[size] = e.internalGetIndex();
    ++size;
    return true;
  }

  @Override
  public boolean remove(DBIDRef o) {
    int rm = o.internalGetIndex();
    // TODO: when sorted, use binary search!
    for (int i = 0; i < size; i++) {
      if (store[i] == rm) {
        --size;
        store[i] = store[size];
        return true;
      }
    }
    return false;
  }

  @Override
  public DBID set(int index, DBIDRef element) {
    int prev = store[index];
    store[index] = element.internalGetIndex();
    return new IntegerDBID(prev);
  }

  @Override
  public DBID remove(int index) {
    DBID ret = new IntegerDBID(store[index]);
    --size;
    if (size > 0) {
      store[index] = store[size];
    }
    return ret;
  }

  @Override
  public void clear() {
    size = 0;
  }

  @Override
  public int binarySearch(DBIDRef key) {
    return Arrays.binarySearch(store, 0, size, key.internalGetIndex());
  }

  @Override
  public boolean contains(DBIDRef o) {
    // TODO: recognize sorted arrays, then use binary search?
    int oid = o.internalGetIndex();
    for (int i = 0; i < size; i++) {
      if (store[i] == oid) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void sort() {
    Arrays.sort(store, 0, size);
  }

  @Override
  public void sort(Comparator<? super DBIDRef> comparator) {
    IntegerDBIDArrayQuickSort.sort(store, 0, size, comparator);
  }

  @Override
  public void sort(int start, int end, Comparator<? super DBIDRef> comparator) {
    IntegerDBIDArrayQuickSort.sort(store, start, end, comparator);
  }

  @Override
  public void swap(int a, int b) {
    int tmp = store[b];
    store[b] = store[a];
    store[a] = tmp;
  }

  @Override
  public Slice slice(int begin, int end) {
    return new Slice(begin, end);
  }

  @Override
  public Itr iter() {
    return new Itr();
  }

  /**
   * Iterator class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private class Itr implements IntegerDBIDArrayMIter {
    /**
     * Iterator position.
     */
    int pos = 0;

    @Override
    public int internalGetIndex() {
      return store[pos];
    }

    @Override
    public boolean valid() {
      return pos < size && pos >= 0;
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
      pos = off;
    }

    @Override
    public void remove() {
      ArrayModifiableIntegerDBIDs.this.remove(pos);
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
      return ArrayModifiableIntegerDBIDs.this.get(begin + i);
    }

    @Override
    public void assignVar(int index, DBIDVar var) {
      ArrayModifiableIntegerDBIDs.this.assignVar(begin + index, var);
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
        pos = off;
      }

      @Override
      public String toString() {
        return Integer.toString(internalGetIndex()) + "@" + pos;
      }
    }
  }
}
