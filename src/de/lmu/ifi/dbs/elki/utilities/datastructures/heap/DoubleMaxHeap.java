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
import java.util.Comparator;

import de.lmu.ifi.dbs.elki.math.MathUtil;

/**
 * Basic in-memory heap structure.
 * 
 * This heap is built lazily: if you first add many elements, then poll the
 * heap, it will be bulk-loaded in O(n) instead of iteratively built in O(n log
 * n). This is implemented via a simple validTo counter.
 * 
 * @author Erich Schubert
 */
public class DoubleMaxHeap {
  /**
   * Heap storage: keys
   */
  protected transient double[] keys;

  /**
   * Current number of objects
   */
  protected int size = 0;

  /**
   * Indicate up to where the heap is valid
   */
  protected int validSize = 0;

  /**
   * (Structural) modification counter. Used to invalidate iterators.
   */
  public transient int modCount = 0;

  /**
   * Default initial capacity
   */
  private static final int DEFAULT_INITIAL_CAPACITY = 11;

  /**
   * Default constructor: default capacity, natural ordering.
   */
  public DoubleMaxHeap() {
    this(DEFAULT_INITIAL_CAPACITY);
  }

  /**
   * Constructor with initial capacity and {@link Comparator}.
   * 
   * @param size initial capacity
   */
  public DoubleMaxHeap(int size) {
    super();
    this.size = 0;
    this.keys = new double[size];
  }

  /**
   * Add a key-value pair to the heap
   * 
   * @param key Key
   */
  public void add(double key) {
    // resize when needed
    if(size + 1 > keys.length) {
      resize(size + 1);
    }
    // final int pos = size;
    this.keys[size] = key;
    this.size += 1;
    heapifyUp(size - 1, key);
    validSize += 1;
    heapModified();
  }

  /**
   * Add a key-value pair to the heap, except if the new element
   * is larger than the top, and we are at design size (overflow)
   * 
   * @param key Key
   * @param max Maximum size of heap
   */
  public void add(double key, int max) {
    if (size < max) {
      add(key);
    } else if (key < peek()) {
      replaceTopElement(key);
    }
  }

  /**
   * Combined operation that removes the top element, and inserts a new element
   * instead.
   * 
   * @param e New element to insert
   * @return Previous top element of the heap
   */
  public double replaceTopElement(double e) {
    ensureValid();
    double oldroot = keys[0];
    heapifyDown(0, e);
    heapModified();
    return oldroot;
  }

  /**
   * Get the current top key
   * 
   * @return Top key
   */
  public double peek() {
    if(size == 0) {
      throw new ArrayIndexOutOfBoundsException("Peek() on an empty heap!");
    }
    ensureValid();
    return keys[0];
  }

  /**
   * Remove the first element
   */
  public void poll() {
    removeAt(0);
  }

  /**
   * Repair the heap
   */
  protected void ensureValid() {
    if(validSize != size) {
      if(size > 1) {
        // Parent of first invalid
        int nextmin = validSize > 0 ? ((validSize - 1) >>> 1) : 0;
        int curmin = MathUtil.nextAllOnesInt(nextmin); // Next line
        int nextmax = curmin - 1; // End of valid line
        int pos = (size - 2) >>> 1; // Parent of last element
        // System.err.println(validSize+"<="+size+" iter:"+pos+"->"+curmin+", "+nextmin);
        while(pos >= nextmin) {
          // System.err.println(validSize+"<="+size+" iter:"+pos+"->"+curmin);
          while(pos >= curmin) {
            if(!heapifyDown(pos, keys[pos])) {
              final int parent = (pos - 1) >>> 1;
              if(parent < curmin) {
                nextmin = Math.min(nextmin, parent);
                nextmax = Math.max(nextmax, parent);
              }
            }
            pos--;
          }
          curmin = nextmin;
          pos = Math.min(pos, nextmax);
          nextmax = -1;
        }
      }
      validSize = size;
    }
  }

  /**
   * Remove the element at the given position.
   * 
   * @param pos Element position.
   */
  protected void removeAt(int pos) {
    if(pos < 0 || pos >= size) {
      return;
    }
    // Replacement object:
    final double reinkey = keys[size - 1];
    // Keep heap in sync
    if(validSize == size) {
      size -= 1;
      validSize -= 1;
      heapifyDown(pos, reinkey);
    }
    else {
      size -= 1;
      validSize = Math.min(pos >>> 1, validSize);
      keys[pos] = reinkey;
    }
    heapModified();
  }

  /**
   * Execute a "Heapify Upwards" aka "SiftUp". Used in insertions.
   * 
   * @param pos insertion position
   * @param curkey Current key
   */
  protected void heapifyUp(int pos, double curkey) {
    while(pos > 0) {
      final int parent = (pos - 1) >>> 1;
      double parkey = keys[parent];

      if(curkey <= parkey) { // Compare
        break;
      }
      keys[pos] = parkey;
      pos = parent;
    }
    keys[pos] = curkey;
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
    final int half = size >>> 1;
    while(pos < half) {
      // Get left child (must exist!)
      int cpos = (pos << 1) + 1;
      double chikey = keys[cpos];
      // Test right child, if present
      final int rchild = cpos + 1;
      if(rchild < size) {
        double right = keys[rchild];
        if(chikey < right) { // Compare
          cpos = rchild;
          chikey = right;
        }
      }

      if(curkey >= chikey) { // Compare
        break;
      }
      keys[pos] = chikey;
      pos = cpos;
    }
    keys[pos] = curkey;
    return (pos == ipos);
  }

  /**
   * Query the size
   * 
   * @return Size
   */
  public int size() {
    return this.size;
  }

  /**
   * Test whether we need to resize to have the requested capacity.
   * 
   * @param requiredSize required capacity
   */
  protected final void resize(int requiredSize) {
    // Double until 64, then increase by 50% each time.
    int newCapacity = ((keys.length < 64) ? ((keys.length + 1) * 2) : ((keys.length / 2) * 3));
    // overflow?
    if(newCapacity < 0) {
      throw new OutOfMemoryError();
    }
    if(requiredSize > newCapacity) {
      newCapacity = requiredSize;
    }
    keys = Arrays.copyOf(keys, newCapacity);
  }

  /**
   * Delete all elements from the heap.
   */
  public void clear() {
    this.size = 0;
    this.validSize = -1;
    heapModified();
  }

  /**
   * Called at the end of each heap modification.
   */
  protected void heapModified() {
    modCount++;
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
      if(keys[parent] < keys[i]) { // Compare
        return "@" + parent + ": " + keys[parent] + " < @" + i + ": " + keys[i];
      }
    }
    return null;
  }
}
