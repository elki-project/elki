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
 * Vector using {@code byte[]} storage.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class ByteVector implements NumberVector {
  /**
   * Static instance (object factory).
   */
  public static final ByteVector.Factory STATIC = new ByteVector.Factory();

  /**
   * Serializer for up to 127 dimensions.
   */
  public static final ByteBufferSerializer<ByteVector> BYTE_SERIALIZER = new SmallSerializer();

  /**
   * Serializer for up to 2^15-1 dimensions.
   */
  public static final ByteBufferSerializer<ByteVector> SHORT_SERIALIZER = new ShortSerializer();

  /**
   * Keeps the values of the real vector.
   */
  private final byte[] values;

  /**
   * Private constructor. NOT for public use.
   * 
   * @param values Value data
   * @param nocopy Flag to use without copying.
   */
  private ByteVector(byte[] values, boolean nocopy) {
    if(nocopy) {
      this.values = values;
    }
    else {
      this.values = new byte[values.length];
      System.arraycopy(values, 0, this.values, 0, values.length);
    }
  }

  /**
   * Create an ByteVector consisting of the given Byte values.
   * 
   * @param values the values to be set as values of the ByteVector
   */
  public ByteVector(byte[] values) {
    this.values = new byte[values.length];
    System.arraycopy(values, 0, this.values, 0, values.length);
  }

  @Override
  public int getDimensionality() {
    return values.length;
  }

  @Override
  @Deprecated
  public Byte getValue(int dimension) {
    return Byte.valueOf(values[dimension]);
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
  public byte byteValue(int dimension) {
    return values[dimension];
  }

  /**
   * Get a copy of the raw byte[] array.
   * 
   * @return copy of values array.
   */
  public byte[] getValues() {
    byte[] copy = new byte[values.length];
    System.arraycopy(values, 0, copy, 0, values.length);
    return copy;
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
   * Factory for Byte vectors.
   * 
   * @author Erich Schubert
   * 
   * @has - - - ByteVector
   */
  public static class Factory implements NumberVector.Factory<ByteVector> {
    @Override
    public <A> ByteVector newFeatureVector(A array, ArrayAdapter<? extends Number, A> adapter) {
      int dim = adapter.size(array);
      byte[] values = new byte[dim];
      for(int i = 0; i < dim; i++) {
        values[i] = adapter.get(array, i).byteValue();
      }
      return new ByteVector(values, true);
    }

    @Override
    public <A> ByteVector newNumberVector(A array, NumberArrayAdapter<?, ? super A> adapter) {
      int dim = adapter.size(array);
      byte[] values = new byte[dim];
      for(int i = 0; i < dim; i++) {
        values[i] = adapter.getByte(array, i);
      }
      return new ByteVector(values, true);
    }

    @Override
    public ByteBufferSerializer<ByteVector> getDefaultSerializer() {
      return SHORT_SERIALIZER;
    }

    @Override
    public Class<? super ByteVector> getRestrictionClass() {
      return ByteVector.class;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     */
    public static class Parameterizer extends AbstractParameterizer {
      @Override
      protected ByteVector.Factory makeInstance() {
        return STATIC;
      }
    }
  }

  /**
   * Serialization class for dense Byte vectors with up to 127 dimensions, by
   * using a byte for storing the dimensionality.
   * 
   * @author Erich Schubert
   * 
   * @assoc - serializes - ByteVector
   */
  public static class SmallSerializer implements ByteBufferSerializer<ByteVector> {
    @Override
    public ByteVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final byte dimensionality = buffer.get();
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_BYTE * dimensionality);
      final byte[] values = new byte[dimensionality];
      for(int i = 0; i < dimensionality; i++) {
        values[i] = buffer.get();
      }
      return new ByteVector(values, true);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, ByteVector vec) throws IOException {
      assert (vec.values.length < Byte.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Byte.MAX_VALUE + "!";
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_BYTE * vec.values.length);
      buffer.put((byte) vec.values.length);
      for(int i = 0; i < vec.values.length; i++) {
        buffer.put(vec.values[i]);
      }
    }

    @Override
    public int getByteSize(ByteVector vec) {
      assert (vec.values.length < Byte.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Byte.MAX_VALUE + "!";
      return ByteArrayUtil.SIZE_BYTE + ByteArrayUtil.SIZE_BYTE * vec.getDimensionality();
    }
  }

  /**
   * Serialization class for dense Byte vectors with up to
   * {@link Short#MAX_VALUE} dimensions, by using a short for storing the
   * dimensionality.
   * 
   * @author Erich Schubert
   * 
   * @assoc - serializes - ByteVector
   */
  public static class ShortSerializer implements ByteBufferSerializer<ByteVector> {
    @Override
    public ByteVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final short dimensionality = buffer.getShort();
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_BYTE * dimensionality);
      final byte[] values = new byte[dimensionality];
      for(int i = 0; i < dimensionality; i++) {
        values[i] = buffer.get();
      }
      return new ByteVector(values, true);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, ByteVector vec) throws IOException {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_BYTE * vec.values.length);
      buffer.putShort((short) vec.values.length);
      for(int i = 0; i < vec.values.length; i++) {
        buffer.put(vec.values[i]);
      }
    }

    @Override
    public int getByteSize(ByteVector vec) {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      return ByteArrayUtil.SIZE_SHORT + ByteArrayUtil.SIZE_BYTE * vec.getDimensionality();
    }
  }
}
