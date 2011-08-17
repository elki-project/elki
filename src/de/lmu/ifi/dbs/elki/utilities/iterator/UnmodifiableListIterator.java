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

import java.util.ListIterator;

/**
 * Iterator proxy that does not allow modifications.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype decorator
 * @apiviz.composedOf ListIterator oneway - - decorates
 * 
 * @param <T>
 */
public final class UnmodifiableListIterator<T> implements ListIterator<T> {
  /**
   * Real iterator
   */
  private ListIterator<T> inner;

  /**
   * Constructor.
   * 
   * @param inner Real iterator to proxy.
   */
  public UnmodifiableListIterator(ListIterator<T> inner) {
    super();
    this.inner = inner;
  }

  @Override
  public boolean hasNext() {
    return inner.hasNext();
  }

  @Override
  public T next() {
    return inner.next();
  }

  @Override
  public boolean hasPrevious() {
    return inner.hasPrevious();
  }

  @Override
  public T previous() {
    return inner.previous();
  }

  @Override
  public int nextIndex() {
    return inner.nextIndex();
  }

  @Override
  public int previousIndex() {
    return inner.previousIndex();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(@SuppressWarnings("unused") T e) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void set(@SuppressWarnings("unused") T e) {
    throw new UnsupportedOperationException();
  }
}