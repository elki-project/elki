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


/**
 * Empty iterator, that never returns any data.
 * 
 * @author Erich Schubert
 *
 * @param <T> Data type
 */
public final class EmptyIterator<T> implements IterableIterator<T>, Iterator<T>, Iterable<T> {
  @Override
  public boolean hasNext() {
    return false;
  }

  @Override
  public T next() {
    return null;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public IterableIterator<T> iterator() {
    return STATIC();
  }

  /**
   * Static instance
   */
  protected final static EmptyIterator<?> STATIC_INSTANCE = new EmptyIterator<Object>();
  
  /**
   * Access the static instance.
   * 
   * @param <T> type to (not) iterate over
   * @return Cast static instance. 
   */
  @SuppressWarnings("unchecked")
  public static <T> IterableIterator<T> STATIC() {
    return (IterableIterator<T>) STATIC_INSTANCE;
  }
}