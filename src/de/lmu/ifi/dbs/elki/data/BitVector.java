package de.lmu.ifi.dbs.elki.data;

import java.io.IOException;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.persistent.ByteArraySerializer;
import de.lmu.ifi.dbs.elki.persistent.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Provides a BitVector wrapping a BitSet.
 * 
 * @author Arthur Zimek
 */
public class BitVector extends AbstractNumberVector<BitVector, Bit> implements ByteArraySerializer<BitVector> {
  /**
   * Storing the bits.
   */
  private BitSet bits;

  /**
   * Dimensionality of this bit vector.
   */
  private int dimensionality;

  /**
   * Provides a new BitVector corresponding to the specified bits and of the
   * specified dimensionality.
   * 
   * @param bits the bits to be set in this BitVector
   * @param dimensionality the dimensionality of this BitVector
   * @throws IllegalArgumentException if the specified dimensionality is to
   *         small to match the given BitSet
   */
  public BitVector(BitSet bits, int dimensionality) throws IllegalArgumentException {
    if(dimensionality < bits.length()) {
      throw new IllegalArgumentException("Specified dimensionality " + dimensionality + " is to low for specified BitSet of length " + bits.length());
    }
    this.bits = bits;
    this.dimensionality = dimensionality;
  }

  /**
   * Provides a new BitVector corresponding to the bits in the given array.
   * 
   * @param bits an array of bits specifying the bits in this bit vector
   */
  public BitVector(Bit[] bits) {
    this.bits = new BitSet(bits.length);
    for(int i = 0; i < bits.length; i++) {
      this.bits.set(i, bits[i].bitValue());
    }
    this.dimensionality = bits.length;
  }

  /**
   * Provides a new BitVector corresponding to the bits in the given list.
   * 
   * @param bits an array of bits specifying the bits in this bit vector
   */
  public BitVector(List<Bit> bits) {
    this.bits = new BitSet(bits.size());
    int i = 0;
    for(Bit bit : bits) {
      this.bits.set(i, bit.bitValue());
      i++;
    }
    this.dimensionality = bits.size();
  }

  /**
   * Returns a BitVector with random values.
   * 
   * @param random an instance of random to facilitate random values
   * @return a new instance of this BitVector with random values
   * 
   * @see de.lmu.ifi.dbs.elki.data.NumberVector#randomInstance(java.util.Random)
   */
  @Override
  public BitVector randomInstance(Random random) {
    Bit[] randomBits = new Bit[getDimensionality()];
    for(int i = 0; i < randomBits.length; i++) {
      randomBits[i] = new Bit(random.nextBoolean());
    }
    return new BitVector(randomBits);
  }

  /**
   * Returns the same as {@link BitVector#randomInstance(Random)
   * randomInstance(random)}.
   * 
   * @param min unused
   * @param max unused
   * @param random as in {@link BitVector#randomInstance(Random)
   *        randomInstance(random)}
   */
  @Override
  public BitVector randomInstance(Bit min, Bit max, Random random) {
    return randomInstance(random);
  }

  /**
   * Returns the same as {@link BitVector#randomInstance(Random)
   * randomInstance(random)}.
   * 
   * @param min unused
   * @param max unused
   * @param random as in {@link BitVector#randomInstance(Random)
   *        randomInstance(random)}
   */
  @Override
  public BitVector randomInstance(BitVector min, BitVector max, Random random) {
    return randomInstance(random);
  }

  /**
   * The dimensionality of the binary vector space of which this BitVector is an
   * element.
   * 
   * @see de.lmu.ifi.dbs.elki.data.NumberVector#getDimensionality()
   */
  @Override
  public int getDimensionality() {
    return dimensionality;
  }

  /**
   * Returns the value in the specified dimension.
   * 
   * @param dimension the desired dimension, where 1 &le; dimension &le;
   *        <code>this.getDimensionality()</code>
   * @return the value in the specified dimension
   * 
   * @see de.lmu.ifi.dbs.elki.data.NumberVector#getValue(int)
   */
  @Override
  public Bit getValue(int dimension) {
    if(dimension < 1 || dimension > dimensionality) {
      throw new IllegalArgumentException("illegal dimension: " + dimension);
    }
    return new Bit(bits.get(dimension - 1));
  }

  /**
   * Returns the value in the specified dimension as double.
   * 
   * @param dimension the desired dimension, where 1 &le; dimension &le;
   *        <code>this.getDimensionality()</code>
   * @return the value in the specified dimension
   * 
   * @see de.lmu.ifi.dbs.elki.data.NumberVector#doubleValue(int)
   */
  @Override
  public double doubleValue(int dimension) {
    if(dimension < 1 || dimension > dimensionality) {
      throw new IllegalArgumentException("illegal dimension: " + dimension);
    }
    return bits.get(dimension - 1) ? 1.0 : 0.0;
  }

