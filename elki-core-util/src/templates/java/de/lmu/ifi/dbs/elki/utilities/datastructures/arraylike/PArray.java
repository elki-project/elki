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

import java.util.Arrays;

/**
 * Array of ${type} values (primitive, avoiding the boxing overhead of ArrayList<${boxedtype}>).
 *
 * This class is generated from a code template.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class ${classname} implements NumberArrayAdapter<${boxedtype}, ${classname}> {
  /**
   * Maximum array size permitted by Java.
   *
   * This is JVM dependent, but 2^31 - 5 is the usual OpenJDK8 value.
   */
  private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 5;

  /**
   * Last value where we can grow by 50%.
   */
  private static final int LAST_GROW_SIZE = 0x5555_5551;

  // Assert that these values match.
  static {
    assert (LAST_GROW_SIZE + (LAST_GROW_SIZE >>> 1) + 1 <= MAX_ARRAY_SIZE);
  }

  /**
   * (Reused) store for numerical attributes.
   */
  public ${type}[] data;

  /**
   * Number of numerical attributes.
   */
  public int size;

  /**
   * Constructor.
   */
  public ${classname}() {
    this(11);
  }

  /**
   * Constructor.
   *
   * @param initialsize Initial size.
   */
  public ${classname}(int initialsize) {
    if(initialsize < 0) {
      initialsize = 11;
    }
    else if(initialsize > MAX_ARRAY_SIZE) {
      initialsize = MAX_ARRAY_SIZE;
    }
    this.data = new ${type}[initialsize];
    this.size = 0;
  }

  /**
   * Constructor from an existing array.
   *
   * The new array will be allocated as small as possible, so modifications will
   * cause a resize!
   *
   * @param existing Existing array
   */
  public ${classname}(${classname} existing) {
    this.data = Arrays.copyOf(existing.data, existing.size);
    this.size = existing.size;
  }

  /**
   * Reset the numeric attribute counter.
   */
  public void clear() {
    size = 0;
  }

  /**
   * Append a value.
   *
   * @param attribute Attribute value.
   */
  public void add(${type} attribute) {
    if(data.length == size) {
      grow();
    }
    data[size++] = attribute;
  }

  /**
   * Grow the current array.
   *
   * @throws OutOfMemoryError
   */
  private void grow() throws OutOfMemoryError {
    if(data.length == MAX_ARRAY_SIZE) {
      throw new OutOfMemoryError("Array size has reached the Java maximum.");
    }
    final int newsize = (size >= LAST_GROW_SIZE) ? MAX_ARRAY_SIZE : (size + (size >> 1) + 1);
    data = Arrays.copyOf(data, newsize);
  }

  /**
   * Get the value at this position.
   *
   * @param pos Position
   * @return Value
   */
  public ${type} get(int pos) {
    if(pos < 0 || pos >= size) {
      throw new ArrayIndexOutOfBoundsException(pos);
    }
    return data[pos];
  }

  /**
   * Set the value at this position.
   *
   * @param pos Position
   * @param value Value
   */
  public void set(int pos, ${type} value) {
    if(pos < 0 || pos > size) {
      throw new ArrayIndexOutOfBoundsException(pos);
    }
    if(pos == size) {
      add(value);
      return;
    }
    data[pos] = value;
  }

  /**
   * Remove a range from the array.
   *
   * @param start Start
   * @param len Length
   */
  public void remove(int start, int len) {
    final int end = start + len;
    if(end > size) {
      throw new ArrayIndexOutOfBoundsException(size);
    }
    System.arraycopy(data, end, data, start, size - end);
    size -= len;
  }

  /**
   * Insert a value at the given position.
   *
   * @param pos Insert position
   * @param val Value to insert
   */
  public void insert(int pos, ${type} val) {
    if(size == data.length) {
      if(data.length == MAX_ARRAY_SIZE) {
        throw new OutOfMemoryError("Array size has reached the Java maximum.");
      }
      final int newsize = (size >= LAST_GROW_SIZE) ? MAX_ARRAY_SIZE : (size + (size >> 1) + 1);
      final ${type}[] oldd = data;
      data = new ${type}[newsize];
      System.arraycopy(oldd, 0, data, 0, pos);
      System.arraycopy(oldd, pos, data, pos + 1, size - pos);
    }
    else {
      System.arraycopy(data, pos, data, pos + 1, size - pos);
    }
    data[pos] = val;
    size++;
  }

  /**
   * Get the size of the array.
   *
   * @return Size
   */
  public int size() {
    return size;
  }

  /**
   * Test if the array is empty.
   *
   * @return {@code true} if empty.
   */
  public boolean isEmpty() {
    return size == 0;
  }

  /**
   * Sort the contents.
   */
  public void sort() {
    Arrays.sort(data, 0, size);
  }

  // Implement NumberArrayAdapter:
  @Override
  public int size(${classname} array) {
    return array.size;
  }

  @Override
  public ${boxedtype} get(${classname} array, int off) throws IndexOutOfBoundsException {
    return array.data[off];
  }

  @Override
  public double getDouble(${classname} array, int off) throws IndexOutOfBoundsException {
    return (double) array.data[off];
  }

  @Override
  public float getFloat(${classname} array, int off) throws IndexOutOfBoundsException {
    return (float) array.data[off];
  }

  @Override
  public int getInteger(${classname} array, int off) throws IndexOutOfBoundsException {
    return (int) array.data[off];
  }

  @Override
  public short getShort(${classname} array, int off) throws IndexOutOfBoundsException {
    return (short) array.data[off];
  }

  @Override
  public long getLong(${classname} array, int off) throws IndexOutOfBoundsException {
    return (long) array.data[off];
  }

  @Override
  public byte getByte(${classname} array, int off) throws IndexOutOfBoundsException {
    return (byte) array.data[off];
  }

  /**
   * Return a copy of the contents as array.
   *
   * @return Copy of the contents.
   */
  public ${type}[] toArray() {
    return Arrays.copyOf(data, size);
  }
}
