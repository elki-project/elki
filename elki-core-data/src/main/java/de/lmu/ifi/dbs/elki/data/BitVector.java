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

import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.io.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * Vector using a dense bit set encoding, based on {@code long[]} storage.
 *
 * @author Arthur Zimek
 * @since 0.1
 *
 * @composed - - - Bit
 */
public class BitVector implements SparseNumberVector {
  /**
   * Static instance.
   */
  public static final BitVector.Factory FACTORY = new BitVector.Factory();

  /**
   * Serializer for up to 2^15-1 dimensions.
   */
  public static final ByteBufferSerializer<BitVector> SHORT_SERIALIZER = new ShortSerializer();

  /**
   * Storing the bits.
   */
  private final long[] bits;

  /**
   * Dimensionality of this bit vector.
   */
  private int dimensionality;

  /**
   * Create a new BitVector corresponding to the specified bits and of the
   * specified dimensionality.
   *
   * @param bits the bits to be set in this BitVector.
   * @param dimensionality the dimensionality of this BitVector
   */
  public BitVector(long[] bits, int dimensionality) {
    this.bits = bits;
    this.dimensionality = dimensionality;
  }

  @Override
  public int getDimensionality() {
    return dimensionality;
  }

  @Override
  public void setDimensionality(int dimensionality) {
    this.dimensionality = dimensionality;
  }

  /**
   * Get the value of a single bit.
   *
   * @param dimension Bit number to get
   * @return {@code true} when set
   */
  public boolean booleanValue(int dimension) {
    return BitsUtil.get(bits, dimension);
  }

  @Override
  @Deprecated
  public Bit getValue(int dimension) {
    return new Bit(booleanValue(dimension));
  }

  @Override
  public double doubleValue(int dimension) {
    return BitsUtil.get(bits, dimension) ? 1. : 0.;
  }

  @Override
  public long longValue(int dimension) {
    return BitsUtil.get(bits, dimension) ? 1L : 0L;
  }

  @Override
  public int iter() {
    return BitsUtil.nextSetBit(bits, 0);
  }

  @Override
  public int iterAdvance(int iter) {
    return BitsUtil.nextSetBit(bits, iter + 1);
  }

  @Override
  public int iterRetract(int iter) {
    return BitsUtil.previousSetBit(bits, iter - 1);
  }

  @Override
  public boolean iterValid(int iter) {
    return iter >= 0;
  }

  @Override
  public int iterDim(int iter) {
    return iter; // Identity
  }

  @Override
  public double iterDoubleValue(int iter) {
    return 1.; // When properly used: always true!
  }

  @Override
  public long iterLongValue(int iter) {
    return 1L; // When properly used: always true!
  }

  /**
   * Returns a Vector representing in one column and
   * <code>getDimensionality()</code> rows the values of this BitVector as
   * double values.
   *
   * @return a Matrix representing in one column and
   *         <code>getDimensionality()</code> rows the values of this BitVector
   *         as double values
   *
   * @see de.lmu.ifi.dbs.elki.data.NumberVector#toArray()
   */
  @Override
  public double[] toArray() {
    double[] data = new double[dimensionality];
    for(int i = BitsUtil.nextSetBit(bits, 0); i >= 0; i = BitsUtil.nextSetBit(bits, i + 1)) {
      data[i] = 1;
    }
    return data;
  }

