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

import java.util.function.Predicate;

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
public class FilteredIt<O> implements It<O> {
  /**
   * Class filter.
   */
  Predicate<? super O> filter;

  /**
   * Current object, if valid.
   */
  O current;

  /**
   * Iterator in primary hierarchy.
   */
  private It<O> it;

  /**
   * Constructor.
   *
   * @param it Iterator in primary hierarchy
   * @param clazz Class filter
   */
  public FilteredIt(It<O> it, Predicate<? super O> clazz) {
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

  @Override
  public FilteredIt<O> advance() {
    current = null;
    while(it.valid()) {
      O o = it.get();
      it.advance();
      if(filter.test(o)) {
        current = o;
        break;
      }
    }
    return this;
  }
}
