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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.iterator.MergedIterator;

/**
 * A size-limited heap similar to {@link TopBoundedHeap}, discarding elements
 * with the highest value. However, this variation keeps a list of tied
 * elements.
 * 
 * @author Erich Schubert
 * 
 * @param <E> Object type
 */
public class TiedTopBoundedHeap<E> extends TopBoundedHeap<E> {
  /**
   * List to keep ties in.
   */
  private List<E> ties = new ArrayList<E>();

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

  @SuppressWarnings("unchecked")
  @Override
  public Iterator<E> iterator() {
    return new MergedIterator<E>(ties.iterator(), super.iterator());
  }

  @Override
  public E peek() {
    if (ties.isEmpty()) {
      return super.peek();
    } else {
      return ties.get(ties.size() - 1);
    }
  }

  @Override
  public E poll() {
    if (ties.isEmpty()) {
      return super.poll();
    } else {
      return ties.remove(ties.size() - 1);
    }
  }

  @Override
  public E replaceTopElement(E e) {
    if (ties.isEmpty()) {
      return super.replaceTopElement(e);
    }
    // Fall back to classic emulation via poll and offer:
    E prev = poll();
    add(e);
    return prev;
  }

  @Override
  protected void handleOverflow(E e) {
    boolean tied = false;
    if (comparator == null) {
      @SuppressWarnings("unchecked")
      Comparable<Object> c = (Comparable<Object>) e;
      if (c.compareTo(queue[0]) == 0) {
        tied = true;
      }
    } else {
      if (comparator.compare(e, queue[0]) == 0) {
        tied = true;
      }
    }
    if (tied) {
      ties.add(e);
    } else {
      // Also remove old ties.
      ties.clear();
    }
  }
}
