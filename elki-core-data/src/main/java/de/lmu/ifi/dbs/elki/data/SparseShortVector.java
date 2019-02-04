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
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * Sparse vector type, using {@code short[]} for storing the values, and
 * {@code int[]} for storing the indexes, approximately 6 bytes per non-zero
 * value.
 *
 * @author Arthur Zimek
 * @since 0.6.0
 */
public class SparseShortVector implements SparseNumberVector {
  /**
   * Static instance.
   */
  public static final SparseShortVector.Factory FACTORY = new SparseShortVector.Factory();

  /**
   * Serializer using varint encoding.
   */
  public static final ByteBufferSerializer<SparseShortVector> VARIABLE_SERIALIZER = new VariableSerializer();

  /**
   * Constant value, for use in (inefficient) {@link #getValue} API.
   */
  private static final short SHORT0 = 0;

  /**
   * Indexes of values.
   */
  private final int[] indexes;

  /**
   * Stored values.
   */
  private final short[] values;

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
  public SparseShortVector(int[] indexes, short[] values, int dimensionality) {
    super();
    this.indexes = indexes;
    this.values = values;
    this.dimensionality = dimensionality;
  }

  /**
   * Create a SparseShortVector consisting of double values according to the
   * specified mapping of indices and values.
   *
   * @param values the values to be set as values of the real vector
   * @param dimensionality the dimensionality of this feature vector
   * @throws IllegalArgumentException if the given dimensionality is too small
   *         to cover the given values (i.e., the maximum index of any value not
   *         zero is bigger than the given dimensionality)
   */
  public SparseShortVector(Int2DoubleOpenHashMap values, int dimensionality) throws IllegalArgumentException {
    if(values.size() > dimensionality) {
      throw new IllegalArgumentException("values.size() > dimensionality!");
    }

    this.indexes = new int[values.size()];
    this.values = new short[values.size()];
    // Import and sort the indexes
    {
      ObjectIterator<Int2DoubleMap.Entry> iter = values.int2DoubleEntrySet().fastIterator();
      for(int i = 0; iter.hasNext(); i++) {
        this.indexes[i] = iter.next().getIntKey();
      }
      Arrays.sort(this.indexes);
    }
    // Import the values accordingly
    {
      for(int i = 0; i < values.size(); i++) {
        this.values[i] = (short) values.get(this.indexes[i]);
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
   * Create a SparseShortVector consisting of double values according to the
   * specified mapping of indices and values.
   *
   * @param values the values to be set as values of the real vector
   * @throws IllegalArgumentException if the given dimensionality is too small
   *         to cover the given values (i.e., the maximum index of any value not
   *         zero is bigger than the given dimensionality)
   */
  public SparseShortVector(short[] values) throws IllegalArgumentException {
    this.dimensionality = values.length;

    // Count the number of non-zero entries
    int size = 0;
    {
      for(int i = 0; i < values.length; i++) {
        if(values[i] != 0) {
          size++;
        }
      }
    }
    this.indexes = new int[size];
    this.values = new short[size];

    // Copy the values
    {
      int pos = 0;
      for(int i = 0; i < values.length; i++) {
        short value = values[i];
        if(value != 0) {
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
  public Short getValue(int dimension) {
    int pos = Arrays.binarySearch(this.indexes, dimension);
    return (pos >= 0) ? values[pos] : SHORT0;
  }

  @Override
  @Deprecated
  public double doubleValue(int dimension) {
    int pos = Arrays.binarySearch(this.indexes, dimension);
    return (pos >= 0) ? values[pos] : 0.;
  }

  @Override
  @Deprecated
  public long longValue(int dimension) {
    int pos = Arrays.binarySearch(this.indexes, dimension);
    return (pos >= 0) ? values[pos] : 0L;
  }

  @Override
  @Deprecated
  public short shortValue(int dimension) {
    int pos = Arrays.binarySearch(this.indexes, dimension);
    return (pos >= 0) ? values[pos] : 0;
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
   * Create a String representation of this SparseShortVector as suitable for
   * {@link de.lmu.ifi.dbs.elki.datasource.parser.SparseNumberVectorLabelParser}
   * .
   *
   * The returned String is a single line with entries separated by
   * {@link NumberVector#ATTRIBUTE_SEPARATOR}. The first entry gives the
   * number of values actually not zero. Following entries are pairs of Short
   * and Short where the Short gives the index of the dimensionality and the
   * Short gives the corresponding value.
   *
   * Example: a vector (0,1.2,1.3,0)<sup>T</sup> would result in the String<br>
   * <code>2 2 1.2 3 1.3</code><br>
   *
   * @return a String representation of this SparseShortVector
   */
  @Override
  public String toString() {
    StringBuilder featureLine = new StringBuilder(10 * this.indexes.length)//
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
  public short iterShortValue(int iter) {
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
   * @has - - - SparseShortVector
   */
  public static class Factory implements SparseNumberVector.Factory<SparseShortVector> {
    @Override
    public <A> SparseShortVector newFeatureVector(A array, ArrayAdapter<? extends Number, A> adapter) {
      int dim = adapter.size(array);
      short[] values = new short[dim];
      for(int i = 0; i < dim; i++) {
        values[i] = adapter.get(array, i).shortValue();
      }
      // TODO: improve efficiency
      return new SparseShortVector(values);
    }

    @Override
    public <A> SparseShortVector newNumberVector(A array, NumberArrayAdapter<?, ? super A> adapter) {
      int dim = adapter.size(array);
      short[] values = new short[dim];
      for(int i = 0; i < dim; i++) {
        values[i] = adapter.getShort(array, i);
      }
      // TODO: improve efficiency
      return new SparseShortVector(values);
    }

    @Override
    public SparseShortVector newNumberVector(Int2DoubleOpenHashMap values, int maxdim) {
      return new SparseShortVector(values, maxdim);
    }

    @Override
    public ByteBufferSerializer<SparseShortVector> getDefaultSerializer() {
      return VARIABLE_SERIALIZER;
    }

    @Override
    public Class<? super SparseShortVector> getRestrictionClass() {
      return SparseShortVector.class;
    }

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
     */
    public static class Parameterizer extends AbstractParameterizer {
      @Override
      protected SparseShortVector.Factory makeInstance() {
        return FACTORY;
      }
    }
  }

  /**
   * Serialization class using VarInt encodings.
   *
   * @author Erich Schubert
   *
   * @assoc - serializes - SparseShortVector
   */
  public static class VariableSerializer implements ByteBufferSerializer<SparseShortVector> {
    @Override
    public SparseShortVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      final int dimensionality = ByteArrayUtil.readUnsignedVarint(buffer);
      final int nonzero = ByteArrayUtil.readUnsignedVarint(buffer);
      final int[] dims = new int[nonzero];
      final short[] values = new short[nonzero];
      for(int i = 0; i < nonzero; i++) {
        dims[i] = ByteArrayUtil.readUnsignedVarint(buffer);
        values[i] = (short) ByteArrayUtil.readSignedVarint(buffer);
      }
      return new SparseShortVector(dims, values, dimensionality);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, SparseShortVector vec) throws IOException {
      ByteArrayUtil.writeUnsignedVarint(buffer, vec.dimensionality);
      ByteArrayUtil.writeUnsignedVarint(buffer, vec.values.length);
      for(int i = 0; i < vec.values.length; i++) {
        ByteArrayUtil.writeUnsignedVarint(buffer, vec.indexes[i]);
        ByteArrayUtil.writeSignedVarint(buffer, vec.values[i]);
      }
    }

    @Override
    public int getByteSize(SparseShortVector vec) {
      int sum = 0;
      sum += ByteArrayUtil.getUnsignedVarintSize(vec.dimensionality);
      sum += ByteArrayUtil.getUnsignedVarintSize(vec.values.length);
      for(int i = 0; i < vec.values.length; i++) {
        sum += ByteArrayUtil.getUnsignedVarintSize(vec.indexes[i]);
        sum += ByteArrayUtil.getSignedVarintSize(vec.values[i]);
      }
      return sum;
    }
  }
}
