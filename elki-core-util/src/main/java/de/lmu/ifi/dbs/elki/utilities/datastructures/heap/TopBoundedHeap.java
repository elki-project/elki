/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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
package de.lmu.ifi.dbs.elki.utilities.datastructures.heap;

import java.util.Comparator;

/**
 * Heap class that is bounded in size from the top. It will keep the bottom
 * {@code k} Elements only.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @param <E> Element type. Should be {@link Comparable} or a {@link Comparator}
 *        needs to be given.
 */
public class TopBoundedHeap<E> extends Heap<E> {
  /**
   * Maximum size.
   */
  protected int maxsize;

  /**
   * Constructor with maximum size only.
   * 
   * @param maxsize Maximum size
   */
  public TopBoundedHeap(int maxsize) {
    this(maxsize, null);
  }

  /**
   * Constructor with maximum size and {@link Comparator}.
   * 
   * @param maxsize Maximum size
   * @param comparator Comparator
   */
  public TopBoundedHeap(int maxsize, Comparator<? super E> comparator) {
    super(maxsize + 1, comparator);
    this.maxsize = maxsize;
    assert (maxsize > 0);
  }

  @Override
  public void add(E e) {
    if (super.size() < maxsize) {
      // Just offer.
      super.add(e);
      return;
    }
    // Peek at the top element, return if we are worse.
    final int comp = comparator.compare(e, queue[0]);
    if (comp < 0) {
      return;
    }
    if (comp == 0) {
      handleOverflow(e);
    } else {
      // Otherwise, replace and repair:
      handleOverflow(super.replaceTopElement(e));
    }
  }

  /**
   * Handle an overflow in the structure. This function can be overridden to get
   * overflow treatment.
   * 
   * @param e Overflowing element.
   */
  protected void handleOverflow(E e) {
    // discard extra element
  }

  /**
   * Get the maximum size.
   * 
   * @return the maximum size
   */
  public int getMaxSize() {
    return maxsize;
  }
}
