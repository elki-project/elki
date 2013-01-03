package de.lmu.ifi.dbs.elki.utilities.datastructures.heap;

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

import java.util.Comparator;

/**
 * Basic in-memory heap structure.
 * 
 * This heap is built lazily: if you first add many elements, then poll the
 * heap, it will be bulk-loaded in O(n) instead of iteratively built in O(n log
 * n). This is implemented via a simple validTo counter.
 * 
 * @author Erich Schubert
 * 
 * @param <V> value type
 */
public class DoubleObjectMinHeap<V> extends DoubleObjectHeap<V> {
  /**
   * Default constructor: default capacity, natural ordering.
   */
  public DoubleObjectMinHeap() {
    this(DEFAULT_INITIAL_CAPACITY);
  }

  /**
   * Constructor with initial capacity and {@link Comparator}.
   * 
   * @param size initial capacity
   */
  public DoubleObjectMinHeap(int size) {
    super();
    this.size = 0;
    this.keys = new double[size];
    this.values = new Object[size];
  }

  @Override
  protected void heapifyUp(int pos, double curkey, Object curval) {
    while(pos > 0) {
      final int parent = (pos - 1) >>> 1;
      double parkey = keys[parent];

      if(curkey >= parkey) { // Compare
        break;
      }
      keys[pos] = parkey;
      values[pos] = values[parent];
      pos = parent;
    }
    keys[pos] = curkey;
    values[pos] = curval;
  }

  @Override
  protected boolean heapifyDown(final int ipos, double curkey, Object curval) {
    int pos = ipos;
    final int half = size >>> 1;
    while(pos < half) {
      // Get left child (must exist!)
      int cpos = (pos << 1) + 1;
      double chikey = keys[cpos];
      Object chival = values[cpos];
      // Test right child, if present
      final int rchild = cpos + 1;
      if(rchild < size) {
        double right = keys[rchild];
        if(chikey > right) { // Compare
          cpos = rchild;
          chikey = right;
          chival = values[rchild];
        }
      }

      if(curkey <= chikey) { // Compare
        break;
      }
      keys[pos] = chikey;
      values[pos] = chival;
      pos = cpos;
    }
    keys[pos] = curkey;
    values[pos] = curval;
    return (pos == ipos);
  }

  /**
   * Test whether the heap is still valid.
   * 
   * Debug method.
   * 
   * @return {@code null} when the heap is correct
   */
  protected String checkHeap() {
    ensureValid();
    for(int i = 1; i < size; i++) {
      final int parent = (i - 1) >>> 1;
      if(keys[parent] > keys[i]) { // Compare
        return "@" + parent + ": " + keys[parent] + " < @" + i + ": " + keys[i];
      }
    }
    return null;
  }
}
