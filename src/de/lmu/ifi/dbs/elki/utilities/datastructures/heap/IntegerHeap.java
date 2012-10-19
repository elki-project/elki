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
public abstract class IntegerHeap extends AbstractHeap {
  /**
   * Heap storage: queue
   */
  protected transient int[] queue;

  /**
   * Constructor with initial capacity.
   * 
   * @param size initial capacity
   */
  public IntegerHeap(int size) {
    super();
    this.size = 0;
    this.queue = new int[size];
  }

  /**
   * Add a key-value pair to the heap
   * 
   * @param key Key
   */
  public void add(int key) {
    // resize when needed
    if (size + 1 > queue.length) {
      resize(size + 1);
    }
    // final int pos = size;
    this.queue[size] = key;
    this.size += 1;
    heapifyUp(size - 1, key);
    validSize += 1;
    heapModified();
  }

  /**
   * Add a key-value pair to the heap, except if the new element is larger than
   * the top, and we are at design size (overflow)
   * 
   * @param key Key
   * @param max Maximum size of heap
   */
  public void add(int key, int max) {
    if (size < max) {
      add(key);
    } else if (comp(key, peek())) {
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
  public int replaceTopElement(int e) {
    ensureValid();
    int oldroot = queue[0];
    heapifyDown(0, e);
    heapModified();
    return oldroot;
  }

  /**
   * Get the current top key
   * 
   * @return Top key
   */
  public int peek() {
    if (size == 0) {
      throw new ArrayIndexOutOfBoundsException("Peek() on an empty heap!");
    }
    ensureValid();
    return queue[0];
  }

  /**
   * Remove the first element
   * 
   * @return Top element
   */
  public int poll() {
    return removeAt(0);
  }

  /**
   * Repair the heap
   */
  protected void ensureValid() {
    if (validSize != size) {
      if (size > 1) {
        // Parent of first invalid
        int nextmin = validSize > 0 ? ((validSize - 1) >>> 1) : 0;
        int curmin = MathUtil.nextAllOnesInt(nextmin); // Next line
        int nextmax = curmin - 1; // End of valid line
        int pos = (size - 2) >>> 1; // Parent of last element
        // System.err.println(validSize+"<="+size+" iter:"+pos+"->"+curmin+", "+nextmin);
        while (pos >= nextmin) {
          // System.err.println(validSize+"<="+size+" iter:"+pos+"->"+curmin);
          while (pos >= curmin) {
            if (!heapifyDown(pos, queue[pos])) {
              final int parent = (pos - 1) >>> 1;
              if (parent < curmin) {
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
   * @return Removed element
   */
  protected int removeAt(int pos) {
    if (pos < 0 || pos >= size) {
      return 0;
    }
    final int top = queue[0];
    // Replacement object:
    final int reinkey = queue[size - 1];
    // Keep heap in sync
    if (validSize == size) {
      size -= 1;
      validSize -= 1;
      heapifyDown(pos, reinkey);
    } else {
      size -= 1;
      validSize = Math.min(pos >>> 1, validSize);
      queue[pos] = reinkey;
    }
    heapModified();
    return top;
  }

  /**
   * Execute a "Heapify Upwards" aka "SiftUp". Used in insertions.
   * 
   * @param pos insertion position
   * @param curkey Current key
   */
  protected void heapifyUp(int pos, int curkey) {
    while (pos > 0) {
      final int parent = (pos - 1) >>> 1;
      int parkey = queue[parent];

      if (comp(curkey, parkey)) { // Compare
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
  protected boolean heapifyDown(final int ipos, int curkey) {
    int pos = ipos;
    final int half = size >>> 1;
    while (pos < half) {
      // Get left child (must exist!)
      int cpos = (pos << 1) + 1;
      int chikey = queue[cpos];
      // Test right child, if present
      final int rchild = cpos + 1;
      if (rchild < size) {
        int right = queue[rchild];
        if (comp(chikey, right)) { // Compare
          cpos = rchild;
          chikey = right;
        }
      }

      if (comp(chikey, curkey)) { // Compare
        break;
      }
      queue[pos] = chikey;
      pos = cpos;
    }
    queue[pos] = curkey;
    return (pos == ipos);
  }

  /**
   * Test whether we need to resize to have the requested capacity.
   * 
   * @param requiredSize required capacity
   */
  protected final void resize(int requiredSize) {
    queue = Arrays.copyOf(queue, desiredSize(requiredSize, queue.length));
  }

  /**
   * Delete all elements from the heap.
   */
  @Override
  public void clear() {
    super.clear();
    for (int i = 0; i < size; i++) {
      queue[i] = 0;
    }
  }

  /**
   * Compare two objects
   */
  abstract protected boolean comp(int o1, int o2);
}
