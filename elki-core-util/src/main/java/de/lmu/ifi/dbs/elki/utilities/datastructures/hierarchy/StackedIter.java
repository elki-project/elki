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
package de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy;

import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.It;

/**
 * Filtered iterator.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @composed - - - Hierarchy
 * @composed - - - It
 *
 * @param <A> Type of first hierarchy
 * @param <B> Type of second level hierarchy
 */
public class StackedIter<B, A extends B> implements It<B> {
  /**
   * Iterator in primary hierarchy.
   */
  private It<? extends A> it1;

  /**
   * Secondary hierarchy.
   */
  private Hierarchy<B> hier2;

  /**
   * Iterator in secondary hierarchy.
   */
  private It<B> it2;

  /**
   * Constructor.
   *
   * @param it1 Iterator in primary hierarchy
   * @param hier2 Iterator in secondary hierarchy
   */
  public StackedIter(It<? extends A> it1, Hierarchy<B> hier2) {
    this.it1 = it1;
    this.hier2 = hier2;
    if(it1.valid()) {
      this.it2 = hier2.iterDescendants(it1.get());
      it1.advance();
    }
    else {
      this.it2 = null;
    }
  }

  @Override
  public B get() {
    return it2.get();
  }

  @Override
  public boolean valid() {
    return it2.valid();
  }

  @Override
  public StackedIter<B, A> advance() {
    if(it2.valid()) {
      it2.advance();
    }
    while(!it2.valid() && it1.valid()) {
      it2 = hier2.iterDescendants(it1.get());
      it1.advance();
    }
    return this;
  }
}