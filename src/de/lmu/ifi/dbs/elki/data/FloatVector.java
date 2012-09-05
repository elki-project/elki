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
public class FloatVector extends AbstractNumberVector<FloatVector, Float> {
  /**
   * Static factory instance.
   */
  public static final FloatVector STATIC = new FloatVector(new float[0], true);

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
  private float[] values;

  /**
   * Private constructor. NOT for public use.
   * 
   * @param values Data values
   * @param nocopy Flag to re-use the values array
   */
  private FloatVector(float[] values, boolean nocopy) {
    if (nocopy) {
      this.values = values;
    } else {
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
    for (Iterator<Float> iter = values.iterator(); iter.hasNext(); i++) {
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
    for (int i = 0; i < values.length; i++) {
      this.values[i] = values[i];
    }
  }

  /**
   * Expects a matrix of one column.
   * 
   * @param columnMatrix a matrix of one column
   */
  public FloatVector(Vector columnMatrix) {
    values = new float[columnMatrix.getDimensionality()];
    for (int i = 0; i < values.length; i++) {
      values[i] = (float) columnMatrix.get(i);
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
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Dimension " + dimension + " out of range.");
    }
  }

  @Override
  public double doubleValue(int dimension) {
    try {
      return values[dimension - 1];
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Dimension " + dimension + " out of range.");
    }
  }

  @Override
  public long longValue(int dimension) {
    try {
      return (long) values[dimension - 1];
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Dimension " + dimension + " out of range.");
    }
  }

  @Override
  public Vector getColumnVector() {
    return new Vector(ArrayLikeUtil.toPrimitiveDoubleArray(values, ArrayLikeUtil.FLOATARRAYADAPTER));
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

  @Override
  public <A> FloatVector newFeatureVector(A array, ArrayAdapter<Float, A> adapter) {
    int dim = adapter.size(array);
    float[] values = new float[dim];
    for (int i = 0; i < dim; i++) {
      values[i] = adapter.get(array, i);
    }
    return new FloatVector(values, true);
  }

  @Override
  public <A> FloatVector newNumberVector(A array, NumberArrayAdapter<?, A> adapter) {
    int dim = adapter.size(array);
    float[] values = new float[dim];
    for (int i = 0; i < dim; i++) {
      values[i] = adapter.getFloat(array, i);
    }
    return new FloatVector(values, true);
  }

  /**
   * Serialization class for dense float vectors with up to 127 dimensions, by
   * using a byte for storing the dimensionality.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has FloatVector
   */
  private static class SmallSerializer implements ByteBufferSerializer<FloatVector> {
    @Override
    public FloatVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final byte dimensionality = buffer.get();
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_FLOAT * dimensionality);
      final float[] values = new float[dimensionality];
      for (int i = 0; i < dimensionality; i++) {
        values[i] = buffer.getFloat();
      }
      return new FloatVector(values, true);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, FloatVector vec) throws IOException {
      assert (vec.values.length < Byte.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Byte.MAX_VALUE + "!";
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_FLOAT * vec.values.length);
      buffer.put((byte) vec.values.length);
      for (int i = 0; i < vec.values.length; i++) {
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
   * @apiviz.has FloatVector
   */
  private static class ShortSerializer implements ByteBufferSerializer<FloatVector> {
    @Override
    public FloatVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final short dimensionality = buffer.getShort();
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_FLOAT * dimensionality);
      final float[] values = new float[dimensionality];
      for (int i = 0; i < dimensionality; i++) {
        values[i] = buffer.getFloat();
      }
      return new FloatVector(values, true);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, FloatVector vec) throws IOException {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_FLOAT * vec.values.length);
      buffer.putShort((short) vec.values.length);
      for (int i = 0; i < vec.values.length; i++) {
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
   * @apiviz.has FloatVector
   */
  private static class VariableSerializer implements ByteBufferSerializer<FloatVector> {
    @Override
    public FloatVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final int dimensionality = ByteArrayUtil.readUnsignedVarint(buffer);
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_FLOAT * dimensionality);
      final float[] values = new float[dimensionality];
      for (int i = 0; i < dimensionality; i++) {
        values[i] = buffer.getFloat();
      }
      return new FloatVector(values, true);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, FloatVector vec) throws IOException {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      assert (buffer.remaining() >= ByteArrayUtil.SIZE_FLOAT * vec.values.length);
      ByteArrayUtil.writeUnsignedVarint(buffer, vec.values.length);
      for (int i = 0; i < vec.values.length; i++) {
        buffer.putFloat(vec.values[i]);
      }
    }

    @Override
    public int getByteSize(FloatVector vec) {
      assert (vec.values.length < Short.MAX_VALUE) : "This serializer only supports a maximum dimensionality of " + Short.MAX_VALUE + "!";
      return ByteArrayUtil.getUnsignedVarintSize(vec.values.length) + ByteArrayUtil.SIZE_FLOAT * vec.values.length;
    }
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
    protected FloatVector makeInstance() {
      return STATIC;
    }
  }
}
