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
 * Vector type using {@code short[]} storage.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class ShortVector implements NumberVector {
  /**
   * Static instance (object factory).
   */
  public static final ShortVector.Factory STATIC = new ShortVector.Factory();

  /**
   * Serializer for up to 2^15-1 dimensions.
   */
  public static final ByteBufferSerializer<ShortVector> SHORT_SERIALIZER = new ShortSerializer();

  /**
   * Serializer using varint encoding.
   */
  public static final ByteBufferSerializer<ShortVector> VARIABLE_SERIALIZER = new VariableSerializer();

  /**
   * Keeps the values of the real vector.
   */
  private final short[] values;

  /**
   * Private constructor. NOT for public use.
   * 
   * @param values Value data
   * @param nocopy Flag to use without copying.
   */
  private ShortVector(short[] values, boolean nocopy) {
    if(nocopy) {
      this.values = values;
    }
    else {
      this.values = new short[values.length];
      System.arraycopy(values, 0, this.values, 0, values.length);
    }
  }

  /**
   * Create an ShortVector consisting of the given Short values.
   * 
   * @param values the values to be set as values of the ShortVector
   */
  public ShortVector(short[] values) {
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
   * 
   * @throws IllegalArgumentException if the specified dimension is out of range
   *         of the possible attributes
   * 
   *         {@inheritDoc}
   */
  @Override
  @Deprecated
  public Short getValue(int dimension) {
    return Short.valueOf(values[dimension]);
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
  public short shortValue(int dimension) {
    return values[dimension];
  }

  /**
   * Get a copy of the raw short[] array.
   * 
   * @return copy of values array.
   */
  public short[] getValues() {
    short[] copy = new short[values.length];
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
   * Factory for Short vectors.
   * 
   * @author Erich Schubert
   * 
   * @has - - - ShortVector
   */
  public static class Factory implements NumberVector.Factory<ShortVector> {
    @Override
    public <A> ShortVector newFeatureVector(A array, ArrayAdapter<? extends Number, A> adapter) {
      int dim = adapter.size(array);
      short[] values = new short[dim];
      for(int i = 0; i < dim; i++) {
        values[i] = adapter.get(array, i).shortValue();
      }
      return new ShortVector(values, true);
    }

    @Override
    public <A> ShortVector newNumberVector(A array, NumberArrayAdapter<?, ? super A> adapter) {
      int dim = adapter.size(array);
      short[] values = new short[dim];
      for(int i = 0; i < dim; i++) {
        values[i] = adapter.getShort(array, i);
      }
      return new ShortVector(values, true);
    }

    @Override
    public ByteBufferSerializer<ShortVector> getDefaultSerializer() {
      return VARIABLE_SERIALIZER;
    }

    @Override
    public Class<? super ShortVector> getRestrictionClass() {
      return ShortVector.class;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     */
    public static class Parameterizer extends AbstractParameterizer {
      @Override
      protected ShortVector.Factory makeInstance() {
        return STATIC;
      }
    }
  }

  /**
   * Serialization class for dense Short vectors with up to
   * {@link Short#MAX_VALUE} dimensions, by using a short for storing the
   * dimensionality.
   * 
   * @author Erich Schubert
   * 
   * @assoc - serializes - ShortVector
   */
  public static class ShortSerializer implements ByteBufferSerializer<ShortVector> {
    @Override
    public ShortVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final short dimensionality = buffer.getShort();
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_SHORT * dimensionality);
      final short[] values = new short[dimensionality];
      for(int i = 0; i < dimensionality; i++) {
        values[i] = buffer.getShort();
      }
      return new ShortVector(values, true);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, ShortVector vec) throws IOException {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_SHORT * vec.values.length);
      buffer.putShort((short) vec.values.length);
      for(int i = 0; i < vec.values.length; i++) {
        buffer.putShort(vec.values[i]);
      }
    }

    @Override
    public int getByteSize(ShortVector vec) {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      return ByteArrayUtil.SIZE_SHORT + ByteArrayUtil.SIZE_SHORT * vec.getDimensionality();
    }
  }

  /**
   * Serialization class for variable dimensionality by using VarInt encoding.
   * 
   * @author Erich Schubert
   * 
   * @assoc - serializes - ShortVector
   */
  public static class VariableSerializer implements ByteBufferSerializer<ShortVector> {
    @Override
    public ShortVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final int dimensionality = ByteArrayUtil.readUnsignedVarint(buffer);
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_INT * dimensionality);
      final short[] values = new short[dimensionality];
      for(int i = 0; i < dimensionality; i++) {
        values[i] = (short) ByteArrayUtil.readSignedVarint(buffer);
      }
      return new ShortVector(values, true);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, ShortVector vec) throws IOException {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      ByteArrayUtil.writeUnsignedVarint(buffer, vec.values.length);
      for(int i = 0; i < vec.values.length; i++) {
        ByteArrayUtil.writeSignedVarint(buffer, vec.values[i]);
      }
    }

    @Override
    public int getByteSize(ShortVector vec) {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      int len = ByteArrayUtil.getUnsignedVarintSize(vec.values.length);
      for(int i = 0; i < vec.values.length; i++) {
        len += ByteArrayUtil.getSignedVarintSize(vec.values[i]);
      }
      return len;
    }
  }
}
