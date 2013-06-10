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

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceKNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceKNNList;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Class to store double distance, integer DBID results.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses DoubleIntegerArrayQuickSort
 */
public class DoubleDistanceIntegerDBIDKNNListHeap implements DoubleDistanceKNNHeap, DoubleDistanceKNNList, IntegerDBIDs {
  /**
   * The k value this list was generated for.
   */
  int k;

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
   * Constructor.
   * 
   * @param k K parameter
   */
  public DoubleDistanceIntegerDBIDKNNListHeap(int k) {
    super();
    this.k = k;
    this.size = 0;
    this.dists = new double[k + 1];
    this.ids = new int[k + 1];
  }

  @Override
  public DoubleDistanceIntegerDBIDListIter iter() {
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
  public int getK() {
    return k;
  }

  @Override
  public DoubleDistanceIntegerDBIDPair get(int index) {
    return new DoubleDistanceIntegerDBIDPair(dists[index], ids[index]);
  }

  @Override
  @Deprecated
  public DoubleDistance getKNNDistance() {
    return new DoubleDistance(doubleKNNDistance());
  }

  @Override
  public double doubleKNNDistance() {
    if(size < k) {
      return Double.POSITIVE_INFINITY;
    }
    return dists[k - 1];
  }

  /**
   * Add an entry, consisting of distance and internal index.
   * 
   * @param dist Distance
   * @param id Internal index
   */
  protected void append(double dist, int id) {
    ensureSize(size + 1);
    dists[size] = dist;
    ids[size] = id;
    ++size;
  }

  /**
   * Add a new element to the heap/list.
   * 
   * @param dist Distance
   * @param id Object ID
   */
  protected void add(double dist, int id) {
    // Ensure we have enough space.
    ensureSize(size + 1);
    if(size < k) {
      dists[size] = dist;
      ids[size] = id;
      ++size;
      if(size == k) {
        sort();
      }
      return;
    }
    if (dist > dists[size - 1]) {
      return;
    }
    // Insertion sort:
    int pos = size;
    while(pos > 0 && dists[pos - 1] > dist) {
      dists[pos] = dists[pos - 1];
      ids[pos] = ids[pos - 1];
      --pos;
    }
    dists[pos] = dist;
    ids[pos] = id;
    ++size;
    // Truncate.
    if(size > k && dists[k] > dists[k - 1]) {
      size = k;
    }
  }

  /**
   * Ensure we have enough space.
   * 
   * @param size Desired size
   */
  private void ensureSize(int size) {
    if(size > dists.length) {
      final int newlength = Math.max(size, (dists.length << 1) + 1);
      dists = Arrays.copyOf(dists, newlength);
      ids = Arrays.copyOf(ids, newlength);
    }
  }

  @Override
  @Deprecated
  public void add(DoubleDistance dist, DBIDRef id) {
    add(dist.doubleValue(), id);
  }

  @Override
  @Deprecated
  public void add(Double dist, DBIDRef id) {
    add(dist.doubleValue(), id);
  }

  @Override
  public void add(double dist, DBIDRef id) {
    add(dist, id.internalGetIndex());
  }

  @Override
  public void add(DoubleDistanceDBIDPair pair) {
    add(pair.doubleDistance(), pair.internalGetIndex());
  }

  /**
   * Sort the current contents of the list.
   */
  protected void sort() {
    DoubleIntegerArrayQuickSort.sort(dists, ids, 0, size);
  }

  @Override
  public void clear() {
    size = 0;
    Arrays.fill(dists, Double.NaN);
    Arrays.fill(ids, -1);
  }

  @Override
  public DoubleDistanceIntegerDBIDPair poll() {
    return new DoubleDistanceIntegerDBIDPair(dists[k], ids[k]);
  }

  @Override
  public DoubleDistanceIntegerDBIDPair peek() {
    return new DoubleDistanceIntegerDBIDPair(dists[k], ids[k]);
  }

  @Override
  public DoubleDistanceKNNList toKNNList() {
    return this;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("kNNListHeap[");
    for(DoubleDistanceDBIDListIter iter = this.iter(); iter.valid();) {
      buf.append(iter.doubleDistance()).append(':').append(iter.internalGetIndex());
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
  private class Itr implements DoubleDistanceIntegerDBIDListIter {
    int offset = 0;

    @Override
    public boolean valid() {
      return offset < size;
    }

    @Override
    public void advance() {
      ++offset;
    }

    @Override
    public int getOffset() {
      return offset;
    }

    @Override
    public void advance(int count) {
      offset += count;
    }

    @Override
    public void retract() {
      offset--;
    }

    @Override
    public void seek(int off) {
      offset = off;
    }

    @Override
    public int internalGetIndex() {
      return ids[offset];
    }

    @Override
    public double doubleDistance() {
      return dists[offset];
    }

    @Override
    public DoubleDistanceDBIDPair getDistancePair() {
      return new DoubleDistanceIntegerDBIDPair(dists[offset], ids[offset]);
    }

    @Override
    @Deprecated
    public DoubleDistance getDistance() {
      return new DoubleDistance(dists[offset]);
    }
  }
}
