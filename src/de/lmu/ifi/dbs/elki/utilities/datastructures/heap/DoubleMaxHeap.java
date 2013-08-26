package de.lmu.ifi.dbs.elki.utilities.datastructures.heap;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import de.lmu.ifi.dbs.elki.math.MathUtil;

/**
 * Advanced priority queue class, based on a binary heap (for small sizes),
 * which will for larger heaps be accompanied by a 4-ary heap (attached below
 * the root of the two-ary heap, making the root actually 3-ary).
 * 
 * This code was automatically instantiated for the type: Double
 * 
 * This combination was found to work quite well in benchmarks, but YMMV.
 * 
 * Some other observations from benchmarking:
 * <ul>
 * <li>Bulk loading did not improve things</li>
 * <li>Primitive heaps are substantially faster.</li>
 * <li>Since an array in Java has an overhead of 12 bytes, odd-sized object and
 * integer arrays are actually well aligned both for 2-ary and 4-ary heaps.</li>
 * <li>Workload makes a huge difference. A load-once, poll-until-empty priority
 * queue is something different than e.g. a top-k heap, which will see a lot of
 * top element replacements.</li>
 * <li>Random vs. increasing vs. decreasing vs. sawtooth insertion patterns for
 * top-k make a difference.</li>
 * <li>Different day, different benchmark results ...</li>
 * </ul>
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has UnsortedIter
 */
public class DoubleMaxHeap implements DoubleHeap {
  /**
   * Base heap.
   */
  protected double[] twoheap;

  /**
   * Extension heap.
   */
  protected double[] fourheap;

  /**
   * Current size of heap.
   */
  protected int size;

  /**
   * (Structural) modification counter. Used to invalidate iterators.
   */
  protected int modCount = 0;

  /**
   * Maximum size of the 2-ary heap. A complete 2-ary heap has (2^k-1) elements.
   */
  private final static int TWO_HEAP_MAX_SIZE = (1 << 9) - 1;

  /**
   * Initial size of the 2-ary heap.
   */
  private final static int TWO_HEAP_INITIAL_SIZE = (1 << 5) - 1;

  /**
   * Initial size of 4-ary heap when initialized.
   * 
   * 21 = 4-ary heap of height 2: 1 + 4 + 4*4
   * 
   * 85 = 4-ary heap of height 3: 21 + 4*4*4
   * 
   * 341 = 4-ary heap of height 4: 85 + 4*4*4*4
   * 
   * Since we last grew by 255 (to 511), let's use 341.
   */
  private final static int FOUR_HEAP_INITIAL_SIZE = 341;

  /**
   * Constructor, with default size.
   */
  public DoubleMaxHeap() {
    super();
    double[] twoheap = new double[TWO_HEAP_INITIAL_SIZE];

    this.twoheap = twoheap;
    this.fourheap = null;
    this.size = 0;
    this.modCount = 0;
  }

  /**
   * Constructor, with given minimum size.
   * 
   * @param minsize Minimum size
   */
  public DoubleMaxHeap(int minsize) {
    super();
    if (minsize < TWO_HEAP_MAX_SIZE) {
      final int size = MathUtil.nextPow2Int(minsize + 1) - 1;
      double[] twoheap = new double[size];
      
      this.twoheap = twoheap;
      this.fourheap = null;
    } else {
      double[] twoheap = new double[TWO_HEAP_INITIAL_SIZE];
      double[] fourheap = new double[Math.max(21, minsize - TWO_HEAP_MAX_SIZE)];
      this.twoheap = twoheap;
      this.fourheap = fourheap;
    }
    this.size = 0;
    this.modCount = 0;
  }

