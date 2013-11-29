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
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceKNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceKNNList;

/**
 * Track the k nearest neighbors, with insertion sort to ensure the correct
 * order.
 * 
 * @author Erich Schubert
 */
public class DoubleDistanceIntegerDBIDSortedKNNList extends DoubleDistanceIntegerDBIDKNNList implements DoubleDistanceKNNHeap {
  /**
   * Constructor.
   * 
   * @param k K parameter
   */
  public DoubleDistanceIntegerDBIDSortedKNNList(int k) {
    super(k, k + 11);
  }

  /**
   * Add a new element to the heap/list.
   * 
   * @param dist Distance
   * @param id Object ID
   */
  @Override
  protected void addInternal(final double dist, final int id) {
    if (size >= k && dist > dists[size - 1]) {
      return;
    }
    // Ensure we have enough space.
    ensureSize(size + 1);
    // Local references may have a performance advantage.
    double[] dists = this.dists;
    int[] ids = this.ids;
    // Insertion sort:
    int pos = size;
    for (; pos > 0 && dists[pos - 1] > dist; --pos) {
      dists[pos] = dists[pos - 1];
      ids[pos] = ids[pos - 1];
    }
    dists[pos] = dist;
    ids[pos] = id;
    ++size;
    // Truncate if necessary:
    if (size > k && dists[k] > dists[k - 1]) {
      size = k;
    }
  }

  /**
   * Ensure we have enough space.
   * 
   * @param size Desired size
   */
  private void ensureSize(int size) {
    final int len = dists.length;
    if (size > len) {
      final int newlength = len + (len >> 1);
      dists = Arrays.copyOf(dists, newlength);
      ids = Arrays.copyOf(ids, newlength);
    }
  }

  @Override
  @Deprecated
  public void add(Double dist, DBIDRef id) {
    add(dist.doubleValue(), id);
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
}
