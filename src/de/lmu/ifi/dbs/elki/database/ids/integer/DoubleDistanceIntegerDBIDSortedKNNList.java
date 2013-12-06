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
    if (size >= k && dist > dists[kminus1]) {
      return;
    }
    // Ensure we have enough space.
    if (size == dists.length) {
      grow();
    }
    // Increases size!
    insertionSort(dists, ids, size, dist, id);
    ++size;
    // Truncate if necessary:
    if (size > k && dists[k] > dists[kminus1]) {
      size = k; // truncate
    }
  }

  /**
   * Insertion sort a single object.
   * 
   * @param dists Distances
   * @param ids IDs
   * @param size Current size
   * @param dist New distance
   * @param id New id
   */
  private static void insertionSort(final double[] dists, final int[] ids, final int size, final double dist, final int id) {
    int pos = size;
    while (pos > 0) {
      final int pre = pos - 1;
      final double predist = dists[pre];
      if (predist <= dist) {
        break;
      }
      dists[pos] = predist;
      ids[pos] = ids[pre];
      --pos;
    }
    dists[pos] = dist;
    ids[pos] = id;
    return;
  }

  @Override
  @Deprecated
  public void add(Double dist, DBIDRef id) {
    add(dist.doubleValue(), id);
  }

  @Override
  public DoubleDistanceIntegerDBIDPair poll() {
    final int last = size - 1;
    return new DoubleDistanceIntegerDBIDPair(dists[last], ids[last]);
  }

  @Override
  public DoubleDistanceIntegerDBIDPair peek() {
    final int last = size - 1;
    return new DoubleDistanceIntegerDBIDPair(dists[last], ids[last]);
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
