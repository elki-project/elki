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

/**
 * A DoubleVector is to store real values approximately as double values.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.landmark
 */
public class DoubleVector extends AbstractNumberVector<DoubleVector, Double> implements ByteBufferSerializer<DoubleVector> {
  /**
   * Keeps the values of the real vector
   */
  private double[] values;

  /**
   * Private constructor. NOT for public use.
   */
  private DoubleVector(double[] values, boolean nocopy) {
    if(nocopy) {
      this.values = values;
    }
    else {
      this.values = new double[values.length];
      System.arraycopy(values, 0, this.values, 0, values.length);
    }
  }

  /**
   * Provides a feature vector consisting of double values according to the
   * given Double values.
   * 
   * @param values the values to be set as values of the real vector
   */
  public DoubleVector(List<Double> values) {
    int i = 0;
    this.values = new double[values.size()];
    for(Iterator<Double> iter = values.iterator(); iter.hasNext(); i++) {
      this.values[i] = (iter.next());
    }
  }

  /**
   * Provides a DoubleVector consisting of the given double values.
   * 
   * @param values the values to be set as values of the DoubleVector
   */
  public DoubleVector(double[] values) {
    this.values = new double[values.length];
    System.arraycopy(values, 0, this.values, 0, values.length);
  }

  /**
   * Provides a DoubleVector consisting of the given double values.
   * 
   * @param values the values to be set as values of the DoubleVector
   */
  public DoubleVector(Double[] values) {
    this.values = new double[values.length];
    for(int i = 0; i < values.length; i++) {
      this.values[i] = values[i];
    }
  }

  /**
   * Expects a matrix of one column.
   * 
   * @param columnMatrix a matrix of one column
   */
  public DoubleVector(Vector columnMatrix) {
    values = new double[columnMatrix.getRowDimensionality()];
    for(int i = 0; i < values.length; i++) {
      values[i] = columnMatrix.get(i);
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
      return (long) values[dimension - 1];
    }
    catch(IndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Dimension " + dimension + " out of range.");
    }
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
  public DoubleVector plus(DoubleVector fv) {
    if(fv.getDimensionality() != this.getDimensionality()) {
      throw new IllegalArgumentException("Incompatible dimensionality: " + this.getDimensionality() + " - " + fv.getDimensionality() + ".");
    }
    double[] values = new double[this.values.length];
    for(int i = 0; i < values.length; i++) {
      values[i] = this.values[i] + fv.values[i];
    }
    return new DoubleVector(values, true);
  }

  @Override
  public DoubleVector minus(DoubleVector fv) {
    if(fv.getDimensionality() != this.getDimensionality()) {
      throw new IllegalArgumentException("Incompatible dimensionality: " + this.getDimensionality() + " - " + fv.getDimensionality() + ".");
    }
    double[] values = new double[this.values.length];
    for(int i = 0; i < values.length; i++) {
      values[i] = this.values[i] - fv.values[i];
    }
    return new DoubleVector(values, true);
  }

  @Override
  public DoubleVector nullVector() {
    return new DoubleVector(new double[this.values.length], true);
  }

  @Override
  public DoubleVector negativeVector() {
    return multiplicate(-1);
  }

  @Override
  public DoubleVector multiplicate(double k) {
    double[] values = new double[this.values.length];
    for(int i = 0; i < values.length; i++) {
      values[i] = this.values[i] * k;
    }
    return new DoubleVector(values, true);
  }

  /**
   * Provides the scalar product (inner product) of this and the given
   * DoubleVector.
   * 
   * @param d the DoubleVector to compute the scalar product for
   * @return the scalar product (inner product) of this and the given
   *         DoubleVector
   */
  @Override
  public Double scalarProduct(DoubleVector d) {
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
  public DoubleVector newInstance(Vector values) {
    return new DoubleVector(values);
  }

  @Override
  public DoubleVector newInstance(Double[] values) {
    return new DoubleVector(values);
  }

  @Override
  public DoubleVector newInstance(double[] values) {
    return new DoubleVector(values);
  }

  @Override
  public DoubleVector newInstance(List<Double> values) {
    return new DoubleVector(values);
  }

  @Override
  public DoubleVector fromByteBuffer(ByteBuffer buffer) throws IOException {
    final short dimensionality = buffer.getShort();
    final int len = ByteArrayUtil.SIZE_SHORT + ByteArrayUtil.SIZE_DOUBLE * dimensionality;
    if(buffer.remaining() < len) {
      throw new IOException("Not enough data for a double vector!");
    }
    double[] values = new double[dimensionality];
    buffer.asDoubleBuffer().get(values);
    return new DoubleVector(values, false);
  }

  @Override
  public void toByteBuffer(ByteBuffer buffer, DoubleVector vec) throws IOException {
    final short dimensionality = buffer.getShort();
    final int len = ByteArrayUtil.SIZE_SHORT + ByteArrayUtil.SIZE_DOUBLE * dimensionality;
    if(buffer.remaining() < len) {
      throw new IOException("Not enough space for the double vector!");
    }
    buffer.putShort(dimensionality);
    buffer.asDoubleBuffer().put(vec.values);
  }

  @Override
  public int getByteSize(DoubleVector vec) {
    return ByteArrayUtil.SIZE_SHORT + ByteArrayUtil.SIZE_DOUBLE * vec.getDimensionality();
  }
}