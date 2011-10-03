package de.lmu.ifi.dbs.elki.data;

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

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

import sun.misc.Unsafe;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.persistent.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.datastructures.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * The UnsafeDoubleVector class provides the same precision as the
 * UnsafeDoubleVector class, however is expected to be slightly faster at the
 * cost of reading inappropriate memory and thus producing incorrect results.
 * 
 * @author Arthur Zimek
 * @author Erich Schubert
 */
public class UnsafeDoubleVector extends AbstractNumberVector<UnsafeDoubleVector, Double> implements ByteBufferSerializer<UnsafeDoubleVector> {
  /**
   * Accessor for unsafe memory accesses.
   */
  private static final Unsafe UNSAFE = getUnsafe();

  /**
   * Static factory instance
   */
  public static final UnsafeDoubleVector STATIC = new UnsafeDoubleVector(new double[0], true);

  /**
   * Keeps the values of the real vector
   */
  private double[] values;

  /**
   * Private constructor. NOT for public use.
   */
  private UnsafeDoubleVector(double[] values, boolean nocopy) {
    if(nocopy) {
      this.values = values;
    }
    else {
      this.values = new double[values.length];
      System.arraycopy(values, 0, this.values, 0, values.length);
    }
  }

  /**
   * Use in class initialization to get the unsafe object.
   * 
   * @return Unsafe object
   */
  private static Unsafe getUnsafe() {
    Unsafe unsafe = null;
    try {
      Class<Unsafe> uc = Unsafe.class;
      Field[] fields = uc.getDeclaredFields();
      for(int i = 0; i < fields.length; i++) {
        if(fields[i].getName().equals("theUnsafe")) {
          fields[i].setAccessible(true);
          unsafe = (Unsafe) fields[i].get(uc);
          break;
        }
      }
    }
    catch(Exception ignore) {
    }
    return unsafe;
  }

  /**
   * Test availablility of this class.
   * 
   * @return true when Unsafe operations are available
   */
  public static boolean isAvailable() {
    return (UNSAFE != null);
  }

  /**
   * Provides a feature vector consisting of double values according to the
   * given Double values.
   * 
   * @param values the values to be set as values of the real vector
   */
  public UnsafeDoubleVector(List<Double> values) {
    int i = 0;
    this.values = new double[values.size()];
    for(Iterator<Double> iter = values.iterator(); iter.hasNext(); i++) {
      this.values[i] = (iter.next());
    }
  }

  /**
   * Provides a UnsafeDoubleVector consisting of the given double values.
   * 
   * @param values the values to be set as values of the UnsafeDoubleVector
   */
  public UnsafeDoubleVector(double[] values) {
    this.values = new double[values.length];
    System.arraycopy(values, 0, this.values, 0, values.length);
  }

  /**
   * Provides a UnsafeDoubleVector consisting of the given double values.
   * 
   * @param values the values to be set as values of the UnsafeDoubleVector
   */
  public UnsafeDoubleVector(Double[] values) {
    this.values = new double[values.length];
    for(int i = 0; i < values.length; i++) {
      this.values[i] = values[i];
    }
  }

  @Override
  public int getDimensionality() {
    return values.length;
  }

