package de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike;

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

import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;

/**
 * Use a matrix as array, by flattening it into a sequence.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @apiviz.exclude
 */
class FlatMatrixAdapter implements NumberArrayAdapter<Double, Matrix> {
  /**
   * Constructor.
   * 
   * Use the static instance from {@link ArrayLikeUtil}!
   */
  protected FlatMatrixAdapter() {
    super();
  }

  @Override
  public int size(Matrix array) {
    return array.getColumnDimensionality() * array.getRowDimensionality();
  }

  @Override
  @Deprecated
  public Double get(Matrix array, int off) throws IndexOutOfBoundsException {
    return Double.valueOf(getDouble(array, off));
  }

  @Override
  public double getDouble(Matrix array, int off) throws IndexOutOfBoundsException {
    return array.get(off / array.getColumnDimensionality(), off % array.getColumnDimensionality());
  }

  @Override
  public float getFloat(Matrix array, int off) throws IndexOutOfBoundsException {
    return (float) getDouble(array, off);
  }

  @Override
  public int getInteger(Matrix array, int off) throws IndexOutOfBoundsException {
    return (int) getDouble(array, off);
  }

  @Override
  public short getShort(Matrix array, int off) throws IndexOutOfBoundsException {
    return (short) getDouble(array, off);
  }

  @Override
  public long getLong(Matrix array, int off) throws IndexOutOfBoundsException {
    return (long) getDouble(array, off);
  }

  @Override
  public byte getByte(Matrix array, int off) throws IndexOutOfBoundsException {
    return (byte) getDouble(array, off);
  }
}
