package de.lmu.ifi.dbs.elki.database.ids.integer;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
  private int size = 0;

  /**
   * Initial size.
   */
  public static final int INITIAL_SIZE = 21;

  /**
   * Constructor.
   * 
   * @param size Initial size
   */
  protected ArrayModifiableIntegerDBIDs(int size) {
    super();
    this.store = new int[size];
  }

  /**
   * Constructor.
   */
  protected ArrayModifiableIntegerDBIDs() {
    super();
    this.store = new int[INITIAL_SIZE];
  }

  /**
   * Constructor.
   * 
   * @param existing Existing ids
   */
  protected ArrayModifiableIntegerDBIDs(DBIDs existing) {
    this(existing.size());
    this.addDBIDs(existing);
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
  public void assign(int index, DBIDVar var) {
    store[index] = var.internalGetIndex();
  }

  /**
   * Resize as desired.
   * 
   * @param minsize Desired size
   */
  private void ensureSize(int minsize) {
    int asize = store.length;
    while (asize < minsize) {
      asize = (asize >> 1) + asize;
    }
    if (asize > store.length) {
      store = Arrays.copyOf(store, asize);
    }
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
      ensureSize(size + 1);
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
  public IntegerDBIDArrayMIter iter() {
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
  }
}
