/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.data;

import java.io.IOException;
import java.nio.ByteBuffer;

import elki.utilities.datastructures.arraylike.ArrayAdapter;
import elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import elki.utilities.io.ByteArrayUtil;
import elki.utilities.io.ByteBufferSerializer;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Vector type using {@code long[]} storage.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public class LongVector implements NumberVector {
  /**
   * Static instance (object factory).
   */
  public static final LongVector.Factory FACTORY = new LongVector.Factory();

  /**
   * Serializer for up to 127 dimensions.
   */
  public static final ByteBufferSerializer<LongVector> BYTE_SERIALIZER = new SmallSerializer();

  /**
   * Serializer for up to 2^15-1 dimensions.
   */
  public static final ByteBufferSerializer<LongVector> SHORT_SERIALIZER = new ShortSerializer();

  /**
   * Serializer using varint encoding.
   */
  public static final ByteBufferSerializer<LongVector> VARIABLE_SERIALIZER = new VariableSerializer();

  /**
   * Keeps the values of the real vector.
   */
  private final long[] values;

  /**
   * Private constructor. NOT for public use.
   * 
   * @param values Value data
   * @param nocopy Flag to use without copying.
   */
  private LongVector(long[] values, boolean nocopy) {
    this.values = nocopy ? values : values.clone();
  }

  /**
   * Create an LongVector consisting of the given integer values.
   * 
   * @param values the values to be set as values of the LongVector
   */
  public LongVector(long[] values) {
    this.values = values.clone();
  }

  @Override
  public int getDimensionality() {
    return values.length;
  }

  /**
   * Returns the value of the specified attribute.
   * 
   * @param dimension the selected attribute. Attributes are counted starting
   *        with 0.
   * @throws IllegalArgumentException if the specified dimension is out of range
   *         of the possible attributes
   */
  @Override
  @Deprecated
  public Long getValue(int dimension) {
    return Long.valueOf(values[dimension]);
  }

  @Override
  public double doubleValue(int dimension) {
    return values[dimension];
  }

  @Override
  public long longValue(int dimension) {
    return values[dimension];
  }

  @Override
  public int intValue(int dimension) {
    return (int) values[dimension];
  }

  @Override
  public double[] toArray() {
    double[] data = new double[values.length];
    for(int i = 0; i < values.length; i++) {
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
   * Copy a double array into a new vector.
   * 
   * @param vals Values
   * @return Wrapped vector
   */
  public static LongVector copy(long[] vals) {
    return new LongVector(vals);
  }

  /**
   * Wrap a double array as vector (without copying).
   * <p>
   * Note: modifying the array afterwards can lead to problems if the data has,
   * e.g., been added to an index, which relies on them being immutable!
   * 
   * @param vals Values
   * @return Wrapped vector
   */
  public static LongVector wrap(long[] vals) {
    return new LongVector(vals, true);
  }

  /**
   * Factory for long vectors.
   * 
   * @author Erich Schubert
   * 
   * @has - - - LongVector
   */
  public static class Factory implements NumberVector.Factory<LongVector> {
    @Override
    public <A> LongVector newFeatureVector(A array, ArrayAdapter<? extends Number, A> adapter) {
      int dim = adapter.size(array);
      long[] values = new long[dim];
      for(int i = 0; i < dim; i++) {
        values[i] = adapter.get(array, i).longValue();
      }
      return new LongVector(values, true);
    }

    @Override
    public <A> LongVector newNumberVector(A array, NumberArrayAdapter<?, ? super A> adapter) {
      int dim = adapter.size(array);
      long[] values = new long[dim];
      for(int i = 0; i < dim; i++) {
        values[i] = adapter.getLong(array, i);
      }
      return new LongVector(values, true);
    }

    @Override
    public ByteBufferSerializer<LongVector> getDefaultSerializer() {
      return VARIABLE_SERIALIZER;
    }

    @Override
    public Class<? super LongVector> getRestrictionClass() {
      return LongVector.class;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     */
    public static class Par implements Parameterizer {
      @Override
      public LongVector.Factory make() {
        return FACTORY;
      }
    }
  }

  /**
   * Serialization class for dense integer vectors with up to 127 dimensions, by
   * using a byte for storing the dimensionality.
   * 
   * @author Erich Schubert
   * 
   * @assoc - serializes - LongVector
   */
  public static class SmallSerializer implements ByteBufferSerializer<LongVector> {
    @Override
    public LongVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final byte dimensionality = buffer.get();
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_LONG * dimensionality);
      final long[] values = new long[dimensionality];
      for(int i = 0; i < dimensionality; i++) {
        values[i] = buffer.getLong();
      }
      return new LongVector(values, true);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, LongVector vec) throws IOException {
      assert (vec.values.length < Byte.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Byte.MAX_VALUE + "!";
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_LONG * vec.values.length);
      buffer.put((byte) vec.values.length);
      for(int i = 0; i < vec.values.length; i++) {
        buffer.putLong(vec.values[i]);
      }
    }

    @Override
    public int getByteSize(LongVector vec) {
      assert (vec.values.length < Byte.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Byte.MAX_VALUE + "!";
      return ByteArrayUtil.SIZE_BYTE + ByteArrayUtil.SIZE_LONG * vec.getDimensionality();
    }
  }

  /**
   * Serialization class for dense long vectors with up to
   * {@link Short#MAX_VALUE} dimensions, by using a short for storing the
   * dimensionality.
   * 
   * @author Erich Schubert
   * 
   * @assoc - serializes - LongVector
   */
  public static class ShortSerializer implements ByteBufferSerializer<LongVector> {
    @Override
    public LongVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final short dimensionality = buffer.getShort();
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_LONG * dimensionality);
      final long[] values = new long[dimensionality];
      for(int i = 0; i < dimensionality; i++) {
        values[i] = buffer.getLong();
      }
      return new LongVector(values, true);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, LongVector vec) throws IOException {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_LONG * vec.values.length);
      buffer.putShort((short) vec.values.length);
      for(int i = 0; i < vec.values.length; i++) {
        buffer.putLong(vec.values[i]);
      }
    }

    @Override
    public int getByteSize(LongVector vec) {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      return ByteArrayUtil.SIZE_SHORT + ByteArrayUtil.SIZE_LONG * vec.getDimensionality();
    }
  }

  /**
   * Serialization class for variable dimensionality by using VarInt encoding.
   * 
   * @author Erich Schubert
   * 
   * @assoc - serializes - LongVector
   */
  public static class VariableSerializer implements ByteBufferSerializer<LongVector> {
    @Override
    public LongVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final int dimensionality = ByteArrayUtil.readUnsignedVarint(buffer);
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_LONG * dimensionality);
      final long[] values = new long[dimensionality];
      for(int i = 0; i < dimensionality; i++) {
        values[i] = ByteArrayUtil.readSignedVarintLong(buffer);
      }
      return new LongVector(values, true);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, LongVector vec) throws IOException {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      ByteArrayUtil.writeUnsignedVarint(buffer, vec.values.length);
      for(int i = 0; i < vec.values.length; i++) {
        ByteArrayUtil.writeSignedVarintLong(buffer, vec.values[i]);
      }
    }

    @Override
    public int getByteSize(LongVector vec) {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      int len = ByteArrayUtil.getUnsignedVarintSize(vec.values.length);
      for(int i = 0; i < vec.values.length; i++) {
        len += ByteArrayUtil.getSignedVarintLongSize(vec.values[i]);
      }
      return len;
    }
  }
}
