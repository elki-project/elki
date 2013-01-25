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

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceKNNList;
import de.lmu.ifi.dbs.elki.database.ids.distance.ModifiableDoubleDistanceDBIDList;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Class to store double distance, integer DBID results.
 * 
 * @author Erich Schubert
 */
public class DoubleDistanceIntegerDBIDKNNList implements ModifiableDoubleDistanceDBIDList, DoubleDistanceKNNList {
  /**
   * Initial size allocation
   */
  private static final int INITIAL_SIZE = 21;

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
  public DoubleDistanceIntegerDBIDKNNList() {
    super();
    this.k = -1;
    this.dists = new double[INITIAL_SIZE];
    this.ids = new int[INITIAL_SIZE];
  }

  /**
   * Constructor.
   * 
   * @param k K parameter
   * @param size Actual size
   */
  public DoubleDistanceIntegerDBIDKNNList(int k, int size) {
    super();
    this.k = k;
    if (size > 0) {
      this.dists = new double[size];
      this.ids = new int[size];
    } else {
      this.dists = new double[INITIAL_SIZE];
      this.ids = new int[INITIAL_SIZE];
    }
  }

  /**
   * Constructor from heap.
   * 
   * @param heap KNN heap.
   */
  public DoubleDistanceIntegerDBIDKNNList(DoubleDistanceIntegerDBIDKNNHeap heap) {
    super();
    this.k = heap.getK();
    this.size = heap.size();
    this.dists = new double[size];
    this.ids = new int[size];
    for (int i = size - 1; i >= 0; i--) {
      dists[i] = heap.peekDistance();
      ids[i] = heap.peekInternalDBID();
      heap.pop();
    }
  }

  @Override
  public DoubleDistanceDBIDListIter iter() {
    return new Itr();
  }

  @Override
  public boolean contains(DBIDRef o) {
    final int q = o.internalGetIndex();
    for (int i = 0; i < size; i++) {
      if (q == ids[i]) {
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
    if (k <= 0) {
      return size - 1;
    }
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
    if (k <= 0) {
      return dists[size - 1];
    }
    if (size < k) {
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
  protected void add(double dist, int id) {
    if (size == dists.length) {
      final int newlength = (dists.length << 2) + dists.length;
      dists = Arrays.copyOf(dists, newlength);
      ids = Arrays.copyOf(ids, newlength);
    }
    dists[size] = dist;
    ids[size] = id;
    ++size;
  }

  @Override
  @Deprecated
  public void add(DoubleDistance dist, DBIDRef id) {
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

  @Override
  public void sort() {
    DoubleIntegerArrayQuickSort.sort(dists, ids, 0, size);
  }

  /**
   * Reverse the list.
   */
  protected void reverse() {
    for (int i = 0, j = size - 1; i < j; i++, j--) {
      double tmpd = dists[j];
      dists[j] = dists[i];
      dists[i] = tmpd;
      int tmpi = ids[j];
      ids[j] = ids[i];
      ids[i] = tmpi;
    }
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("kNNList[");
    for (DoubleDistanceDBIDListIter iter = this.iter(); iter.valid();) {
      buf.append(iter.doubleDistance()).append(':').append(iter.internalGetIndex());
      iter.advance();
      if (iter.valid()) {
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
  private class Itr implements DoubleDistanceDBIDListIter {
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
