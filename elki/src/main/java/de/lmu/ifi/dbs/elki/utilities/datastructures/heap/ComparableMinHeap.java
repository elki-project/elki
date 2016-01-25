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
public class ComparableMinHeap<K extends Comparable<? super K>> implements ObjectHeap<K> {
  /**
   * Base heap.
   */
  protected Comparable<Object>[] twoheap;

  /**
   * Current size of heap.
   */
  protected int size;

  /**
   * Initial size of the 2-ary heap.
   */
  private final static int TWO_HEAP_INITIAL_SIZE = (1 << 5) - 1;

  /**
   * Constructor, with default size.
   */
  @SuppressWarnings("unchecked")
  public ComparableMinHeap() {
    super();
    Comparable<Object>[] twoheap = (Comparable<Object>[]) java.lang.reflect.Array.newInstance(Comparable.class, TWO_HEAP_INITIAL_SIZE);

    this.twoheap = twoheap;
  }

  /**
   * Constructor, with given minimum size.
   * 
   * @param minsize Minimum size
   */
  @SuppressWarnings("unchecked")
  public ComparableMinHeap(int minsize) {
    super();
    final int size = MathUtil.nextPow2Int(minsize + 1) - 1;
    Comparable<Object>[] twoheap = (Comparable<Object>[]) java.lang.reflect.Array.newInstance(Comparable.class, size);
      
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
  @SuppressWarnings("unchecked")
  public void add(K o) {
    final Comparable<Object> co = (Comparable<Object>)o;
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
    } else if (twoheap[0].compareTo(key) < 0) {
      replaceTopElement(key);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public K replaceTopElement(K reinsert) {
    final Comparable<Object> ret = twoheap[0];
    heapifyDown((Comparable<Object>) reinsert);
    return (K)ret;
  }

  /**
   * Heapify-Up method for 2-ary heap.
   * 
   * @param twopos Position in 2-ary heap.
   * @param cur Current object
   */
  private void heapifyUp(int twopos, Comparable<Object> cur) {
    while (twopos > 0) {
      final int parent = (twopos - 1) >>> 1;
      Comparable<Object> par = twoheap[parent];
      if (cur.compareTo(par) >= 0) {
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
    final Comparable<Object> ret = twoheap[0];
    --size;
    // Replacement object:
    if (size > 0) {
      final Comparable<Object> reinsert = twoheap[size];
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
  private void heapifyDown(Comparable<Object> cur) {
    final int stop = size >>> 1;
    int twopos = 0;
    while (twopos < stop) {
      int bestchild = (twopos << 1) + 1;
      Comparable<Object> best = twoheap[bestchild];
      final int right = bestchild + 1;
      if (right < size && best.compareTo(twoheap[right]) > 0) {
        bestchild = right;
        best = twoheap[right];
      }
      if (cur.compareTo(best) <= 0) {
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
    buf.append(ComparableMinHeap.class.getSimpleName()).append(" [");
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
