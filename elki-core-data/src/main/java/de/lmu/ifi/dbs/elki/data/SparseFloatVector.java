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
import java.util.Arrays;

import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.io.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * Sparse vector type, using {@code float[]} for storing the values, and
 * {@code int[]} for storing the indexes, approximately 8 bytes per non-zero
 * value.
 *
 * @author Arthur Zimek
 * @since 0.2
 */
public class SparseFloatVector implements SparseNumberVector {
  /**
   * Static instance.
   */
  public static final SparseFloatVector.Factory FACTORY = new SparseFloatVector.Factory();

  /**
   * Serializer using varint encoding.
   */
  public static final ByteBufferSerializer<SparseFloatVector> VARIABLE_SERIALIZER = new VariableSerializer();

  /**
   * Float constant, for missing values in (inefficient) {@link #getValue} API.
   */
  private static final float FLOAT0 = 0.f;

  /**
   * Indexes of values.
   */
  private final int[] indexes;

  /**
   * Stored values.
   */
  private final float[] values;

  /**
   * The dimensionality of this feature vector.
   */
  private int dimensionality;

  /**
   * Direct constructor.
   *
   * @param indexes Indexes Must be sorted!
   * @param values Associated value.
   * @param dimensionality "true" dimensionality
   */
  public SparseFloatVector(int[] indexes, float[] values, int dimensionality) {
    super();
    this.indexes = indexes;
    this.values = values;
    this.dimensionality = dimensionality;
  }

  /**
   * Create a SparseFloatVector consisting of double values according to the
   * specified mapping of indices and values.
   *
   * @param values the values to be set as values of the real vector
   * @param dimensionality the dimensionality of this feature vector
   * @throws IllegalArgumentException if the given dimensionality is too small
   *         to cover the given values (i.e., the maximum index of any value not
   *         zero is bigger than the given dimensionality)
   */
  public SparseFloatVector(Int2FloatOpenHashMap values, int dimensionality) throws IllegalArgumentException {
    if(values.size() > dimensionality) {
      throw new IllegalArgumentException("values.size() > dimensionality!");
    }

    this.indexes = new int[values.size()];
    this.values = new float[values.size()];
    // Import and sort the indexes
    {
      ObjectIterator<Int2FloatMap.Entry> iter = values.int2FloatEntrySet().fastIterator();
      for(int i = 0; iter.hasNext(); i++) {
        this.indexes[i] = iter.next().getIntKey();
      }
      Arrays.sort(this.indexes);
    }
    // Import the values accordingly
    {
      for(int i = 0; i < values.size(); i++) {
        this.values[i] = values.get(this.indexes[i]);
      }
    }
    this.dimensionality = dimensionality;
    final int maxdim = getMaxDim();
    if(maxdim > dimensionality) {
      throw new IllegalArgumentException("Given dimensionality " + dimensionality + " is too small w.r.t. the given values (occurring maximum: " + maxdim + ").");
    }
  }

  /**
   * Get the maximum dimensionality.
   *
   * @return the maximum dimensionality seen
   */
  private int getMaxDim() {
    return (this.indexes.length == 0) ? 0 : this.indexes[this.indexes.length - 1];
  }

  /**
   * Create a SparseFloatVector consisting of double values according to the
   * specified mapping of indices and values.
   *
   * @param values the values to be set as values of the real vector
   * @throws IllegalArgumentException if the given dimensionality is too small
   *         to cover the given values (i.e., the maximum index of any value not
   *         zero is bigger than the given dimensionality)
   */
  public SparseFloatVector(float[] values) throws IllegalArgumentException {
    this.dimensionality = values.length;

    // Count the number of non-zero entries
    int size = 0;
    {
      for(int i = 0; i < values.length; i++) {
        if(values[i] != 0.0f) {
          size++;
        }
      }
    }
    this.indexes = new int[size];
    this.values = new float[size];

    // Copy the values
    {
      int pos = 0;
      for(int i = 0; i < values.length; i++) {
        float value = values[i];
        if(value != 0.0f) {
          this.indexes[pos] = i;
          this.values[pos] = value;
          pos++;
        }
      }
    }
  }

  @Override
  public int getDimensionality() {
    return dimensionality;
  }

  /**
   * Sets the dimensionality to the new value.
   *
   *
   * @param dimensionality the new dimensionality
   * @throws IllegalArgumentException if the given dimensionality is too small
   *         to cover the given values (i.e., the maximum index of any value not
   *         zero is bigger than the given dimensionality)
   */
  @Override
  public void setDimensionality(int dimensionality) throws IllegalArgumentException {
    final int maxdim = getMaxDim();
    if(maxdim > dimensionality) {
      throw new IllegalArgumentException("Given dimensionality " + dimensionality + " is too small w.r.t. the given values (occurring maximum: " + maxdim + ").");
    }
    this.dimensionality = dimensionality;
  }

  @Override
  @Deprecated
  public Float getValue(int dimension) {
    int pos = Arrays.binarySearch(this.indexes, dimension);
    return (pos >= 0) ? values[pos] : FLOAT0;
  }

  @Override
  @Deprecated
  public double doubleValue(int dimension) {
    int pos = Arrays.binarySearch(this.indexes, dimension);
    return (pos >= 0) ? values[pos] : 0.;
  }

  @Override
  @Deprecated
  public float floatValue(int dimension) {
    int pos = Arrays.binarySearch(this.indexes, dimension);
    return (pos >= 0) ? values[pos] : 0.f;
  }

  @Override
  @Deprecated
  public long longValue(int dimension) {
    int pos = Arrays.binarySearch(this.indexes, dimension);
    return (pos >= 0) ? (long) values[pos] : 0L;
  }

