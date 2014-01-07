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

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceKNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceKNNList;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Finalized KNN List.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf DoubleDistanceDBIDPair
 * @apiviz.has DoubleDistanceDBIDListIter
 */
public class DoubleDistanceIntegerDBIDPairKNNListHeap implements DoubleDistanceKNNList, DoubleDistanceKNNHeap {
  /**
   * The value of k this was materialized for.
   */
  private final int k;

  /**
   * The actual data array.
   */
  private DoubleDistanceIntegerDBIDPair[] data;

  /**
   * Current size
   */
  private int size;

  /**
   * Constructor.
   * 
   * @param k K parameter
   */
  public DoubleDistanceIntegerDBIDPairKNNListHeap(int k) {
    super();
    this.data = new DoubleDistanceIntegerDBIDPair[k + 5];
    this.k = k;
    this.size = 0;
  }

  @Override
  public void clear() {
    for(int i = 0; i < size; i++) {
      data[i] = null; // discard
    }
    size = 0;
  }

  @Override
  public double insert(double distance, DBIDRef id) {
    final int kminus1 = k - 1;
    if(size < k || distance <= data[kminus1].doubleDistance()) {
      // Ensure we have enough space.
      if(size > data.length) {
        grow();
      }
      insertionSort(new DoubleDistanceIntegerDBIDPair(distance, id.internalGetIndex()));
      // Truncate if necessary:
      if(size > k && data[k].doubleDistance() > data[kminus1].doubleDistance()) {
        truncate();
      }
    }
    return (size < k) ? Double.POSITIVE_INFINITY : get(kminus1).doubleDistance();
  }

  private void truncate() {
    for(int i = k; i < size; i++) {
      data[i] = null; // discard
    }
    size = k;
  }

  @Override
  @Deprecated
  public void insert(Double distance, DBIDRef id) {
    final int kminus1 = k - 1;
    if(size < k || distance.doubleValue() <= data[kminus1].doubleDistance()) {
      // Ensure we have enough space.
      if(size > data.length) {
        grow();
      }
      insertionSort(new DoubleDistanceIntegerDBIDPair(distance.doubleValue(), id.internalGetIndex()));
      // Truncate if necessary:
      if(size > k && data[k].doubleDistance() > data[kminus1].doubleDistance()) {
        truncate();
      }
    }
  }

  @Override
  @Deprecated
  public void insert(DoubleDistance dist, DBIDRef id) {
    final int kminus1 = k - 1;
    if(size < k || dist.doubleValue() <= data[kminus1].doubleDistance()) {
      // Ensure we have enough space.
      if(size > data.length) {
        grow();
      }
      insertionSort(new DoubleDistanceIntegerDBIDPair(dist.doubleValue(), id.internalGetIndex()));
      // Truncate if necessary:
      if(size > k && data[k].doubleDistance() > data[kminus1].doubleDistance()) {
        truncate();
      }
    }
  }

  @Override
  public void insert(DoubleDistanceDBIDPair e) {
    final int kminus1 = k - 1;
    final double dist = e.doubleDistance();
    if(size < k || dist <= data[kminus1].doubleDistance()) {
      // Ensure we have enough space.
      if(size > data.length) {
        grow();
      }
      if(e instanceof DoubleDistanceIntegerDBIDPair) {
        insertionSort((DoubleDistanceIntegerDBIDPair) e);
      }
      else {
        insertionSort(new DoubleDistanceIntegerDBIDPair(dist, e.internalGetIndex()));
      }
      // Truncate if necessary:
      if(size > k && data[k].doubleDistance() > data[kminus1].doubleDistance()) {
        truncate();
      }
    }
  }

  /**
   * Perform insertion sort.
   * 
   * @param obj Object to insert
   */
  private void insertionSort(DoubleDistanceIntegerDBIDPair obj) {
    // Insertion sort:
    int pos = size;
    while(pos > 0) {
      final int prev = pos - 1;
      DoubleDistanceIntegerDBIDPair pobj = data[prev];
      if(pobj.doubleDistance() <= obj.doubleDistance()) {
        break;
      }
      data[pos] = pobj;
      pos = prev;
    }
    data[pos] = obj;
    ++size;
  }

  private void grow() {
    final DoubleDistanceIntegerDBIDPair[] old = data;
    data = new DoubleDistanceIntegerDBIDPair[data.length + (data.length >> 1)];
    System.arraycopy(old, 0, data, 0, old.length);
  }

  @Override
  public DoubleDistanceIntegerDBIDPair poll() {
    assert (size > 0);
    return data[size--];
  }

  @Override
  public DoubleDistanceIntegerDBIDPair peek() {
    assert (size > 0);
    return data[size - 1];
  }

  @Override
  public DoubleDistanceKNNList toKNNList() {
    return this;
  }

  @Override
  public int getK() {
    return k;
  }

  @Override
  @Deprecated
  public DoubleDistance getKNNDistance() {
    if(size < k) {
      return DoubleDistance.INFINITE_DISTANCE;
    }
    return get(k - 1).getDistance();
  }

  @Override
  public double doubleKNNDistance() {
    if(size < k) {
      return Double.POSITIVE_INFINITY;
    }
    return get(k - 1).doubleDistance();
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("kNNList[");
    for(DoubleDistanceDBIDListIter iter = this.iter(); iter.valid();) {
      buf.append(iter.doubleDistance()).append(':').append(DBIDUtil.toString(iter));
      iter.advance();
      if(iter.valid()) {
        buf.append(',');
      }
    }
    buf.append(']');
    return buf.toString();
  }

  @Override
  public DoubleDistanceIntegerDBIDPair get(int index) {
    return data[index];
  }

  @Override
  public DoubleDistanceIntegerDBIDListIter iter() {
    return new Itr();
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean contains(DBIDRef o) {
    for(DBIDIter iter = iter(); iter.valid(); iter.advance()) {
      if(DBIDUtil.equal(iter, o)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  /**
   * Iterator.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private class Itr implements DoubleDistanceIntegerDBIDListIter {
    /**
     * Cursor position.
     */
    private int pos = 0;

    @Override
    public int internalGetIndex() {
      return get(pos).internalGetIndex();
    }

    @Override
    public boolean valid() {
      return pos < size;
    }

    @Override
    public void advance() {
      pos++;
    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated use {@link #doubleDistance}!
     */
    @Override
    @Deprecated
    public DoubleDistance getDistance() {
      return get(pos).getDistance();
    }

    @Override
    public double doubleDistance() {
      return get(pos).doubleDistance();
    }

    @Override
    public DoubleDistanceIntegerDBIDPair getDistancePair() {
      return get(pos);
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
  }
}
