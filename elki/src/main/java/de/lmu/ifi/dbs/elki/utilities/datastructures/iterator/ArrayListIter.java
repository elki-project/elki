package de.lmu.ifi.dbs.elki.utilities.datastructures.iterator;

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

import java.util.List;

/**
 * ELKI style Iterator for array lists.
 * 
 * Note: this implementation is only efficient for lists with efficient random
 * access and seeking (i.e. ArrayLists, but not Linked Lists!)
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @apiviz.excludeSubtypes
 * 
 * @param <O> contained object type.
 */
public class ArrayListIter<O> implements ArrayIter {
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
  public ArrayIter advance() {
    pos++;
    return this;
  }

  @Override
  public int getOffset() {
    return pos;
  }

  @Override
  public ArrayIter advance(int count) {
    pos += count;
    return this;
  }

  @Override
  public ArrayIter retract() {
    pos--;
    return this;
  }

  @Override
  public ArrayIter seek(int off) {
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