  /**
   * Returns the value in the specified dimension as long.
   * 
   * @param dimension the desired dimension, where 1 &le; dimension &le;
   *        <code>this.getDimensionality()</code>
   * @return the value in the specified dimension
   * 
   * @see de.lmu.ifi.dbs.elki.data.NumberVector#longValue(int)
   */
  @Override
  public long longValue(int dimension) {
    if(dimension < 1 || dimension > dimensionality) {
      throw new IllegalArgumentException("illegal dimension: " + dimension);
    }
    return bits.get(dimension - 1) ? 1 : 0;
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
   * @see de.lmu.ifi.dbs.elki.data.NumberVector#getColumnVector()
   */
  @Override
  public Vector getColumnVector() {
    double[] values = new double[dimensionality];
    for(int i = 0; i < dimensionality; i++) {
      values[i] = bits.get(i) ? 1 : 0;
    }
    return new Vector(values);
  }

  /**
   * Returns a Matrix representing in one row and
   * <code>getDimensionality()</code> columns the values of this BitVector as
   * double values.
   * 
   * @return a Matrix representing in one row and
   *         <code>getDimensionality()</code> columns the values of this
   *         BitVector as double values
   * 
   * @see de.lmu.ifi.dbs.elki.data.NumberVector#getRowVector()
   */
  @Override
  public Matrix getRowVector() {
    double[] values = new double[dimensionality];
    for(int i = 0; i < dimensionality; i++) {
      values[i] = bits.get(i) ? 1 : 0;
    }
    return new Matrix(new double[][] { values });
  }

  /**
   * Returns a bit vector equal to this bit vector, if k is not 0, a bit vector
   * with all components equal to zero otherwise.
   * 
   * @param k used as multiplier 1 if k &ne; 0, otherwise the resulting bit
   *        vector will have all values equal to zero
   * @return a bit vector equal to this bit vector, if k is not 0, a bit vector
   *         with all components equal to zero otherwise
   */
  @Override
  public BitVector multiplicate(double k) {
    if(k == 0) {
      return nullVector();
    }
    else {
      return new BitVector(bits, dimensionality);
    }
  }

  /**
   * Returns the inverse of the bit vector.
   * 
   * The result is the same as obtained by flipping all bits in the underlying
   * BitSet.
   * 
   * @return the inverse of the bit vector
   * @see BitSet#flip(int,int)
   */
  @Override
  public BitVector negativeVector() {
    BitSet newBits = (BitSet) bits.clone();
    newBits.flip(0, dimensionality);
    return new BitVector(newBits, dimensionality);
  }

  /**
   * Returns a bit vector of equal dimensionality but containing 0 only.
   * 
   * @return a bit vector of equal dimensionality but containing 0 only
   */
  @Override
  public BitVector nullVector() {
    return new BitVector(new BitSet(), dimensionality);
  }

  /**
   * Returns a bit vector corresponding to an XOR operation on this and the
   * specified bit vector.
   * 
   * @param fv the bit vector to add
   * @return a new bit vector corresponding to an XOR operation on this and the
   *         specified bit vector
   */
  @Override
  public BitVector plus(BitVector fv) {
    if(this.getDimensionality() != fv.getDimensionality()) {
      throw new IllegalArgumentException("Incompatible dimensionality: " + this.getDimensionality() + " - " + fv.getDimensionality() + ".");
    }

    BitVector bv = new BitVector((BitSet) fv.bits.clone(), this.dimensionality);
    bv.bits.xor(this.bits);
    return bv;
  }

  /**
   * Returns a bit vector corresponding to an NXOR operation on this and the
   * specified bit vector.
   * 
   * @param fv the bit vector to add
   * @return a new bit vector corresponding to an NXOR operation on this and the
   *         specified bit vector
   */
  @Override
  public BitVector minus(BitVector fv) {
    if(this.getDimensionality() != fv.getDimensionality()) {
      throw new IllegalArgumentException("Incompatible dimensionality: " + this.getDimensionality() + " - " + fv.getDimensionality() + ".");
    }

    BitVector bv = new BitVector((BitSet) fv.bits.clone(), this.dimensionality);
    bv.bits.flip(0, dimensionality);
    bv.bits.xor(this.bits);
    return bv;
  }

  /**
   * Returns whether this BitVector contains all bits that are set to true in
   * the specified BitSet.
   * 
   * @param bitset the bits to inspect in this BitVector
   * @return true if this BitVector contains all bits that are set to true in
   *         the specified BitSet, false otherwise
   */
  public boolean contains(BitSet bitset) {
    boolean contains = true;
    for(int i = bitset.nextSetBit(0); i >= 0 && contains; i = bitset.nextSetBit(i + 1)) {
      // noinspection ConstantConditions
      contains &= bits.get(i);
    }
    return contains;
  }

  /**
   * Returns a copy of the bits currently set in this BitVector.
   * 
   * @return a copy of the bits currently set in this BitVector
   */
  public BitSet getBits() {
    return (BitSet) bits.clone();
  }

  /**
   * Returns a String representation of this BitVector. The representation is
   * suitable to be parsed by
   * {@link de.lmu.ifi.dbs.elki.parser.BitVectorLabelParser
   * BitVectorLabelParser}.
   * 
   * @see Object#toString()
   */
  @Override
  public String toString() {
    Bit[] bitArray = new Bit[dimensionality];
    for(int i = 0; i < dimensionality; i++) {
      bitArray[i] = new Bit(bits.get(i));
    }
    StringBuffer representation = new StringBuffer();
    for(Bit bit : bitArray) {
      if(representation.length() > 0) {
        representation.append(ATTRIBUTE_SEPARATOR);
      }
      representation.append(bit.toString());
    }
    return representation.toString();
  }

  /**
   * Indicates whether some other object is "equal to" this BitVector. This
   * BitVector is equal to the given object, if the object is a BitVector of
   * same dimensionality and with identical bits set.
   * 
   * @see DatabaseObject#equals(Object)
   */
  @Override
  public boolean equals(Object obj) {
    if(obj instanceof BitVector) {
      BitVector bv = (BitVector) obj;
      return this.getDimensionality() == bv.getDimensionality() && this.bits.equals(bv.bits);

    }
    else {
      return false;
    }
  }

  /**
   * Provides the scalar product (inner product) of this BitVector and the given
   * BitVector.
   * 
   * As multiplication of Bits, the logical AND operation is used. The result is
   * 0 if the number of bits after the AND operation is a multiple of 2,
   * otherwise the result is 1.
   * 
   * @param fv the BitVector to compute the scalar product for
   * @return the scalar product (inner product) of this and the given BitVector
   */
  @Override
  public Bit scalarProduct(BitVector fv) {
    if(this.getDimensionality() != fv.getDimensionality()) {
      throw new IllegalArgumentException("Incompatible dimensionality: " + this.getDimensionality() + " - " + fv.getDimensionality() + ".");
    }
    BitSet bs = (BitSet) this.bits.clone();
    bs.and(fv.bits);

    return new Bit(bs.cardinality() % 2 == 1);
  }

  @Override
  public BitVector newInstance(double[] values) {
    int dim = values.length;
    BitSet bits = new BitSet(dim);
    for(int i = 0; i < dim; i++) {
      if(values[i] >= 0.5) {
        bits.set(i);
      }
    }
    return new BitVector(bits, dim);
  }

  @Override
  public BitVector newInstance(Vector values) {
    int dim = values.getDimensionality();
    BitSet bits = new BitSet(dim);
    for(int i = 0; i < dim; i++) {
      if(values.get(i) >= 0.5) {
        bits.set(i);
      }
    }
    return new BitVector(bits, dim);
  }

  /**
   * Creates and returns a new BitVector based on the passed values.
   * 
   * @return a new instance of this BitVector with the specified values
   * 
   */
  @Override
  public BitVector newInstance(Bit[] values) {
    return new BitVector(values);
  }

  /**
   * Creates and returns a new BitVector based on the passed values.
   * 
   * @return a new instance of this BitVector with the specified values
   * 
   */
  @Override
  public BitVector newInstance(List<Bit> values) {
    return new BitVector(values);
  }

  @Override
  public Pair<BitVector, Integer> fromByteArray(byte[] buffer, int offset) throws IOException {
    short dimensionality = ByteArrayUtil.readShort(buffer, offset);
    final int len = ByteArrayUtil.SIZE_SHORT + (dimensionality + 7) / 8;
    if(buffer.length < offset + len) {
      throw new IOException("Not enough data for a bit vector!");
    }
    offset += ByteArrayUtil.SIZE_SHORT;
    // read values
    BitSet values = new BitSet(dimensionality);
    for(int i = 0; i < dimensionality; i++) {
      final int off = offset + (i >> 3);
      final byte bit = (byte) (1 << (i & 7));
      if((buffer[off] & bit) != 0) {
        values.set(i + 1);
      }
    }
    return new Pair<BitVector, Integer>(new BitVector(values, dimensionality), len);
  }

  @Override
  public int toByteArray(byte[] buffer, int offset, BitVector vec) throws IOException {
    final int len = getByteSize(vec);
    if(buffer.length < offset + len) {
      throw new IOException("Not enough space for the bit vector!");
    }
    // write size
    ByteArrayUtil.writeShort(buffer, offset, vec.getDimensionality());
    offset += ByteArrayUtil.SIZE_SHORT;
    // write values
    for(int i = 0; i < vec.getDimensionality(); i++) {
      final int off = offset + (i >> 3);
      final byte mask = (byte) (1 << (i & 7));
      if(vec.bits.get(i)) {
        buffer[off] |= mask;
      }
      else {
        buffer[off] &= ~mask;
      }
    }
    return len;
  }
  
  @Override
  public int getByteSize(BitVector vec) {
    return ByteArrayUtil.SIZE_SHORT + (vec.getDimensionality() + 7) / 8;
  }
}