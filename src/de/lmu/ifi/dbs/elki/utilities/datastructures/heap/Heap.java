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
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import de.lmu.ifi.dbs.elki.math.MathUtil;

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
 * 
 * @param <E> Element type. Should be {@link java.lang.Comparable} or a
 *        {@link java.util.Comparator} needs to be given.
 */
public class Heap<E> implements Iterable<E> {
  /**
   * Heap storage.
   */
  protected transient Object[] queue;

  /**
   * Current number of objects.
   */
  protected int size = 0;

  /**
   * Indicate up to where the heap is valid.
   */
  protected int validSize = 0;

  /**
   * The comparator or {@code null}.
   */
  protected final Comparator<Object> comparator;

  /**
   * (Structural) modification counter. Used to invalidate iterators.
   */
  private transient int modCount = 0;

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
    this.comparator = (Comparator<Object>) comparator;
  }

  /**
   * Add an element to the heap.
   * 
   * @param e Element to add
   */
  public void add(E e) {
    // resize when needed
    if (size + 1 > queue.length) {
      resize(size + 1);
    }
    // final int pos = size;
    this.queue[size] = e;
    this.size += 1;
    heapifyUp(size - 1, e);
    validSize += 1;
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
    ensureValid();
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
    if (size == 0) {
      return null;
    }
    ensureValid();
    return (E) queue[0];
  }

  /**
   * Remove the top element.
   * 
   * @return Top element.
   */
  public E poll() {
    ensureValid();
    return removeAt(0);
  }

  /**
   * Perform pending heap repair operations in a single bulk operation.
   */
  protected void ensureValid() {
    if (validSize != size) {
      if (size > 1) {
        // Bottom up heap update.
        if (comparator != null) {
          // Parent of first invalid
          int nextmin = validSize > 0 ? ((validSize - 1) >>> 1) : 0;
          int curmin = MathUtil.nextAllOnesInt(nextmin); // Next line
          int nextmax = curmin - 1; // End of valid line
          int pos = (size - 2) >>> 1; // Parent of last element
          // System.err.println(validSize+"<="+size+" iter:"+pos+"->"+curmin+", "+nextmin);
          while (pos >= nextmin) {
            // System.err.println(validSize+"<="+size+" iter:"+pos+"->"+curmin);
            while (pos >= curmin) {
              if (!heapifyDownComparator(pos, queue[pos])) {
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
        } else {
          // Parent of first invalid
          int nextmin = validSize > 0 ? ((validSize - 1) >>> 1) : 0;
          int curmin = MathUtil.nextAllOnesInt(nextmin); // Next line
          int nextmax = curmin - 1; // End of valid line
          int pos = (size - 2) >>> 1; // Parent of last element
          // System.err.println(validSize+"<="+size+" iter:"+pos+"->"+curmin+", "+nextmin);
          while (pos >= nextmin) {
            // System.err.println(validSize+"<="+size+" iter:"+pos+"->"+curmin);
            while (pos >= curmin) {
              if (!heapifyDownComparable(pos, queue[pos])) {
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
      }
      validSize = size;
    }
  }

  /**
   * Remove the element at the given position.
   * 
   * @param pos Element position.
   * @return Element that was at this position.
   */
  @SuppressWarnings("unchecked")
  protected E removeAt(int pos) {
    if (pos < 0 || pos >= size) {
      return null;
    }
    final E ret = (E) queue[pos];
    // Replacement object:
    final Object reinsert = queue[size - 1];
    queue[size - 1] = null;
    // Keep heap in sync
    if (validSize == size) {
      size -= 1;
      validSize -= 1;
      heapifyDown(pos, reinsert);
    } else {
      size -= 1;
      validSize = Math.min(pos >>> 1, validSize);
      queue[pos] = reinsert;
    }
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
    if (comparator != null) {
      heapifyUpComparator(pos, elem);
    } else {
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
  protected void heapifyUpComparable(int pos, Object elem) {
    final Comparable<Object> cur = (Comparable<Object>) elem; // queue[pos];
    while (pos > 0) {
      final int parent = (pos - 1) >>> 1;
      Object par = queue[parent];

      if (cur.compareTo(par) >= 0) {
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
  protected void heapifyUpComparator(int pos, Object cur) {
    while (pos > 0) {
      final int parent = (pos - 1) >>> 1;
      Object par = queue[parent];

      if (comparator.compare(cur, par) >= 0) {
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
   * @return true when the order was changed
   */
  protected boolean heapifyDown(int pos, Object reinsert) {
    assert (pos >= 0);
    if (comparator != null) {
      return heapifyDownComparator(pos, reinsert);
    } else {
      return heapifyDownComparable(pos, reinsert);
    }
  }

  /**
   * Execute a "Heapify Downwards" aka "SiftDown". Used in deletions.
   * 
   * @param ipos re-insertion position
   * @param reinsert Object to reinsert
   * @return true when the order was changed
   */
  @SuppressWarnings("unchecked")
  protected boolean heapifyDownComparable(final int ipos, Object reinsert) {
    Comparable<Object> cur = (Comparable<Object>) reinsert;
    int pos = ipos;
    final int half = size >>> 1;
    while (pos < half) {
      // Get left child (must exist!)
      int cpos = (pos << 1) + 1;
      Object child = queue[cpos];
      // Test right child, if present
      final int rchild = cpos + 1;
      if (rchild < size) {
        Object right = queue[rchild];
        if (((Comparable<Object>) child).compareTo(right) > 0) {
          cpos = rchild;
          child = right;
        }
      }

      if (cur.compareTo(child) <= 0) {
        break;
      }
      queue[pos] = child;
      pos = cpos;
    }
    queue[pos] = cur;
    return (pos == ipos);
  }

  /**
   * Execute a "Heapify Downwards" aka "SiftDown". Used in deletions.
   * 
   * @param ipos re-insertion position
   * @param cur Object to reinsert
   * @return true when the order was changed
   */
  protected boolean heapifyDownComparator(final int ipos, Object cur) {
    int pos = ipos;
    final int half = size >>> 1;
    while (pos < half) {
      int min = pos;
      Object best = cur;

      final int lchild = (pos << 1) + 1;
      Object left = queue[lchild];
      if (comparator.compare(best, left) > 0) {
        min = lchild;
        best = left;
      }
      final int rchild = lchild + 1;
      if (rchild < size) {
        Object right = queue[rchild];
        if (comparator.compare(best, right) > 0) {
          min = rchild;
          best = right;
        }
      }
      if (min == pos) {
        break;
      }
      queue[pos] = best;
      pos = min;
    }
    queue[pos] = cur;
    return (pos == ipos);
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
    int newCapacity = ((queue.length < 64) ? ((queue.length + 1) * 2) : ((queue.length / 2) * 3));
    // overflow?
    if (newCapacity < 0) {
      throw new OutOfMemoryError();
    }
    if (requiredSize > newCapacity) {
      newCapacity = requiredSize;
    }
    queue = Arrays.copyOf(queue, newCapacity);
  }

  /**
   * Clear the heap.
   */
  public void clear() {
    // clean up references in the array for memory management
    for (int i = 0; i < size; i++) {
      queue[i] = null;
    }
    this.size = 0;
    this.validSize = -1;
    heapModified();
  }

  @Override
  public Iterator<E> iterator() {
    return new Itr();
  }

  /**
   * Called at the end of each heap modification.
   */
  protected void heapModified() {
    modCount++;
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
     * Cursor position.
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

    @SuppressWarnings("unchecked")
    @Override
    public E next() {
      if (expectedModCount != modCount) {
        throw new ConcurrentModificationException();
      }
      if (cursor < size) {
        return (E) queue[cursor++];
      }
      throw new NoSuchElementException();
    }

    @Override
    public void remove() {
      if (expectedModCount != modCount) {
        throw new ConcurrentModificationException();
      }
      if (cursor > 0) {
        cursor--;
      } else {
        throw new IllegalStateException();
      }
      expectedModCount = modCount;
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
    ensureValid();
    if (comparator == null) {
      for (int i = 1; i < size; i++) {
        final int parent = (i - 1) >>> 1;
        @SuppressWarnings("unchecked")
        Comparable<Object> po = (Comparable<Object>) queue[parent];
        if (po.compareTo(queue[i]) > 0) {
          return "@" + parent + ": " + queue[parent] + " < @" + i + ": " + queue[i];
        }
      }
    } else {
      for (int i = 1; i < size; i++) {
        final int parent = (i - 1) >>> 1;
        if (comparator.compare(queue[parent], queue[i]) > 0) {
          return "@" + parent + ": " + queue[parent] + " < @" + i + ": " + queue[i];
        }
      }
    }
    return null;
  }
}
