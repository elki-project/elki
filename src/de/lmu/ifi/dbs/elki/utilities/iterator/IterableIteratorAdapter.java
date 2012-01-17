package de.lmu.ifi.dbs.elki.utilities.iterator;

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

import java.util.Iterator;

/**
 * This interface can convert an {@link Iterable} to an {@link Iterator} or the
 * other way round. Note that {@link Iterator} to {@link Iterable} is for
 * single-shot use only. This allows for using an Iterator in for:
 * 
 * <blockquote><pre>{@code
 * for (Type var : new IterableIterator<Type>(iterator)) {
 *   // ...
 * }
 * }</pre></blockquote>
 * 
 * @apiviz.stereotype decorator
 * @apiviz.uses Iterable
 * @apiviz.uses Iterator
 * 
 * @author Erich Schubert
 * @param <T> object type
 */
public final class IterableIteratorAdapter<T> implements IterableIterator<T> {
  /**
   * Parent Iterable
   */
  Iterable<T> parent = null;

  /**
   * Parent Iterator
   */
  Iterator<T> iter = null;

  /**
   * Constructor from an Iterable (preferred).
   * 
   * @param parent Iterable parent
   */
  public IterableIteratorAdapter(Iterable<T> parent) {
    this.parent = parent;
    assert (parent != null);
  }

  /**
   * Constructor from an Iterator.
   * 
   * If possible, wrap an Iterable object.
   * 
   * @param iter Iterator
   */
  public IterableIteratorAdapter(Iterator<T> iter) {
    this.iter = iter;
    assert (iter != null);
  }

  @Override
  public Iterator<T> iterator() {
    if(parent == null) {
      return this;
    }
    return parent.iterator();
  }

  @Override
  public boolean hasNext() {
    if(iter == null) {
      iter = parent.iterator();
    }
    return iter.hasNext();
  }

  @Override
  public T next() {
    if(iter == null) {
      iter = parent.iterator();
    }
    return iter.next();
  }

  @Override
  public void remove() {
    if(iter == null) {
      iter = parent.iterator();
    }
    iter.remove();
  }
}