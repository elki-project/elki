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
package de.lmu.ifi.dbs.elki.data;

import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Specialized class implementing a one-dimensional double vector without using
 * an array. Saves a little bit of memory, albeit we cannot avoid boxing as long
 * as we want to implement the interface.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public class OneDimensionalDoubleVector implements NumberVector {
  /**
   * Static factory instance.
   */
  public static final OneDimensionalDoubleVector.Factory STATIC = new OneDimensionalDoubleVector.Factory();

  /**
   * The actual data value.
   */
  double val;

  /**
   * Constructor.
   * 
   * @param val Value
   */
  public OneDimensionalDoubleVector(double val) {
    this.val = val;
  }

  @Override
  public int getDimensionality() {
    return 1;
  }

  @Override
  public double doubleValue(int dimension) {
    assert (dimension == 0) : "Non-existant dimension accessed.";
    return val;
  }

  @Override
  public long longValue(int dimension) {
    assert (dimension == 0) : "Non-existant dimension accessed.";
    return (long) val;
  }

  @Override
  public double[] toArray() {
    return new double[] { val };
  }

  /**
   * Factory class.
   * 
   * @author Erich Schubert
   * 
   * @has - - - OneDimensionalDoubleVector
   */
  public static class Factory implements NumberVector.Factory<OneDimensionalDoubleVector> {
    @Override
    public <A> OneDimensionalDoubleVector newFeatureVector(A array, ArrayAdapter<? extends Number, A> adapter) {
      assert (adapter.size(array) == 1) : "Incorrect dimensionality for 1-dimensional vector.";
      return new OneDimensionalDoubleVector(adapter.get(array, 0).doubleValue());
    }

    @Override
    public <A> OneDimensionalDoubleVector newNumberVector(A array, NumberArrayAdapter<?, ? super A> adapter) {
      assert (adapter.size(array) == 1) : "Incorrect dimensionality for 1-dimensional vector.";
      return new OneDimensionalDoubleVector(adapter.getDouble(array, 0));
    }

    @Override
    public ByteBufferSerializer<OneDimensionalDoubleVector> getDefaultSerializer() {
      // FIXME: add a serializer
      return null;
    }

    @Override
    public Class<? super OneDimensionalDoubleVector> getRestrictionClass() {
      return OneDimensionalDoubleVector.class;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     */
    public static class Parameterizer extends AbstractParameterizer {
      @Override
      protected OneDimensionalDoubleVector.Factory makeInstance() {
        return STATIC;
      }
    }
  }
}
