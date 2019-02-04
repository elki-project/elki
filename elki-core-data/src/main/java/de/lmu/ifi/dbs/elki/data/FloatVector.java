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

import java.io.IOException;
import java.nio.ByteBuffer;

import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.io.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Vector type using {@code float[]} storage, thus needing approximately half as
 * much memory as {@link DoubleVector}.
 * 
 * @author Elke Achtert
 * @since 0.1
 */
public class FloatVector implements NumberVector {
  /**
   * Static factory instance.
   */
  public static final FloatVector.Factory FACTORY = new FloatVector.Factory();

  /**
   * Serializer for up to 127 dimensions.
   */
  public static final ByteBufferSerializer<FloatVector> BYTE_SERIALIZER = new SmallSerializer();

  /**
   * Serializer for up to 2^15-1 dimensions.
   */
  public static final ByteBufferSerializer<FloatVector> SHORT_SERIALIZER = new ShortSerializer();

  /**
   * Serializer using varint encoding.
   */
  public static final ByteBufferSerializer<FloatVector> VARIABLE_SERIALIZER = new VariableSerializer();

  /**
   * Keeps the values of the float vector.
   */
  private final float[] values;

  /**
   * Private constructor. NOT for public use.
   * 
   * @param values Data values
   * @param nocopy Flag to re-use the values array
   */
  private FloatVector(float[] values, boolean nocopy) {
    this.values = nocopy ? values : values.clone();
  }

  /**
   * Create a FloatVector consisting of the given float values.
   * 
   * @param values the values to be set as values of the float vector
   */
  public FloatVector(float[] values) {
    this.values = values.clone();
  }

  @Override
  public int getDimensionality() {
    return values.length;
  }

  @Deprecated
  @Override
  public Float getValue(int dimension) {
    return values[dimension];
  }

  @Override
  public double doubleValue(int dimension) {
    return values[dimension];
  }

  @Override
  public float floatValue(int dimension) {
    return values[dimension];
  }

  @Override
  public long longValue(int dimension) {
    return (long) values[dimension];
  }

  @Override
  public double[] toArray() {
    double[] data = new double[values.length];
    for(int i = 0; i < data.length; i++) {
      data[i] = values[i];
    }
    return data;
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
   * Factory for float vectors.
   * 
   * @author Erich Schubert
   * 
   * @has - - - FloatVector
   */
  public static class Factory implements NumberVector.Factory<FloatVector> {
    @Override
    public <A> FloatVector newFeatureVector(A array, ArrayAdapter<? extends Number, A> adapter) {
      int dim = adapter.size(array);
      float[] values = new float[dim];
      for(int i = 0; i < dim; i++) {
        values[i] = adapter.get(array, i).floatValue();
      }
      return new FloatVector(values, true);
    }

    @Override
    public <A> FloatVector newNumberVector(A array, NumberArrayAdapter<?, ? super A> adapter) {
      int dim = adapter.size(array);
      float[] values = new float[dim];
      for(int i = 0; i < dim; i++) {
        values[i] = adapter.getFloat(array, i);
      }
      return new FloatVector(values, true);
    }

    @Override
    public ByteBufferSerializer<FloatVector> getDefaultSerializer() {
      return VARIABLE_SERIALIZER;
    }

    @Override
    public Class<? super FloatVector> getRestrictionClass() {
      return FloatVector.class;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     */
    public static class Parameterizer extends AbstractParameterizer {
      @Override
      protected FloatVector.Factory makeInstance() {
        return FACTORY;
      }
    }
  }

  /**
   * Serialization class for dense float vectors with up to 127 dimensions, by
   * using a byte for storing the dimensionality.
   * 
   * @author Erich Schubert
   * 
   * @assoc - serializes - FloatVector
   */
  public static class SmallSerializer implements ByteBufferSerializer<FloatVector> {
    @Override
    public FloatVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final byte dimensionality = buffer.get();
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_FLOAT * dimensionality);
      final float[] values = new float[dimensionality];
      for(int i = 0; i < dimensionality; i++) {
        values[i] = buffer.getFloat();
      }
      return new FloatVector(values, true);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, FloatVector vec) throws IOException {
      assert (vec.values.length < Byte.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Byte.MAX_VALUE + "!";
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_FLOAT * vec.values.length);
      buffer.put((byte) vec.values.length);
      for(int i = 0; i < vec.values.length; i++) {
        buffer.putFloat(vec.values[i]);
      }
    }

    @Override
    public int getByteSize(FloatVector vec) {
      assert (vec.values.length < Byte.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Byte.MAX_VALUE + "!";
      return ByteArrayUtil.SIZE_BYTE + ByteArrayUtil.SIZE_FLOAT * vec.getDimensionality();
    }
  }

  /**
   * Serialization class for dense float vectors with up to
   * {@link Short#MAX_VALUE} dimensions, by using a short for storing the
   * dimensionality.
   * 
   * @author Erich Schubert
   * 
   * @assoc - serializes - FloatVector
   */
  public static class ShortSerializer implements ByteBufferSerializer<FloatVector> {
    @Override
    public FloatVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final short dimensionality = buffer.getShort();
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_FLOAT * dimensionality);
      final float[] values = new float[dimensionality];
      for(int i = 0; i < dimensionality; i++) {
        values[i] = buffer.getFloat();
      }
      return new FloatVector(values, true);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, FloatVector vec) throws IOException {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_FLOAT * vec.values.length);
      buffer.putShort((short) vec.values.length);
      for(int i = 0; i < vec.values.length; i++) {
        buffer.putFloat(vec.values[i]);
      }
    }

    @Override
    public int getByteSize(FloatVector vec) {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      return ByteArrayUtil.SIZE_SHORT + ByteArrayUtil.SIZE_FLOAT * vec.getDimensionality();
    }
  }

  /**
   * Serialization class for variable dimensionality by using VarInt encoding.
   * 
   * @author Erich Schubert
   * 
   * @assoc - serializes - FloatVector
   */
  public static class VariableSerializer implements ByteBufferSerializer<FloatVector> {
    @Override
    public FloatVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final int dimensionality = ByteArrayUtil.readUnsignedVarint(buffer);
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_FLOAT * dimensionality);
      final float[] values = new float[dimensionality];
      for(int i = 0; i < dimensionality; i++) {
        values[i] = buffer.getFloat();
      }
      return new FloatVector(values, true);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, FloatVector vec) throws IOException {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_FLOAT * vec.values.length);
      ByteArrayUtil.writeUnsignedVarint(buffer, vec.values.length);
      for(int i = 0; i < vec.values.length; i++) {
        buffer.putFloat(vec.values[i]);
      }
    }

    @Override
    public int getByteSize(FloatVector vec) {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      return ByteArrayUtil.getUnsignedVarintSize(vec.values.length) + ByteArrayUtil.SIZE_FLOAT * vec.values.length;
    }
  }
}
