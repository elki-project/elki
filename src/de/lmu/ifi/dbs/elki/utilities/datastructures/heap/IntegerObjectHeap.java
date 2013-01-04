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
import de.lmu.ifi.dbs.elki.utilities.iterator.Iter;

/**
 * Basic in-memory heap structure using int keys and V values.
 * 
 * This heap is built lazily: if you first add many elements, then poll the
 * heap, it will be bulk-loaded in O(n) instead of iteratively built in O(n log
 * n). This is implemented via a simple validTo counter.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Value type
 */
public abstract class IntegerObjectHeap<V> extends AbstractHeap {
  /**
   * Heap storage: keys
   */
  protected int[] keys;

  /**
   * Heap storage: values
   */
  protected Object[] values;

  /**
   * Default constructor: default capacity.
   */
  public IntegerObjectHeap() {
    this(DEFAULT_INITIAL_CAPACITY);
  }

  /**
   * Constructor with initial capacity.
   * 
   * @param size initial capacity
   */
  public IntegerObjectHeap(int size) {
    super();
    this.size = 0;
    this.keys = new int[size];
    this.values = new Object[size];
  }

  /**
   * Add a key-value pair to the heap
   * 
   * @param key Key
   * @param val Value
   */
  public void add(int key, V val) {
    // resize when needed
    if (size + 1 > keys.length) {
      resize(size + 1);
    }
    // final int pos = size;
    this.keys[size] = key;
    this.values[size] = val;
    this.size += 1;
    // As bulk repairs do not (yet) perform as expected
    // We will for now immediately repair the heap!
    heapifyUp(size - 1, key, val);
    validSize += 1;
    heapModified();
  }

  /**
   * Combined operation that removes the top element, and inserts a new element
   * instead.
   * 
   * @param key Key of new element
   * @param val Value of new element
   */
  public void replaceTopElement(int key, V val) {
    ensureValid();
    heapifyDown(0, key, val);
    heapModified();
  }

  /**
   * Get the current top key
   * 
   * @return Top key
   */
  public int peekKey() {
    if (size == 0) {
      throw new ArrayIndexOutOfBoundsException("Peek() on an empty heap!");
    }
    ensureValid();
    return keys[0];
  }

  /**
   * Get the current top value
   * 
   * @return Value
   */
  @SuppressWarnings("unchecked")
  public V peekValue() {
    if (size == 0) {
      throw new ArrayIndexOutOfBoundsException("Peek() on an empty heap!");
    }
    ensureValid();
    return (V) values[0];
  }

  /**
   * Remove the first element
   */
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
    // Replacement object:
    final int reinkey = keys[size - 1];
    final Object reinval = values[size - 1];
    values[size - 1] = null;
    // Keep heap in sync
    if (validSize == size) {
      size -= 1;
      validSize -= 1;
      heapifyDown(pos, reinkey, reinval);
    } else {
      size -= 1;
      validSize = Math.min(pos >>> 1, validSize);
      keys[pos] = reinkey;
      values[pos] = reinval;
    }
    heapModified();
  }

  /**
   * Execute a "Heapify Upwards" aka "SiftUp". Used in insertions.
   * 
   * @param pos insertion position
   * @param curkey Current key
   * @param curval Current value
   */
  abstract protected void heapifyUp(int pos, int curkey, Object curval);

  /**
   * Execute a "Heapify Downwards" aka "SiftDown". Used in deletions.
   * 
   * @param ipos re-insertion position
   * @param curkey Current key
   * @param curval Current value
   * @return true when the order was changed
   */
  abstract protected boolean heapifyDown(int ipos, int curkey, Object curval);

  /**
   * Repair the heap, if necessary.
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
            if (!heapifyDown(pos, keys[pos], values[pos])) {
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
   * Test whether we need to resize to have the requested capacity.
   * 
   * @param requiredSize required capacity
   */
  protected final void resize(int requiredSize) {
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

  /**
   * Get an unsorted iterator to inspect the heap.
   * 
   * @return Iterator
   */
  public UnsortedIter unsortedIter() {
    return new UnsortedIter();
  }
  
  /**
   * Unsorted iterator - in heap order. Does not poll the heap.
   * 
   * @author Erich Schubert
   */
  public class UnsortedIter implements Iter {
    /**
     * Iterator position.
     */
    int pos = 0;

    @Override
    public boolean valid() {
      return pos < size;
    }

    @Override
    public void advance() {
      pos ++;
    }

    /**
     * Get the current key
     * 
     * @return Current key
     */
    public int getKey() {
      return keys[pos];
    }


    /**
     * Get the current value
     * 
     * @return Current value
     */
    @SuppressWarnings("unchecked")
    public V getValue() {
      return (V) values[pos];
    }
  }
}
