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

import de.lmu.ifi.dbs.elki.utilities.iterator.Iter;

/**
 * Basic in-memory heap structure using int keys and Object values.
 * 
 * After extensive microbenchmarking we arrived back at this very simple heap:
 * Bulk-loading did not improve the performance in the general case.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Value type
 * 
 * @apiviz.has UnsortedIter
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
    this.size++;
    heapifyUp(size - 1, key, val);
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
    size--;
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

    /**
     * Modification counter we were initialized at.
     */
    final int myModCount = modCount;

    @Override
    public boolean valid() {
      if (modCount != myModCount) {
        throw new ConcurrentModificationException();
      }
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
      if (modCount != myModCount) {
        throw new ConcurrentModificationException();
      }
      return (V) values[pos];
    }
  }
}
