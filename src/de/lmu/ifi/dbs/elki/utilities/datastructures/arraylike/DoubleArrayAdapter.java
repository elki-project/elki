package de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

/**
 * Use a double array as, well, double array in the ArrayAdapter API.
 * 
 * @author Erich Schubert
 *
 * @apiviz.exclude
 */
class DoubleArrayAdapter implements NumberArrayAdapter<Double, double[]> {
  /**
   * Constructor.
   * 
   * Use the static instance from {@link ArrayLikeUtil}!
   */
  protected DoubleArrayAdapter() {
    super();
  }

  @Override
  public int size(double[] array) {
    return array.length;
  }

  @Override
  public Double get(double[] array, int off) throws IndexOutOfBoundsException {
    return array[off];
  }

  @Override
  public double getDouble(double[] array, int off) throws IndexOutOfBoundsException {
    return array[off];
  }

  @Override
  public float getFloat(double[] array, int off) throws IndexOutOfBoundsException {
    return (float) array[off];
  }

  @Override
  public int getInteger(double[] array, int off) throws IndexOutOfBoundsException {
    return (int) array[off];
  }

  @Override
  public short getShort(double[] array, int off) throws IndexOutOfBoundsException {
    return (short) array[off];
  }

  @Override
  public long getLong(double[] array, int off) throws IndexOutOfBoundsException {
    return (long) array[off];
  }

  @Override
  public byte getByte(double[] array, int off) throws IndexOutOfBoundsException {
    return (byte) array[off];
  }    
}