/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.utilities.datastructures.heap;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Basic in-memory heap structure. Closely related to a
 * {@link java.util.PriorityQueue}, but here we can override methods to obtain
 * e.g. a {@link TopBoundedHeap}
 * 
 * Additionally, this heap is built lazily: if you first add many elements, then
 * poll the heap, it will be bulk-loaded in O(n) instead of iteratively built in
 * O(n log n). This is implemented via a simple validTo counter.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @has - - - UnorderedIter
 * 
 * @param <E> Element type. Should be {@link java.lang.Comparable} or a
 *        {@link java.util.Comparator} needs to be given.
 */
public class Heap<E> {
  /**
   * Heap storage.
   */
  protected Object[] queue;

  /**
   * Current number of objects.
   */
  protected int size = 0;

  /**
   * The comparator.
   */
  protected final Comparator<Object> comparator;

  /**
   * (Structural) modification counter. Used to invalidate iterators.
   */
  private int modCount = 0;

  /**
   * Default initial capacity.
   */
  private static final int DEFAULT_INITIAL_CAPACITY = 11;

  /**
   * Default constructor: default capacity, natural ordering.
   */
  public Heap() {
    this(DEFAULT_INITIAL_CAPACITY, null);
  }

  /**
   * Constructor with initial capacity, natural ordering.
   * 
   * @param size initial size
   */
  public Heap(int size) {
    this(size, null);
  }

  /**
   * Constructor with {@link Comparator}.
   * 
   * @param comparator Comparator
   */
  public Heap(Comparator<? super E> comparator) {
    this(DEFAULT_INITIAL_CAPACITY, comparator);
  }

  /**
   * Constructor with initial capacity and {@link Comparator}.
   * 
   * @param size initial capacity
   * @param comparator Comparator
   */
  @SuppressWarnings("unchecked")
  public Heap(int size, Comparator<? super E> comparator) {
    super();
    this.size = 0;
    this.queue = new Object[size];
    this.comparator = (Comparator<Object>) (comparator != null ? comparator : Comparator.naturalOrder());
  }

  /**
   * Add an element to the heap.
   * 
   * @param e Element to add
   */
  public void add(E e) {
    // resize when needed
    if(size + 1 > queue.length) {
      resize(size + 1);
    }
    // final int pos = size;
    this.size += 1;
    heapifyUp(size - 1, e);
    heapModified();
  }

  /**
   * Combined operation that removes the top element, and inserts a new element
   * instead.
   * 
   * @param e New element to insert
   * @return Previous top element of the heap
   */
  @SuppressWarnings("unchecked")
  public E replaceTopElement(E e) {
    E oldroot = (E) queue[0];
    heapifyDown(0, e);
    heapModified();
    return oldroot;
  }

  /**
   * Peek at the top element.
   * 
   * @return Top element.
   */
  @SuppressWarnings("unchecked")
  public E peek() {
    return size == 0 ? null : (E) queue[0];
  }

  /**
   * Remove the top element.
   * 
   * @return Top element.
   */
  public E poll() {
    return removeAt(0);
  }

  /**
   * Remove the element at the given position.
   * 
   * @param pos Element position.
   * @return Element that was at this position.
   */
  @SuppressWarnings("unchecked")
  protected E removeAt(int pos) {
    if(pos < 0 || pos >= size) {
      return null;
    }
    final E ret = (E) queue[pos];
    // Replacement object:
    final Object reinsert = queue[size - 1];
    queue[size - 1] = null;
    size--;
    heapifyDown(pos, reinsert);
    heapModified();
    return ret;
  }

  /**
   * Execute a "Heapify Upwards" aka "SiftUp". Used in insertions.
   * 
   * @param pos insertion position
   * @param elem Element to insert
   */
  protected void heapifyUp(int pos, E elem) {
    assert (pos < size && pos >= 0);
    while(pos > 0) {
      final int parent = (pos - 1) >>> 1;
      Object par = queue[parent];

      if(comparator.compare(elem, par) >= 0) {
        break;
      }
      queue[pos] = par;
      pos = parent;
    }
    queue[pos] = elem;
  }

  /**
   * Execute a "Heapify Downwards" aka "SiftDown". Used in deletions.
   * 
   * @param ipos re-insertion position
   * @param cur Object to reinsert
   * @return true when the order was changed
   */
  protected boolean heapifyDown(final int ipos, Object cur) {
    assert (ipos >= 0);
    int pos = ipos;
    final int half = size >>> 1;
    while(pos < half) {
      int min = pos;
      Object best = cur;

      final int lchild = (pos << 1) + 1;
      Object left = queue[lchild];
      if(comparator.compare(best, left) > 0) {
        min = lchild;
        best = left;
      }
      final int rchild = lchild + 1;
      if(rchild < size) {
        Object right = queue[rchild];
        if(comparator.compare(best, right) > 0) {
          min = rchild;
          best = right;
        }
      }
      if(min == pos) {
        break;
      }
      queue[pos] = best;
      pos = min;
    }
    queue[pos] = cur;
    return (pos != ipos);
  }

  /**
   * Get the heap size.
   * 
   * @return Heap size
   */
  public int size() {
    return this.size;
  }

  /**
   * Test for emptiness.
   * 
   * @return true when the heap is empty
   */
  public boolean isEmpty() {
    return size == 0;
  }

  /**
   * Test whether we need to resize to have the requested capacity.
   * 
   * @param requiredSize required capacity
   */
  protected final void resize(int requiredSize) {
    // Double until 64, then increase by 50% each time.
    int newCapacity = ((queue.length < 64) ? ((queue.length + 1) << 1) : ((queue.length >> 1) + queue.length));
    // overflow?
    if(newCapacity < 0) {
      throw new OutOfMemoryError();
    }
    if(requiredSize > newCapacity) {
      newCapacity = requiredSize;
    }
    queue = Arrays.copyOf(queue, newCapacity);
  }

  /**
   * Clear the heap.
   */
  public void clear() {
    // clean up references in the array for memory management
    for(int i = 0; i < size; i++) {
      queue[i] = null;
    }
    this.size = 0;
    heapModified();
  }

  /**
   * Called at the end of each heap modification.
   */
  protected void heapModified() {
    modCount++;
  }

  /**
   * Get an unordered heap iterator.
   * 
   * @return Iterator.
   */
  public UnorderedIter unorderedIter() {
    return new UnorderedIter();
  }

  /**
   * Test whether the heap is still valid.
   * 
   * Debug method.
   * 
   * @return {@code null} when the heap is correct
   */
  protected String checkHeap() {
    for(int i = 1; i < size; i++) {
      final int parent = (i - 1) >>> 1;
      if(comparator.compare(queue[parent], queue[i]) > 0) {
        return "@" + parent + ": " + queue[parent] + " < @" + i + ": " + queue[i];
      }
    }
    return null;
  }

  /**
   * Heap iterator.
   * 
   * @author Erich Schubert
   */
  public class UnorderedIter implements de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.Iter {
    /**
     * Current iterator position.
     */
    int pos = 0;

    /**
     * Constructor.
     */
    protected UnorderedIter() {
      super();
    }

    @Override
    public boolean valid() {
      return pos < size();
    }

    @Override
    public UnorderedIter advance() {
      pos++;
      return this;
    }

    /**
     * Get the current queue element.
     * 
     * @return Element
     */
    @SuppressWarnings("unchecked")
    public E get() {
      return (E) queue[pos];
    }
  }
}