  /**
   * Returns the value of the specified attribute.
   * 
   * @param dimension the selected attribute. Attributes are counted starting
   *        with 1.
   * 
   * @throws IllegalArgumentException if the specified dimension is out of range
   *         of the possible attributes
   */
  @Override
  public Double getValue(int dimension) {
    long dim = dimension - 1;
    return UNSAFE.getDouble(values, Unsafe.ARRAY_DOUBLE_BASE_OFFSET + dim * Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
  }

  /**
   * Returns the value of the specified attribute.
   * 
   * @param dimension the selected attribute. Attributes are counted starting
   *        with 1.
   * 
   * @throws IllegalArgumentException if the specified dimension is out of range
   *         of the possible attributes
   */
  @Override
  public double doubleValue(int dimension) {
    long dim = dimension - 1;
    return UNSAFE.getDouble(values, Unsafe.ARRAY_DOUBLE_BASE_OFFSET + dim * Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
  }

  /**
   * Returns the value of the specified attribute as long.
   * 
   * @param dimension the selected attribute. Attributes are counted starting
   *        with 1.
   * 
   * @throws IllegalArgumentException if the specified dimension is out of range
   *         of the possible attributes
   */
  @Override
  public long longValue(int dimension) {
    long dim = dimension - 1;
    return (long) UNSAFE.getDouble(values, Unsafe.ARRAY_DOUBLE_BASE_OFFSET + dim * Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
  }

  /**
   * Get a copy of the raw double[] array.
   * 
   * @return copy of values array.
   */
  public double[] getValues() {
    double[] copy = new double[values.length];
    System.arraycopy(values, 0, copy, 0, values.length);
    return copy;
  }

  @Override
  public Vector getColumnVector() {
    // TODO: can we sometimes save this copy?
    // Is this worth the more complex API?
    return new Vector(values.clone());
  }

  @Override
  public Matrix getRowVector() {
    return new Matrix(new double[][] { values.clone() });
  }

  @Override
  public UnsafeDoubleVector plus(UnsafeDoubleVector fv) {
    if(fv.getDimensionality() != this.getDimensionality()) {
      throw new IllegalArgumentException("Incompatible dimensionality: " + this.getDimensionality() + " - " + fv.getDimensionality() + ".");
    }
    double[] values = new double[this.values.length];
    for(int i = 0; i < values.length; i++) {
      values[i] = this.values[i] + fv.values[i];
    }
    return new UnsafeDoubleVector(values, true);
  }

  @Override
  public UnsafeDoubleVector minus(UnsafeDoubleVector fv) {
    if(fv.getDimensionality() != this.getDimensionality()) {
      throw new IllegalArgumentException("Incompatible dimensionality: " + this.getDimensionality() + " - " + fv.getDimensionality() + ".");
    }
    double[] values = new double[this.values.length];
    for(int i = 0; i < values.length; i++) {
      values[i] = this.values[i] - fv.values[i];
    }
    return new UnsafeDoubleVector(values, true);
  }

  @Override
  public UnsafeDoubleVector nullVector() {
    return new UnsafeDoubleVector(new double[this.values.length], true);
  }

  @Override
  public UnsafeDoubleVector negativeVector() {
    return multiplicate(-1);
  }

  @Override
  public UnsafeDoubleVector multiplicate(double k) {
    double[] values = new double[this.values.length];
    for(int i = 0; i < values.length; i++) {
      values[i] = this.values[i] * k;
    }
    return new UnsafeDoubleVector(values, true);
  }

  /**
   * Provides the scalar product (inner product) of this and the given
   * UnsafeDoubleVector.
   * 
   * @param d the UnsafeDoubleVector to compute the scalar product for
   * @return the scalar product (inner product) of this and the given
   *         UnsafeDoubleVector
   */
  @Override
  public Double scalarProduct(UnsafeDoubleVector d) {
    if(this.getDimensionality() != d.getDimensionality()) {
      throw new IllegalArgumentException("Incompatible dimensionality: " + this.getDimensionality() + " - " + d.getDimensionality() + ".");
    }
    double result = 0.0;
    for(int i = 0; i < this.getDimensionality(); i++) {
      result += this.values[i] * d.values[i];
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuffer featureLine = new StringBuffer();
    for(int i = 0; i < values.length; i++) {
      featureLine.append(values[i]);
      if(i + 1 < values.length) {
        featureLine.append(ATTRIBUTE_SEPARATOR);
      }
    }
    return featureLine.toString();
  }

  @Override
  public UnsafeDoubleVector newInstance(Vector values) {
    return new UnsafeDoubleVector(values.getArrayRef());
  }

  @Override
  public UnsafeDoubleVector newInstance(Double[] values) {
    return new UnsafeDoubleVector(values);
  }

  @Override
  public UnsafeDoubleVector newInstance(double[] values) {
    return new UnsafeDoubleVector(values);
  }

  @Override
  public UnsafeDoubleVector newInstance(List<Double> values) {
    return new UnsafeDoubleVector(values);
  }

  @Override
  public <A> UnsafeDoubleVector newInstance(A array, NumberArrayAdapter<?, A> adapter) {
    int dim = adapter.size(array);
    double[] values = new double[dim];
    for(int i = 0; i < dim; i++) {
      values[i] = adapter.getDouble(array, i);
    }
    return new UnsafeDoubleVector(values, true);
  }

  @Override
  public UnsafeDoubleVector fromByteBuffer(ByteBuffer buffer) throws IOException {
    final short dimensionality = buffer.getShort();
    final int len = ByteArrayUtil.SIZE_SHORT + ByteArrayUtil.SIZE_DOUBLE * dimensionality;
    if(buffer.remaining() < len) {
      throw new IOException("Not enough data for a double vector!");
    }
    double[] values = new double[dimensionality];
    buffer.asDoubleBuffer().get(values);
    return new UnsafeDoubleVector(values, false);
  }

  @Override
  public void toByteBuffer(ByteBuffer buffer, UnsafeDoubleVector vec) throws IOException {
    final short dimensionality = buffer.getShort();
    final int len = ByteArrayUtil.SIZE_SHORT + ByteArrayUtil.SIZE_DOUBLE * dimensionality;
    if(buffer.remaining() < len) {
      throw new IOException("Not enough space for the double vector!");
    }
    buffer.putShort(dimensionality);
    buffer.asDoubleBuffer().put(vec.values);
  }

  @Override
  public int getByteSize(UnsafeDoubleVector vec) {
    return ByteArrayUtil.SIZE_SHORT + ByteArrayUtil.SIZE_DOUBLE * vec.getDimensionality();
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected UnsafeDoubleVector makeInstance() {
      if(!isAvailable()) {
        throw new AbortException("UnsafeDoubleVector does not seem to be available!");
      }
      return STATIC;
    }
  }
}