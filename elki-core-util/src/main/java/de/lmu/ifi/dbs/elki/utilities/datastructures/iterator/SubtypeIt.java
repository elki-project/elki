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

/**
 * Filtered iterator.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @composed - - - It
 *
 * @param <O> Object type
 */
public class SubtypeIt<O> implements It<O> {
  /**
   * Class filter.
   */
  Class<? super O> filter;

  /**
   * Current object, if valid.
   */
  O current;

  /**
   * Iterator in primary hierarchy.
   */
  private It<?> it;

  /**
   * Constructor.
   *
   * @param it Iterator in primary hierarchy
   * @param clazz Class filter
   */
  public SubtypeIt(It<?> it, Class<? super O> clazz) {
    this.it = it;
    this.filter = clazz;
    this.current = null;
  }

  @Override
  public O get() {
    if(current == null && it.valid()) {
      advance();
    }
    return current;
  }

  @Override
  public boolean valid() {
    if(current == null && it.valid()) {
      advance();
    }
    return current != null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public SubtypeIt<O> advance() {
    current = null;
    while(it.valid()) {
      Object o = it.get();
      it.advance();
      if(filter.isInstance(o)) {
        current = (O) filter.cast(o);
        break;
      }
    }
    return this;
  }
}
