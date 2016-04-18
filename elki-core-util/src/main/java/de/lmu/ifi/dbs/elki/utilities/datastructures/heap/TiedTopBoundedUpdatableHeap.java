package de.lmu.ifi.dbs.elki.utilities.datastructures.heap;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * A size-limited heap similar to {@link TopBoundedHeap}, discarding elements
 * with the highest value. However, this variation keeps a list of tied
 * elements.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @param <E> Object type
 */
public class TiedTopBoundedUpdatableHeap<E> extends TopBoundedUpdatableHeap<E> {
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
  public TiedTopBoundedUpdatableHeap(int maxsize, Comparator<? super E> comparator) {
    super(maxsize, comparator);
  }

  /**
   * Constructor for Comparable objects.
   * 
   * @param maxsize Maximum size of heap (unless tied)
   */
  public TiedTopBoundedUpdatableHeap(int maxsize) {
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
  public void offerAt(int pos, E e) {
    if(pos == IN_TIES) {
      for(Iterator<E> i = ties.iterator(); i.hasNext();) {
        E e2 = i.next();
        if(e.equals(e2)) {
          if(compare(e, e2) <= 0) {
            i.remove();
            index.remove(e2);
          }
          return;
        }
      }
      throw new AbortException("Heap corrupt - should not be reached");
    }
    // Updated object will be worse than the current ties
    if(pos >= 0 && !ties.isEmpty() && compare(e, ties.get(0)) < 0) {
      removeAt(pos);
      index.remove(e);
      // assert(checkHeap() == null) : "removeObject broke heap: "+ checkHeap();
      // Move one object back from ties
      final E e2 = ties.remove(ties.size() - 1);
      // index.remove(e2);
      super.offerAt(NO_VALUE, e2);
      return;
    }
    super.offerAt(pos, e);
  }

  @Override
  public E peek() {
    if(ties.isEmpty()) {
      return super.peek();
    }
    else {
      return ties.get(ties.size() - 1);
    }
  }

  @Override
  public E poll() {
    if(ties.isEmpty()) {
      return super.poll();
    }
    else {
      E e = ties.remove(ties.size() - 1);
      index.remove(e);
      return e;
    }
  }

  @Override
  protected void handleOverflow(E e) {
    boolean tied = false;
    if(comparator == null) {
      @SuppressWarnings("unchecked")
      Comparable<Object> c = (Comparable<Object>) e;
      if(c.compareTo(queue[0]) == 0) {
        tied = true;
      }
    }
    else {
      if(comparator.compare(e, queue[0]) == 0) {
        tied = true;
      }
    }
    if(tied) {
      ties.add(e);
      index.put(e, IN_TIES);
    }
    else {
      index.remove(e);
      // Also remove old ties.
      for(E e2 : ties) {
        index.remove(e2);
      }
      ties.clear();
    }
  }
}