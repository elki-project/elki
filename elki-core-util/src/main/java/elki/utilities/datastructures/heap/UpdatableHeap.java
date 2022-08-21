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

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * A heap as used in OPTICS that allows updating entries.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @param <O> object type
 */
public class UpdatableHeap<O> extends Heap<O> {
  /**
   * Constant for "not in heap".
   */
  protected static final int NO_VALUE = Integer.MIN_VALUE;

  /**
   * Constant for "in ties list", for tied heaps.
   */
  protected static final int IN_TIES = -1;

  /**
   * Holds the indices in the heap of each element.
   */
  protected final Object2IntOpenHashMap<Object> index;

  /**
   * Simple constructor with default size.
   */
  public UpdatableHeap() {
    super();
    index = new Object2IntOpenHashMap<>();
    index.defaultReturnValue(NO_VALUE);
  }

  /**
   * Constructor with predefined size.
   *
   * @param size Size
   */
  public UpdatableHeap(int size) {
    super(size);
    index = new Object2IntOpenHashMap<>();
    index.defaultReturnValue(NO_VALUE);
  }

  /**
   * Constructor with comparator.
   *
   * @param comparator Comparator
   */
  public UpdatableHeap(Comparator<? super O> comparator) {
    super(comparator);
    index = new Object2IntOpenHashMap<>();
    index.defaultReturnValue(NO_VALUE);
  }

  /**
   * Constructor with predefined size and comparator.
   *
   * @param size Size
   * @param comparator Comparator
   */
  public UpdatableHeap(int size, Comparator<? super O> comparator) {
    super(size, comparator);
    index = new Object2IntOpenHashMap<>();
    index.defaultReturnValue(NO_VALUE);
  }

  @Override
  public void clear() {
    super.clear();
    index.clear();
  }

  @Override
  public void add(O e) {
    offerAt(index.getInt(e), e);
  }

  /**
   * Offer element at the given position.
   *
   * @param pos Position
   * @param e Element
   */
  protected void offerAt(final int pos, O e) {
    if(pos == NO_VALUE) {
      // resize when needed
      if(size + 1 > queue.length) {
        queue = Arrays.copyOf(queue, HeapUtil.nextSize(queue.length));
      }
      index.put(e, size);
      size++;
      heapifyUp(size - 1, e);
      heapModified();
      return;
    }
    assert (pos >= 0) : "Unexpected negative position.";
    assert (queue[pos].equals(e));
    // Did the value improve?
    if(comparator.compare(e, queue[pos]) >= 0) {
      return;
    }
    heapifyUp(pos, e);
    heapModified();
  }

  @SuppressWarnings("unchecked")
  @Override
  public O poll() {
    if(size == 0) {
      return null;
    }
    final Object ret = queue[0];
    if(--size > 0) {
      final Object reinsert = queue[size];
      queue[size] = null;
      heapifyDown(0, reinsert);
      // Keep index up to date
      index.removeInt(ret);
    }
    heapModified();
    return (O) ret;
  }

  /**
   * Execute a "Heapify Upwards" aka "SiftUp". Used in insertions.
   *
   * @param pos insertion position
   * @param cur Element to insert
   */
  @Override
  protected void heapifyUp(int pos, Object cur) {
    while(pos > 0) {
      final int parent = (pos - 1) >>> 1;
      Object par = queue[parent];

      if(comparator.compare(cur, par) >= 0) {
        break;
      }
      queue[pos] = par;
      index.put(par, pos);
      pos = parent;
    }
    queue[pos] = cur;
    index.put(cur, pos);
  }

  @Override
  protected boolean heapifyDown(final int ipos, Object cur) {
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
      index.put(best, pos);
      pos = min;
    }
    queue[pos] = cur;
    index.put(cur, pos);
    return (pos != ipos);
  }
}
