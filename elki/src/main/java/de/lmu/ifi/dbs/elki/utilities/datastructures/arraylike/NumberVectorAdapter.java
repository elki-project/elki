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

import de.lmu.ifi.dbs.elki.data.NumberVector;

/**
 * Adapter to use a feature vector as an array of features.
 * 
 * Use the static instance from {@link ArrayLikeUtil}!
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class NumberVectorAdapter implements NumberArrayAdapter<Number, NumberVector> {
  /**
   * Constructor.
   * 
   * Use the static instance from {@link ArrayLikeUtil}!
   */
  protected NumberVectorAdapter() {
    super();
  }

  @Override
  public int size(NumberVector array) {
    return array.getDimensionality();
  }

  @Override
  @Deprecated
  public Number get(NumberVector array, int off) throws IndexOutOfBoundsException {
    return array.getValue(off + 1);
  }

  @Override
  public double getDouble(NumberVector array, int off) throws IndexOutOfBoundsException {
    return array.doubleValue(off);
  }

  @Override
  public float getFloat(NumberVector array, int off) throws IndexOutOfBoundsException {
    return array.floatValue(off);
  }

  @Override
  public int getInteger(NumberVector array, int off) throws IndexOutOfBoundsException {
    return array.intValue(off);
  }

  @Override
  public short getShort(NumberVector array, int off) throws IndexOutOfBoundsException {
    return array.shortValue(off);
  }

  @Override
  public long getLong(NumberVector array, int off) throws IndexOutOfBoundsException {
    return array.longValue(off);
  }

  @Override
  public byte getByte(NumberVector array, int off) throws IndexOutOfBoundsException {
    return array.byteValue(off);
  }
}
