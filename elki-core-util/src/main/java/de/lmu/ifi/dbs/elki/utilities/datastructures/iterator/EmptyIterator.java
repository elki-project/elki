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
 * Empty object iterator.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @param <O> Object
 */
public class EmptyIterator<O> implements It<O> {
  /**
   * Static instance.
   */
  private static final It<Object> STATIC = new EmptyIterator<>();

  /**
   * Get an empty hierarchy iterator.
   *
   * @return Empty iterator
   */
  @SuppressWarnings("unchecked")
  public static <O> It<O> empty() {
    return (It<O>) STATIC;
  }

  /**
   * Private constructor, use static {@link #empty()} instead.
   */
  private EmptyIterator() {
    // Use static instance.
  }

  @Override
  public boolean valid() {
    return false;
  }

  @Override
  public It<O> advance() {
    throw new UnsupportedOperationException("Empty iterators must not be advanced.");
  }

  @Override
  public O get() {
    throw new UnsupportedOperationException("Iterator is empty.");
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> It<T> filter(Class<? super T> clz) {
    return (It<T>) this;
  }

  @Override
  public It<O> filter(Predicate<? super O> predicate) {
    return this;
  }

  @Override
  public void forEach(Consumer<? super O> action) {
    // Empty!
  }

}
