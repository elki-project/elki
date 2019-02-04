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

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Object iterator interface. This is the most common case, hence the short
 * class name. Several operations (such as filter and forEach) can only be
 * defined if we know the data type. But for primitive values and, e.g., kNN
 * lists (which have distance and objects), we cannot use this interface.
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @param <O> Object type.
 */
public interface It<O> extends Iter {
  /**
   * Access the current object.
   *
   * @return Current object
   */
  O get();

  @Override
  It<O> advance();

  /**
   * Filtered iteration.
   * 
   * Important: using the filtered iterator will also advance this iterator; so
   * usually you should stop using the original iterator after calling
   * this method.
   * 
   * @param clz Class filter
   * @return Filtered iterator.
   */
  default <T> It<T> filter(Class<? super T> clz) {
    return this.valid() ? new SubtypeIt<>(this, clz) : EmptyIterator.empty();
  }

  /**
   * Filtered iteration.
   * 
   * Important: using the filtered iterator will also advance this iterator; so
   * usually you should stop using the original iterator after calling
   * this method.
   * 
   * @param predicate Test
   * @return Filtered iterator.
   */
  default It<O> filter(Predicate<? super O> predicate) {
    return this.valid() ? new FilteredIt<>(this, predicate) : EmptyIterator.empty();
  }

  /**
   * Find a given object in the iterator; consumes the iterator.
   * 
   * @param o Object to find
   * @return {@code true} if found
   */
  default boolean find(Object o) {
    while(valid()) {
      final O cur = get();
      if(o == cur || o.equals(cur)) {
        return true;
      }
      advance();
    }
    return false;
  }

  /**
   * Process the remaining elements - this will invalidate the iterator.
   *
   * @param action Action to perform
   */
  default void forEach(Consumer<? super O> action) {
    while(valid()) {
      action.accept(get());
      advance();
    }
  }
}
