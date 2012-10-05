package de.lmu.ifi.dbs.elki.distance.distanceresultlist;

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

import java.util.Collection;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDistanceDBIDPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.Heap;

/**
 * Finalized KNN List.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf DoubleDistanceDBIDPair
 * @apiviz.has DoubleDistanceDBIDResultIter
 */
public class DoubleDistanceKNNList implements KNNResult<DoubleDistance> {
  /**
   * The value of k this was materialized for.
   */
  private final int k;

  /**
   * The actual data array.
   */
  private final DoubleDistanceDBIDPair[] data;

  /**
   * Constructor. This will <em>clone</em> the given collection!
   * 
   * @param col Existing collection
   * @param k K parameter
   */
  public DoubleDistanceKNNList(Collection<DoubleDistanceDBIDPair> col, int k) {
    super();
    this.data = new DoubleDistanceDBIDPair[col.size()];
    this.k = k;
    assert (col.size() >= this.k) : "Collection doesn't contain enough objects!";
    // Get sorted data from heap; but in reverse.
    Iterator<DoubleDistanceDBIDPair> it = col.iterator();
    for(int i = 0; it.hasNext(); i++) {
      data[i] = it.next();
    }
    assert (data.length == 0 || data[0] != null);
  }

  /**
   * Constructor, to be called from KNNHeap only. Use {@link KNNHeap#toKNNList}
   * instead!
   * 
   * @param heap Calling heap
   */
  protected DoubleDistanceKNNList(DoubleDistanceKNNHeap heap) {
    super();
    this.data = new DoubleDistanceDBIDPair[heap.size()];
    this.k = heap.getK();
    assert (heap.size() >= this.k) : "Heap doesn't contain enough objects!";
    // Get sorted data from heap; but in reverse.
    int i = heap.size();
    while(heap.size() > 0) {
      i--;
      assert (i >= 0);
      data[i] = heap.poll();
    }
    assert (data.length == 0 || data[0] != null);
    assert (heap.size() == 0);
  }

  /**
   * Constructor, to be called from KNNHeap only. Use {@link KNNHeap#toKNNList}
   * instead!
   * 
   * @param heap Calling heap
   * @param k Target number of neighbors (before ties)
   */
  public DoubleDistanceKNNList(Heap<DoubleDistanceDBIDPair> heap, int k) {
    super();
    this.data = new DoubleDistanceDBIDPair[heap.size()];
    this.k = k;
    assert (heap.size() >= this.k) : "Heap doesn't contain enough objects!";
    // Get sorted data from heap; but in reverse.
    int i = heap.size();
    while(heap.size() > 0) {
      i--;
      assert (i >= 0);
      data[i] = heap.poll();
    }
    assert (data.length == 0 || data[0] != null);
    assert (heap.size() == 0);
  }

  @Override
  public int getK() {
    return k;
  }

  /**
   * {@inheritDoc}
   * 
   * @deprecated use doubleKNNDistance()!
   */
  @Override
  @Deprecated
  public DoubleDistance getKNNDistance() {
    return get(getK() - 1).getDistance();
  }

  /**
   * Get the kNN distance as double value.
   * 
   * @return Distance
   */
  public double doubleKNNDistance() {
    return get(getK() - 1).doubleDistance();
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("kNNList[");
    for(DoubleDistanceDBIDResultIter iter = this.iter(); iter.valid();) {
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
  public DoubleDistanceDBIDPair get(int index) {
    return data[index];
  }

  @Override
  public DoubleDistanceDBIDResultIter iter() {
    return new Itr();
  }

  @Override
  public int size() {
    return data.length;
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
    return size() == 0;
  }

  /**
   * Iterator.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private class Itr implements DoubleDistanceDBIDResultIter {
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
  }
}