package de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

/**
 * Filtered iterator.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @apiviz.composedOf Hierarchy.Iter
 *
 * @param <O> Object type
 */
public class FilteredIter<O> implements Hierarchy.Iter<O> {
  /**
   * Class filter.
   */
  Class<? super O> filter;

  /**
   * Current object, if valid.
   */
  Object current;

  /**
   * Iterator in primary hierarchy.
   */
  private Hierarchy.Iter<?> it;

  /**
   * Constructor.
   *
   * @param it Iterator in primary hierarchy
   * @param clazz Class filter
   */
  public FilteredIter(Hierarchy.Iter<?> it, Class<? super O> clazz) {
    this.it = it;
    this.filter = clazz;
    this.next();
  }

  @SuppressWarnings("unchecked")
  @Override
  public O get() {
    return (O) current;
  }

  @Override
  public boolean valid() {
    return current != null;
  }

  @Override
  public FilteredIter<O> advance() {
    if(!it.valid()) {
      current = null;
      return this;
    }
    next();
    return this;
  }

  /**
   * Java iterator style, because we need to "peek" the next element.
   */
  private void next() {
    while(it.valid()) {
      Object o = it.get();
      it.advance();
      if(filter.isInstance(o)) {
        current = filter.cast(o);
        return;
      }
    }
    current = null;
  }
}