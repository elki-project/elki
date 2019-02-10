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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A size-limited heap similar to {@link TopBoundedHeap}, discarding elements
 * with the highest value. However, this variation keeps a list of tied
 * elements.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @has - - - UnorderedIter
 * 
 * @param <E> Object type
 */
public class TiedTopBoundedHeap<E> extends TopBoundedHeap<E> {
  /**
   * List to keep ties in.
   */
  private List<E> ties = new ArrayList<>();

  /**
   * Constructor with comparator.
   * 
   * @param maxsize Maximum size of heap (unless tied)
   * @param comparator Comparator
   */
  public TiedTopBoundedHeap(int maxsize, Comparator<? super E> comparator) {
    super(maxsize, comparator);
  }

  /**
   * Constructor for Comparable objects.
   * 
   * @param maxsize Maximum size of heap (unless tied)
   */
  public TiedTopBoundedHeap(int maxsize) {
    this(maxsize, null);
  }

  @Override
  public int size() {
    return super.size() + ties.size();
  }

  @Override
  public void clear() {
    super.clear();
    ties.clear();
  }

  @Override
  public E peek() {
    return ties.isEmpty() ? super.peek() : ties.get(ties.size() - 1);
  }

  @Override
  public E poll() {
    return ties.isEmpty() ? super.poll() : ties.remove(ties.size() - 1);
  }

  @Override
  public E replaceTopElement(E e) {
    if(ties.isEmpty()) {
      return super.replaceTopElement(e);
    }
    // Fall back to classic emulation via poll and offer:
    E prev = poll();
    add(e);
    return prev;
  }

  @Override
  protected void handleOverflow(E e) {
    if(comparator.compare(e, queue[0]) == 0) {
      ties.add(e);
    }
    else {
      // Also remove old ties.
      ties.clear();
    }
  }

  /**
   * Get an unordered heap iterator.
   * 
   * @return Iterator.
   */
  @Override
  public UnorderedIter unorderedIter() {
    return new UnorderedIter();
  }

  /**
   * Unordered heap iterator class.
   * 
   * @author Erich Schubert
   * 
   */
  public class UnorderedIter extends Heap<E>.UnorderedIter {
    /**
     * Constructor.
     */
    protected UnorderedIter() {
      super();
    }

    @Override
    public boolean valid() {
      return pos < size();
    }

    @Override
    public E get() {
      final int ssize = TiedTopBoundedHeap.super.size();
      return pos < ssize ? super.get() : ties.get(pos - ssize);
    }
  }
}
