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

import java.util.List;

/**
 * Static adapter class to use a {@link java.util.List} in an array of number
 * API.
 * 
 * Use the static instance from {@link ArrayLikeUtil}!
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @param <T> Data object type.
 */
public class NumberListArrayAdapter<T extends Number> implements NumberArrayAdapter<T, List<? extends T>> {
  /**
   * Constructor.
   * 
   * Use the static instance from {@link ArrayLikeUtil}!
   */
  protected NumberListArrayAdapter() {
    super();
  }

  @Override
  public int size(List<? extends T> array) {
    return array.size();
  }

  @Override
  public T get(List<? extends T> array, int off) throws IndexOutOfBoundsException {
    return array.get(off);
  }

  @Override
  public double getDouble(List<? extends T> array, int off) throws IndexOutOfBoundsException {
    return array.get(off).doubleValue();
  }

  @Override
  public float getFloat(List<? extends T> array, int off) throws IndexOutOfBoundsException {
    return array.get(off).floatValue();
  }

  @Override
  public int getInteger(List<? extends T> array, int off) throws IndexOutOfBoundsException {
    return array.get(off).intValue();
  }

  @Override
  public short getShort(List<? extends T> array, int off) throws IndexOutOfBoundsException {
    return array.get(off).shortValue();
  }

  @Override
  public long getLong(List<? extends T> array, int off) throws IndexOutOfBoundsException {
    return array.get(off).longValue();
  }

  @Override
  public byte getByte(List<? extends T> array, int off) throws IndexOutOfBoundsException {
    return array.get(off).byteValue();
  }
}