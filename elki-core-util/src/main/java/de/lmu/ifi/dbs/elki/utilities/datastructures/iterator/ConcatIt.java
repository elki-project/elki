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
 * Concatenate multiple iterators.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @param <O> Data type
 */
public class ConcatIt<O> implements It<O> {
  /**
   * Iterators
   */
  private It<? extends O>[] its;

  /**
   * Current iterator.
   */
  private int it = 0;

  /**
   * Constructor.
   *
   * @param its Iterators to concatenate
   */
  @SafeVarargs
  public ConcatIt(It<? extends O>... its) {
    this.its = its;
  }

  @Override
  public boolean valid() {
    while(it < its.length) {
      if(its[it].valid()) {
        return true;
      }
      ++it;
    }
    return false;
  }

  @Override
  public O get() {
    return its[it].get();
  }

  @Override
  public It<O> advance() {
    its[it].advance();
    return this;
  }
}