  @Override
  public void clear() {
    size = 0;
    ++modCount;
    fourheap = null;
    Arrays.fill(twoheap, 0.0);
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
  public void add(double o) {
    final double co = o;
    // System.err.println("Add: " + o);
    if (size < TWO_HEAP_MAX_SIZE) {
      if (size >= twoheap.length) {
        // Grow by one layer.
        twoheap = Arrays.copyOf(twoheap, twoheap.length + twoheap.length + 1);
      }
      final int twopos = size;
      twoheap[twopos] = co;
      ++size;
      heapifyUp2(twopos, co);
      ++modCount;
    } else {
      final int fourpos = size - TWO_HEAP_MAX_SIZE;
      if (fourheap == null) {
        fourheap = new double[FOUR_HEAP_INITIAL_SIZE];
      } else if (fourpos >= fourheap.length) {
        // Grow extension heap by half.
        fourheap = Arrays.copyOf(fourheap, fourheap.length + (fourheap.length >> 1));
      }
      fourheap[fourpos] = co;
      ++size;
      heapifyUp4(fourpos, co);
      ++modCount;
    }
  }

  @Override
  public void add(double key, int max) {
    if (size < max) {
      add(key);
    } else if (twoheap[0] >= key) {
      replaceTopElement(key);
    }
  }

  @Override
  public double replaceTopElement(double reinsert) {
    final double ret = twoheap[0];
    heapifyDown( reinsert);
    ++modCount;
    return ret;
  }

  /**
   * Heapify-Up method for 2-ary heap.
   * 
   * @param twopos Position in 2-ary heap.
   * @param cur Current object
   */
  private void heapifyUp2(int twopos, double cur) {
    while (twopos > 0) {
      final int parent = (twopos - 1) >>> 1;
      double par = twoheap[parent];
      if (cur <= par) {
        break;
      }
      twoheap[twopos] = par;
      twopos = parent;
    }
    twoheap[twopos] = cur;
  }

  /**
   * Heapify-Up method for 4-ary heap.
   * 
   * @param fourpos Position in 4-ary heap.
   * @param cur Current object
   */
  private void heapifyUp4(int fourpos, double cur) {
    while (fourpos > 0) {
      final int parent = (fourpos - 1) >> 2;
      double par = fourheap[parent];
      if (cur <= par) {
        break;
      }
      fourheap[fourpos] = par;
      fourpos = parent;
    }
    if (fourpos == 0 && twoheap[0] < cur) {
      fourheap[0] = twoheap[0];
      twoheap[0] = cur;
    } else {
      fourheap[fourpos] = cur;
    }
  }

  @Override
  public double poll() {
    final double ret = twoheap[0];
    --size;
    // Replacement object:
    if (size >= TWO_HEAP_MAX_SIZE) {
      final int last = size - TWO_HEAP_MAX_SIZE;
      final double reinsert = fourheap[last];
      fourheap[last] = 0.0;
      heapifyDown(reinsert);
    } else if (size > 0) {
      final double reinsert = twoheap[size];
      twoheap[size] = 0.0;
      heapifyDown(reinsert);
    } else {
      twoheap[0] = 0.0;
    }
    ++modCount;
    return ret;
  }

  /**
   * Invoke heapify-down for the root object.
   * 
   * @param reinsert Object to insert.
   */
  private void heapifyDown(double reinsert) {
    if (size > TWO_HEAP_MAX_SIZE) {
      // Special case: 3-ary situation.
      final int best = (twoheap[1] >= twoheap[2]) ? 1 : 2;
      if (fourheap[0] > twoheap[best]) {
        twoheap[0] = fourheap[0];
        heapifyDown4(0, reinsert);
      } else {
        twoheap[0] = twoheap[best];
        heapifyDown2(best, reinsert);
      }
      return;
    }
    heapifyDown2(0, reinsert);
  }

  /**
   * Heapify-Down for 2-ary heap.
   * 
   * @param twopos Position in 2-ary heap.
   * @param cur Current object
   */
  private void heapifyDown2(int twopos, double cur) {
    final int stop = Math.min(size, TWO_HEAP_MAX_SIZE) >>> 1;
    while (twopos < stop) {
      int bestchild = (twopos << 1) + 1;
      double best = twoheap[bestchild];
      final int right = bestchild + 1;
      if (right < size && best < twoheap[right]) {
        bestchild = right;
        best = twoheap[right];
      }
      if (cur >= best) {
        break;
      }
      twoheap[twopos] = best;
      twopos = bestchild;
    }
    twoheap[twopos] = cur;
  }

  /**
   * Heapify-Down for 4-ary heap.
   * 
   * @param fourpos Position in 4-ary heap.
   * @param cur Current object
   */
  private void heapifyDown4(int fourpos, double cur) {
    final int stop = (size - TWO_HEAP_MAX_SIZE + 2) >>> 2;
    while (fourpos < stop) {
      final int child = (fourpos << 2) + 1;
      double best = fourheap[child];
      int bestchild = child, candidate = child + 1, minsize = candidate + TWO_HEAP_MAX_SIZE;
      if (size > minsize) {
        double nextchild = fourheap[candidate];
        if (best < nextchild) {
          bestchild = candidate;
          best = nextchild;
        }

        minsize += 2;
        if (size >= minsize) {
          nextchild = fourheap[++candidate];
          if (best < nextchild) {
            bestchild = candidate;
            best = nextchild;
          }

          if (size > minsize) {
            nextchild = fourheap[++candidate];
            if (best < nextchild) {
              bestchild = candidate;
              best = nextchild;
            }
          }
        }
      }
      if (cur >= best) {
        break;
      }
      fourheap[fourpos] = best;
      fourpos = bestchild;
    }
    fourheap[fourpos] = cur;
  }

  @Override
  public double peek() {
    return twoheap[0];
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append(DoubleMaxHeap.class.getSimpleName()).append(" [");
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
   * for (DoubleHeap.UnsortedIter iter = heap.unsortedIter(); iter.valid(); iter.next()) {
   *   doSomething(iter.get());
   * }
   * }
   * </pre>
   * 
   * @author Erich Schubert
   */
  private class UnsortedIter implements DoubleHeap.UnsortedIter {
    /**
     * Iterator position.
     */
    protected int pos = 0;

    /**
     * Modification counter we were initialized at.
     */
    protected final int myModCount = modCount;

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

    @Override
    public double get() {
      return ((pos < TWO_HEAP_MAX_SIZE) ? twoheap[pos] : fourheap[pos - TWO_HEAP_MAX_SIZE]);
    }
  }
}
