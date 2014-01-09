package de.lmu.ifi.dbs.elki.database.ids.generic;

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
public class DoubleDistanceDBIDPairKNNListHeap implements DoubleDistanceKNNList, DoubleDistanceKNNHeap {
  /**
   * The value of k this was materialized for.
   */
  private final int k;

  /**
   * The actual data array.
   */
  private DoubleDistanceDBIDPair[] data;

  /**
   * Current size
   */
  private int size;

  /**
   * Constructor.
   * 
   * @param k K parameter
   */
  public DoubleDistanceDBIDPairKNNListHeap(int k) {
    super();
    this.data = new DoubleDistanceDBIDPair[k + 11];
    this.k = k;
  }

  @Override
  public void clear() {
    size = 0;
    Arrays.fill(data, null);
  }

  @Override
  public double insert(double distance, DBIDRef id) {
    if (size < k || distance <= data[k - 1].doubleDistance()) {
      insert(DBIDUtil.newDistancePair(distance, id));
    }
    return (size < k) ? Double.POSITIVE_INFINITY :  get(k - 1).doubleDistance();
  }

  @Override
  @Deprecated
  public void insert(Double distance, DBIDRef id) {
    insert(DBIDUtil.newDistancePair(distance.doubleValue(), id));
  }

  @Override
  @Deprecated
  public void insert(DoubleDistance dist, DBIDRef id) {
    insert(DBIDUtil.newDistancePair(dist.doubleValue(), id));
  }

  @Override
  public void insert(DoubleDistanceDBIDPair e) {
    if (size >= k) {
      if (e.doubleDistance() > data[size - 1].doubleDistance()) {
        return;
      }
      // Ensure we have enough space.
      final int len = data.length;
      if (size > len) {
        final int newlength = len + (len >>> 1);
        assert (newlength > size);
        data = Arrays.copyOf(data, newlength);
      }
    }
    insertionSort(e);
    // Truncate if necessary:
    if (size > k && data[k].doubleDistance() > data[k - 1].doubleDistance()) {
      size = k;
    }
  }

  /**
   * Perform insertion sort.
   * 
   * @param obj Object to insert
   */
  private void insertionSort(DoubleDistanceDBIDPair obj) {
    // Insertion sort:
    int pos = size;
    while (pos > 0) {
      DoubleDistanceDBIDPair pobj = data[pos - 1];
      if (pobj.doubleDistance() <= obj.doubleDistance()) {
        break;
      }
      data[pos] = pobj;
      --pos;
    }
    data[pos] = obj;
    ++size;
  }

  @Override
  public DoubleDistanceDBIDPair poll() {
    assert (size > 0);
    return data[size--];
  }

  @Override
  public DoubleDistanceDBIDPair peek() {
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
    if (size < k) {
      return DoubleDistance.INFINITE_DISTANCE;
    }
    return get(k - 1).getDistance();
  }

  @Override
  public double doubleKNNDistance() {
    return (size < k) ? Double.POSITIVE_INFINITY :  get(k - 1).doubleDistance();
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("kNNList[");
    for (DoubleDistanceDBIDListIter iter = this.iter(); iter.valid();) {
      buf.append(iter.doubleDistance()).append(':').append(DBIDUtil.toString(iter));
      iter.advance();
      if (iter.valid()) {
        buf.append(',');
      }
    }
    buf.append(']');
    return buf.toString();
  }

  @Override
  public DoubleDistanceDBIDPair get(int index) {
    return data[index];
  }

  @Override
  public DoubleDistanceDBIDListIter iter() {
    return new Itr();
  }

  @Override
  public int size() {
    return data.length;
  }

  @Override
  public boolean contains(DBIDRef o) {
    for (DBIDIter iter = iter(); iter.valid(); iter.advance()) {
      if (DBIDUtil.equal(iter, o)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  /**
   * Iterator.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private class Itr implements DoubleDistanceDBIDListIter {
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
      return pos < data.length;
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
    public DoubleDistanceDBIDPair getDistancePair() {
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
