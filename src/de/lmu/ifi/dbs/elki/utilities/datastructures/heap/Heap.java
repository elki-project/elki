package de.lmu.ifi.dbs.elki.utilities.datastructures.heap;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Basic in-memory heap structure. Closely related to a
 * {@link java.util.PriorityQueue}, but here we can override methods to obtain
 * e.g. a {@link TopBoundedHeap}
 * 
 * @author Erich Schubert
 * 
 * @param <E> Element type. Should be {@link java.lang.Comparable} or a
 *        {@link java.util.Comparator} needs to be given.
 */
public class Heap<E> extends AbstractQueue<E> implements Serializable {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Heap storage
   * 
   * Note: keep private; all write access should be done through
   * {@link #putInQueue} for subclasses to track!
   */
  protected transient Object[] queue;

  /**
   * Current number of objects
   */
  protected int size = 0;

  /**
   * The comparator or {@code null}
   */
  protected final Comparator<Object> comparator;

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
    this.comparator = (Comparator<Object>) comparator;
  }

  @Override
  public boolean add(E e) {
    // Never full - overriding probably slightly faster
    return offer(e);
  }

  @Override
  public boolean offer(E e) {
    // resize when needed
    if(size + 1 > queue.length) {
      resize(size + 1);
    }
    final int pos = size;
    this.size += 1;
    heapifyUp(pos, e);
    // We have changed - return true according to {@link Collection#put}
    modCount++;
    return true;
  }

  @Override
  public E peek() {
    if(size == 0) {
      return null;
    }
    return castQueueElement(0);
  }

  @Override
  public E poll() {
    return removeAt(0);
  }

  /**
   * Remove the element at the given position.
   * 
   * @param pos Element position.
   */
  protected E removeAt(int pos) {
    if(pos < 0 || pos >= size) {
      return null;
    }
    final E ret = castQueueElement(pos);
    // Replacement object:
    final Object reinsert = queue[size - 1];
    queue[size - 1] = null;
    size -= 1;
    heapifyDown(pos, reinsert);
    modCount++;
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
    if(comparator != null) {
      heapifyUpComparator(pos, elem);
    }
    else {
      heapifyUpComparable(pos, elem);
    }
  }

  /**
   * Execute a "Heapify Upwards" aka "SiftUp". Used in insertions.
   * 
   * @param pos insertion position
   * @param elem Element to insert
   */
  @SuppressWarnings("unchecked")
  protected void heapifyUpComparable(int pos, E elem) {
    final Comparable<Object> cur = (Comparable<Object>) elem; // queue[pos];
    while(pos > 0) {
      final int parent = (pos - 1) >>> 1;
      Object par = queue[parent];

      if(cur.compareTo(par) >= 0) {
        break;
      }
      queue[pos] = par;    
      pos = parent;
    }
    queue[pos] = cur;    
  }

  /**
   * Execute a "Heapify Upwards" aka "SiftUp". Used in insertions.
   * 
   * @param pos insertion position
   * @param cur Element to insert
   */
  protected void heapifyUpComparator(int pos, E cur) {
    while(pos > 0) {
      final int parent = (pos - 1) >>> 1;
      Object par = queue[parent];

      if(comparator.compare(cur, par) >= 0) {
        break;
      }
      queue[pos] = par;    
      pos = parent;
    }
    queue[pos] = cur;    
  }

  /**
   * Execute a "Heapify Downwards" aka "SiftDown". Used in deletions.
   * 
   * @param pos re-insertion position
   * @param reinsert Object to reinsert
   */
  protected void heapifyDown(int pos, Object reinsert) {
    assert (pos >= 0);
    if(comparator != null) {
      heapifyDownComparator(pos, reinsert);
    }
    else {
      heapifyDownComparable(pos, reinsert);
    }
  }

  /**
   * Execute a "Heapify Downwards" aka "SiftDown". Used in deletions.
   * 
   * @param pos re-insertion position
   */
  @SuppressWarnings("unchecked")
  protected void heapifyDownComparable(int pos, Object reinsert) {
    Comparable<Object> cur = (Comparable<Object>) reinsert;
    final int half = size >>> 1;
    while(pos < half) {
      // Get left child (must exist!)
      int cpos = (pos << 1) + 1;
      Object child = queue[cpos];
      // Test right child, if present
      final int rchild = cpos + 1;
      if(rchild < size) {
        Object right = queue[rchild];
        if(((Comparable<Object>)child).compareTo(right) > 0) {
          cpos = rchild;
          child = right;
        }
      }
      
      if(cur.compareTo(child) <= 0) {
        break;
      }
      queue[pos] = child;    
      pos = cpos;
    }
    queue[pos] = cur;    
  }

  /**
   * Execute a "Heapify Downwards" aka "SiftDown". Used in deletions.
   * 
   * @param pos re-insertion position
   */
  protected void heapifyDownComparator(int pos, Object cur) {
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
  }

  @SuppressWarnings("unchecked")
  protected final E castQueueElement(int n) {
    return (E) queue[n];
  }

  @Override
  public int size() {
    return this.size;
  }

  /**
   * Test whether we need to resize to have the requested capacity.
   * 
   * @param requiredSize required capacity
   */
  private final void resize(int requiredSize) {
    // Double until 64, then increase by 50% each time.
    int newCapacity = ((queue.length < 64) ? ((queue.length + 1) * 2) : ((queue.length / 2) * 3));
    // overflow?
    if(newCapacity < 0) {
      throw new OutOfMemoryError();
    }
    if(requiredSize > newCapacity) {
      newCapacity = requiredSize;
    }
    queue = Arrays.copyOf(queue, newCapacity);
  }

  @Override
  public void clear() {
    // clean up references in the array for memory management
    for(int i = 0; i < size; i++) {
      queue[i] = null;
    }
    this.size = 0;
    modCount++;
  }

  @Override
  public boolean contains(Object o) {
    if(o != null) {
      // TODO: use order to reduce search space?
      for(int i = 0; i < size; i++) {
        if(o.equals(queue[i])) {
          return true;
        }
      }
    }
    return false;
  }

  // TODO: bulk add implementation of addAll?

  @Override
  public Iterator<E> iterator() {
    return new Itr();
  }

  /**
   * Iterator over queue elements. No particular order (i.e. heap order!)
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected final class Itr implements Iterator<E> {
    /**
     * Cursor position
     */
    private int cursor = 0;

    /**
     * Modification counter this iterator is valid for.
     */
    private int expectedModCount = modCount;

    @Override
    public boolean hasNext() {
      return cursor < size;
    }

    @Override
    public E next() {
      if(expectedModCount != modCount) {
        throw new ConcurrentModificationException();
      }
      if(cursor < size) {
        return castQueueElement(cursor++);
      }
      throw new NoSuchElementException();
    }

    @Override
    public void remove() {
      if(expectedModCount != modCount) {
        throw new ConcurrentModificationException();
      }
      if(cursor > 0) {
        cursor--;
      }
      else {
        throw new IllegalStateException();
      }
      expectedModCount = modCount;
    }
  }

  /**
   * Return the heap as a sorted array list, by repeated polling. This will
   * empty the heap!
   * 
   * @return new array list
   */
  public ArrayList<E> toSortedArrayList() {
    ArrayList<E> ret = new ArrayList<E>(size());
    while(!isEmpty()) {
      ret.add(poll());
    }
    return ret;
  }
}