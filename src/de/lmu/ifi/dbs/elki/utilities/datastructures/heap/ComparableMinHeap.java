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
 * Basic in-memory heap structure.
 * 
 * This heap is built lazily: if you first add many elements, then poll the
 * heap, it will be bulk-loaded in O(n) instead of iteratively built in O(n log
 * n). This is implemented via a simple validTo counter.
 * 
 * @author Erich Schubert
 */
public class ComparableMinHeap<K extends Comparable<K>> extends ObjectHeap<K> {
  /**
   * Constructor with default capacity.
   */
  public ComparableMinHeap() {
    super(DEFAULT_INITIAL_CAPACITY);
  }

  /**
   * Constructor with initial capacity.
   * 
   * @param size initial capacity
   */
  public ComparableMinHeap(int size) {
    super(size);
  }

  /**
   * Compare two objects
   * 
   * @param o1 First object
   * @param o2 Second object
   */
  @Override
  @SuppressWarnings("unchecked")
  protected boolean comp(Object o1, Object o2) {
    return ((K) o1).compareTo((K) o2) > 0;
  }
}
