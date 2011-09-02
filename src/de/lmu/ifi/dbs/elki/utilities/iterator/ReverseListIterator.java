package de.lmu.ifi.dbs.elki.utilities.iterator;

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

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Reverse iterator for lists.
 * 
 * @author Erich Schubert
 * 
 * @param <E> Element type
 */
public class ReverseListIterator<E> implements Iterator<E>, ListIterator<E> {
  /**
   * The actual iterator
   */
  final ListIterator<E> iter;

  /**
   * Constructor.
   * 
   * @param iter List iterator
   */
  public ReverseListIterator(ListIterator<E> iter) {
    this.iter = iter;
  }

  /**
   * Constructor.
   * 
   * @param list Existing list
   */
  public ReverseListIterator(List<E> list) {
    this.iter = list.listIterator(list.size());
  }

  @Override
  public boolean hasNext() {
    return iter.hasPrevious();
  }

  @Override
  public E next() {
    return iter.previous();
  }

  @Override
  public void remove() {
    iter.remove();
  }

  @Override
  public boolean hasPrevious() {
    return iter.hasNext();
  }

  @Override
  public E previous() {
    return iter.next();
  }

  @Override
  public int nextIndex() {
    return iter.previousIndex();
  }

  @Override
  public int previousIndex() {
    return iter.nextIndex();
  }

  @Override
  public void set(E e) {
    iter.set(e);
  }

  @Override
  public void add(E e) {
    iter.add(e);
  }
}