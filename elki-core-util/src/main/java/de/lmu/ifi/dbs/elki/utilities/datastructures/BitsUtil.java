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
package de.lmu.ifi.dbs.elki.utilities.datastructures;

import java.util.Arrays;
import java.util.Random;

import it.unimi.dsi.fastutil.Hash.Strategy;

/**
 * Utilities for bit operations.
 * <p>
 * Implementation note: words are stored in little-endian word order. This can
 * be a bit confusing, because a shift-right means "left" on the word level.
 * <p>
 * Naming: methods with a <code>C</code> return a copy, methods with
 * <code>I</code> modify in-place.
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
public final class BitsUtil {
  /**
   * Private constructor. Static methods only.
   */
  private BitsUtil() {
    // Do not use.
  }

  /**
   * Shift factor for a long: 2^6 == 64 == Long.SIZE
   */
  private static final int LONG_LOG2_SIZE = 6;

  /** Masking for long shifts. */
  private static final int LONG_LOG2_MASK = 0x3f; // 6 bits

  /** Long with all bits set */
  private static final long LONG_ALL_BITS = -1L;

  /** Long, with 63 bits set */
  private static final long LONG_63_BITS = 0x7FFFFFFFFFFFFFFFL;

  /** Masking 32 bit **/
  private static final long LONG_32_BITS = 0xFFFFFFFFL;

  /** Precomputed powers of 5 for pow5, pow10 on the bit representation. */
  private static final int[] POW5_INT = { //
      1, 5, 25, 125, 625, //
      3125, 15625, 78125, 390625, 1953125, //
      9765625, 48828125, 244140625, 1220703125 };

  /**
   * Hashing strategy to use with Fastutils.
   */
  public static final Strategy<long[]> FASTUTIL_HASH_STRATEGY = new Strategy<long[]>() {
    @Override
    public int hashCode(long[] o) {
      return BitsUtil.hashCode(o);
    }

    @Override
    public boolean equals(long[] a, long[] b) {
      return equal(a, b);
    }
  };

  /**
   * Allocate a new long[].
   *
   * @param bits Number of bits in storage
   * @return New array
   */
  public static long[] zero(int bits) {
    return new long[(bits > 0) ? ((bits - 1) >>> LONG_LOG2_SIZE) + 1 : 1];
  }

  /**
   * Allocate a new long[].
   *
   * @param bits Number of bits in storage
   * @param init Initial value (of at most the size of a long, remaining bits
   *        will be 0)
   * @return New array
   */
  public static long[] make(int bits, long init) {
    long[] v = new long[((bits - 1) >>> LONG_LOG2_SIZE) + 1];
    v[0] = init;
    return v;
  }

  /**
   * Create a vector initialized with "bits" ones.
   *
   * @param bits Size
   * @return new vector
   */
  public static long[] ones(int bits) {
    long[] v = new long[((bits - 1) >>> LONG_LOG2_SIZE) + 1];
    onesI(v, bits);
    return v;
  }

  /**
   * Creates a new BitSet of fixed cardinality with randomly set bits.
   * 
   * @param card the cardinality of the BitSet to create
   * @param capacity the capacity of the BitSet to create - the randomly
   *        generated indices of the bits set to true will be uniformly
   *        distributed between 0 (inclusive) and capacity (exclusive)
   * @param random a Random Object to create the sequence of indices set to true
   *        - the same number occurring twice or more is ignored but the already
   *        selected bit remains true
   * @return a new BitSet with randomly set bits
   */
  public static long[] random(int card, int capacity, Random random) {
    if(card < 0 || card > capacity) {
      throw new IllegalArgumentException("Cannot set " + card + " out of " + capacity + " bits.");
    }
    // FIXME: Avoid recomputing the cardinality.
    if(card < capacity >>> 1) {
      long[] bitset = BitsUtil.zero(capacity);
      for(int todo = card; todo > 0; //
          todo = (todo == 1) ? (card - cardinality(bitset)) : (todo - 1)) {
        setI(bitset, random.nextInt(capacity));
      }
      return bitset;
    }
    else {
      long[] bitset = BitsUtil.ones(capacity);
      for(int todo = capacity - card; todo > 0; //
          todo = (todo == 1) ? (cardinality(bitset) - card) : (todo - 1)) {
        clearI(bitset, random.nextInt(capacity));
      }
      return bitset;
    }
  }

  /**
   * Copy a bitset
   *
   * @param v Array to copy
   * @return Copy
   */
  public static long[] copy(long[] v) {
    return Arrays.copyOf(v, v.length);
  }

  /**
   * Copy a bitset.
   *
   * Note: Bits beyond mincap <em>may</em> be retained!
   *
   * @param v Array to copy
   * @param mincap Target <em>minimum</em> capacity
   * @return Copy with space for at least "capacity" bits
   */
  public static long[] copy(long[] v, int mincap) {
    int words = ((mincap - 1) >>> LONG_LOG2_SIZE) + 1;
    if(v.length == words) {
      return Arrays.copyOf(v, v.length);
    }
    long[] ret = new long[words];
    System.arraycopy(v, 0, ret, 0, Math.min(v.length, words));
    return ret;
  }

  /**
   * Copy a bitset.
   * <p>
   * Note: Bits beyond mincap <em>may</em> be retained!
   *
   * @param v Array to copy
   * @param mincap Target <em>minimum</em> capacity
   * @param shift Number of bits to shift left
   * @return Copy with space for at least "capacity" bits
   */
  public static long[] copy(long[] v, int mincap, int shift) {
    int words = ((mincap - 1) >>> LONG_LOG2_SIZE) + 1;
    if(v.length == words && shift == 0) {
      return Arrays.copyOf(v, v.length);
    }
    long[] ret = new long[words];
    final int shiftWords = shift >>> LONG_LOG2_SIZE;
    final int shiftBits = shift & LONG_LOG2_MASK;
    // Simple case - multiple of word size
    if(shiftBits == 0) {
      for(int i = shiftWords; i < ret.length; i++) {
        ret[i] |= v[i - shiftWords];
      }
      return ret;
    }
    // Overlapping case
    final int unshiftBits = Long.SIZE - shiftBits;
    final int end = Math.min(ret.length, v.length + shiftWords) - 1;
    for(int i = end; i > shiftWords; i--) {
      final int src = i - shiftWords;
      ret[i] |= (v[src] << shiftBits) | (v[src - 1] >>> unshiftBits);
    }
    ret[shiftWords] |= v[0] << shiftBits;
    return ret;
  }

  /**
   * Compute corresponding gray code as v XOR (v &gt;&gt;&gt; 1)
   *
   * @param v Value
   * @return Gray code
   */
  public static long grayC(long v) {
    return v ^ (v >>> 1);
  }

  /**
   * Compute corresponding gray code as v XOR (v &gt;&gt;&gt; 1)
   *
   * @param v Value
   * @return Gray code
   */
  public static long[] grayI(long[] v) {
    return xorI(v, v, -1);
  }

  /**
   * Compute the inverted gray code, v XOR (v &gt;&gt;&gt; 1) XOR (v
   * &gt;&gt;&gt; 2) ...
   *
   * @param v Value
   * @return Inverted gray code
   */
  public static long invgrayC(long v) {
    v ^= (v >>> 1);
    v ^= (v >>> 2);
    v ^= (v >>> 4);
    v ^= (v >>> 8);
    v ^= (v >>> 16);
    v ^= (v >>> 32);
    return v;
  }

  /**
   * Compute the inverted gray code, v XOR (v &gt;&gt;&gt; 1) XOR (v
   * &gt;&gt;&gt; 2) ...
   *
   * @param v Value
   * @return Inverted gray code
   */
  public static long[] invgrayI(long[] v) {
    final int last = v.length - 1;
    int o;
    // Sub word level:
    for(o = 1; o < Long.SIZE; o <<= 1) {
      for(int i = 0; i < last; i++) {
        v[i] ^= (v[i] >>> o) ^ (v[i + 1] << (Long.SIZE - o));
      }
      v[last] ^= (v[last] >>> o);
    }
    // Word level:
    for(o = 1; o <= last; o <<= 1) {
      for(int i = o; i <= last; i++) {
        v[i - o] ^= v[i];
      }
    }
    return v;
  }

  /**
   * Test for the bitstring to be all-zero.
   *
   * @param v Bitstring
   * @return true when all zero
   */
  public static boolean isZero(long[] v) {
    for(int i = 0; i < v.length; i++) {
      if(v[i] != 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * Compute the cardinality (number of set bits)
   *
   * @param v Value
   * @return Number of bits set in long
   */
  public static int cardinality(long v) {
    return Long.bitCount(v);
  }

  /**
   * Compute the cardinality (number of set bits)
   * <p>
   * Low-endian layout for the array.
   *
   * @param v Value
   * @return Number of bits set in long[]
   */
  public static int cardinality(long[] v) {
    int sum = 0;
    for(int i = 0; i < v.length; i++) {
      sum += Long.bitCount(v[i]);
    }
    return sum;
  }

  /**
   * Invert bit number "off" in v.
   *
   * @param v Buffer
   * @param off Offset to flip
   */
  public static long flipC(long v, int off) {
    return v ^ (1L << off);
  }

  /**
   * Invert bit number "off" in v.
   * <p>
   * Low-endian layout for the array.
   *
   * @param v Buffer
   * @param off Offset to flip
   */
  public static long[] flipI(long[] v, int off) {
    final int wordindex = off >>> LONG_LOG2_SIZE;
    v[wordindex] ^= (1L << off);
    return v;
  }

  /**
   * Set bit number "off" in v.
   *
   * @param v Buffer
   * @param off Offset to set
   */
  public static long setC(long v, int off) {
    return v | (1L << off);
  }

  /**
   * Set bit number "off" in v.
   * <p>
   * Low-endian layout for the array.
   *
   * @param v Buffer
   * @param off Offset to set
   */
  public static long[] setI(long[] v, int off) {
    final int wordindex = off >>> LONG_LOG2_SIZE;
    v[wordindex] |= (1L << off);
    return v;
  }

  /**
   * Put o onto v in-place, i.e. v = o
   *
   * @param v Primary object
   * @param o data to initialize to.
   * @return v
   */
  public static long[] setI(long[] v, long[] o) {
    assert (o.length <= v.length) : "Bit set sizes do not agree.";
    final int max = Math.min(v.length, o.length);
    for(int i = 0; i < max; i++) {
      v[i] = o[i];
    }
    return v;
  }

  /**
   * Clear bit number "off" in v.
   *
   * @param v Buffer
   * @param off Offset to clear
   */
  public static long clearC(long v, int off) {
    return v & ~(1L << off);
  }

  /**
   * Clear bit number "off" in v.
   * <p>
   * Low-endian layout for the array.
   *
   * @param v Buffer
   * @param off Offset to clear
   */
  public static long[] clearI(long[] v, int off) {
    final int wordindex = off >>> LONG_LOG2_SIZE;
    v[wordindex] &= ~(1L << off);
    return v;
  }

  /**
   * Set bit number "off" in v.
   *
   * @param v Buffer
   * @param off Offset to set
   */
  public static boolean get(long v, int off) {
    return (v & (1L << off)) != 0;
  }

  /**
   * Set bit number "off" in v.
   * <p>
   * Low-endian layout for the array.
   *
   * @param v Buffer
   * @param off Offset to set
   */
  public static boolean get(long[] v, int off) {
    final int wordindex = off >>> LONG_LOG2_SIZE;
    return (wordindex < v.length) && (v[wordindex] & (1L << off)) != 0;
  }

  /**
   * Fill a vector initialized with "bits" ones.
   *
   * @param v Vector to fill.
   * @param bits Size
   */
  public static void onesI(long[] v, int bits) {
    final int fillWords = bits >>> LONG_LOG2_SIZE;
    final int fillBits = bits & LONG_LOG2_MASK;
    Arrays.fill(v, 0, fillWords, LONG_ALL_BITS);
    if(fillBits > 0) {
      v[fillWords] = (1L << fillBits) - 1;
    }
    if(fillWords + 1 < v.length) {
      Arrays.fill(v, fillWords + 1, v.length, 0L);
    }
  }

  /**
   * Zero the given set
   *
   * Low-endian layout for the array.
   *
   * @param v existing set
   * @return array set to zero
   */
  public static long[] zeroI(long[] v) {
    Arrays.fill(v, 0);
    return v;
  }

  /**
   * XOR o onto v in-place, i.e. v ^= o
   *
   * @param v Primary object
   * @param o data to xor
   * @return v
   */
  public static long[] xorI(long[] v, long[] o) {
    assert (o.length <= v.length) : "Bit set sizes do not agree.";
    for(int i = 0; i < o.length; i++) {
      v[i] ^= o[i];
    }
    return v;
  }

  /**
   * XOR o onto v in-place, i.e. v ^= (o &lt;&lt; off)
   *
   * @param v Primary object
   * @param o data to or
   * @param off Offset
   * @return v
   */
  public static long[] xorI(long[] v, long[] o, int off) {
    if(off == 0) {
      return xorI(v, o);
    }
    // Break shift into integers to shift and bits to shift
    final int shiftWords = off >> LONG_LOG2_SIZE;
    final int shiftBits = off & LONG_LOG2_MASK;

    if(shiftWords >= v.length) {
      return v;
    }
    // Simple case - multiple of word size
    if(shiftBits == 0) {
      int i = Math.min(v.length, o.length + shiftWords), j = i - shiftWords;
      while(i > 0 && j > 0) {
        v[--i] ^= o[--j];
      }
      return v;
    }
    // Overlapping case
    final int unshiftBits = Long.SIZE - shiftBits;
    int i = Math.min(v.length, o.length + shiftWords), j = i - shiftWords;
    long t = o[--j];
    if(i < v.length) { // partial word of negative shift
      v[i] ^= t >>> unshiftBits;
    }
    while(i > 0 && j > 0) {
      v[--i] ^= (t << shiftBits) | ((t = o[--j]) >>> unshiftBits);
    }
    if(i > 0) { // partial word of positive shift
      v[--i] ^= t << shiftBits;
    }
    return v;
  }

  /**
   * OR o onto v in-place, i.e. v |= o
   *
   * @param v Primary object
   * @param o data to or
   * @return v
   */
  public static long[] orI(long[] v, long[] o) {
    assert (o.length <= v.length) : "Bit set sizes do not agree.";
    final int max = Math.min(v.length, o.length);
    for(int i = 0; i < max; i++) {
      v[i] |= o[i];
    }
    return v;
  }

  /**
   * OR o onto v in-place, i.e. v |= (o &lt;&lt; off)
   * <p>
   * Note: Bits that are shifted outside of the size of v are discarded.
   *
   * @param v Primary object
   * @param o data to or
   * @param off Offset
   * @return v
   */
  public static long[] orI(long[] v, long[] o, int off) {
    if(off == 0) {
      return orI(v, o);
    }
    // Break shift into integers to shift and bits to shift
    final int shiftWords = off >> LONG_LOG2_SIZE;
    final int shiftBits = off & LONG_LOG2_MASK;

    if(shiftWords >= v.length) {
      return v;
    }
    // Simple case - multiple of word size
    if(shiftBits == 0) {
      int i = Math.min(v.length, o.length + shiftWords), j = i - shiftWords;
      while(i > 0 && j > 0) {
        v[--i] |= o[--j];
      }
      return v;
    }
    // Overlapping case
    final int unshiftBits = Long.SIZE - shiftBits;
    int i = Math.min(v.length, o.length + shiftWords), j = i - shiftWords;
    long t = o[--j];
    if(i < v.length) { // partial word of negative shift
      v[i] |= t >>> unshiftBits;
    }
    while(i > 0 && j > 0) {
      v[--i] |= (t << shiftBits) | ((t = o[--j]) >>> unshiftBits);
    }
    if(i > 0) { // partial word of positive shift
      v[--i] |= t << shiftBits;
    }
    return v;
  }

  /**
   * AND o onto v in-place, i.e. v &amp;= o
   *
   * @param v Primary object
   * @param o data to and
   * @return v
   */
  public static long[] andI(long[] v, long[] o) {
    int i = 0;
    for(; i < o.length; i++) {
      v[i] &= o[i];
    }
    // Zero higher words
    Arrays.fill(v, i, v.length, 0);
    return v;
  }

  /**
   * AND o onto v in-place, i.e. v &amp;= (o &lt;&lt; off)
   *
   * @param v Primary object
   * @param o data to or
   * @param off Offset
   * @return v
   */
  public static long[] andI(long[] v, long[] o, int off) {
    if(off == 0) {
      return andI(v, o);
    }
    // Break shift into integers to shift and bits to shift
    final int shiftWords = off >> LONG_LOG2_SIZE;
    final int shiftBits = off & LONG_LOG2_MASK;

    if(shiftWords >= v.length) {
      return v;
    }
    // Simple case - multiple of word size
    if(shiftBits == 0) {
      int i = Math.min(v.length, o.length + shiftWords), j = i - shiftWords;
      while(i > 0 && j > 0) {
        v[--i] &= o[--j];
      }
      // Clear bottom words
      if(shiftWords > 0) {
        Arrays.fill(v, 0, shiftWords, 0);
      }
      return v;
    }
    // Overlapping case
    final int unshiftBits = Long.SIZE - shiftBits;
    int i = Math.min(v.length, o.length + shiftWords), j = i - shiftWords;
    long t = o[--j];
    if(i < v.length) { // partial word of negative shift
      v[i] &= t >>> unshiftBits;
    }
    while(i > 0 && j > 0) {
      v[--i] &= (t << shiftBits) | ((t = o[--j]) >>> unshiftBits);
    }
    if(i > 0) { // partial word of positive shift
      v[--i] &= t << shiftBits;
    }
    // Clear bottom words
    Arrays.fill(v, 0, shiftWords, 0);
    return v;
  }

  /**
   * AND o onto v in a copy, i.e. v &amp; o
   * <p>
   * The resulting array will have the shorter length of the two.
   *
   * @param v Primary object
   * @param o data to and
   * @return Copy of v and o
   */
  public static long[] andCMin(long[] v, long[] o) {
    final int min = Math.min(v.length, o.length);
    long[] out = new long[min];
    int i = 0;
    for(; i < min; i++) {
      out[i] = v[i] & o[i];
    }
    return out;
  }

  /**
   * NOTAND o onto v in-place, i.e. v &amp;= ~o
   *
   * @param v Primary object
   * @param o data to and
   * @return v
   */
  public static long[] nandI(long[] v, long[] o) {
    int i = 0;
    for(; i < o.length; i++) {
      v[i] &= ~o[i];
    }
    return v;
  }

  /**
   * Invert v in-place.
   *
   * @param v Object to invert
   * @return v
   */
  public static long[] invertI(long[] v) {
    for(int i = 0; i < v.length; i++) {
      v[i] = ~v[i];
    }
    return v;
  }

  /**
   * Shift a long[] bitset in-place.
   * <p>
   * Low-endian layout for the array.
   *
   * @param v existing bitset
   * @param off Offset to shift by
   * @return Bitset
   */
  public static long[] shiftRightI(long[] v, int off) {
    if(off == 0) {
      return v;
    }
    if(off < 0) {
      return shiftLeftI(v, -off);
    }
    // Break shift into integers to shift and bits to shift
    final int shiftWords = off >>> LONG_LOG2_SIZE;
    final int shiftBits = off & LONG_LOG2_MASK;

    if(shiftWords >= v.length) {
      return zeroI(v);
    }
    // Simple case - multiple of word size
    if(shiftBits == 0) {
      // Move whole words down
      System.arraycopy(v, shiftWords, v, 0, v.length - shiftWords);
      // Fill top words with zeros
      Arrays.fill(v, v.length - shiftWords, v.length, 0);
      return v;
    }
    // Overlapping case
    final int unshiftBits = Long.SIZE - shiftBits;
    // Bottom-up to not overlap the operations.
    for(int i = 0; i < v.length - shiftWords - 1; i++) {
      final int src = i + shiftWords;
      v[i] = (v[src + 1] << unshiftBits) | (v[src] >>> shiftBits);
    }
    // The last original word
    v[v.length - shiftWords - 1] = v[v.length - 1] >>> shiftBits;
    // Fill whole words "lost" by the shift
    Arrays.fill(v, v.length - shiftWords, v.length, 0);
    return v;
  }

  /**
   * Shift a long[] bitset in-place.
   * <p>
   * Low-endian layout for the array.
   *
   * @param v existing bitset
   * @param off Offset to shift by
   * @return Bitset
   */
  public static long[] shiftLeftI(long[] v, int off) {
    if(off == 0) {
      return v;
    }
    if(off < 0) {
      return shiftRightI(v, -off);
    }
    // Break shift into integers to shift and bits to shift
    final int shiftWords = off >>> LONG_LOG2_SIZE;
    final int shiftBits = off & LONG_LOG2_MASK;

    if(shiftWords >= v.length) {
      return zeroI(v);
    }
    // Simple case - multiple of word size
    if(shiftBits == 0) {
      // Move whole words up
      System.arraycopy(v, 0, v, shiftWords, v.length - shiftWords);
      // Fill the initial words with zeros
      Arrays.fill(v, 0, shiftWords, 0);
      return v;
    }
    // Overlapping case
    final int unshiftBits = Long.SIZE - shiftBits;
    // Top-Down to not overlap the operations.
    for(int i = v.length - 1; i > shiftWords; i--) {
      final int src = i - shiftWords;
      v[i] = (v[src] << shiftBits) | (v[src - 1] >>> unshiftBits);
    }
    v[shiftWords] = v[0] << shiftBits;
    // Fill the initial words with zeros
    Arrays.fill(v, 0, shiftWords, 0);
    return v;
  }

  /**
   * Rotate a long to the right, cyclic with length len
   *
   * @param v Bits
   * @param shift Shift value
   * @param len Length
   * @return cycled bit set
   */
  public static long cycleRightC(long v, int shift, int len) {
    return shift == 0 ? v : shift < 0 ? cycleLeftC(v, -shift, len) : //
        (((v) >>> (shift)) | ((v) << ((len) - (shift)))) & ((1 << len) - 1);
  }

  /**
   * Cycle a bitstring to the right.
   *
   * @param v Bit string
   * @param shift Number of steps to cycle
   * @param len Length
   */
  public static long[] cycleRightI(long[] v, int shift, int len) {
    long[] t = copy(v, len, len - shift);
    return orI(shiftRightI(v, shift), truncateI(t, len));
  }

  /**
   * Truncate a bit string to the given length (setting any higher bit to 0).
   *
   * @param v String to process
   * @param len Length (in bits) to truncate to
   */
  public static long[] truncateI(long[] v, int len) {
    final int zap = (v.length * Long.SIZE) - len;
    final int zapWords = (zap >>> LONG_LOG2_SIZE);
    final int zapbits = zap & LONG_LOG2_MASK;
    Arrays.fill(v, v.length - zapWords, v.length, 0);
    if(zapbits > 0) {
      v[v.length - zapWords - 1] &= (LONG_ALL_BITS >>> zapbits);
    }
    return v;
  }

  /**
   * Rotate a long to the left, cyclic with length len
   *
   * @param v Bits
   * @param shift Shift value
   * @param len Length
   * @return cycled bit set
   */
  public static long cycleLeftC(long v, int shift, int len) {
    return shift == 0 ? v : shift < 0 ? cycleRightC(v, -shift, len) : //
        (((v) << (shift)) | ((v) >>> ((len) - (shift)))) & ((1 << len) - 1);
  }

  /**
   * Cycle a bitstring to the right.
   *
   * @param v Bit string
   * @param shift Number of steps to cycle
   * @param len Length
   */
  public static long[] cycleLeftI(long[] v, int shift, int len) {
    long[] t = copy(v, len, shift);
    return orI(shiftRightI(v, len - shift), truncateI(t, len));
  }

  /**
   * Convert bitset to a string consisting of "0" and "1", in high-endian order.
   *
   * @param v Value to process
   * @return String representation
   */
  public static String toString(long[] v) {
    if(v == null) {
      return "null";
    }
    final int mag = magnitude(v);
    if(mag == 0) {
      return "0";
    }
    char[] digits = new char[mag];
    int pos = mag - 1;

    outer: for(int w = 0; w < v.length; w++) {
      long f = 1L;
      for(int i = 0; i < Long.SIZE; i++) {
        digits[pos] = ((v[w] & f) == 0) ? '0' : '1';
        f <<= 1;
        --pos;
        if(pos < 0) {
          break outer;
        }
      }
    }
    if(pos > 0) {
      Arrays.fill(digits, 0, pos, '0');
    }
    return new String(digits);
  }

  /**
   * Convert bitset to a string consisting of "0" and "1", in high-endian order.
   *
   * @param v Value to process
   * @param minw Minimum width
   * @return String representation
   */
  public static String toString(long[] v, int minw) {
    if(v == null) {
      return "null";
    }
    int mag = magnitude(v);
    mag = mag >= minw ? mag : minw;
    if(mag == 0) {
      return "0";
    }
    char[] digits = new char[mag];
    int pos = mag - 1;

    outer: for(int w = 0; w < v.length; w++) {
      long f = 1L;
      for(int i = 0; i < Long.SIZE; i++) {
        digits[pos] = ((v[w] & f) == 0) ? '0' : '1';
        f <<= 1;
        --pos;
        if(pos < 0) {
          break outer;
        }
      }
    }
    for(; pos >= 0; --pos) {
      digits[pos] = '0';
    }
    return new String(digits);
  }

  /**
   * Convert bitset to a string consisting of "0" and "1", in high-endian order.
   *
   * @param v Value to process
   * @return String representation
   */
  public static String toString(long v) {
    final int mag = magnitude(v);
    if(mag == 0) {
      return "0";
    }
    char[] digits = new char[mag];

    long f = 1L;
    for(int pos = mag - 1; pos >= 0; --pos, f <<= 1) {
      digits[pos] = ((v & f) == 0) ? '0' : '1';
    }
    return new String(digits);
  }

  /**
   * Convert bitset to a string consisting of "0" and "1", in low-endian order.
   *
   * @param v Value to process
   * @return String representation
   */
  public static String toStringLow(long[] v) {
    if(v == null) {
      return "null";
    }
    final int mag = magnitude(v);
    if(mag == 0) {
      return "0";
    }
    char[] digits = new char[mag];
    int pos = 0;

    outer: for(int w = 0; w < v.length; w++) {
      long f = 1L;
      for(int i = 0; i < Long.SIZE; i++) {
        digits[pos] = ((v[w] & f) == 0) ? '0' : '1';
        f <<= 1;
        ++pos;
        if(pos >= mag) {
          break outer;
        }
      }
    }
    for(; pos < mag; ++pos) {
      digits[pos] = '0';
    }
    return new String(digits);
  }

  /**
   * Convert bitset to a string consisting of "0" and "1", in low-endian order.
   *
   * @param v Value to process
   * @param minw Minimum width
   * @return String representation
   */
  public static String toStringLow(long[] v, int minw) {
    if(v == null) {
      return "null";
    }
    int mag = magnitude(v);
    mag = mag >= minw ? mag : minw;
    if(mag == 0) {
      return "0";
    }
    char[] digits = new char[mag];
    int pos = 0;

    outer: for(int w = 0; w < v.length; w++) {
      long f = 1L;
      for(int i = 0; i < Long.SIZE; i++) {
        digits[pos] = ((v[w] & f) == 0) ? '0' : '1';
        f <<= 1;
        ++pos;
        if(pos >= mag) {
          break outer;
        }
      }
    }
    for(; pos < mag; ++pos) {
      digits[pos] = '0';
    }
    return new String(digits);
  }

  /**
   * Convert bitset to a string consisting of "0" and "1", in low-endian order.
   *
   * @param v Value to process
   * @return String representation
   */
  public static String toStringLow(long v) {
    final int mag = magnitude(v);
    if(mag == 0) {
      return "0";
    }
    char[] digits = new char[mag];

    long f = 1L;
    for(int pos = 0; pos < mag; ++pos, f <<= 1) {
      digits[pos] = ((v & f) == 0) ? '0' : '1';
    }
    return new String(digits);
  }

  /**
   * Convert the bitset into a decimal representation, e.g. <tt>0, 3, 5</tt>
   *
   * @param v Value
   * @param sep Value separator
   * @param offset Counting offset (usually, 0 or 1)
   * @return String representation
   */
  public static String toString(long[] v, String sep, int offset) {
    int p = nextSetBit(v, 0);
    if(p < 0) {
      return "";
    }
    StringBuilder buf = new StringBuilder();
    buf.append(p + offset);
    for(p = nextSetBit(v, p + 1); p >= 0; p = nextSetBit(v, p + 1)) {
      buf.append(sep).append(p + offset);
    }
    return buf.toString();
  }

  /**
   * Find the number of trailing zeros.
   *
   * @param v Bitset
   * @return Position of first set bit, -1 if no set bit was found.
   */
  public static int numberOfTrailingZerosSigned(long[] v) {
    for(int p = 0;; p++) {
      if(p == v.length) {
        return -1;
      }
      if(v[p] != 0) {
        return Long.numberOfTrailingZeros(v[p]) + p * Long.SIZE;
      }
    }
  }

  /**
   * Find the number of trailing zeros.
   *
   * @param v Bitset
   * @return Position of first set bit, v.length * 64 if no set bit was found.
   */
  public static int numberOfTrailingZeros(long[] v) {
    for(int p = 0;; p++) {
      if(p == v.length) {
        return p * Long.SIZE;
      }
      if(v[p] != 0) {
        return Long.numberOfTrailingZeros(v[p]) + p * Long.SIZE;
      }
    }
  }

  /**
   * Find the number of trailing zeros.
   * <p>
   * Note: this has different semantics to {@link Long#numberOfLeadingZeros}
   * when the number is 0.
   *
   * @param v Long
   * @return Position of first set bit, -1 if no set bit was found.
   */
  public static int numberOfTrailingZerosSigned(long v) {
    return v == 0 ? -1 : Long.numberOfTrailingZeros(v);
  }

  /**
   * Find the number of trailing zeros.
   * <p>
   * Note: this is the same as {@link Long#numberOfTrailingZeros}
   *
   * @param v Long
   * @return Position of first set bit, 64 if no set bit was found.
   */
  public static int numberOfTrailingZeros(long v) {
    return Long.numberOfTrailingZeros(v);
  }

  /**
   * Find the number of trailing zeros.
   * <p>
   * Note: this is the same as {@link Long#numberOfTrailingZeros}
   *
   * @param v Long
   * @return Position of first set bit, 64 if no set bit was found.
   */
  public static int numberOfTrailingZeros(int v) {
    return Integer.numberOfTrailingZeros(v);
  }

  /**
   * Find the number of leading zeros.
   *
   * @param v Bitset
   * @return Position of first set bit, -1 if no set bit was found.
   */
  public static int numberOfLeadingZerosSigned(long[] v) {
    for(int p = 0, ip = v.length - 1; p < v.length; p++, ip--) {
      if(v[ip] != 0) {
        return Long.numberOfLeadingZeros(v[ip]) + p * Long.SIZE;
      }
    }
    return -1;
  }

  /**
   * Find the number of leading zeros.
   *
   * @param v Bitset
   * @return Position of first set bit, v.length * 64 if no set bit was found.
   */
  public static int numberOfLeadingZeros(long[] v) {
    for(int p = 0, ip = v.length - 1;; p++, ip--) {
      if(p == v.length) {
        return p * Long.SIZE;
      }
      if(v[ip] != 0) {
        return Long.numberOfLeadingZeros(v[ip]) + p * Long.SIZE;
      }
    }
  }

  /**
   * Find the number of leading zeros; -1 if all zero
   * <p>
   * Note: this has different semantics to {@link Long#numberOfLeadingZeros}
   * when the number is 0.
   *
   * @param v Bitset
   * @return Position of first set bit, -1 if no set bit was found.
   */
  public static int numberOfLeadingZerosSigned(long v) {
    return v == 0 ? -1 : Long.numberOfLeadingZeros(v);
  }

  /**
   * Find the number of leading zeros; -1 if all zero
   * <p>
   * Note: this has different semantics to {@link Long#numberOfLeadingZeros}
   * when the number is 0.
   *
   * @param v Bitset
   * @return Position of first set bit, -1 if no set bit was found.
   */
  public static int numberOfLeadingZerosSigned(int v) {
    return v == 0 ? -1 : Integer.numberOfLeadingZeros(v);
  }

  /**
   * Find the number of leading zeros; 64 if all zero
   * <p>
   * Note: this the same as {@link Long#numberOfLeadingZeros}.
   *
   * @param v Bitset
   * @return Position of first set bit, 64 if no set bit was found.
   */
  public static int numberOfLeadingZeros(long v) {
    return Long.numberOfLeadingZeros(v);
  }

  /**
   * Find the number of leading zeros; 32 if all zero
   * <p>
   * Note: this the same as {@link Integer#numberOfLeadingZeros}.
   *
   * @param v Bitset
   * @return Position of first set bit, 32 if no set bit was found.
   */
  public static int numberOfLeadingZeros(int v) {
    return Integer.numberOfLeadingZeros(v);
  }

  /**
   * Find the previous set bit.
   *
   * @param v Value to process
   * @param start Start position (inclusive)
   * @return Position of previous set bit, or -1.
   */
  public static int previousSetBit(long v, int start) {
    if(start < 0) {
      return -1;
    }
    start = start < Long.SIZE ? start : Long.SIZE - 1;
    long cur = v & (LONG_ALL_BITS >>> -(start + 1));
    return cur == 0 ? -1 : cur == LONG_ALL_BITS ? 0 : 63 - Long.numberOfLeadingZeros(cur);
  }

  /**
   * Find the previous set bit.
   *
   * @param v Values to process
   * @param start Start position (inclusive)
   * @return Position of previous set bit, or -1.
   */
  public static int previousSetBit(long[] v, int start) {
    if(start < 0) {
      return -1;
    }
    int wordindex = start >>> LONG_LOG2_SIZE;
    if(wordindex >= v.length) {
      return magnitude(v) - 1;
    }
    // Initial word
    final int off = 63 - (start & LONG_LOG2_MASK);
    long cur = v[wordindex] & (LONG_ALL_BITS >>> off);
    for(;;) {
      if(cur != 0) {
        return wordindex * Long.SIZE + 63 - ((cur == LONG_ALL_BITS) ? 0 : Long.numberOfLeadingZeros(cur));
      }
      if(wordindex == 0) {
        return -1;
      }
      cur = v[--wordindex];
    }
  }

  /**
   * Find the previous clear bit.
   *
   * @param v Values to process
   * @param start Start position (inclusive)
   * @return Position of previous clear bit, or -1.
   */
  public static int previousClearBit(long v, int start) {
    if(start < 0) {
      return -1;
    }
    start = start < Long.SIZE ? start : Long.SIZE - 1;
    long cur = ~v & (LONG_ALL_BITS >>> -(start + 1));
    return cur == 0 ? -1 : 63 - Long.numberOfLeadingZeros(cur);
  }

  /**
   * Find the previous clear bit.
   *
   * @param v Values to process
   * @param start Start position (inclusive)
   * @return Position of previous clear bit, or -1.
   */
  public static int previousClearBit(long[] v, int start) {
    if(start < 0) {
      return -1;
    }
    int wordindex = start >>> LONG_LOG2_SIZE;
    if(wordindex >= v.length) {
      return magnitude(v) - 1;
    }
    // Initial word
    final int off = 63 - (start & LONG_LOG2_MASK);
    long cur = ~v[wordindex] & (LONG_ALL_BITS >>> off);
    for(;;) {
      if(cur != 0) {
        return wordindex * Long.SIZE + 63 - ((cur == LONG_ALL_BITS) ? 0 : Long.numberOfLeadingZeros(cur));
      }
      if(wordindex == 0) {
        return -1;
      }
      cur = ~v[--wordindex];
    }
  }

  /**
   * Find the next set bit.
   *
   * @param v Value to process
   * @param start Start position (inclusive)
   * @return Position of next set bit, or -1.
   */
  public static int nextSetBit(long v, int start) {
    if(start >= Long.SIZE) {
      return -1;
    }
    start = start < 0 ? 0 : start;
    long cur = v & (LONG_ALL_BITS << start);
    return cur == 0 ? -1 : cur == LONG_ALL_BITS ? 0 : Long.numberOfTrailingZeros(cur);
  }

  /**
   * Find the next set bit.
   *
   * @param v Value to process
   * @param start Start position (inclusive)
   * @return Position of next set bit, or -1.
   */
  public static int nextSetBit(long[] v, int start) {
    start = start < 0 ? 0 : start;
    int wordindex = start >>> LONG_LOG2_SIZE;
    if(wordindex >= v.length) {
      return -1;
    }

    // Initial word
    long cur = v[wordindex] & (LONG_ALL_BITS << start);
    for(;;) {
      if(cur != 0) {
        return (wordindex * Long.SIZE) + Long.numberOfTrailingZeros(cur);
      }
      if(++wordindex == v.length) {
        return -1;
      }
      cur = v[wordindex];
    }
  }

  /**
   * Find the next clear bit.
   *
   * @param v Value to process
   * @param start Start position (inclusive)
   * @return Position of next clear bit, or -1.
   */
  public static int nextClearBit(long v, int start) {
    if(start >= Long.SIZE) {
      return -1;
    }
    start = start < 0 ? 0 : start;
    long cur = ~v & (LONG_ALL_BITS << start);
    return cur == 0 ? -1 : Long.numberOfTrailingZeros(cur);
  }

  /**
   * Find the next clear bit.
   *
   * @param v Value to process
   * @param start Start position (inclusive)
   * @return Position of next clear bit, or -1.
   */
  public static int nextClearBit(long[] v, int start) {
    start = start < 0 ? 0 : start;
    int wordindex = start >>> LONG_LOG2_SIZE;
    if(wordindex >= v.length) {
      return -1;
    }

    // Initial word
    long cur = ~v[wordindex] & (LONG_ALL_BITS << start);
    for(;;) {
      if(cur != 0) {
        return (wordindex * Long.SIZE) + Long.numberOfTrailingZeros(cur);
      }
      if(++wordindex == v.length) {
        return -1;
      }
      cur = ~v[wordindex];
    }
  }

  /**
   * The magnitude is the position of the highest bit set
   *
   * @param v Vector v
   * @return position of highest bit set, or 0.
   */
  public static int magnitude(long[] v) {
    return capacity(v) - numberOfLeadingZeros(v);
  }

  /**
   * The magnitude is the position of the highest bit set
   *
   * @param v Vector v
   * @return position of highest bit set, or 0.
   */
  public static int magnitude(long v) {
    return Long.SIZE - Long.numberOfLeadingZeros(v);
  }

  /**
   * The magnitude is the position of the highest bit set
   *
   * @param v Vector v
   * @return position of highest bit set, or 0.
   */
  public static int magnitude(int v) {
    return Integer.SIZE - Integer.numberOfLeadingZeros(v);
  }

  /**
   * Test whether two Bitsets intersect.
   *
   * @param x First bitset
   * @param y Second bitset
   * @return {@code true} when the bitsets intersect.
   */
  public static boolean intersect(long x, long y) {
    return (x & y) != 0L;
  }

  /**
   * Test whether two Bitsets intersect.
   *
   * @param x First bitset
   * @param y Second bitset
   * @return {@code true} when the bitsets intersect.
   */
  public static boolean intersect(long[] x, long[] y) {
    final int min = (x.length < y.length) ? x.length : y.length;
    for(int i = 0; i < min; i++) {
      if((x[i] & y[i]) != 0L) {
        return true;
      }
    }
    return false;
  }

  /**
   * Compute the intersection size of two Bitsets.
   *
   * @param x First bitset
   * @param y Second bitset
   * @return Intersection size
   */
  public static int intersectionSize(long x, long y) {
    return Long.bitCount(x & y);
  }

  /**
   * Compute the intersection size of two Bitsets.
   *
   * @param x First bitset
   * @param y Second bitset
   * @return Intersection size
   */
  public static int intersectionSize(long[] x, long[] y) {
    final int lx = x.length, ly = y.length;
    final int min = (lx < ly) ? lx : ly;
    int res = 0;
    for(int i = 0; i < min; i++) {
      res += Long.bitCount(x[i] & y[i]);
    }
    return res;
  }

  /**
   * Compute the union size of two Bitsets.
   *
   * @param x First bitset
   * @param y Second bitset
   * @return Union size
   */
  public static int unionSize(long x, long y) {
    return Long.bitCount(x | y);
  }

  /**
   * Compute the union size of two Bitsets.
   *
   * @param x First bitset
   * @param y Second bitset
   * @return Union size
   */
  public static int unionSize(long[] x, long[] y) {
    final int lx = x.length, ly = y.length;
    final int min = (lx < ly) ? lx : ly;
    int i = 0, res = 0;
    for(; i < min; i++) {
      res += Long.bitCount(x[i] | y[i]);
    }
    for(; i < lx; i++) {
      res += Long.bitCount(x[i]);
    }
    for(; i < ly; i++) {
      res += Long.bitCount(y[i]);
    }
    return res;
  }

  /**
   * Compute the Hamming distance (Size of symmetric difference), i.e.
   * {@code cardinality(a ^ b)}.
   *
   * @param b1 First vector
   * @param b2 Second vector
   * @return Cardinality of symmetric difference
   */
  public static int hammingDistance(long b1, long b2) {
    return Long.bitCount(b1 ^ b2);
  }

  /**
   * Compute the Hamming distance (Size of symmetric difference), i.e.
   * {@code cardinality(a ^ b)}.
   *
   * @param x First vector
   * @param y Second vector
   * @return Cardinality of symmetric difference
   */
  public static int hammingDistance(long[] x, long[] y) {
    final int lx = x.length, ly = y.length;
    final int min = (lx < ly) ? lx : ly;
    int i = 0, h = 0;
    for(; i < min; i++) {
      h += Long.bitCount(x[i] ^ y[i]);
    }
    for(; i < lx; i++) {
      h += Long.bitCount(x[i]);
    }
    for(; i < ly; i++) {
      h += Long.bitCount(y[i]);
    }
    return h;
  }

  /**
   * Capacity of the vector v.
   *
   * @param v Vector v
   * @return Capacity
   */
  public static int capacity(long[] v) {
    return v.length * Long.SIZE;
  }

  /**
   * Test two bitsets for equality
   *
   * @param x First bitset
   * @param y Second bitset
   * @return {@code true} when the bitsets are equal
   */
  public static boolean equal(long x, long y) {
    return x == y;
  }

  /**
   * Test two bitsets for equality
   * 
   * @param x First bitset
   * @param y Second bitset
   * @return {@code true} when the bitsets are equal
   */
  public static boolean equal(long[] x, long[] y) {
    if(x == null || y == null) {
      return (x == null) && (y == null);
    }
    int p = Math.min(x.length, y.length) - 1;
    for(int i = x.length - 1; i > p; i--) {
      if(x[i] != 0L) {
        return false;
      }
    }
    for(int i = y.length - 1; i > p; i--) {
      if(y[i] != 0L) {
        return false;
      }
    }
    for(; p >= 0; p--) {
      if(x[p] != y[p]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Compare two bitsets.
   *
   * @param x First bitset
   * @param y Second bitset
   * @return Comparison result
   */
  public static int compare(long x, long y) {
    return Long.compare(x, y);
  }

  /**
   * Compare two bitsets.
   * 
   * @param x First bitset
   * @param y Second bitset
   * @return Comparison result
   */
  public static int compare(long[] x, long[] y) {
    if(x == null) {
      return (y == null) ? 0 : -1;
    }
    if(y == null) {
      return +1;
    }
    int p = Math.min(x.length, y.length) - 1;
    for(int i = x.length - 1; i > p; i--) {
      if(x[i] != 0) {
        return +1;
      }
    }
    for(int i = y.length - 1; i > p; i--) {
      if(y[i] != 0) {
        return -1;
      }
    }
    for(; p >= 0; p--) {
      final long xp = x[p], yp = y[p];
      if(xp != yp) {
        return xp < 0 ? (yp < 0 && yp < xp) ? -1 : +1 : (yp < 0 || xp < yp) ? -1 : +1;
      }
    }
    return 0;
  }

  /**
   * Compute a hash code for the given bitset.
   *
   * @param x Bitset bitset
   * @return Hash code
   */
  public static int hashCode(long x) {
    // We use almost the same hash code function as Java BitSet.
    // Optimized for speed only, not protected against collision attacks
    // If you need that, consider using murmur hashing with custom seeds.
    long hash = 0x76543210L ^ x;
    return (int) ((hash >> 32) ^ hash);
  }

  /**
   * Compute a hash code for the given bitset.
   *
   * @param x Bitset bitset
   * @return Hash code
   */
  public static int hashCode(long[] x) {
    // We use almost the same hash code function as Java BitSet.
    // Optimized for speed only, not protected against collision attacks
    // If you need that, consider using murmur hashing with custom seeds.
    long hash = 0x76543210L;
    for(int i = 0; i < x.length;) {
      hash ^= x[i] * ++i;
    }
    return (int) ((hash >> 32) ^ hash);
  }

  /**
   * Compute <code>m * pow(2., n)</code> using bit operations.
   *
   * @param m Mantissa
   * @param n Exponent
   * @return Double value
   */
  public static double lpow2(long m, int n) {
    if(m == 0) {
      return 0.0;
    }
    if(m == Long.MIN_VALUE) {
      return lpow2(Long.MIN_VALUE >> 1, n + 1);
    }
    if(m < 0) {
      return -lpow2(-m, n);
    }
    assert (m >= 0);
    int bitLength = magnitude(m);
    int shift = bitLength - 53;
    long exp = 1023L + 52 + n + shift; // Use long to avoid overflow.
    if(exp >= 0x7FF) {
      return Double.POSITIVE_INFINITY;
    }
    if(exp <= 0) { // Degenerated number (subnormal, assume 0 for bit 52)
      if(exp <= -54) {
        return 0.0;
      }
      return lpow2(m, n + 54) / 18014398509481984L; // 2^54 Exact.
    }
    // Normal number.
    long bits = (shift > 0) ? (m >> shift) + ((m >> (shift - 1)) & 1) : // Rounding.
        m << -shift;
    if(((bits >> 52) != 1) && (++exp >= 0x7FF)) {
      return Double.POSITIVE_INFINITY;
    }
    bits &= 0x000fffffffffffffL; // Clears MSB (bit 52)
    bits |= exp << 52;
    return Double.longBitsToDouble(bits);
  }

  /**
   * Compute {@code m * Math.pow(10,e)} on the bit representation, for
   * assembling a floating point decimal value.
   *
   * @param m Mantisse
   * @param n Exponent to base 10.
   * @return Double value.
   */
  public static double lpow10(long m, int n) {
    if(m == 0) {
      return 0.0;
    }
    if(m == Long.MIN_VALUE) {
      return lpow10(Long.MIN_VALUE / 10, n + 1);
    }
    if(m < 0) {
      return -lpow10(-m, n);
    }
    if(n >= 0) { // Positive power.
      if(n > 308) {
        return Double.POSITIVE_INFINITY;
      }
      // Works with 4 x 32 bits registers (x3:x2:x1:x0)
      long x0 = 0; // 32 bits.
      long x1 = 0; // 32 bits.
      long x2 = m & LONG_32_BITS; // 32 bits.
      long x3 = m >>> 32; // 32 bits.
      int pow2 = 0;
      while(n != 0) {
        int i = (n >= POW5_INT.length) ? POW5_INT.length - 1 : n;
        int coef = POW5_INT[i]; // 31 bits max.

        if(((int) x0) != 0) {
          x0 *= coef; // 63 bits max.
        }
        if(((int) x1) != 0) {
          x1 *= coef; // 63 bits max.
        }
        x2 *= coef; // 63 bits max.
        x3 *= coef; // 63 bits max.

        x1 += x0 >>> 32;
        x0 &= LONG_32_BITS;

        x2 += x1 >>> 32;
        x1 &= LONG_32_BITS;

        x3 += x2 >>> 32;
        x2 &= LONG_32_BITS;

        // Adjusts powers.
        pow2 += i;
        n -= i;

        // Normalizes (x3 should be 32 bits max).
        long carry = x3 >>> 32;
        if(carry != 0) { // Shift.
          x0 = x1;
          x1 = x2;
          x2 = x3 & LONG_32_BITS;
          x3 = carry;
          pow2 += 32;
        }
      }

      // Merges registers to a 63 bits mantissa.
      assert (x3 >= 0);
      int shift = 31 - magnitude(x3); // -1..30
      pow2 -= shift;
      long mantissa = (shift < 0) ? (x3 << 31) | (x2 >>> 1) : // x3 is 32 bits.
          (((x3 << 32) | x2) << shift) | (x1 >>> (32 - shift));
      return lpow2(mantissa, pow2);
    }
    else { // n < 0
      if(n < -324 - 20) {
        return 0.;
      }

      // Works with x1:x0 126 bits register.
      long x1 = m; // 63 bits.
      long x0 = 0; // 63 bits.
      int pow2 = 0;
      while(true) {
        // Normalizes x1:x0
        assert (x1 >= 0);
        int shift = 63 - magnitude(x1);
        x1 <<= shift;
        x1 |= x0 >>> (63 - shift);
        x0 = (x0 << shift) & LONG_63_BITS;
        pow2 -= shift;

        // Checks if division has to be performed.
        if(n == 0) {
          break; // Done.
        }

        // Retrieves power of 5 divisor.
        int i = (-n >= POW5_INT.length) ? POW5_INT.length - 1 : -n;
        int divisor = POW5_INT[i];

        // Performs the division (126 bits by 31 bits).
        long wh = (x1 >>> 32);
        long qh = wh / divisor;
        long r = wh - qh * divisor;
        long wl = (r << 32) | (x1 & LONG_32_BITS);
        long ql = wl / divisor;
        r = wl - ql * divisor;
        x1 = (qh << 32) | ql;

        wh = (r << 31) | (x0 >>> 32);
        qh = wh / divisor;
        r = wh - qh * divisor;
        wl = (r << 32) | (x0 & LONG_32_BITS);
        ql = wl / divisor;
        x0 = (qh << 32) | ql;

        // Adjusts powers.
        n += i;
        pow2 -= i;
      }
      return lpow2(x1, pow2);
    }
  }
}
