package de.lmu.ifi.dbs.elki.utilities.datastructures.heap;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
 * Binary heap for primitive types.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @apiviz.has UnsortedIter
 * @param <K> Key type
 */
public class ComparatorMaxHeap<K> implements ObjectHeap<K> {
  /**
   * Base heap.
   */
  protected Object[] twoheap;

  /**
   * Current size of heap.
   */
  protected int size;

  /**
   * Initial size of the 2-ary heap.
   */
  private final static int TWO_HEAP_INITIAL_SIZE = (1 << 5) - 1;


  /**
   * Comparator
   */
  protected java.util.Comparator<Object> comparator;

  /**
   * Constructor, with default size.
   * @param comparator Comparator
   */
  @SuppressWarnings("unchecked")
  public ComparatorMaxHeap(java.util.Comparator<? super K> comparator) {
    super();
    this.comparator = (java.util.Comparator<Object>) java.util.Comparator.class.cast(comparator);
    Object[] twoheap = new Object[TWO_HEAP_INITIAL_SIZE];

    this.twoheap = twoheap;
  }

  /**
   * Constructor, with given minimum size.
   *
   * @param minsize Minimum size
   * @param comparator Comparator
   */
  @SuppressWarnings("unchecked")
  public ComparatorMaxHeap(int minsize, java.util.Comparator<? super K> comparator) {
    super();
    this.comparator = (java.util.Comparator<Object>) java.util.Comparator.class.cast(comparator);
    final int size = MathUtil.nextPow2Int(minsize + 1) - 1;
    Object[] twoheap = new Object[size];

    this.twoheap = twoheap;
  }

  @Override
  public void clear() {
    size = 0;
    Arrays.fill(twoheap, null);
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean isEmpty() {
    return (size == 0);
  }

  @Override
  public void add(K o) {
    final Object co = o;
    // System.err.println("Add: " + o);
    if (size >= twoheap.length) {
      // Grow by one layer.
      twoheap = Arrays.copyOf(twoheap, twoheap.length + twoheap.length + 1);
    }
    final int twopos = size;
    twoheap[twopos] = co;
    ++size;
    heapifyUp(twopos, co);
  }

  @Override
  public void add(K key, int max) {
    if (size < max) {
      add(key);
    } else if (comparator.compare(twoheap[0], key) > 0) {
      replaceTopElement(key);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public K replaceTopElement(K reinsert) {
    final Object ret = twoheap[0];
    heapifyDown( reinsert);
    return (K)ret;
  }

  /**
   * Heapify-Up method for 2-ary heap.
   *
   * @param twopos Position in 2-ary heap.
   * @param cur Current object
   */
  private void heapifyUp(int twopos, Object cur) {
    while (twopos > 0) {
      final int parent = (twopos - 1) >>> 1;
      Object par = twoheap[parent];
      if (comparator.compare(cur, par) <= 0) {
        break;
      }
      twoheap[twopos] = par;
      twopos = parent;
    }
    twoheap[twopos] = cur;
  }

  @Override
  @SuppressWarnings("unchecked")
  public K poll() {
    final Object ret = twoheap[0];
    --size;
    // Replacement object:
    if (size > 0) {
      final Object reinsert = twoheap[size];
      twoheap[size] = null;
      heapifyDown(reinsert);
    } else {
      twoheap[0] = null;
    }
    return (K)ret;
  }

  /**
   * Invoke heapify-down for the root object.
   *
   * @param cur Object to insert.
   */
  private void heapifyDown(Object cur) {
    final int stop = size >>> 1;
    int twopos = 0;
    while (twopos < stop) {
      int bestchild = (twopos << 1) + 1;
      Object best = twoheap[bestchild];
      final int right = bestchild + 1;
      if (right < size && comparator.compare(best, twoheap[right]) < 0) {
        bestchild = right;
        best = twoheap[right];
      }
      if (comparator.compare(cur, best) >= 0) {
        break;
      }
      twoheap[twopos] = best;
      twopos = bestchild;
    }
    twoheap[twopos] = cur;
  }

  @Override
  @SuppressWarnings("unchecked")
  public K peek() {
    return (K)twoheap[0];
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append(ComparatorMaxHeap.class.getSimpleName()).append(" [");
    for (UnsortedIter iter = new UnsortedIter(); iter.valid(); iter.advance()) {
      buf.append(iter.get()).append(',');
    }
    buf.append(']');
    return buf.toString();
  }

  @Override
  public UnsortedIter unsortedIter() {
    return new UnsortedIter();
  }

  /**
   * Unsorted iterator - in heap order. Does not poll the heap.
   *
   * Use this class as follows:
   *
   * <pre>
   * {@code
   * for (ObjectHeap.UnsortedIter<K> iter = heap.unsortedIter(); iter.valid(); iter.next()) {
   *   doSomething(iter.get());
   * }
   * }
   * </pre>
   *
   * @author Erich Schubert
   */
  private class UnsortedIter implements ObjectHeap.UnsortedIter<K> {
    /**
     * Iterator position.
     */
    protected int pos = 0;

    @Override
    public boolean valid() {
      return pos < size;
    }

    @Override
    public de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.Iter advance() {
      pos++;
      return this;
    }

    @SuppressWarnings("unchecked")

    @Override
    public K get() {
      return (K)twoheap[pos];
    }
  }
}
