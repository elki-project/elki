package de.lmu.ifi.dbs.elki.database.ids.integer;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
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
 * @apiviz.uses DoubleIntegerArrayQuickSort
 */
class DoubleIntegerDBIDList implements ModifiableDoubleDBIDList, IntegerDBIDs {
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
  protected DoubleIntegerDBIDList() {
    dists = EMPTY_DISTS;
    ids = EMPTY_IDS;
  }

  /**
   * Constructor.
   *
   * @param size Initial size
   */
  protected DoubleIntegerDBIDList(int size) {
    this.dists = new double[size];
    this.ids = new int[size];
    // This is default: this.size = 0;
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
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public DoubleIntegerDBIDPair get(int index) {
    return new DoubleIntegerDBIDPair(dists[index], ids[index]);
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
    final int newlength = len + (len >> 1);
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
  public void remove(int p) {
    if(p >= size) {
      throw new ArrayIndexOutOfBoundsException(p);
    }
    if(p < --size) {
      System.arraycopy(dists, p + 1, dists, p, size - p);
      System.arraycopy(ids, p + 1, ids, p, size - p);
    }
    // TODO: put NaN, -1?
  }

  @Override
  public void removeSwap(int p) {
    if(p >= size) {
      throw new ArrayIndexOutOfBoundsException(p);
    }
    if(--size > 0) {
      dists[p] = dists[size];
      ids[p] = ids[size];
    }
  }

  @Override
  public void swap(int i, int j) {
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
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("DoubleDBIDList[");
    for(DoubleDBIDListIter iter = this.iter(); iter.valid();) {
      buf.append(iter.doubleValue()).append(':').append(iter.internalGetIndex());
      iter.advance();
      if(iter.valid()) {
        buf.append(',');
      }
    }
    buf.append(']');
    return buf.toString();
  }

  /**
   * List iterator.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
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
      DoubleIntegerDBIDList.this.remove(pos);
    }

    @Override
    public void setDBID(DBIDRef ref) {
      ids[pos] = ref.internalGetIndex();
    }

    @Override
    public void setDouble(double value) {
      dists[pos] = value;
    }

    @Override
    public DoubleDBIDPair getPair() {
      return new DoubleIntegerDBIDPair(dists[pos], ids[pos]);
    }

    @Override
    public String toString() {
      return doubleValue() + ":" + internalGetIndex() + "@" + pos;
    }
  }
}
