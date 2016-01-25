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
 * @since 0.5.5
 * 
 * @apiviz.has UnsortedIter
 */
public class DoubleLongMinHeap implements DoubleLongHeap {
  /**
   * Base heap.
   */
  protected double[] twoheap;

  /**
   * Base heap values.
   */
  protected long[] twovals;

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
  public DoubleLongMinHeap() {
    super();
    double[] twoheap = new double[TWO_HEAP_INITIAL_SIZE];
    long[] twovals = new long[TWO_HEAP_INITIAL_SIZE];

    this.twoheap = twoheap;
    this.twovals = twovals;
  }

  /**
   * Constructor, with given minimum size.
   * 
   * @param minsize Minimum size
   */
  public DoubleLongMinHeap(int minsize) {
    super();
    final int size = MathUtil.nextPow2Int(minsize + 1) - 1;
    double[] twoheap = new double[size];
    long[] twovals = new long[size];
      
    this.twoheap = twoheap;
    this.twovals = twovals;
  }

  @Override
  public void clear() {
    size = 0;
    Arrays.fill(twoheap, 0.0);
    Arrays.fill(twovals, 0);
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
  public void add(double o, long v) {
    final double co = o;
    final long cv = v;
    // System.err.println("Add: " + o);
    if (size >= twoheap.length) {
      // Grow by one layer.
      twoheap = Arrays.copyOf(twoheap, twoheap.length + twoheap.length + 1);
      twovals = Arrays.copyOf(twovals, twovals.length + twovals.length + 1);
    }
    final int twopos = size;
    twoheap[twopos] = co;
    twovals[twopos] = cv;
    ++size;
    heapifyUp(twopos, co, cv);
  }

  @Override
  public void add(double key, long val, int max) {
    if (size < max) {
      add(key, val);
    } else if (twoheap[0] < key) {
      replaceTopElement(key, val);
    }
  }

  @Override
  public void replaceTopElement(double reinsert, long val) {
    heapifyDown(reinsert, val);
  }

  /**
   * Heapify-Up method for 2-ary heap.
   * 
   * @param twopos Position in 2-ary heap.
   * @param cur Current object
   * @param val Current value
   */
  private void heapifyUp(int twopos, double cur, long val) {
    while (twopos > 0) {
      final int parent = (twopos - 1) >>> 1;
      double par = twoheap[parent];
      if (cur >= par) {
        break;
      }
      twoheap[twopos] = par;
      twovals[twopos] = twovals[parent];
      twopos = parent;
    }
    twoheap[twopos] = cur;
    twovals[twopos] = val;
  }

  @Override
  public void poll() {
    --size;
    // Replacement object:
    if (size > 0) {
      final double reinsert = twoheap[size];
      final long reinsertv = twovals[size];
      twoheap[size] = 0.0;
      twovals[size] = 0;
      heapifyDown(reinsert, reinsertv);
    } else {
      twoheap[0] = 0.0;
      twovals[0] = 0;
    }
  }

  /**
   * Invoke heapify-down for the root object.
   * 
   * @param cur Object to insert.
   * @param val Value to reinsert.
   */
  private void heapifyDown(double cur, long val) {
    final int stop = size >>> 1;
    int twopos = 0;
    while (twopos < stop) {
      int bestchild = (twopos << 1) + 1;
      double best = twoheap[bestchild];
      final int right = bestchild + 1;
      if (right < size && best > twoheap[right]) {
        bestchild = right;
        best = twoheap[right];
      }
      if (cur <= best) {
        break;
      }
      twoheap[twopos] = best;
      twovals[twopos] = twovals[bestchild];
      twopos = bestchild;
    }
    twoheap[twopos] = cur;
    twovals[twopos] = val;
  }

  @Override
  public double peekKey() {
    return twoheap[0];
  }

  @Override
  public long peekValue() {
    return twovals[0];
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append(DoubleLongMinHeap.class.getSimpleName()).append(" [");
    for (UnsortedIter iter = new UnsortedIter(); iter.valid(); iter.advance()) {
      buf.append(iter.getKey()).append(':').append(iter.getValue()).append(',');
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
   * for (DoubleLongHeap.UnsortedIter iter = heap.unsortedIter(); iter.valid(); iter.next()) {
   *   doSomething(iter.get());
   * }
   * }
   * </pre>
   * 
   * @author Erich Schubert
   */
  private class UnsortedIter implements DoubleLongHeap.UnsortedIter {
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

    @Override
    public double getKey() {
      return twoheap[pos];
    }

    @Override
    public long getValue() {
      return twovals[pos];
    }
  }
}
