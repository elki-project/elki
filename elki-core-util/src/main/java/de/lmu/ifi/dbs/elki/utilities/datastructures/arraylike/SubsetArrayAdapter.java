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
package de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike;

/**
 * Subset array adapter (allows reordering and projection).
 * 
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @param <T> Entry type
 * @param <A> Array type
 */
public class SubsetArrayAdapter<T, A> implements ArrayAdapter<T, A> {
  /**
   * Wrapped adapter.
   */
  ArrayAdapter<T, ? super A> wrapped;

  /**
   * Offsets to return.
   */
  int[] offs;
  
  /**
   * Constructor.
   *
   * @param wrapped Wrapped adapter
   * @param offs Offsets
   */
  public SubsetArrayAdapter(ArrayAdapter<T, ? super A> wrapped, int[] offs) {
    super();
    this.wrapped = wrapped;
    this.offs = offs;
  }

  @Override
  public int size(A array) {
    return offs.length;
  }

  @Override
  public T get(A array, int off) throws IndexOutOfBoundsException {
    return wrapped.get(array, offs[off]);
  }
}