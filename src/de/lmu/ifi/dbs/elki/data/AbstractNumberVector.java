package de.lmu.ifi.dbs.elki.data;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;

/**
 * AbstractNumberVector is an abstract implementation of FeatureVector.
 * 
 * @author Arthur Zimek
 * 
 * @param <N> the type of number stored in this vector
 */
public abstract class AbstractNumberVector<N extends Number> implements NumberVector<N> {
  /**
   * The String to separate attribute values in a String that represents the
   * values.
   */
  public static final String ATTRIBUTE_SEPARATOR = " ";

  @Override
  public double getMin(int dimension) {
    return doubleValue(dimension);
  }

  @Override
  public double getMax(int dimension) {
    return doubleValue(dimension);
  }

  @Override
  public byte byteValue(int dimension) {
    return (byte) longValue(dimension);
  }

  @Override
  public float floatValue(int dimension) {
    return (float) doubleValue(dimension);
  }

  @Override
  public int intValue(int dimension) {
    return (int) longValue(dimension);
  }

  @Override
  public short shortValue(int dimension) {
    return (short) longValue(dimension);
  }

  /**
   * Factory class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has AbstractNumberVector
   * 
   * @param <V> Vector type
   * @param <N> Number type
   */
  public abstract static class Factory<V extends AbstractNumberVector<N>, N extends Number> implements NumberVector.Factory<V, N> {
    @Override
    public V newNumberVector(double[] values) {
      return newNumberVector(values, ArrayLikeUtil.doubleArrayAdapter());
    }
  }
}
