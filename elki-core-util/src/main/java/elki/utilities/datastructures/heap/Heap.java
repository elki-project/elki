/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.utilities.datastructures.heap;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Basic in-memory heap structure. Similar to a {@link java.util.PriorityQueue},
 * but with some additional operations.
 * <p>
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
    if(++size > queue.length) {
      queue = Arrays.copyOf(queue, HeapUtil.nextSize(queue.length));
    }
    heapifyUp(size - 1, e);
    heapModified();
  }

  /**
   * Add an element to the heap.
   * 
   * @param e Element to add
   * @param maxsize Maximum size
   */
  public void add(E e, int maxsize) {
    if(size == maxsize) {
      if(comparator.compare(e, queue[0]) > 0) {
        // Replace top element, and repair:
        heapifyDown(0, e);
        heapModified();
      }
      return;
    }
    if(++size > queue.length) {
      queue = Arrays.copyOf(queue, HeapUtil.nextSize(queue.length));
    }
    heapifyUp(size - 1, e);
    heapModified();
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
  @SuppressWarnings("unchecked")
  public E poll() {
    if(size == 0) {
      return null;
    }
    Object ret = queue[0], reinsert = queue[--size];
    queue[size] = null;
    heapifyDown(0, reinsert);
    heapModified();
    return (E) ret;
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
   * Clear the heap.
   */
  public void clear() {
    Arrays.fill(queue, 0, size, null);
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
   * <p>
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
  public class UnorderedIter implements elki.utilities.datastructures.iterator.Iter {
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
