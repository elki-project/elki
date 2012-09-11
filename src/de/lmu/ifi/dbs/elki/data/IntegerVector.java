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

import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.persistent.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * An IntegerVector is to store integer values.
 * 
 * @author Erich Schubert
 */
public class IntegerVector extends AbstractNumberVector<Integer> {
  /**
   * Static instance (object factory).
   */
  public static final IntegerVector.Factory STATIC = new IntegerVector.Factory();

  /**
   * Serializer for up to 127 dimensions.
   */
  public static final ByteBufferSerializer<IntegerVector> BYTE_SERIALIZER = new SmallSerializer();

  /**
   * Serializer for up to 2^15-1 dimensions.
   */
  public static final ByteBufferSerializer<IntegerVector> SHORT_SERIALIZER = new ShortSerializer();

  /**
   * Serializer using varint encoding.
   */
  public static final ByteBufferSerializer<IntegerVector> VARIABLE_SERIALIZER = new VariableSerializer();

  /**
   * Keeps the values of the real vector.
   */
  private int[] values;

  /**
   * Private constructor. NOT for public use.
   * 
   * @param values Value data
   * @param nocopy Flag to use without copying.
   */
  private IntegerVector(int[] values, boolean nocopy) {
    if (nocopy) {
      this.values = values;
    } else {
      this.values = new int[values.length];
      System.arraycopy(values, 0, this.values, 0, values.length);
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
   * 
   *         {@inheritDoc}
   */
  @Override
  public Integer getValue(int dimension) {
    try {
      return values[dimension - 1];
    } catch (IndexOutOfBoundsException e) {
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
   * 
   *         {@inheritDoc}
   */
  @Override
  public double doubleValue(int dimension) {
    try {
      return values[dimension - 1];
    } catch (IndexOutOfBoundsException e) {
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
   * 
   *         {@inheritDoc}
   */
  @Override
  public long longValue(int dimension) {
    try {
      return values[dimension - 1];
    } catch (IndexOutOfBoundsException e) {
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
    for (int i = 0; i < values.length; i++) {
      data[i] = values[i];
    }
    return new Vector(data);
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
   * Factory for integer vectors.
   * 
   * @author Erich Schubert
   */
  public static class Factory extends AbstractNumberVector.Factory<IntegerVector, Integer> {
    @Override
    public <A> IntegerVector newFeatureVector(A array, ArrayAdapter<Integer, A> adapter) {
      int dim = adapter.size(array);
      int[] values = new int[dim];
      for (int i = 0; i < dim; i++) {
        values[i] = adapter.get(array, i);
      }
      return new IntegerVector(values, true);
    }

    @Override
    public <A> IntegerVector newNumberVector(A array, NumberArrayAdapter<?, ? super A> adapter) {
      int dim = adapter.size(array);
      int[] values = new int[dim];
      for (int i = 0; i < dim; i++) {
        values[i] = adapter.getInteger(array, i);
      }
      return new IntegerVector(values, true);
    }

    @Override
    public ByteBufferSerializer<IntegerVector> getDefaultSerializer() {
      return VARIABLE_SERIALIZER;
    }
    
    @Override
    public Class<? super IntegerVector> getRestrictionClass() {
      return IntegerVector.class;
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
      protected IntegerVector.Factory makeInstance() {
        return STATIC;
      }
    }
  }

  /**
   * Serialization class for dense integer vectors with up to 127 dimensions, by
   * using a byte for storing the dimensionality.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has IntegerVector
   */
  public static class SmallSerializer implements ByteBufferSerializer<IntegerVector> {
    @Override
    public IntegerVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final byte dimensionality = buffer.get();
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_INT * dimensionality);
      final int[] values = new int[dimensionality];
      for (int i = 0; i < dimensionality; i++) {
        values[i] = buffer.getInt();
      }
      return new IntegerVector(values, true);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, IntegerVector vec) throws IOException {
      assert (vec.values.length < Byte.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Byte.MAX_VALUE + "!";
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_INT * vec.values.length);
      buffer.put((byte) vec.values.length);
      for (int i = 0; i < vec.values.length; i++) {
        buffer.putInt(vec.values[i]);
      }
    }

    @Override
    public int getByteSize(IntegerVector vec) {
      assert (vec.values.length < Byte.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Byte.MAX_VALUE + "!";
      return ByteArrayUtil.SIZE_BYTE + ByteArrayUtil.SIZE_INT * vec.getDimensionality();
    }
  }

  /**
   * Serialization class for dense integer vectors with up to
   * {@link Short#MAX_VALUE} dimensions, by using a short for storing the
   * dimensionality.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has IntegerVector
   */
  public static class ShortSerializer implements ByteBufferSerializer<IntegerVector> {
    @Override
    public IntegerVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final short dimensionality = buffer.getShort();
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_INT * dimensionality);
      final int[] values = new int[dimensionality];
      for (int i = 0; i < dimensionality; i++) {
        values[i] = buffer.getInt();
      }
      return new IntegerVector(values, true);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, IntegerVector vec) throws IOException {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_INT * vec.values.length);
      buffer.putShort((short) vec.values.length);
      for (int i = 0; i < vec.values.length; i++) {
        buffer.putInt(vec.values[i]);
      }
    }

    @Override
    public int getByteSize(IntegerVector vec) {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      return ByteArrayUtil.SIZE_SHORT + ByteArrayUtil.SIZE_INT * vec.getDimensionality();
    }
  }

  /**
   * Serialization class for variable dimensionality by using VarInt encoding.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has IntegerVector
   */
  public static class VariableSerializer implements ByteBufferSerializer<IntegerVector> {
    @Override
    public IntegerVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final int dimensionality = ByteArrayUtil.readUnsignedVarint(buffer);
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_INT * dimensionality);
      final int[] values = new int[dimensionality];
      for (int i = 0; i < dimensionality; i++) {
        values[i] = ByteArrayUtil.readSignedVarint(buffer);
      }
      return new IntegerVector(values, true);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, IntegerVector vec) throws IOException {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      ByteArrayUtil.writeUnsignedVarint(buffer, vec.values.length);
      for (int i = 0; i < vec.values.length; i++) {
        ByteArrayUtil.writeSignedVarint(buffer, vec.values[i]);
      }
    }

    @Override
    public int getByteSize(IntegerVector vec) {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      int len = ByteArrayUtil.getUnsignedVarintSize(vec.values.length);
      for (int i = 0; i < vec.values.length; i++) {
        len += ByteArrayUtil.getSignedVarintSize(vec.values[i]);
      }
      return len;
    }
  }
}
