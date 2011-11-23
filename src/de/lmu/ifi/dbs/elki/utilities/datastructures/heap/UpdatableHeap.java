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

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.custom_hash.TObjectIntCustomHashMap;

import java.util.Comparator;

import de.lmu.ifi.dbs.elki.utilities.datastructures.TroveJavaHashingStrategy;

/**
 * A heap as used in OPTICS that allows updating entries.
 * 
 * @author Erich Schubert
 * 
 * @param <O> object type
 */
public class UpdatableHeap<O> extends Heap<O> {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Holds the indices in the heap of each element.
   */
  private final TObjectIntMap<Object> index = new TObjectIntCustomHashMap<Object>(TroveJavaHashingStrategy.STATIC, 100, 0.5f, -1);

  /**
   * Simple constructor with default size.
   */
  public UpdatableHeap() {
    super();
  }

  /**
   * Constructor with predefined size.
   * 
   * @param size Size
   */
  public UpdatableHeap(int size) {
    super(size);
  }

  /**
   * Constructor with comparator
   * 
   * @param comparator Comparator
   */
  public UpdatableHeap(Comparator<? super O> comparator) {
    super(comparator);
  }

  /**
   * Constructor with predefined size and comparator
   * 
   * @param size Size
   * @param comparator Comparator
   */
  public UpdatableHeap(int size, Comparator<? super O> comparator) {
    super(size, comparator);
  }

  @Override
  public void clear() {
    super.clear();
    index.clear();
  }

  @Override
  public boolean offer(O e) {
    final int pos = index.get(e);
    if(pos == -1) {
      // resize when needed
      if(size + 1 > queue.length) {
        resize(size + 1);
      }
      // final int pos = size;
      this.queue[size] = e;
      index.put(e, size);
      this.size += 1;
      // We do NOT YET update the heap. This is done lazily.
      // We have changed - return true according to {@link Collection#put}
      modCount++;
      return true;
    }
    else {
      // System.err.println("Updating in UpdatableHeap: " + e.toString());
      // assert(queue[pos].equals(e));
      // Did the value improve?
      if(comparator == null) {
        @SuppressWarnings("unchecked")
        Comparable<Object> c = (Comparable<Object>) e;
        if(c.compareTo(queue[pos]) >= 0) {
          // Ignore, but return true according to {@link Collection#put}
          return true;
        }
      }
      else {
        if(comparator.compare(e, queue[pos]) >= 0) {
          // Ignore, but return true according to {@link Collection#put}
          return true;
        }
      }
      if (pos >= validSize) {
        queue[pos] = e;
        // validSize = Math.min(pos, validSize);
      } else {
        // ensureValid();
        heapifyUp(pos, e); 
      }
      modCount++;
      // We have changed - return true according to {@link Collection#put}
      return true;
    }
  }

  @Override
  protected O removeAt(int pos) {
    if(pos < 0 || pos >= size) {
      return null;
    }
    final O ret = castQueueElement(pos);
    // Replacement object:
    final Object reinsert = queue[size - 1];
    queue[size - 1] = null;
    // Keep heap in sync?
    if(validSize == size) {
      size -= 1;
      validSize -= 1;
      heapifyDown(pos, reinsert);
    }
    else {
      size -= 1;
      validSize = Math.min(pos >>> 1, validSize);
      queue[pos] = reinsert;
      index.put(reinsert, pos);
    }
    modCount++;
    // Keep index up to date
    index.remove(ret);
    return ret;
  }

  /**
   * Remove the given object from the queue.
   * 
   * @param e Object to remove
   * @return Existing entry
   */
  public O removeObject(O e) {
    int pos = index.get(e);
    if(pos >= 0) {
      return removeAt(pos);
    }
    else {
      return null;
    }
  }

  @Override
  public O poll() {
    O node = super.poll();
    index.remove(node);
    return node;
  }

  /**
   * Execute a "Heapify Upwards" aka "SiftUp". Used in insertions.
   * 
   * @param pos insertion position
   * @param elem Element to insert
   */
  @SuppressWarnings("unchecked")
  protected void heapifyUpComparable(int pos, O elem) {
    final Comparable<Object> cur = (Comparable<Object>) elem; // queue[pos];
    while(pos > 0) {
      final int parent = (pos - 1) >>> 1;
      Object par = queue[parent];

      if(cur.compareTo(par) >= 0) {
        break;
      }
      queue[pos] = par;
      index.put(par, pos);
      pos = parent;
    }
    queue[pos] = cur;
    index.put(cur, pos);
  }

  /**
   * Execute a "Heapify Upwards" aka "SiftUp". Used in insertions.
   * 
   * @param pos insertion position
   * @param cur Element to insert
   */
  protected void heapifyUpComparator(int pos, O cur) {
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

  @SuppressWarnings("unchecked")
  @Override
  protected boolean heapifyDownComparable(final int ipos, Object reinsert) {
    Comparable<Object> cur = (Comparable<Object>) reinsert;
    int pos = ipos;
    final int half = size >>> 1;
    while(pos < half) {
      // Get left child (must exist!)
      int cpos = (pos << 1) + 1;
      Object child = queue[cpos];
      // Test right child, if present
      final int rchild = cpos + 1;
      if(rchild < size) {
        Object right = queue[rchild];
        if(((Comparable<Object>) child).compareTo(right) > 0) {
          cpos = rchild;
          child = right;
        }
      }

      if(cur.compareTo(child) <= 0) {
        break;
      }
      queue[pos] = child;
      index.put(child, pos);
      pos = cpos;
    }
    queue[pos] = cur;
    index.put(cur, pos);
    return (pos == ipos);
  }

  @Override
  protected boolean heapifyDownComparator(final int ipos, Object cur) {
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
    return (pos == ipos);
  }
}