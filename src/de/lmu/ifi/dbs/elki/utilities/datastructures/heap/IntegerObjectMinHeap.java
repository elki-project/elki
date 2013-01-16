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

import java.util.Arrays;
import java.util.ConcurrentModificationException;

/**
 * Basic in-memory heap structure for int keys and V values,
 * ordered by maximum first.
 * 
 * Basic 4-ary heap implementation.
 * 
 * No bulk load, because it did not perform better in our benchmarks!
 * 
 * @author Erich Schubert
 * 
 * @param <V> Value type
 */
public class IntegerObjectMinHeap<V> extends AbstractHeap implements IntegerObjectHeap<V> {
  /**
   * Heap storage: keys
   */
  private int[] keys;

  /**
   * Heap storage: values
   */
  private Object[] values;

  /**
   * Default constructor: default capacity.
   */
  public IntegerObjectMinHeap() {
    this(DEFAULT_INITIAL_CAPACITY);
  }

  /**
   * Constructor with initial capacity.
   * 
   * @param size initial capacity
   */
  public IntegerObjectMinHeap(int size) {
    super();
    this.size = 0;
    this.keys = new int[size];
    this.values = new Object[size];
  }

  @Override
  public void add(int key, V val) {
    this.size++;
    // resize when needed
    if (size > keys.length) {
      resize(size);
    }
    heapifyUp(size - 1, key, val);
    heapModified();
  }

  @Override
  public void replaceTopElement(int key, V val) {
    heapifyDown(0, key, val);
    heapModified();
  }

  @Override
  public int peekKey() {
    if (size == 0) {
      throw new ArrayIndexOutOfBoundsException("Peek() on an empty heap!");
    }
    return keys[0];
  }

  @Override
  @SuppressWarnings("unchecked")
  public V peekValue() {
    if (size == 0) {
      throw new ArrayIndexOutOfBoundsException("Peek() on an empty heap!");
    }
    return (V) values[0];
  }

  @Override
  public void poll() {
    removeAt(0);
  }

  /**
   * Remove the element at the given position.
   * 
   * @param pos Element position.
   */
  protected void removeAt(int pos) {
    if (pos < 0 || pos >= size) {
      return;
    }
    size--;
    // Replacement object:
    final int reinkey = keys[size];
    final Object reinval = values[size];
    keys[size] = 0;
    values[size] = null;
    heapifyDown(pos, reinkey, reinval);
    heapModified();
  }

  /**
   * Execute a "Heapify Upwards" aka "SiftUp". Used in insertions.
   * 
   * @param pos insertion position
   * @param curkey Current key
   * @param curval Current value
   */
  protected void heapifyUp(int pos, int curkey, Object curval) {
    while (pos > 0) {
      final int parent = (pos - 1) >>> 2;
      int parkey = keys[parent];

      if (curkey >= parkey) { // Compare
        break;
      }
      keys[pos] = parkey;
      values[pos] = values[parent];
      pos = parent;
    }
    keys[pos] = curkey;
    values[pos] = curval;
  }

  /**
   * Execute a "Heapify Downwards" aka "SiftDown". Used in deletions.
   * 
   * @param ipos re-insertion position
   * @param curkey Current key
   * @param curval Current value
   * @return true when the order was changed
   */
  protected boolean heapifyDown(final int ipos, int curkey, Object curval) {
    int pos = ipos;
    final int half = (size + 2) >>> 2;
    while (pos < half) {
      // Get left child (must exist!)
      final int cpos = (pos << 2) + 1;
      int bestpos = cpos;
      int bestkey = keys[cpos];
      Object bestval = values[cpos];
      // Test second child, if present
      final int schild = cpos + 1;
      if (schild < size) {
        int secondc = keys[schild];
        if (bestkey > secondc) { // Compare
          bestpos = schild;
          bestkey = secondc;
          bestval = values[schild];
        }
        // Test third child, if present
        final int tchild = cpos + 2;
        if (tchild < size) {
          int thirdc = keys[tchild];
          if (bestkey > thirdc) { // Compare
            bestpos = tchild;
            bestkey = thirdc;
            bestval = values[tchild];
          }
          // Test fourth child, if present
          final int fchild = cpos + 3;
          if (fchild < size) {
            int firstc = keys[fchild];
            if (bestkey > firstc) { // Compare
              bestpos = fchild;
              bestkey = firstc;
              bestval = values[fchild];
            }
          }
        }
      }

      if (bestkey > curkey) { // Compare
        break;
      }
      keys[pos] = bestkey;
      values[pos] = bestval;
      pos = bestpos;
    }
    keys[pos] = curkey;
    values[pos] = curval;
    return (pos != ipos);
  }

  /**
   * Test whether we need to resize to have the requested capacity.
   * 
   * @param requiredSize required capacity
   */
  private final void resize(int requiredSize) {
    // Double until 64, then increase by 50% each time.
    int newCapacity = ((keys.length < 64) ? ((keys.length + 1) << 1) : ((keys.length >> 1) * 3));
    // overflow?
    if (newCapacity < 0) {
      throw new OutOfMemoryError();
    }
    if (requiredSize > newCapacity) {
      newCapacity = requiredSize;
    }
    keys = Arrays.copyOf(keys, newCapacity);
    values = Arrays.copyOf(values, newCapacity);
  }

  @Override
  public void clear() {
    // clean up references in the array for memory management
    Arrays.fill(keys, 0);
    Arrays.fill(values, null);
    super.clear();
  }

  @Override
  public UnsortedIter unsortedIter() {
    return new UnsortedIter();
  }

  /**
   * Unsorted iterator - in heap order. Does not poll the heap.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected class UnsortedIter extends AbstractHeap.UnsortedIter implements IntegerObjectHeap.UnsortedIter<V> {
    @Override
    public int getKey() {
      return keys[pos];
    }

    @Override
    @SuppressWarnings("unchecked")
    public V getValue() {
      if (modCount != myModCount) {
        throw new ConcurrentModificationException();
      }
      return (V) values[pos];
    }
  }

  /**
   * Test whether the heap is still valid.
   * 
   * Debug method.
   * 
   * @return {@code null} when the heap is correct
   */
  protected String checkHeap() {
    for (int i = 1; i < size; i++) {
      final int parent = (i - 1) >>> 2;
      if (keys[parent] > keys[i]) { // Compare
        return "@" + parent + ": " + keys[parent] + " < @" + i + ": " + keys[i];
      }
    }
    return null;
  }
}
