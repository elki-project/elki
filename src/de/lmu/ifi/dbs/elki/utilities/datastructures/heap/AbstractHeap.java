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

import java.util.ConcurrentModificationException;

import de.lmu.ifi.dbs.elki.utilities.iterator.Iter;

/**
 * Abstract base class for heaps.
 * 
 * @author Erich Schubert
 */
public class AbstractHeap {
  /**
   * Default initial capacity
   */
  public static final int DEFAULT_INITIAL_CAPACITY = 21;

  /**
   * Current number of objects
   */
  public int size = 0;

  /**
   * (Structural) modification counter. Used to invalidate iterators.
   */
  public int modCount = 0;

  /**
   * Constructor.
   */
  public AbstractHeap() {
    super();
  }

  /**
   * Query the size
   * 
   * @return Size
   */
  public int size() {
    return this.size;
  }

  /**
   * Is the heap empty?
   * 
   * @return {@code true} when the size is 0.
   */
  public boolean isEmpty() {
    return (size == 0);
  }

  /**
   * Delete all elements from the heap.
   */
  public void clear() {
    this.size = 0;
    heapModified();
  }

  /**
   * Test whether we need to resize to have the requested capacity.
   * 
   * @param requiredSize required capacity
   * @param capacity Current capacity
   * @return new capacity
   */
  protected final int desiredSize(int requiredSize, int capacity) {
    // Double until 64, then increase by 50% each time.
    int newCapacity = ((capacity < 64) ? ((capacity + 1) << 1) : ((capacity >> 1) * 3));
    // overflow?
    if (newCapacity < 0) {
      throw new OutOfMemoryError();
    }
    if (requiredSize > newCapacity) {
      newCapacity = requiredSize;
    }
    return newCapacity;
  }

  /**
   * Called at the end of each heap modification.
   */
  protected void heapModified() {
    modCount++;
  }

  /**
   * Unsorted iterator - in heap order. Does not poll the heap.
   * 
   * @author Erich Schubert
   */
  protected abstract class UnsortedIter implements Iter {
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
  }
}
