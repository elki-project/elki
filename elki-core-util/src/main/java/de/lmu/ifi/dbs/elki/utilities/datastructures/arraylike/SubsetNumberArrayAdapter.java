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
 * Subset array adapter (allows reordering and projection)
 * 
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @param <T> Entry type
 * @param <A> Array type
 */
public class SubsetNumberArrayAdapter<T extends Number, A> implements NumberArrayAdapter<T, A> {
  /**
   * Wrapped adapter
   */
  NumberArrayAdapter<T, ? super A> wrapped;

  /**
   * Offsets to return
   */
  int[] offs;
  
  /**
   * Constructor.
   *
   * @param wrapped Wrapped adapter
   * @param offs Offsets
   */
  public SubsetNumberArrayAdapter(NumberArrayAdapter<T, ? super A> wrapped, int[] offs) {
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

  @Override
  public double getDouble(A array, int off) throws IndexOutOfBoundsException {
    return wrapped.getDouble(array, offs[off]);
  }

  @Override
  public float getFloat(A array, int off) throws IndexOutOfBoundsException {
    return wrapped.getFloat(array, offs[off]);
  }

  @Override
  public int getInteger(A array, int off) throws IndexOutOfBoundsException {
    return wrapped.getInteger(array, offs[off]);
  }

  @Override
  public short getShort(A array, int off) throws IndexOutOfBoundsException {
    return wrapped.getShort(array, offs[off]);
  }

  @Override
  public long getLong(A array, int off) throws IndexOutOfBoundsException {
    return wrapped.getLong(array, offs[off]);
  }

  @Override
  public byte getByte(A array, int off) throws IndexOutOfBoundsException {
    return wrapped.getByte(array, offs[off]);
  }
}