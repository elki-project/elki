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
import de.lmu.ifi.dbs.elki.database.ids.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;

/**
 * Track the k nearest neighbors, with insertion sort to ensure the correct
 * order.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
class DoubleIntegerDBIDListKNNHeap extends DoubleIntegerDBIDKNNList implements KNNHeap {
  /**
   * Constructor.
   * 
   * @param k K parameter
   */
  protected DoubleIntegerDBIDListKNNHeap(int k) {
    super(k, k + 11);
  }

  /**
   * Add a new element to the heap/list.
   * 
   * @param dist Distance
   * @param id Object ID
   */
  @Override
  protected final void addInternal(final double dist, final int id) {
    if(size >= k && dist > dists[k - 1]) {
      return;
    }
    insertionSort(dist, id);
  }

  /**
   * Insertion sort a single object.
   * 
   * @param dist New distance
   * @param id New id
   */
  private void insertionSort(final double dist, final int id) {
    // Ensure we have enough space.
    if(size == dists.length) {
      grow();
    }
    int pos = size;
    while(pos > 0) {
      final int pre = pos - 1;
      final double predist = dists[pre];
      if(predist <= dist) {
        break;
      }
      dists[pos] = predist;
      ids[pos] = ids[pre];
      pos = pre;
    }
    dists[pos] = dist;
    ids[pos] = id;
    ++size;
    if(size > k && dists[k] > dists[k - 1]) {
      size = k; // truncate
    }
    return;
  }

  @Override
  public double insert(double dist, DBIDRef id) {
    final int kminus1 = k - 1;
    final int iid = id.internalGetIndex();
    if(size >= k && dist > dists[kminus1]) {
      return (size >= k) ? dists[kminus1] : Double.POSITIVE_INFINITY;
    }
    insertionSort(dist, iid);
    return (size >= k) ? dists[kminus1] : Double.POSITIVE_INFINITY;
  }

  @Override
  public void add(double dist, DBIDRef id) {
    addInternal(dist, id.internalGetIndex());
  }

  @Override
  public void insert(DoubleDBIDPair e) {
    addInternal(e.doubleValue(), e.internalGetIndex());
  }

  @Override
  public DoubleIntegerDBIDPair poll() {
    final int last = size - 1;
    return new DoubleIntegerDBIDPair(dists[last], ids[last]);
  }

  @Override
  public DoubleIntegerDBIDPair peek() {
    final int last = size - 1;
    return new DoubleIntegerDBIDPair(dists[last], ids[last]);
  }

  @Override
  public KNNList toKNNList() {
    return this;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("kNNListHeap[");
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
}