  @Override
  public double[] toArray() {
    double[] vals = new double[dimensionality];
    for(int i = 0; i < indexes.length; i++) {
      vals[this.indexes[i]] = this.values[i];
    }
    return vals;
  }

  /**
   * Create a String representation of this SparseFloatVector as suitable for
   * {@link de.lmu.ifi.dbs.elki.datasource.parser.SparseNumberVectorLabelParser}
   * .
   *
   * The returned String is a single line with entries separated by
   * {@link NumberVector#ATTRIBUTE_SEPARATOR}. The first entry gives the
   * number of values actually not zero. Following entries are pairs of Integer
   * and Float where the Integer gives the index of the dimensionality and the
   * Float gives the corresponding value.
   *
   * Example: a vector (0,1.2,1.3,0)<sup>T</sup> would result in the String<br>
   * <code>2 2 1.2 3 1.3</code><br>
   *
   * @return a String representation of this SparseFloatVector
   */
  @Override
  public String toString() {
    StringBuilder featureLine = new StringBuilder(15 * this.indexes.length)//
        .append(this.indexes.length);
    for(int i = 0; i < this.indexes.length; i++) {
      featureLine.append(ATTRIBUTE_SEPARATOR).append(this.indexes[i])//
          .append(ATTRIBUTE_SEPARATOR).append(this.values[i]);
    }
    return featureLine.toString();
  }

  @Override
  public int iterDim(int iter) {
    return indexes[iter];
  }

  @Override
  public boolean iterValid(int iter) {
    return iter < indexes.length;
  }

  @Override
  public double iterDoubleValue(int iter) {
    return (double) values[iter];
  }

  @Override
  public float iterFloatValue(int iter) {
    return values[iter];
  }

  @Override
  public long iterLongValue(int iter) {
    return (long) values[iter];
  }

  /**
   * Factory class.
   *
   * @author Erich Schubert
   *
   * @has - - - SparseFloatVector
   */
  public static class Factory implements SparseNumberVector.Factory<SparseFloatVector> {
    @Override
    public <A> SparseFloatVector newFeatureVector(A array, ArrayAdapter<? extends Number, A> adapter) {
      int dim = adapter.size(array);
      float[] values = new float[dim];
      for(int i = 0; i < dim; i++) {
        values[i] = adapter.get(array, i).floatValue();
      }
      // TODO: inefficient
      return new SparseFloatVector(values);
    }

    @Override
    public <A> SparseFloatVector newNumberVector(A array, NumberArrayAdapter<?, ? super A> adapter) {
      int dim = adapter.size(array);
      float[] values = new float[dim];
      for(int i = 0; i < dim; i++) {
        values[i] = adapter.getFloat(array, i);
      }
      // TODO: inefficient
      return new SparseFloatVector(values);
    }

    @Override
    public SparseFloatVector newNumberVector(Int2DoubleOpenHashMap dvalues, int maxdim) {
      int[] indexes = new int[dvalues.size()];
      float[] values = new float[dvalues.size()];
      // Import and sort the indexes
      ObjectIterator<Int2DoubleMap.Entry> iter = dvalues.int2DoubleEntrySet().fastIterator();
      for(int i = 0; iter.hasNext(); i++) {
        indexes[i] = iter.next().getIntKey();
      }
      Arrays.sort(indexes);
      // Import the values accordingly
      for(int i = 0; i < dvalues.size(); i++) {
        values[i] = (float) dvalues.get(indexes[i]);
      }
      return new SparseFloatVector(indexes, values, maxdim);
    }

    @Override
    public ByteBufferSerializer<SparseFloatVector> getDefaultSerializer() {
      return VARIABLE_SERIALIZER;
    }

    @Override
    public Class<? super SparseFloatVector> getRestrictionClass() {
      return SparseFloatVector.class;
    }

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
     */
    public static class Parameterizer extends AbstractParameterizer {
      @Override
      protected SparseFloatVector.Factory makeInstance() {
        return FACTORY;
      }
    }
  }

  /**
   * Serialization class using VarInt encodings.
   *
   * @author Erich Schubert
   *
   * @assoc - serializes - SparseFloatVector
   */
  public static class VariableSerializer implements ByteBufferSerializer<SparseFloatVector> {
    @Override
    public SparseFloatVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final int dimensionality = ByteArrayUtil.readUnsignedVarint(buffer);
      final int nonzero = ByteArrayUtil.readUnsignedVarint(buffer);
      final int[] dims = new int[nonzero];
      final float[] values = new float[nonzero];
      for(int i = 0; i < nonzero; i++) {
        dims[i] = ByteArrayUtil.readUnsignedVarint(buffer);
        values[i] = buffer.getFloat();
      }
      return new SparseFloatVector(dims, values, dimensionality);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, SparseFloatVector vec) throws IOException {
      ByteArrayUtil.writeUnsignedVarint(buffer, vec.dimensionality);
      ByteArrayUtil.writeUnsignedVarint(buffer, vec.values.length);
      for(int i = 0; i < vec.values.length; i++) {
        ByteArrayUtil.writeUnsignedVarint(buffer, vec.indexes[i]);
        buffer.putFloat(vec.values[i]);
      }
    }

    @Override
    public int getByteSize(SparseFloatVector vec) {
      int sum = 0;
      sum += ByteArrayUtil.getUnsignedVarintSize(vec.dimensionality);
      sum += ByteArrayUtil.getUnsignedVarintSize(vec.values.length);
      for(int d : vec.indexes) {
        sum += ByteArrayUtil.getUnsignedVarintSize(d);
      }
      sum += vec.values.length * ByteArrayUtil.SIZE_FLOAT;
      return sum;
    }
  }
}
