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
 * Class to extend an array with a single element virtually.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @param <T> Object type
 */
public class ExtendedArray<T> implements ArrayAdapter<T, ExtendedArray<T>> {
  /**
   * The array
   */
  final Object array;

  /**
   * The array adapter
   */
  final ArrayAdapter<T, Object> getter;

  /**
   * The extra element
   */
  final T extra;

  /**
   * Our size
   */
  final int size;

  /**
   * Constructor.
   * 
   * @param array Original array
   * @param getter Adapter for array
   * @param extra Extra element
   */
  protected ExtendedArray(Object array, ArrayAdapter<T, Object> getter, T extra) {
    super();
    this.array = array;
    this.getter = getter;
    this.extra = extra;
    this.size = getter.size(array) + 1;
  }

  @Override
  public int size(ExtendedArray<T> array) {
    assert (this == array);
    return size;
  }

  @Override
  public T get(ExtendedArray<T> array, int off) throws IndexOutOfBoundsException {
    assert (this == array);
    if(off == size - 1) {
      return extra;
    }
    return getter.get(this.array, off);
  }

  /**
   * Static wrapper that has a nicer generics signature.
   * 
   * @param array Array to extend
   * @param getter Getter for array
   * @param extra Extra element
   * @return Extended array
   */
  @SuppressWarnings("unchecked")
  public static <T, A> ExtendedArray<T> extend(A array, ArrayAdapter<T, A> getter, T extra) {
    return new ExtendedArray<>(array, (ArrayAdapter<T, Object>) getter, extra);
  }
}