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
 * Basic in-memory heap structure for double keys and int values,
 * ordered by maximum first.
 * 
 * Basic 4-ary heap implementation.
 * 
 * No bulk load, because it did not perform better in our benchmarks!
 * 
 * @author Erich Schubert
 */
public class DoubleIntegerMinHeap extends AbstractHeap implements DoubleIntegerHeap {
  /**
   * Heap storage: keys
   */
  private double[] keys;

  /**
   * Heap storage: values
   */
  private int[] values;

  /**
   * Default constructor: default capacity.
   */
  public DoubleIntegerMinHeap() {
    this(DEFAULT_INITIAL_CAPACITY);
  }

  /**
   * Constructor with initial capacity.
   * 
   * @param size initial capacity
   */
  public DoubleIntegerMinHeap(int size) {
    super();
    this.size = 0;
    this.keys = new double[size];
    this.values = new int[size];
  }

  @Override
  public void add(double key, int val) {
    this.size++;
    // resize when needed
    if (size > keys.length) {
      resize(size);
    }
    heapifyUp(size - 1, key, val);
    heapModified();
  }

  @Override
  public void replaceTopElement(double key, int val) {
    heapifyDown(0, key, val);
    heapModified();
  }

  @Override
  public double peekKey() {
    if (size == 0) {
      throw new ArrayIndexOutOfBoundsException("Peek() on an empty heap!");
    }
    return keys[0];
  }

  @Override
  public int peekValue() {
    if (size == 0) {
      throw new ArrayIndexOutOfBoundsException("Peek() on an empty heap!");
    }
    return values[0];
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
    final double reinkey = keys[size];
    final int reinval = values[size];
    keys[size] = 0.0;
    values[size] = 0;
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
  protected void heapifyUp(int pos, double curkey, int curval) {
    while (pos > 0) {
      final int parent = (pos - 1) >>> 2;
      double parkey = keys[parent];

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
  protected boolean heapifyDown(final int ipos, double curkey, int curval) {
    int pos = ipos;
    final int half = (size + 2) >>> 2;
    while (pos < half) {
      // Get left child (must exist!)
      final int cpos = (pos << 2) + 1;
      int bestpos = cpos;
      double bestkey = keys[cpos];
      int bestval = values[cpos];
      // Test second child, if present
      final int schild = cpos + 1;
      if (schild < size) {
        double secondc = keys[schild];
        if (bestkey > secondc) { // Compare
          bestpos = schild;
          bestkey = secondc;
          bestval = values[schild];
        }
        // Test third child, if present
        final int tchild = cpos + 2;
        if (tchild < size) {
          double thirdc = keys[tchild];
          if (bestkey > thirdc) { // Compare
            bestpos = tchild;
            bestkey = thirdc;
            bestval = values[tchild];
          }
          // Test fourth child, if present
          final int fchild = cpos + 3;
          if (fchild < size) {
            double firstc = keys[fchild];
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
    Arrays.fill(keys, 0.0);
    Arrays.fill(values, 0);
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
  protected class UnsortedIter extends AbstractHeap.UnsortedIter implements DoubleIntegerHeap.UnsortedIter {
    @Override
    public double getKey() {
      return keys[pos];
    }

    @Override
    public int getValue() {
      if (modCount != myModCount) {
        throw new ConcurrentModificationException();
      }
      return values[pos];
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
