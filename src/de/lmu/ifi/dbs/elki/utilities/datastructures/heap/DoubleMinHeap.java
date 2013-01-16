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
 * Basic in-memory min-heap for double values.
 * 
 * Basic 4-ary heap implementation.
 * 
 * No bulk load, because it did not perform better in our benchmarks!
 * 
 * @author Erich Schubert
 */
public class DoubleMinHeap extends AbstractHeap implements DoubleHeap {
  /**
   * Heap storage: queue
   */
  protected double[] queue;

  /**
   * Constructor with default capacity.
   */
  public DoubleMinHeap() {
    this(DEFAULT_INITIAL_CAPACITY);
  }

  /**
   * Constructor with initial capacity.
   * 
   * @param size initial capacity
   */
  public DoubleMinHeap(int size) {
    super();
    this.size = 0;
    this.queue = new double[size];
  }

  @Override
  public void add(double key) {
    this.size++;
    // resize when needed
    if (size > queue.length) {
      resize(size);
    }
    heapifyUp(size - 1, key);
    heapModified();
  }

  @Override
  public void add(double key, int max) {
    if (size < max) {
      add(key);
    } else if (key > peek()) {
      replaceTopElement(key);
    }
  }

  @Override
  public double replaceTopElement(double e) {
    final double oldroot = queue[0];
    heapifyDown(0, e);
    heapModified();
    return oldroot;
  }

  @Override
  public double peek() {
    if (size == 0) {
      throw new ArrayIndexOutOfBoundsException("Peek() on an empty heap!");
    }
    return queue[0];
  }

  @Override
  public double poll() {
    return removeAt(0);
  }

  /**
   * Remove the element at the given position.
   * 
   * @param pos Element position.
   * @return Removed element
   */
  protected double removeAt(int pos) {
    if (pos < 0 || pos >= size) {
      return 0.0;
    }
    final double top = queue[pos];
    size--;
    // Replacement object:
    final double reinkey = queue[size];
    heapifyDown(pos, reinkey);
    heapModified();
    return top;
  }

  /**
   * Execute a "Heapify Upwards" aka "SiftUp". Used in insertions.
   * 
   * @param pos insertion position
   * @param curkey Current key
   */
  protected void heapifyUp(int pos, double curkey) {
    while (pos > 0) {
      final int parent = (pos - 1) >>> 2;
      double parkey = queue[parent];

      if (curkey > parkey) { // Compare
        break;
      }
      queue[pos] = parkey;
      pos = parent;
    }
    queue[pos] = curkey;
  }

  /**
   * Execute a "Heapify Downwards" aka "SiftDown". Used in deletions.
   * 
   * @param ipos re-insertion position
   * @param curkey Current key
   * @return true when the order was changed
   */
  protected boolean heapifyDown(final int ipos, double curkey) {
    int pos = ipos;
    final int half = (size + 2) >>> 2;
    while (pos < half) {
      // Get left child (must exist!)
      final int cpos = (pos << 2) + 1;
      int bestpos = cpos;
      double bestkey = queue[cpos];
      // Test second child, if present
      final int schild = cpos + 1;
      if (schild < size) {
        double secondc = queue[schild];
        if (bestkey > secondc) { // Compare
          bestpos = schild;
          bestkey = secondc;
        }

        // Test third child, if present
        final int tchild = cpos + 2;
        if (tchild < size) {
          double thirdc = queue[tchild];
          if (bestkey > thirdc) { // Compare
            bestpos = tchild;
            bestkey = thirdc;
          }

          // Test fourth child, if present
          final int fchild = cpos + 3;
          if (fchild < size) {
            double fourthc = queue[fchild];
            if (bestkey > fourthc) { // Compare
              bestpos = fchild;
              bestkey = fourthc;
            }
          }
        }
      }

      if (bestkey > curkey) { // Compare
        break;
      }
      queue[pos] = bestkey;
      pos = bestpos;
    }
    queue[pos] = curkey;
    return (pos != ipos);
  }

  /**
   * Test whether we need to resize to have the requested capacity.
   * 
   * @param requiredSize required capacity
   */
  protected final void resize(int requiredSize) {
    queue = Arrays.copyOf(queue, desiredSize(requiredSize, queue.length));
  }

  @Override
  public void clear() {
    super.clear();
    for (int i = 0; i < size; i++) {
      queue[i] = 0.0;
    }
  }

  @Override
  public UnsortedIter unsortedIter() {
    return new UnsortedIter();
  }

  /**
   * Unsorted iterator - in heap order. Does not poll the heap.
   * 
   * @author Erich Schubert
   */
  protected class UnsortedIter extends AbstractHeap.UnsortedIter implements DoubleHeap.UnsortedIter {
    @Override
    public double get() {
      if (modCount != myModCount) {
        throw new ConcurrentModificationException();
      }
      return queue[pos];
    }
  }
}
