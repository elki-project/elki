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

import gnu.trove.list.TDoubleList;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.persistent.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * A DoubleVector is to store real values approximately as double values.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.landmark
 */
public class DoubleVector extends AbstractNumberVector<Double> {
  /**
   * Static factory instance.
   */
  public static final DoubleVector.Factory FACTORY = new DoubleVector.Factory();

  /**
   * Serializer for up to 127 dimensions.
   */
  public static final ByteBufferSerializer<DoubleVector> BYTE_SERIALIZER = new SmallSerializer();

  /**
   * Serializer for up to 2^15-1 dimensions.
   */
  public static final ByteBufferSerializer<DoubleVector> SHORT_SERIALIZER = new ShortSerializer();

  /**
   * Serializer using varint encoding.
   */
  public static final ByteBufferSerializer<DoubleVector> VARIABLE_SERIALIZER = new VariableSerializer();

  /**
   * Stores the values of the real vector.
   */
  private double[] values;

  /**
   * Private constructor. NOT for public use.
   * 
   * @param values Values to use
   * @param nocopy Flag to not copy the array
   */
  private DoubleVector(double[] values, boolean nocopy) {
    if (nocopy) {
      this.values = values;
    } else {
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
    for (Iterator<Double> iter = values.iterator(); iter.hasNext(); i++) {
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
    for (int i = 0; i < values.length; i++) {
      this.values[i] = values[i];
    }
  }

  /**
   * Expects a matrix of one column.
   * 
   * @param columnMatrix a matrix of one column
   */
  public DoubleVector(Vector columnMatrix) {
    values = new double[columnMatrix.getDimensionality()];
    for (int i = 0; i < values.length; i++) {
      values[i] = columnMatrix.get(i);
    }
  }

  @Override
  public int getDimensionality() {
    return values.length;
  }

  @Override
  @Deprecated
  public Double getValue(int dimension) {
    try {
      return values[dimension];
    } catch (IndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Dimension " + dimension + " out of range.");
    }
  }

  @Override
  public double doubleValue(int dimension) {
    try {
      return values[dimension];
    } catch (IndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Dimension " + dimension + " out of range.");
    }
  }

  @Override
  public long longValue(int dimension) {
    try {
      return (long) values[dimension];
    } catch (IndexOutOfBoundsException e) {
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
  public String toString() {
    StringBuffer featureLine = new StringBuffer();
    for (int i = 0; i < values.length; i++) {
      featureLine.append(values[i]);
      if (i + 1 < values.length) {
        featureLine.append(ATTRIBUTE_SEPARATOR);
      }
    }
    return featureLine.toString();
  }

  /**
   * Factory for Double vectors.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has DoubleVector
   */
  public static class Factory extends AbstractNumberVector.Factory<DoubleVector, Double> {
    @Override
    public DoubleVector newNumberVector(double[] values) {
      return new DoubleVector(values);
    }

    @Override
    public <A> DoubleVector newFeatureVector(A array, ArrayAdapter<Double, A> adapter) {
      int dim = adapter.size(array);
      double[] values = new double[dim];
      for (int i = 0; i < dim; i++) {
        values[i] = adapter.get(array, i);
      }
      return new DoubleVector(values, true);
    }

    @Override
    public <A> DoubleVector newNumberVector(A array, NumberArrayAdapter<?, ? super A> adapter) {
      if (adapter == ArrayLikeUtil.TDOUBLELISTADAPTER) {
        return new DoubleVector(((TDoubleList) array).toArray(), true);
      }
      final int dim = adapter.size(array);
      double[] values = new double[dim];
      for (int i = 0; i < dim; i++) {
        values[i] = adapter.getDouble(array, i);
      }
      return new DoubleVector(values, true);
    }

    @Override
    public ByteBufferSerializer<DoubleVector> getDefaultSerializer() {
      return VARIABLE_SERIALIZER;
    }

    @Override
    public Class<? super DoubleVector> getRestrictionClass() {
      return DoubleVector.class;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer extends AbstractParameterizer {
      @Override
      protected DoubleVector.Factory makeInstance() {
        return FACTORY;
      }
    }
  }

  /**
   * Serialization class for dense double vectors with up to 127 dimensions, by
   * using a byte for storing the dimensionality.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has DoubleVector
   */
  public static class SmallSerializer implements ByteBufferSerializer<DoubleVector> {
    @Override
    public DoubleVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final byte dimensionality = buffer.get();
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_DOUBLE * dimensionality);
      final double[] values = new double[dimensionality];
      for (int i = 0; i < dimensionality; i++) {
        values[i] = buffer.getDouble();
      }
      return new DoubleVector(values, true);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, DoubleVector vec) throws IOException {
      assert (vec.values.length < Byte.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Byte.MAX_VALUE + "!";
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_DOUBLE * vec.values.length);
      buffer.put((byte) vec.values.length);
      for (int i = 0; i < vec.values.length; i++) {
        buffer.putDouble(vec.values[i]);
      }
    }

    @Override
    public int getByteSize(DoubleVector vec) {
      assert (vec.values.length < Byte.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Byte.MAX_VALUE + "!";
      return ByteArrayUtil.SIZE_BYTE + ByteArrayUtil.SIZE_DOUBLE * vec.getDimensionality();
    }
  }

  /**
   * Serialization class for dense double vectors with up to
   * {@link Short#MAX_VALUE} dimensions, by using a short for storing the
   * dimensionality.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has DoubleVector
   */
  public static class ShortSerializer implements ByteBufferSerializer<DoubleVector> {
    @Override
    public DoubleVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final short dimensionality = buffer.getShort();
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_DOUBLE * dimensionality);
      final double[] values = new double[dimensionality];
      for (int i = 0; i < dimensionality; i++) {
        values[i] = buffer.getDouble();
      }
      return new DoubleVector(values, true);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, DoubleVector vec) throws IOException {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_DOUBLE * vec.values.length);
      buffer.putShort((short) vec.values.length);
      for (int i = 0; i < vec.values.length; i++) {
        buffer.putDouble(vec.values[i]);
      }
    }

    @Override
    public int getByteSize(DoubleVector vec) {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      return ByteArrayUtil.SIZE_SHORT + ByteArrayUtil.SIZE_DOUBLE * vec.getDimensionality();
    }
  }

  /**
   * Serialization class for variable dimensionality by using VarInt encoding.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has DoubleVector
   */
  public static class VariableSerializer implements ByteBufferSerializer<DoubleVector> {
    @Override
    public DoubleVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final int dimensionality = ByteArrayUtil.readUnsignedVarint(buffer);
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_DOUBLE * dimensionality);
      final double[] values = new double[dimensionality];
      for (int i = 0; i < dimensionality; i++) {
        values[i] = buffer.getDouble();
      }
      return new DoubleVector(values, true);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, DoubleVector vec) throws IOException {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_DOUBLE * vec.values.length);
      ByteArrayUtil.writeUnsignedVarint(buffer, vec.values.length);
      for (int i = 0; i < vec.values.length; i++) {
        buffer.putDouble(vec.values[i]);
      }
    }

    @Override
    public int getByteSize(DoubleVector vec) {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      return ByteArrayUtil.getUnsignedVarintSize(vec.values.length) + ByteArrayUtil.SIZE_DOUBLE * vec.values.length;
    }
  }
}
