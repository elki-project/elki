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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.persistent.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * A FloatVector is to store real values approximately as float values.
 * 
 * @author Elke Achtert
 */
public class FloatVector extends AbstractNumberVector<FloatVector, Float> implements ByteBufferSerializer<FloatVector> {
  /**
   * Static factory instance
   */
  public static final FloatVector STATIC = new FloatVector(new float[0], true);

  /**
   * Keeps the values of the float vector
   */
  private float[] values;

  /**
   * Private constructor. NOT for public use.
   */
  private FloatVector(float[] values, boolean nocopy) {
    if(nocopy) {
      this.values = values;
    }
    else {
      this.values = new float[values.length];
      System.arraycopy(values, 0, this.values, 0, values.length);
    }
  }

  /**
   * Provides a FloatVector consisting of float values according to the given
   * Float values.
   * 
   * @param values the values to be set as values of the float vector
   */
  public FloatVector(List<Float> values) {
    int i = 0;
    this.values = new float[values.size()];
    for(Iterator<Float> iter = values.iterator(); iter.hasNext(); i++) {
      this.values[i] = (iter.next());
    }
  }

  /**
   * Provides a FloatVector consisting of the given float values.
   * 
   * @param values the values to be set as values of the float vector
   */
  public FloatVector(float[] values) {
    this.values = new float[values.length];
    System.arraycopy(values, 0, this.values, 0, values.length);
  }

  /**
   * Provides a FloatVector consisting of the given float values.
   * 
   * @param values the values to be set as values of the float vector
   */
  public FloatVector(Float[] values) {
    this.values = new float[values.length];
    for(int i = 0; i < values.length; i++) {
      this.values[i] = values[i];
    }
  }

  /**
   * Expects a matrix of one column.
   * 
   * @param columnMatrix a matrix of one column
   */
  public FloatVector(Vector columnMatrix) {
    values = new float[columnMatrix.getRowDimensionality()];
    for(int i = 0; i < values.length; i++) {
      values[i] = (float) columnMatrix.get(i, 0);
    }
  }

  @Override
  public int getDimensionality() {
    return values.length;
  }

  @Override
  public Float getValue(int dimension) {
    try {
      return values[dimension - 1];
    }
    catch(ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Dimension " + dimension + " out of range.");
    }
  }

  @Override
  public double doubleValue(int dimension) {
    try {
      return values[dimension - 1];
    }
    catch(ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Dimension " + dimension + " out of range.");
    }
  }

  @Override
  public long longValue(int dimension) {
    try {
      return (long) values[dimension - 1];
    }
    catch(ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Dimension " + dimension + " out of range.");
    }
  }

  @Override
  public Vector getColumnVector() {
    return new Vector(ArrayLikeUtil.toPrimitiveDoubleArray(values,  ArrayLikeUtil.FLOATARRAYADAPTER));
  }

  @Override
  public Matrix getRowVector() {
    return new Matrix(new double[][] { ArrayLikeUtil.toPrimitiveDoubleArray(values,  ArrayLikeUtil.FLOATARRAYADAPTER) });
  }

  @Override
  public FloatVector plus(FloatVector fv) {
    if(fv.getDimensionality() != this.getDimensionality()) {
      throw new IllegalArgumentException("Incompatible dimensionality: " + this.getDimensionality() + " - " + fv.getDimensionality() + ".");
    }
    float[] values = new float[this.values.length];
    for(int i = 0; i < values.length; i++) {
      values[i] = this.values[i] + fv.getValue(i + 1);
    }
    return new FloatVector(values, true);
  }

  @Override
  public FloatVector minus(FloatVector fv) {
    if(fv.getDimensionality() != this.getDimensionality()) {
      throw new IllegalArgumentException("Incompatible dimensionality: " + this.getDimensionality() + " - " + fv.getDimensionality() + ".");
    }
    float[] values = new float[this.values.length];
    for(int i = 0; i < values.length; i++) {
      values[i] = this.values[i] - fv.getValue(i + 1);
    }
    return new FloatVector(values, true);
  }

  /**
   * Provides the scalar product (inner product) of this and the given
   * FloatVector.
   * 
   * @param f the FloatVector to compute the scalar product for
   * @return the scalar product (inner product) of this and the given
   *         FloatVector
   */
  @Override
  public Float scalarProduct(FloatVector f) {
    if(this.getDimensionality() != f.getDimensionality()) {
      throw new IllegalArgumentException("Incompatible dimensionality: " + this.getDimensionality() + " - " + f.getDimensionality() + ".");
    }
    float result = 0.0f;
    for(int i = 0; i < this.getDimensionality(); i++) {
      result += this.values[i] * f.values[i];
    }
    return result;
  }

  @Override
  public FloatVector multiplicate(double k) {
    float[] values = new float[this.values.length];
    for(int i = 0; i < values.length; i++) {
      values[i] = (float) (this.values[i] * k);
    }
    return new FloatVector(values, true);
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
  public <A> FloatVector newFeatureVector(A array, ArrayAdapter<Float, A> adapter) {
    int dim = adapter.size(array);
    float[] values = new float[dim];
    for(int i = 0; i < dim; i++) {
      values[i] = adapter.get(array, i);
    }
    return new FloatVector(values, true);
  }

  @Override
  public <A> FloatVector newNumberVector(A array, NumberArrayAdapter<?, A> adapter) {
    int dim = adapter.size(array);
    float[] values = new float[dim];
    for(int i = 0; i < dim; i++) {
      values[i] = adapter.getFloat(array, i);
    }
    return new FloatVector(values, true);
  }

  @Override
  public FloatVector fromByteBuffer(ByteBuffer buffer) throws IOException {
    final short dimensionality = buffer.getShort();
    final int len = ByteArrayUtil.SIZE_SHORT + ByteArrayUtil.SIZE_FLOAT * dimensionality;
    if(buffer.remaining() < len) {
      throw new IOException("Not enough data for a float vector!");
    }
    // read the values
    float[] values = new float[dimensionality];
    buffer.asFloatBuffer().get(values);
    return new FloatVector(values, false);
  }

  @Override
  public void toByteBuffer(ByteBuffer buffer, FloatVector vec) throws IOException {
    final short dimensionality = buffer.getShort();
    final int len = getByteSize(vec);
    if(buffer.remaining() < len) {
      throw new IOException("Not enough space for the float vector!");
    }
    // write dimensionality
    buffer.putShort(dimensionality);
    buffer.asFloatBuffer().put(vec.values);
  }

  @Override
  public int getByteSize(FloatVector vec) {
    return ByteArrayUtil.SIZE_SHORT + ByteArrayUtil.SIZE_FLOAT * vec.getDimensionality();
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
    protected FloatVector makeInstance() {
      return STATIC;
    }
  }
}