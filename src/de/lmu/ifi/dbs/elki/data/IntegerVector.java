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
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.persistent.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.datastructures.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * An IntegerVector is to store integer values.
 * 
 * @author Erich Schubert
 */
public class IntegerVector extends AbstractNumberVector<IntegerVector, Integer> implements ByteBufferSerializer<IntegerVector> {
  /**
   * Static instance (object factory)
   */
  public static final IntegerVector STATIC = new IntegerVector(new int[0], true);

  /**
   * Keeps the values of the real vector
   */
  private int[] values;

  /**
   * Private constructor. NOT for public use.
   */
  private IntegerVector(int[] values, boolean nocopy) {
    if(nocopy) {
      this.values = values;
    }
    else {
      this.values = new int[values.length];
      System.arraycopy(values, 0, this.values, 0, values.length);
    }
  }

  /**
   * Provides a feature vector consisting of int values according to the
   * given Integer values.
   * 
   * @param values the values to be set as values of the integer vector
   */
  public IntegerVector(List<Integer> values) {
    int i = 0;
    this.values = new int[values.size()];
    for(Iterator<Integer> iter = values.iterator(); iter.hasNext(); i++) {
      this.values[i] = (iter.next());
    }
  }

  /**
   * Provides an IntegerVector consisting of the given integer values.
   * 
   * @param values the values to be set as values of the IntegerVector
   */
  public IntegerVector(int[] values) {
    this.values = new int[values.length];
    System.arraycopy(values, 0, this.values, 0, values.length);
  }

  /**
   * Provides an IntegerVector consisting of the given integer values.
   * 
   * @param values the values to be set as values of the IntegerVector
   */
  public IntegerVector(Integer[] values) {
    this.values = new int[values.length];
    for(int i = 0; i < values.length; i++) {
      this.values[i] = values[i];
    }
  }

  /**
   * Provides an IntegerVector consisting of the given double values.
   * 
   * @param values the values to be set as values of the IntegerVector
   */
  public IntegerVector(double[] values) {
    this.values = new int[values.length];
    for(int i = 0; i < this.values.length; i++) {
      this.values[i] = (int) values[i];
    }
  }

  /**
   * Provides an IntegerVector consisting of the given double vectors values.
   * 
   * @param values the values to be set as values of the IntegerVector
   */
  public IntegerVector(Vector values) {
    this(values.getArrayRef());
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
  public Integer getValue(int dimension) {
    try {
      return values[dimension - 1];
    }
    catch(IndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Dimension " + dimension + " out of range.");
    }
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
    try {
      return values[dimension - 1];
    }
    catch(IndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Dimension " + dimension + " out of range.");
    }
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
    try {
      return values[dimension - 1];
    }
    catch(IndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Dimension " + dimension + " out of range.");
    }
  }

  /**
   * Get a copy of the raw int[] array.
   * 
   * @return copy of values array.
   */
  public int[] getValues() {
    int[] copy = new int[values.length];
    System.arraycopy(values, 0, copy, 0, values.length);
    return copy;
  }

  @Override
  public Vector getColumnVector() {
    double[] data = new double[values.length];
    for(int i = 0; i < values.length; i++) {
      data[i] = values[i];
    }
    return new Vector(data);
  }

  @Override
  public Matrix getRowVector() {
    double[] data = new double[values.length];
    for(int i = 0; i < values.length; i++) {
      data[i] = values[i];
    }
    return new Matrix(new double[][] { data });
  }

  @Override
  public IntegerVector plus(IntegerVector fv) {
    if(fv.getDimensionality() != this.getDimensionality()) {
      throw new IllegalArgumentException("Incompatible dimensionality: " + this.getDimensionality() + " - " + fv.getDimensionality() + ".");
    }
    int[] values = new int[this.values.length];
    for(int i = 0; i < values.length; i++) {
      values[i] = this.values[i] + fv.values[i];
    }
    return new IntegerVector(values, true);
  }

  @Override
  public IntegerVector minus(IntegerVector fv) {
    if(fv.getDimensionality() != this.getDimensionality()) {
      throw new IllegalArgumentException("Incompatible dimensionality: " + this.getDimensionality() + " - " + fv.getDimensionality() + ".");
    }
    int[] values = new int[this.values.length];
    for(int i = 0; i < values.length; i++) {
      values[i] = this.values[i] - fv.values[i];
    }
    return new IntegerVector(values, true);
  }

  @Override
  public IntegerVector nullVector() {
    return new IntegerVector(new int[this.values.length], true);
  }

  @Override
  public IntegerVector negativeVector() {
    return multiplicate(-1);
  }

  @Override
  public IntegerVector multiplicate(double k) {
    int[] values = new int[this.values.length];
    for(int i = 0; i < values.length; i++) {
      values[i] = (int) (this.values[i] * k);
    }
    return new IntegerVector(values, true);
  }

  /**
   * Provides the scalar product (inner product) of this and the given
   * IntegerVector.
   * 
   * @param d the IntegerVector to compute the scalar product for
   * @return the scalar product (inner product) of this and the given
   *         IntegerVector
   */
  @Override
  public Integer scalarProduct(IntegerVector d) {
    if(this.getDimensionality() != d.getDimensionality()) {
      throw new IllegalArgumentException("Incompatible dimensionality: " + this.getDimensionality() + " - " + d.getDimensionality() + ".");
    }
    double result = 0.0;
    for(int i = 0; i < this.getDimensionality(); i++) {
      result += this.values[i] * d.values[i];
    }
    return (int) result;
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
  public IntegerVector newInstance(Vector values) {
    return new IntegerVector(values);
  }

  @Override
  public IntegerVector newInstance(Integer[] values) {
    return new IntegerVector(values);
  }

  @Override
  public IntegerVector newInstance(double[] values) {
    return new IntegerVector(values);
  }

  @Override
  public IntegerVector newInstance(List<Integer> values) {
    return new IntegerVector(values);
  }

  @Override
  public <A> IntegerVector newInstance(A array, NumberArrayAdapter<?, A> adapter) {
    int dim = adapter.size(array);
    int[] values = new int[dim];
    for(int i = 0; i < dim; i++) {
      values[i] = adapter.getInteger(array, i);
    }
    return new IntegerVector(values, true);
  }

  @Override
  public IntegerVector fromByteBuffer(ByteBuffer buffer) throws IOException {
    final short dimensionality = buffer.getShort();
    final int len = ByteArrayUtil.SIZE_SHORT + ByteArrayUtil.SIZE_DOUBLE * dimensionality;
    if(buffer.remaining() < len) {
      throw new IOException("Not enough data for a double vector!");
    }
    int[] values = new int[dimensionality];
    buffer.asIntBuffer().get(values);
    return new IntegerVector(values, false);
  }

  @Override
  public void toByteBuffer(ByteBuffer buffer, IntegerVector vec) throws IOException {
    final short dimensionality = buffer.getShort();
    final int len = ByteArrayUtil.SIZE_SHORT + ByteArrayUtil.SIZE_DOUBLE * dimensionality;
    if(buffer.remaining() < len) {
      throw new IOException("Not enough space for the double vector!");
    }
    buffer.putShort(dimensionality);
    buffer.asIntBuffer().put(vec.values);
  }

  @Override
  public int getByteSize(IntegerVector vec) {
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
    protected IntegerVector makeInstance() {
      return STATIC;
    }
  }
}