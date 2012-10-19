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

/**
 * Abstract base class for heaps.
 * 
 * @author Erich Schubert
 */
public class AbstractHeap {
  /**
   * Default initial capacity
   */
  public static final int DEFAULT_INITIAL_CAPACITY = 11;

  /**
   * Current number of objects
   */
  public int size = 0;

  /**
   * Indicate up to where the heap is valid
   */
  public int validSize = 0;

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
   * Delete all elements from the heap.
   */
  public void clear() {
    this.size = 0;
    this.validSize = -1;
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
    int newCapacity = ((capacity < 64) ? ((capacity + 1) * 2) : ((capacity / 2) * 3));
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
}
