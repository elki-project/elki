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
 * Dummy iterator/iterable that returns a single object, once.
 * 
 * Note: a "null" object is not supported.
 * 
 * @author Erich Schubert
 *
 * @param <T> Object type to return
 */
public class OneItemIterator<T> implements IterableIterator<T> {
  /**
   * Object to return.
   */
  private T object = null;
  
  /**
   * Constructor.
   * 
   * @param object Object to return once.
   */
  public OneItemIterator(T object) {
    super();
    this.object = object;
  }

  @Override
  public boolean hasNext() {
    return (object != null);
  }

  @Override
  public T next() {
    T ret = object;
    object = null;
    return ret;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<T> iterator() {
    return this;
  }
}