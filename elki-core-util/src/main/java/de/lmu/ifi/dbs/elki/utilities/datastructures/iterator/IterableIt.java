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
package de.lmu.ifi.dbs.elki.utilities.datastructures.iterator;

import java.util.Iterator;
import java.util.RandomAccess;

/**
 * ELKI style Iterator wrapper for collections.
 * 
 * Note: for collections implementing {@link RandomAccess}, always use
 * {@link ArrayListIter} instead (less wrapping)!
 * 
 * @author Erich Schubert
 * @since 0.7.5
 * 
 * 
 * @param <O> contained object type.
 */
public class IterableIt<O> implements It<O> {
  /**
   * The proxied iterator.
   */
  final Iterator<O> inner;

  /**
   * Current object.
   */
  Object cur;

  /**
   * End sentinel value.
   */
  private static final Object END_VALUE = new Object();

  /**
   * Constructor.
   * 
   * @param data Data array.
   */
  public IterableIt(Iterable<O> data) {
    super();
    this.inner = data.iterator();
    cur = inner.hasNext() ? inner.next() : END_VALUE;
  }

  @Override
  public boolean valid() {
    return cur != END_VALUE;
  }

  @Override
  public It<O> advance() {
    cur = inner.hasNext() ? inner.next() : END_VALUE;
    return this;
  }

  /**
   * Get the current element.
   * 
   * @return current element
   */
  @SuppressWarnings("unchecked")
  public O get() {
    return cur != END_VALUE ? (O) cur : null;
  }
}
