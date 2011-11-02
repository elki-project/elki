package de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike;

import de.lmu.ifi.dbs.elki.data.NumberVector;

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
 * Adapter to use a feature vector as an array of features.
 * 
 * Use the static instance from {@link ArrayLikeUtil}!
 * 
 * @author Erich Schubert
 * 
 * @param <N> Number type
 */
public class NumberVectorAdapter<N extends Number> implements NumberArrayAdapter<N, NumberVector<?, N>> {
  /**
   * Constructor.
   * 
   * Use the static instance from {@link ArrayLikeUtil}!
   */
  protected NumberVectorAdapter() {
    super();
  }

  @Override
  public int size(NumberVector<?, N> array) {
    return array.getDimensionality();
  }

  @Override
  public N get(NumberVector<?, N> array, int off) throws IndexOutOfBoundsException {
    return array.getValue(off - 1);
  }

  @Override
  public double getDouble(NumberVector<?, N> array, int off) throws IndexOutOfBoundsException {
    return array.doubleValue(off - 1);
  }

  @Override
  public float getFloat(NumberVector<?, N> array, int off) throws IndexOutOfBoundsException {
    return array.floatValue(off - 1);
  }

  @Override
  public int getInteger(NumberVector<?, N> array, int off) throws IndexOutOfBoundsException {
    return array.intValue(off - 1);
  }

  @Override
  public short getShort(NumberVector<?, N> array, int off) throws IndexOutOfBoundsException {
    return array.shortValue(off - 1);
  }

  @Override
  public long getLong(NumberVector<?, N> array, int off) throws IndexOutOfBoundsException {
    return array.longValue(off - 1);
  }

  @Override
  public byte getByte(NumberVector<?, N> array, int off) throws IndexOutOfBoundsException {
    return array.byteValue(off - 1);
  }
}