  /**
   * Returns whether this BitVector contains all bits that are set to true in
   * the specified BitSet.
   *
   * @param bitset the bits to inspect in this BitVector
   * @return true if this BitVector contains all bits that are set to true in
   *         the specified BitSet, false otherwise
   */
  public boolean contains(long[] bitset) {
    for(int i = 0; i < bitset.length; i++) {
      final long b = bitset[i];
      if(i >= bits.length && b != 0L) {
        return false;
      }
      if((b & bits[i]) != b) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns a copy of the bits currently set in this BitVector.
   *
   * @return a copy of the bits currently set in this BitVector
   */
  public long[] cloneBits() {
    return bits.clone();
  }

  /**
   * Compute the vector cardinality (uncached!)
   *
   * @return Vector cardinality
   */
  public int cardinality() {
    return BitsUtil.cardinality(bits);
  }

  /**
   * Compute the Jaccard similarity of two bit vectors.
   *
   * @param v2 Second bit vector
   * @return Jaccard similarity (intersection / union)
   */
  public double jaccardSimilarity(BitVector v2) {
    return BitsUtil.intersectionSize(bits, v2.bits) / (double) BitsUtil.unionSize(bits, v2.bits);
  }

  /**
   * Compute the Hamming distance of two bit vectors.
   *
   * @param v2 Second bit vector
   * @return Hamming distance (number of bits difference)
   */
  public int hammingDistance(BitVector v2) {
    return BitsUtil.hammingDistance(bits, v2.bits);
  }

  /**
   * Compute the vector intersection size.
   *
   * @param v2 Second bit vector
   * @return Intersection size (number of bits in both)
   */
  public int intersectionSize(BitVector v2) {
    return BitsUtil.intersectionSize(bits, v2.bits);
  }

  /**
   * Compute the vector union size.
   *
   * @param v2 Second bit vector
   * @return Intersection size (number of bits in both)
   */
  public int unionSize(BitVector v2) {
    return BitsUtil.unionSize(bits, v2.bits);
  }

  /**
   * Compute whether two vectors intersect.
   *
   * @param v2 Second bit vector
   * @return {@code true} if they intersect in at least one bit.
   */
  public boolean intersect(BitVector v2) {
    return BitsUtil.intersect(bits, v2.bits);
  }

  /**
   * Combine onto v using the AND operation, i.e. {@code v &= this}.
   *
   * @param v Existing bit set of same length.
   */
  public void andOnto(long[] v) {
    BitsUtil.andI(v, bits);
  }

  /**
   * Combine onto v using the OR operation, i.e. {@code v |= this}.
   *
   * @param v Existing bit set of same length.
   */
  public void orOnto(long[] v) {
    BitsUtil.orI(v, bits);
  }

  /**
   * Combine onto v using the XOR operation, i.e. {@code v ^= this}.
   *
   * @param v Existing bit set of same length.
   */
  public void xorOnto(long[] v) {
    BitsUtil.xorI(v, bits);
  }

  /**
   * Returns a String representation of this BitVector. The representation is
   * suitable to be parsed by
   * {@link de.lmu.ifi.dbs.elki.datasource.parser.BitVectorLabelParser
   * BitVectorLabelParser}.
   *
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder representation = new StringBuilder(dimensionality);
    for(int i = 0; i < dimensionality; i++) {
      if(i > 0) {
        representation.append(ATTRIBUTE_SEPARATOR);
      }
      representation.append(BitsUtil.get(bits, i) ? '1' : '0');
    }
    return representation.toString();
  }

  /**
   * Indicates whether some other object is "equal to" this BitVector. This
   * BitVector is equal to the given object, if the object is a BitVector of
   * same dimensionality and with identical bits set.
   *
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if(obj instanceof BitVector) {
      BitVector bv = (BitVector) obj;
      return this.getDimensionality() == bv.getDimensionality() && Arrays.equals(this.bits, bv.bits);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return BitsUtil.hashCode(bits);
  }

  /**
   * Factory for bit vectors.
   *
   * @author Erich Schubert
   *
   * @has - - - BitVector
   */
  public static class Factory implements SparseNumberVector.Factory<BitVector> {
    @Override
    public <A> BitVector newFeatureVector(A array, ArrayAdapter<? extends Number, A> adapter) {
      int dim = adapter.size(array);
      long[] bits = BitsUtil.zero(dim);
      for(int i = 0; i < dim; i++) {
        if(adapter.get(array, i).doubleValue() >= 0.5) {
          BitsUtil.setI(bits, i);
        }
      }
      return new BitVector(bits, dim);
    }

    @Override
    public <A> BitVector newNumberVector(A array, NumberArrayAdapter<?, ? super A> adapter) {
      int dim = adapter.size(array);
      long[] bits = BitsUtil.zero(dim);
      for(int i = 0; i < dim; i++) {
        if(adapter.getDouble(array, i) >= 0.5) {
          BitsUtil.setI(bits, i);
        }
      }
      return new BitVector(bits, dim);
    }

    @Override
    public BitVector newNumberVector(Int2DoubleOpenHashMap values, int maxdim) {
      long[] bits = BitsUtil.zero(maxdim);
      // Import and sort the indexes
      for(ObjectIterator<Int2DoubleMap.Entry> iter = values.int2DoubleEntrySet().iterator(); iter.hasNext();) {
        Int2DoubleMap.Entry entry = iter.next();
        if(entry.getDoubleValue() != 0.) {
          BitsUtil.setI(bits, entry.getIntKey());
        }
      }
      return new BitVector(bits, maxdim);
    }

    @Override
    public ByteBufferSerializer<BitVector> getDefaultSerializer() {
      return SHORT_SERIALIZER;
    }

    @Override
    public Class<? super BitVector> getRestrictionClass() {
      return BitVector.class;
    }

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
     */
    public static class Parameterizer extends AbstractParameterizer {
      @Override
      protected BitVector.Factory makeInstance() {
        return FACTORY;
      }
    }
  }

  /**
   * Serialization class for dense integer vectors with up to
   * {@link Short#MAX_VALUE} dimensions, by using a short for storing the
   * dimensionality.
   *
   * @author Erich Schubert
   *
   * @assoc - serializes - BitVector
   */
  public static class ShortSerializer implements ByteBufferSerializer<BitVector> {
    @Override
    public BitVector fromByteBuffer(ByteBuffer buffer) throws IOException {
      short dimensionality = buffer.getShort();
      final int len = ByteArrayUtil.SIZE_SHORT + (dimensionality + 7) / 8;
      if(buffer.remaining() < len) {
        throw new IOException("Not enough data for a bit vector!");
      }
      // read values
      long[] bits = BitsUtil.zero(dimensionality);
      byte b = 0;
      for(int i = 0; i < dimensionality; i++) {
        // read the next byte when needed.
        if((i & 7) == 0) {
          b = buffer.get();
        }
        final byte bit = (byte) (1 << (i & 7));
        if((b & bit) != 0) {
          BitsUtil.setI(bits, i);
        }
      }
      return new BitVector(bits, dimensionality);
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, BitVector vec) throws IOException {
      final int len = getByteSize(vec);
      assert (vec.getDimensionality() <= Short.MAX_VALUE);
      final short dim = (short) vec.getDimensionality();
      if(buffer.remaining() < len) {
        throw new IOException("Not enough space for the bit vector!");
      }
      // write size
      buffer.putShort(dim);
      // write values
      // Next byte to write:
      byte b = 0;
      for(int i = 0; i < dim; i++) {
        final byte mask = (byte) (1 << (i & 7));
        if(BitsUtil.get(vec.bits, i)) {
          b |= mask;
        }
        else {
          b &= ~mask;
        }
        // Write when appropriate
        if((i & 7) == 7 || i == dim - 1) {
          buffer.put(b);
          b = 0;
        }
      }
    }

    @Override
    public int getByteSize(BitVector vec) {
      return ByteArrayUtil.SIZE_SHORT + (vec.getDimensionality() + 7) / 8;
    }
  }
}
