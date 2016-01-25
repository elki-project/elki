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

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;

/**
 * KNN Heap implemented using a list of DoubleInt pair objects.
 * 
 * Currently unused, needs benchmarking.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @apiviz.composedOf DoubleIntegerDBIDPair
 */
class DoubleIntegerDBIDPairKNNListHeap implements IntegerDBIDKNNList, KNNHeap {
  /**
   * The value of k this was materialized for.
   */
  private final int k;

  /**
   * The actual data array.
   */
  private DoubleIntegerDBIDPair[] data;

  /**
   * Current size
   */
  private int size;

  /**
   * Constructor.
   * 
   * @param k K parameter
   */
  protected DoubleIntegerDBIDPairKNNListHeap(int k) {
    super();
    this.data = new DoubleIntegerDBIDPair[k + 5];
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
    if(size < k || distance <= data[kminus1].doubleValue()) {
      // Ensure we have enough space.
      if(size > data.length) {
        grow();
      }
      insertionSort(new DoubleIntegerDBIDPair(distance, id.internalGetIndex()));
      // Truncate if necessary:
      if(size > k && data[k].doubleValue() > data[kminus1].doubleValue()) {
        truncate();
      }
    }
    return (size < k) ? Double.POSITIVE_INFINITY : get(kminus1).doubleValue();
  }

  private void truncate() {
    for(int i = k; i < size; i++) {
      data[i] = null; // discard
    }
    size = k;
  }

  @Override
  public void insert(DoubleDBIDPair e) {
    final int kminus1 = k - 1;
    final double dist = e.doubleValue();
    if(size < k || dist <= data[kminus1].doubleValue()) {
      // Ensure we have enough space.
      if(size > data.length) {
        grow();
      }
      if(e instanceof DoubleIntegerDBIDPair) {
        insertionSort((DoubleIntegerDBIDPair) e);
      }
      else {
        insertionSort(new DoubleIntegerDBIDPair(dist, e.internalGetIndex()));
      }
      // Truncate if necessary:
      if(size > k && data[k].doubleValue() > data[kminus1].doubleValue()) {
        truncate();
      }
    }
  }

  /**
   * Perform insertion sort.
   * 
   * @param obj Object to insert
   */
  private void insertionSort(DoubleIntegerDBIDPair obj) {
    // Insertion sort:
    int pos = size;
    while(pos > 0) {
      final int prev = pos - 1;
      DoubleIntegerDBIDPair pobj = data[prev];
      if(pobj.doubleValue() <= obj.doubleValue()) {
        break;
      }
      data[pos] = pobj;
      pos = prev;
    }
    data[pos] = obj;
    ++size;
  }

  private void grow() {
    final DoubleIntegerDBIDPair[] old = data;
    data = new DoubleIntegerDBIDPair[data.length + (data.length >> 1)];
    System.arraycopy(old, 0, data, 0, old.length);
  }

  @Override
  public DoubleIntegerDBIDPair poll() {
    assert (size > 0);
    return data[size--];
  }

  @Override
  public DoubleIntegerDBIDPair peek() {
    assert (size > 0);
    return data[size - 1];
  }

  @Override
  public KNNList toKNNList() {
    return this;
  }

  @Override
  public int getK() {
    return k;
  }

  @Override
  public double getKNNDistance() {
    return (size >= k) ? get(k - 1).doubleValue() : Double.POSITIVE_INFINITY;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("kNNList[");
    for(DoubleDBIDListIter iter = this.iter(); iter.valid();) {
      buf.append(iter.doubleValue()).append(':').append(DBIDUtil.toString(iter));
      iter.advance();
      if(iter.valid()) {
        buf.append(',');
      }
    }
    buf.append(']');
    return buf.toString();
  }

  @Override
  public DoubleIntegerDBIDPair get(int index) {
    return data[index];
  }

  @Override
  public Itr iter() {
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
  private class Itr implements DoubleIntegerDBIDListIter, IntegerDBIDArrayIter {
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
      return pos < size && pos >= 0;
    }

    @Override
    public Itr advance() {
      pos++;
      return this;
    }

    @Override
    public double doubleValue() {
      return get(pos).doubleValue();
    }

    @Override
    public DoubleIntegerDBIDPair getPair() {
      return get(pos);
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
  }
}
