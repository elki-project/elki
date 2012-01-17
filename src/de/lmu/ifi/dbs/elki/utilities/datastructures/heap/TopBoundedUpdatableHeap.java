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

import java.util.Comparator;

/**
 * Heap class that is bounded in size from the top. It will keep the bottom
 * {@code k} Elements only.
 * 
 * @author Erich Schubert
 * 
 * @param <E> Element type. Should be {@link Comparable} or a {@link Comparator}
 *        needs to be given.
 */
public class TopBoundedUpdatableHeap<E> extends UpdatableHeap<E> {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Maximum size
   */
  protected int maxsize;

  /**
   * Constructor with maximum size only.
   * 
   * @param maxsize Maximum size
   */
  public TopBoundedUpdatableHeap(int maxsize) {
    this(maxsize, null);
  }

  /**
   * Constructor with maximum size and {@link Comparator}.
   * 
   * @param maxsize Maximum size
   * @param comparator Comparator
   */
  public TopBoundedUpdatableHeap(int maxsize, Comparator<? super E> comparator) {
    super(maxsize + 1, comparator);
    this.maxsize = maxsize;
    assert (maxsize > 0);
  }

  @Override
  public boolean offerAt(int pos, E e) {
    // don't add if we hit maxsize and are worse
    if(pos == NO_VALUE && super.size() >= maxsize) {
      ensureValid();
      if(compare(e, queue[0]) < 0) {
        // while we did not change, this still was "successful".
        return true;
      }
      pos = index.get(e);
    }
    boolean result = super.offerAt(pos, e);
    // purge unneeded entry(s)
    while(super.size() > maxsize) {
      handleOverflow(super.poll());
    }
    return result;
  }

  /**
   * Test if the priority of an object is higher.
   * 
   * @param e New object
   * @param object Reference object
   * @return True when an update is needed
   */
  protected int compare(Object e, Object object) {
    if(comparator == null) {
      @SuppressWarnings("unchecked")
      Comparable<Object> c = (Comparable<Object>) e;
      return c.compareTo(queue[0]);
    }
    else {
      return comparator.compare(e, queue[0]);
    }
  }

  /**
   * Handle an overflow in the structure. This function can be overridden to get
   * overflow treatment.
   * 
   * @param e Overflowing element.
   */
  protected void handleOverflow(E e) {
    index.remove(e);
  }

  /**
   * @return the maximum size
   */
  public int getMaxSize() {
    return maxsize;
  }
}