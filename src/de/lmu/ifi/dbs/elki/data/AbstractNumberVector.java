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

import de.lmu.ifi.dbs.elki.utilities.Util;
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

  /**
   * An Object obj is equal to this AbstractNumberVector if it is an instance of
   * the same runtime class and is of the identical dimensionality and the
   * values of this AbstractNumberVector are equal to the values of obj in all
   * dimensions, respectively.
   * 
   * @param obj another Object
   * @return true if the specified Object is an instance of the same runtime
   *         class and is of the identical dimensionality and the values of this
   *         AbstractNumberVector are equal to the values of obj in all
   *         dimensions, respectively
   */
  @Override
  public boolean equals(Object obj) {
    if (this.getClass().isInstance(obj)) {
      AbstractNumberVector<?> rv = (AbstractNumberVector<?>) obj;
      boolean equal = (this.getDimensionality() == rv.getDimensionality());
      for (int i = 1; i <= getDimensionality() && equal; i++) {
        equal &= this.getValue(i).equals(rv.getValue(i));
      }
      return equal;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    if (this.getDimensionality() == 0) {
      return 0;
    }
    int hash = this.getValue(1).hashCode();
    for (int i = 2; i <= this.getDimensionality(); i++) {
      hash = Util.mixHashCodes(hash, this.getValue(i).hashCode());
    }
    return hash;
  }

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
