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

import java.util.List;

/**
 * ELKI style Iterator for array lists.
 * 
 * Note: this implementation is only efficient for lists with efficient random
 * access and seeking (i.e. ArrayLists, but not Linked Lists!)
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * 
 * @param <O> contained object type.
 */
public class ArrayListIter<O> implements ArrayIter, It<O> {
  /**
   * The array list to iterate over.
   */
  final List<O> data;

  /**
   * Current position.
   */
  int pos = 0;

  /**
   * Constructor.
   * 
   * @param data Data array.
   */
  public ArrayListIter(List<O> data) {
    super();
    this.data = data;
  }

  @Override
  public boolean valid() {
    return pos < data.size() && pos >= 0;
  }

  @Override
  public ArrayListIter<O> advance() {
    pos++;
    return this;
  }

  @Override
  public int getOffset() {
    return pos;
  }

  @Override
  public ArrayListIter<O> advance(int count) {
    pos += count;
    return this;
  }

  @Override
  public ArrayListIter<O> retract() {
    pos--;
    return this;
  }

  @Override
  public ArrayListIter<O> seek(int off) {
    pos = off;
    return this;
  }

  /**
   * Get the current element.
   * 
   * @return current element
   */
  public O get() {
    return data.get(pos);
  }
}
