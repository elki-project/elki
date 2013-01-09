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
 * Basic in-memory heap structure for Object values.
 * 
 * After extensive microbenchmarking we arrived back at this very simple heap:
 * Bulk-loading did not improve the performance in the general case.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has UnsortedIter
 */
public abstract class ObjectHeap<K> extends AbstractHeap {
  /**
   * Heap storage: queue
   */
  protected transient Object[] queue;

  /**
   * Constructor with initial capacity.
   * 
   * @param size initial capacity
   */
  public ObjectHeap(int size) {
    super();
    this.size = 0;
    this.queue = new Object[size];
  }

  /**
   * Add a key-value pair to the heap
   * 
   * @param key Key
   */
  public void add(K key) {
    // resize when needed
    if (size + 1 > queue.length) {
      resize(size + 1);
    }
    this.size++;
    heapifyUp(size - 1, key);
    heapModified();
  }

  /**
   * Add a key-value pair to the heap, except if the new element is larger than
   * the top, and we are at design size (overflow)
   * 
   * @param key Key
   * @param max Maximum size of heap
   */
  public void add(K key, int max) {
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
  @SuppressWarnings("unchecked")
  public K replaceTopElement(K e) {
    K oldroot = (K) queue[0];
    heapifyDown(0, e);
    heapModified();
    return oldroot;
  }

  /**
   * Get the current top key
   * 
   * @return Top key
   */
  @SuppressWarnings("unchecked")
  public K peek() {
    if (size == 0) {
      throw new ArrayIndexOutOfBoundsException("Peek() on an empty heap!");
    }
    return (K) queue[0];
  }

  /**
   * Remove the first element
   * 
   * @return Top element
   */
  @SuppressWarnings("unchecked")
  public K poll() {
    return (K) removeAt(0);
  }

  /**
   * Remove the element at the given position.
   * 
   * @param pos Element position.
   * @return Removed element
   */
  protected Object removeAt(int pos) {
    if (pos < 0 || pos >= size) {
      return null;
    }
    final Object top = queue[0];
    // Replacement object:
    final Object reinkey = queue[size - 1];
    size--;
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
  protected void heapifyUp(int pos, Object curkey) {
    while (pos > 0) {
      final int parent = (pos - 1) >>> 1;
      Object parkey = queue[parent];

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
  protected boolean heapifyDown(final int ipos, Object curkey) {
    int pos = ipos;
    final int half = size >>> 1;
    while (pos < half) {
      // Get left child (must exist!)
      int cpos = (pos << 1) + 1;
      Object chikey = queue[cpos];
      // Test right child, if present
      final int rchild = cpos + 1;
      if (rchild < size) {
        Object right = queue[rchild];
        if (comp(chikey, right)) { // Compare
          cpos = rchild;
          chikey = right;
        }
      }

      if (!comp(curkey, chikey)) { // Compare
        break;
      }
      queue[pos] = chikey;
      pos = cpos;
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

  /**
   * Delete all elements from the heap.
   */
  @Override
  public void clear() {
    super.clear();
    for (int i = 0; i < size; i++) {
      queue[i] = null;
    }
  }

  /**
   * Compare two objects
   */
  abstract protected boolean comp(Object o1, Object o2);

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
      pos++;
    }

    /**
     * Get the iterators current object.
     * 
     * @return Current object
     */
    @SuppressWarnings("unchecked")
    public K get() {
      if (modCount != myModCount) {
        throw new ConcurrentModificationException();
      }
      return (K) queue[pos];
    }
  }
}
