package de.lmu.ifi.dbs.elki.data;
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

import java.io.IOException;
import java.nio.ByteBuffer;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArray;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.io.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Vector type using {@code double[]} storage for real numbers.
 *
 * @author Arthur Zimek
 * @since 0.2
 *
 * @apiviz.landmark
 */
public class DoubleVector extends AbstractNumberVector {
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
  private final double[] values;

  /**
   * Private constructor. NOT for public use.
   *
   * @param values Values to use
   * @param nocopy Flag to not copy the array
   */
  private DoubleVector(double[] values, boolean nocopy) {
    this.values = nocopy ? values : values.clone();
  }

  /**
   * Create a DoubleVector consisting of the given double values.
   *
   * @param values the values to be set as values of the DoubleVector
   */
  public DoubleVector(double[] values) {
    this.values = values.clone();
  }

  /**
   * Expects a matrix of one column.
   *
   * @param columnMatrix a matrix of one column
   */
  public DoubleVector(Vector columnMatrix) {
    this.values = columnMatrix.getArrayCopy();
  }

  @Override
  public int getDimensionality() {
    return values.length;
  }

  @Override
  @Deprecated
  public Double getValue(int dimension) {
    return values[dimension];
  }

  @Override
  public double doubleValue(int dimension) {
    return values[dimension];
  }

  @Override
  public long longValue(int dimension) {
    return (long) values[dimension];
  }

  /**
   * Get a copy of the raw double[] array.
   *
   * @return copy of values array.
   */
  public double[] getValues() {
    return values.clone();
  }

  @Override
  public Vector getColumnVector() {
    // TODO: can we sometimes save this copy?
    // Is this worth the more complex API?
    return new Vector(values.clone());
  }

  @Override
  public String toString() {
    StringBuilder featureLine = new StringBuilder();
    for(int i = 0; i < values.length; i++) {
      featureLine.append(values[i]);
      if(i + 1 < values.length) {
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
  public static class Factory extends AbstractNumberVector.Factory<DoubleVector> {
    @Override
    public DoubleVector newNumberVector(double[] values) {
      return new DoubleVector(values);
    }

    @Override
    public <A> DoubleVector newFeatureVector(A array, ArrayAdapter<? extends Number, A> adapter) {
      int dim = adapter.size(array);
      double[] values = new double[dim];
      for(int i = 0; i < dim; i++) {
        values[i] = adapter.get(array, i).doubleValue();
      }
      return new DoubleVector(values, true);
    }

    @Override
    public <A> DoubleVector newNumberVector(A array, NumberArrayAdapter<?, ? super A> adapter) {
      if(adapter.getClass() == DoubleArray.class) {
        return new DoubleVector(((DoubleArray)array).toArray(), true);
      }
      final int dim = adapter.size(array);
      double[] values = new double[dim];
      for(int i = 0; i < dim; i++) {
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
   * @apiviz.uses DoubleVector - - «serializes»
   */
  public static class SmallSerializer implements ByteBufferSerializer<DoubleVector> {
    @Override
    public DoubleVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final byte dimensionality = buffer.get();
      assert(buffer.remaining() >= ByteArrayUtil.SIZE_DOUBLE * dimensionality) : "Not enough data remaining in buffer to read " + dimensionality + " doubles";
      ;
      final double[] values = new double[dimensionality];
      for(int i = 0; i < dimensionality; i++) {
        values[i] = buffer.getDouble();
      }
      return new DoubleVector(values, true);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, DoubleVector vec) throws IOException {
      assert(vec.values.length < Byte.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Byte.MAX_VALUE + "!";
      assert(buffer.remaining() >= ByteArrayUtil.SIZE_DOUBLE * vec.values.length) : "Not enough space remaining in buffer to write " + vec.values.length + " doubles";
      buffer.put((byte) vec.values.length);
      for(int i = 0; i < vec.values.length; i++) {
        buffer.putDouble(vec.values[i]);
      }
    }

    @Override
    public int getByteSize(DoubleVector vec) {
      assert(vec.values.length < Byte.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Byte.MAX_VALUE + "!";
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
   * @apiviz.uses DoubleVector - - «serializes»
   */
  public static class ShortSerializer implements ByteBufferSerializer<DoubleVector> {
    @Override
    public DoubleVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final short dimensionality = buffer.getShort();
      assert(buffer.remaining() >= ByteArrayUtil.SIZE_DOUBLE * dimensionality) : "Not enough data remaining in buffer to read " + dimensionality + " doubles";
      ;
      final double[] values = new double[dimensionality];
      for(int i = 0; i < dimensionality; i++) {
        values[i] = buffer.getDouble();
      }
      return new DoubleVector(values, true);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, DoubleVector vec) throws IOException {
      assert(vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      assert(buffer.remaining() >= ByteArrayUtil.SIZE_DOUBLE * vec.values.length) : "Not enough space remaining in buffer to write " + vec.values.length + " doubles";
      buffer.putShort((short) vec.values.length);
      for(int i = 0; i < vec.values.length; i++) {
        buffer.putDouble(vec.values[i]);
      }
    }

    @Override
    public int getByteSize(DoubleVector vec) {
      assert(vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      return ByteArrayUtil.SIZE_SHORT + ByteArrayUtil.SIZE_DOUBLE * vec.getDimensionality();
    }
  }

  /**
   * Serialization class for variable dimensionality by using VarInt encoding.
   *
   * @author Erich Schubert
   *
   * @apiviz.uses DoubleVector - - «serializes»
   */
  public static class VariableSerializer implements ByteBufferSerializer<DoubleVector> {
    @Override
    public DoubleVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final int dimensionality = ByteArrayUtil.readUnsignedVarint(buffer);
      assert(buffer.remaining() >= ByteArrayUtil.SIZE_DOUBLE * dimensionality) : "Not enough data remaining in buffer to read " + dimensionality + " doubles";
      final double[] values = new double[dimensionality];
      for(int i = 0; i < dimensionality; i++) {
        values[i] = buffer.getDouble();
      }
      return new DoubleVector(values, true);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, DoubleVector vec) throws IOException {
      assert(buffer.remaining() >= ByteArrayUtil.SIZE_DOUBLE * vec.values.length) : "Not enough space remaining in buffer to write " + vec.values.length + " doubles";
      ByteArrayUtil.writeUnsignedVarint(buffer, vec.values.length);
      for(int i = 0; i < vec.values.length; i++) {
        buffer.putDouble(vec.values[i]);
      }
    }

    @Override
    public int getByteSize(DoubleVector vec) {
      return ByteArrayUtil.getUnsignedVarintSize(vec.values.length) + ByteArrayUtil.SIZE_DOUBLE * vec.values.length;
    }
  }
}
