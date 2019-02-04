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
 * Use a {@code ${type}[]} in the ArrayAdapter API.
 *
 * This class is generated from a code template.
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
public class ${classname} implements NumberArrayAdapter<${boxedtype}, ${type}[]> {
  /**
   * Static instance.
   */
  public static final ${classname} STATIC = new ${classname}();

  /**
   * Private constructor - use the static instance in {@link #STATIC}!
   */
  protected ${classname}() {
    super();
  }

  @Override
  public int size(${type}[] array) {
    return array.length;
  }

  @Override
  @Deprecated
  public ${boxedtype} get(${type}[] array, int off) throws IndexOutOfBoundsException {
    return ${boxedtype}.valueOf(array[off]);
  }

  @Override
  public double getDouble(${type}[] array, int off) throws IndexOutOfBoundsException {
    return (double) array[off];
  }

  @Override
  public float getFloat(${type}[] array, int off) throws IndexOutOfBoundsException {
    return (float) array[off];
  }

  @Override
  public int getInteger(${type}[] array, int off) throws IndexOutOfBoundsException {
    return (int) array[off];
  }

  @Override
  public short getShort(${type}[] array, int off) throws IndexOutOfBoundsException {
    return (short) array[off];
  }

  @Override
  public long getLong(${type}[] array, int off) throws IndexOutOfBoundsException {
    return (long) array[off];
  }

  @Override
  public byte getByte(${type}[] array, int off) throws IndexOutOfBoundsException {
    return (byte) array[off];
  }
}
