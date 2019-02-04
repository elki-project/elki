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

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.DoubleIntegerArrayQuickSort;

/**
 * Class to store double distance, integer DBID results.
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @depend - - - DoubleIntegerArrayQuickSort
 */
class DoubleIntegerDBIDArrayList implements ModifiableDoubleDBIDList, DoubleIntegerDBIDList {
  /**
   * Initial size allocation.
   */
  private static final int INITIAL_SIZE = 21;

  /**
   * The size
   */
  int size;

  /**
   * Distance values
   */
  double[] dists;

  /**
   * DBIDs
   */
  int[] ids;

  /**
   * Empty.
   */
  private static final double[] EMPTY_DISTS = new double[0];

  /**
   * Empty.
   */
  private static final int[] EMPTY_IDS = new int[0];

  /**
   * Constructor.
   */
  protected DoubleIntegerDBIDArrayList() {
    dists = EMPTY_DISTS;
    ids = EMPTY_IDS;
  }

  /**
   * Constructor.
   *
   * @param size Initial size
   */
  protected DoubleIntegerDBIDArrayList(int size) {
    this.dists = size > 0 ? new double[size] : EMPTY_DISTS;
    this.ids = size > 0 ? new int[size] : EMPTY_IDS;
  }

  @Override
  public Itr iter() {
    return new Itr();
  }

  @Override
  public boolean contains(DBIDRef o) {
    final int q = o.internalGetIndex();
    for(int i = 0; i < size; i++) {
      if(q == ids[i]) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public DBIDVar assignVar(int index, DBIDVar var) {
    assert index < size : "Index: " + index + " Size: " + size;
    if(var instanceof IntegerDBIDVar) {
      ((IntegerDBIDVar) var).internalSetIndex(ids[index]);
    }
    else {
      var.set(new IntegerDBID(ids[index]));
    }
    return var;
  }

  @Override
  public double doubleValue(int index) {
    assert index < size : "Index: " + index + " Size: " + size;
    return dists[index];
  }

  /**
   * Add an entry, consisting of distance and internal index.
   *
   * @param dist Distance
   * @param id Internal index
   */
  protected void addInternal(double dist, int id) {
    if(size == dists.length) {
      grow();
    }
    dists[size] = dist;
    ids[size] = id;
    ++size;
  }

  /**
   * Grow the data storage.
   */
  protected void grow() {
    if(dists == EMPTY_DISTS) {
      dists = new double[INITIAL_SIZE];
      ids = new int[INITIAL_SIZE];
      return;
    }
    final int len = dists.length;
    final int newlength = len + (len >> 1) + 1;
    double[] odists = dists;
    dists = new double[newlength];
    System.arraycopy(odists, 0, dists, 0, odists.length);
    int[] oids = ids;
    ids = new int[newlength];
    System.arraycopy(oids, 0, ids, 0, oids.length);
  }

  @Override
  public void add(double dist, DBIDRef id) {
    addInternal(dist, id.internalGetIndex());
  }

  @Override
  public void add(DoubleDBIDPair pair) {
    addInternal(pair.doubleValue(), pair.internalGetIndex());
  }

  @Override
  public void clear() {
    size = 0;
  }

  @Override
  public void sort() {
    DoubleIntegerArrayQuickSort.sort(dists, ids, 0, size);
  }

  /**
   * Reverse the list.
   */
  protected void reverse() {
    for(int i = 0, j = size - 1; i < j; i++, j--) {
      double tmpd = dists[j];
      dists[j] = dists[i];
      dists[i] = tmpd;
      int tmpi = ids[j];
      ids[j] = ids[i];
      ids[i] = tmpi;
    }
  }

  @Override
  public void remove(int index) {
    assert index < size : "Index: " + index + " Size: " + size;
    if(index < --size) {
      System.arraycopy(dists, index + 1, dists, index, size - index);
      System.arraycopy(ids, index + 1, ids, index, size - index);
    }
    // TODO: put NaN, -1?
  }

  @Override
  public void removeSwap(int index) {
    assert index < size : "Index: " + index + " Size: " + size;
    if(--size > 0) {
      dists[index] = dists[size];
      ids[index] = ids[size];
    }
  }

  @Override
  public void swap(int i, int j) {
    assert i < size : "Index: " + i + " Size: " + size;
    assert j < size : "Index: " + j + " Size: " + size;
    final double tmpd = dists[i];
    dists[i] = dists[j];
    dists[j] = tmpd;
    final int tmpi = ids[i];
    ids[i] = ids[j];
    ids[j] = tmpi;
  }

  /**
   * Truncate the list to the given size, freeing the memory.
   *
   * @param newsize New size
   */
  public void truncate(int newsize) {
    if(newsize < size) {
      double[] odists = dists;
      dists = new double[newsize];
      System.arraycopy(odists, 0, dists, 0, newsize);
      int[] oids = ids;
      ids = new int[newsize];
      System.arraycopy(oids, 0, ids, 0, newsize);
      size = newsize;
    }
  }

  @Override
  public DoubleIntegerDBIDList slice(int begin, int end) {
    return begin == 0 && end == size ? this : new DoubleIntegerDBIDSubList(this, begin, end);
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(20 + size * 20).append("DoubleDBIDList[");
    DoubleDBIDListIter iter = this.iter();
    if(iter.valid()) {
      buf.append(iter.doubleValue()).append(':').append(iter.internalGetIndex());
      while(iter.advance().valid()) {
        buf.append(',').append(iter.doubleValue()).append(':').append(iter.internalGetIndex());
      }
    }
    return buf.append(']').toString();
  }

  /**
   * List iterator.
   *
   * @author Erich Schubert
   */
  private class Itr implements DoubleIntegerDBIDListMIter {
    /**
     * Current offset.
     */
    int pos = 0;

    /**
     * Constructor.
     */
    private Itr() {
      super();
    }

    @Override
    public boolean valid() {
      return pos < size && pos >= 0;
    }

    @Override
    public Itr advance() {
      ++pos;
      return this;
    }

    @Override
    public int getOffset() {
      return pos;
    }

    @Override
    public Itr advance(int count) {
      pos += count;
      return this;
    }

    @Override
    public Itr retract() {
      --pos;
      return this;
    }

    @Override
    public Itr seek(int off) {
      pos = off;
      return this;
    }

    @Override
    public int internalGetIndex() {
      return ids[pos];
    }

    @Override
    public double doubleValue() {
      return dists[pos];
    }

    @Override
    public void remove() {
      DoubleIntegerDBIDArrayList.this.remove(pos--);
    }

    @Override
    public void setDBID(DBIDRef ref) {
      if(pos >= size) {
        throw new ArrayIndexOutOfBoundsException();
      }
      ids[pos] = ref.internalGetIndex();
    }

    @Override
    public void setDouble(double value) {
      if(pos >= size) {
        throw new ArrayIndexOutOfBoundsException();
      }
      dists[pos] = value;
    }

    @Override
    public String toString() {
      return doubleValue() + ":" + internalGetIndex() + "@" + pos;
    }
  }
}
