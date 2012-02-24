package experimentalcode.erich;

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

import java.util.Arrays;

/**
 * Utilities for bit operations.
 * 
 * Implementation note: words are stored in little-endian word order. This can
 * be a bit confusing, because a shift-right means "left" on the word level.
 * 
 * Naming: methods with a <code>C</code> return a copy, methods with
 * <code>I</code> modify in-place.
 * 
 * @author Erich Schubert
 */
public final class BitsUtil {
  /**
   * Shift factor for a long: 2^6 == 64 == Long.SIZE
   */
  private static final int LONG_LOG2_SIZE = 6;

  /**
   * Masking for long shifts.
   */
  private static final int LONG_LOG2_MASK = 0x3f; // 6 bits

  /**
   * Long with all bits set
   */
  private static final long LONG_ALL_BITS = -1L;

  /**
   * Allocate a new long[].
   * 
   * @param bits Number of bits in storage
   * @return New array
   */
  public static long[] zero(int bits) {
    return new long[((bits - 1) >>> LONG_LOG2_SIZE) + 1];
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
    final int fillWords = bits >>> LONG_LOG2_SIZE;
    final int fillBits = bits & LONG_LOG2_MASK;
    Arrays.fill(v, 0, fillWords, LONG_ALL_BITS);
    v[v.length - 1] = (1L << fillBits) - 1;
    return v;
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
   * 
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
   * Compute corresponding gray code as v XOR (v >>> 1)
   * 
   * @param v Value
   * @return Gray code
   */
  public static long grayC(long v) {
    return v ^ (v >>> 1);
  }

  /**
   * Compute corresponding gray code as v XOR (v >>> 1)
   * 
   * @param v Value
   * @return Gray code
   */
  public static long[] grayI(long[] v) {
    // TODO: copy less
    long[] t = copy(v);
    shiftRightI(t, 1);
    xorI(v, t);
    return v;
  }

  /**
   * Compute the inverted gray code, v XOR (v >>> 1) XOR (v >>> 2) ...
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
   * Compute the inverted gray code, v XOR (v >>> 1) XOR (v >>> 2) ...
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
   * 
   * Low-endian layout for the array.
   * 
   * @param v Value
   * @return Number of bits set in long[]
   */
  public static long cardinality(long[] v) {
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
    v ^= (1L << off);
    return v;
  }

  /**
   * Invert bit number "off" in v.
   * 
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
    v |= (1L << off);
    return v;
  }

  /**
   * Set bit number "off" in v.
   * 
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
   * Clear bit number "off" in v.
   * 
   * @param v Buffer
   * @param off Offset to clear
   */
  public static long clearC(long v, int off) {
    v &= ~(1L << off);
    return v;
  }

  /**
   * Clear bit number "off" in v.
   * 
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
   * 
   * Low-endian layout for the array.
   * 
   * @param v Buffer
   * @param off Offset to set
   */
  public static boolean get(long[] v, int off) {
    final int wordindex = off >>> LONG_LOG2_SIZE;
    return (v[wordindex] & (1L << off)) != 0;
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
   * XOR o onto v inplace, i.e. v ^= o
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
   * XOR o onto v inplace, i.e. v ^= (o << off)
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
    if(off < 0) {
      throw new UnsupportedOperationException("Negative shifts are not supported.");
    }
    // Break shift into integers to shift and bits to shift
    final int shiftWords = off >>> LONG_LOG2_SIZE;
    final int shiftBits = off & LONG_LOG2_MASK;

    if(shiftWords >= v.length) {
      return v;
    }
    // Simple case - multiple of word size
    if(shiftBits == 0) {
      for(int i = shiftWords; i < v.length; i++) {
        v[i] ^= o[i - shiftWords];
      }
      return v;
    }
    // Overlapping case
    final int unshiftBits = Long.SIZE - shiftBits;
    final int end = Math.min(v.length, o.length + shiftWords) - 1;
    for(int i = end; i > shiftWords; i--) {
      final int src = i - shiftWords;
      v[i] ^= (o[src] << shiftBits) | (o[src - 1] >>> unshiftBits);
    }
    v[shiftWords] ^= o[0] << shiftBits;
    return v;
  }

  /**
   * OR o onto v inplace, i.e. v |= o
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
   * OR o onto v inplace, i.e. v |= (o << off)
   * 
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
    if(off < 0) {
      throw new UnsupportedOperationException("Negative shifts are not supported.");
    }
    // Break shift into integers to shift and bits to shift
    final int shiftWords = off >>> LONG_LOG2_SIZE;
    final int shiftBits = off & LONG_LOG2_MASK;

    if(shiftWords >= v.length) {
      return v;
    }
    // Simple case - multiple of word size
    if(shiftBits == 0) {
      for(int i = shiftWords; i < v.length; i++) {
        v[i] |= o[i - shiftWords];
      }
      return v;
    }
    // Overlapping case
    final int unshiftBits = Long.SIZE - shiftBits;
    final int end = Math.min(v.length, o.length + shiftWords) - 1;
    for(int i = end; i > shiftWords; i--) {
      final int src = i - shiftWords;
      v[i] |= (o[src] << shiftBits) | (o[src - 1] >>> unshiftBits);
    }
    v[shiftWords] |= o[0] << shiftBits;
    return v;
  }

  /**
   * AND o onto v inplace, i.e. v &= o
   * 
   * @param v Primary object
   * @param o data to and
   * @return v
   */
  public static long[] andI(long[] v, long[] o) {
    int i = 0;
    for(; i < o.length; i++) {
      v[i] |= o[i];
    }
    // Zero higher words
    Arrays.fill(v, i, v.length, 0);
    return v;
  }

  /**
   * AND o onto v inplace, i.e. v &= (o << off)
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
    if(off < 0) {
      throw new UnsupportedOperationException("Negative shifts are not supported.");
    }
    // Break shift into integers to shift and bits to shift
    final int shiftWords = off >>> LONG_LOG2_SIZE;
    final int shiftBits = off & LONG_LOG2_MASK;

    if(shiftWords >= v.length) {
      return v;
    }
    // Simple case - multiple of word size
    if(shiftBits == 0) {
      for(int i = shiftWords; i < v.length; i++) {
        v[i] &= o[i - shiftWords];
      }
      // Clear bottom words
      Arrays.fill(v, 0, shiftWords, 0);
      return v;
    }
    // Overlapping case
    final int unshiftBits = Long.SIZE - shiftBits;
    final int end = Math.min(v.length, o.length + shiftWords) - 1;
    Arrays.fill(v, end + 1, v.length, 0);
    for(int i = end; i > shiftWords; i--) {
      final int src = i - shiftWords;
      v[i] &= (o[src] << shiftBits) | (o[src - 1] >>> unshiftBits);
    }
    v[shiftWords] &= o[0] << shiftBits;
    // Clear bottom words
    Arrays.fill(v, 0, shiftWords, 0);
    return v;
  }

  /**
   * Invert v inplace.
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
   * Shift a long[] bitset inplace.
   * 
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
   * Shift a long[] bitset inplace.
   * 
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
    if(shift == 0) {
      return v;
    }
    if(shift < 0) {
      return cycleLeftC(v, -shift, len);
    }
    final long ones = (1 << len) - 1;
    return (((v) >>> (shift)) | ((v) << ((len) - (shift)))) & ones;
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
    truncateI(t, len);
    shiftRightI(v, shift);
    orI(v, t);
    return v;
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
    if(shift == 0) {
      return v;
    }
    if(shift < 0) {
      return cycleRightC(v, -shift, len);
    }
    final long ones = (1 << len) - 1;
    return (((v) << (shift)) | ((v) >>> ((len) - (shift)))) & ones;
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
    truncateI(t, len);
    shiftRightI(v, len - shift);
    orI(v, t);
    return v;
  }

  /**
   * Convert bitset to a string consisting of "0" and "1", in high-endian order.
   * 
   * @param v Value to process
   * @return String representation
   */
  public static String toString(long[] v) {
    final int mag = magnitude(v);
    if(v.length == 0 || mag == 0) {
      return "0";
    }
    final int words = ((mag - 1) >>> LONG_LOG2_SIZE) + 1;
    char[] digits = new char[mag];

    int pos = mag - 1;
    for(int w = 0; w < words; w++) {
      long f = 1l;
      for(int i = 0; i < Long.SIZE; i++) {
        digits[pos] = ((v[w] & f) == 0) ? '0' : '1';
        pos--;
        f <<= 1;
        if(pos < 0) {
          break;
        }
      }
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
    final int mag = Math.max(magnitude(v), minw);
    if(v.length == 0 || mag == 0) {
      return "0";
    }
    final int words = ((mag - 1) >>> LONG_LOG2_SIZE) + 1;
    char[] digits = new char[mag];

    int pos = mag - 1;
    for(int w = 0; w < words; w++) {
      long f = 1l;
      for(int i = 0; i < Long.SIZE; i++) {
        digits[pos] = ((v[w] & f) == 0) ? '0' : '1';
        pos--;
        f <<= 1;
        if(pos < 0) {
          break;
        }
      }
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

    int pos = mag - 1;
    long f = 1l;
    for(int i = 0; i < Long.SIZE; i++) {
      digits[pos] = ((v & f) == 0) ? '0' : '1';
      pos--;
      f <<= 1;
      if(pos < 0) {
        break;
      }
    }
    return new String(digits);
  }

  /**
   * Find the number of trailing zeros.
   * 
   * @param v Bitset
   * @return Position of first set bit, -1 if no one was found.
   */
  public static int numberOfTrailingZeros(long[] v) {
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
   * Find the number of leading zeros.
   * 
   * @param v Bitset
   * @return Position of first set bit, -1 if no one was found.
   */
  public static int numberOfLeadingZeros(long[] v) {
    for(int p = 0;; p++) {
      if(p == v.length) {
        return -1;
      }
      final int ip = v.length - 1 - p;
      if(v[ip] != 0) {
        return Long.numberOfLeadingZeros(v[ip]) + p * Long.SIZE;
      }
    }
  }

  /**
   * Find the number of leading zeros.
   * 
   * Note: this has different semantics to {@link Long.numberOfLeadingZeros}
   * when the number is 0.
   * 
   * @param v Bitset
   * @return Position of first set bit, -1 if no one was found.
   */
  public static int numberOfLeadingZeros(long v) {
    if(v == 0) {
      return -1;
    }
    return Long.numberOfLeadingZeros(v);
  }

  /**
   * Find the previous set bit.
   * 
   * @param v Values to process
   * @param start Start position (inclusive)
   * @return Position of previous set bit, or -1.
   */
  public static int previousSetBit(long[] v, int start) {
    if(start == -1) {
      return -1;
    }
    int wordindex = start >>> LONG_LOG2_SIZE;
    if(wordindex >= v.length) {
      return magnitude(v) - 1;
    }
    // Initial word
    final int off = Long.SIZE - 1 - (start & LONG_LOG2_MASK);
    long cur = v[wordindex] & (LONG_ALL_BITS >>> off);
    for(;;) {
      if(cur != 0) {
        return (wordindex + 1) * Long.SIZE - 1 - Long.numberOfLeadingZeros(cur);
      }
      if(wordindex == 0) {
        return -1;
      }
      wordindex--;
      cur = v[wordindex];
    }
  }

  /**
   * Find the previous clear bit.
   * 
   * @param v Values to process
   * @param start Start position (inclusive)
   * @return Position of previous clear bit, or -1.
   */
  public static int previousClearBit(long[] v, int start) {
    if(start == -1) {
      return -1;
    }
    int wordindex = start >>> LONG_LOG2_SIZE;
    if(wordindex >= v.length) {
      return magnitude(v);
    }
    final int off = Long.SIZE + 1 - (start & LONG_LOG2_MASK);
    // Initial word
    long cur = ~v[wordindex] & (LONG_ALL_BITS >>> off);
    for(;;) {
      if(cur != 0) {
        return (wordindex + 1) * Long.SIZE - 1 - Long.numberOfTrailingZeros(cur);
      }
      if(wordindex == 0) {
        return -1;
      }
      wordindex--;
      cur = ~v[wordindex];
    }
  }

  /**
   * Find the next set bit.
   * 
   * @param v Value to process
   * @param start Start position (inclusive)
   * @return Position of next set bit, or -1.
   */
  public static int nextSetBit(long[] v, int start) {
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
      wordindex++;
      if(wordindex == v.length) {
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
  public static int nextClearBit(long[] v, int start) {
    int wordindex = start >>> LONG_LOG2_SIZE;
    if(wordindex >= v.length) {
      return -1;
    }

    // Initial word
    long cur = ~v[wordindex] & (LONG_ALL_BITS << start);
    for(; wordindex < v.length;) {
      if(cur != 0) {
        return (wordindex * Long.SIZE) + Long.numberOfTrailingZeros(cur);
      }
      wordindex++;
      cur = ~v[wordindex];
    }
    return -1;
  }

  /**
   * The magnitude is the position of the highest bit set
   * 
   * @param v Vector v
   * @return position of highest bit set, or 0.
   */
  public static int magnitude(long[] v) {
    final int l = numberOfLeadingZeros(v);
    if(l < 0) {
      return 0;
    }
    return capacity(v) - l;
  }

  /**
   * The magnitude is the position of the highest bit set
   * 
   * @param v Vector v
   * @return position of highest bit set, or 0.
   */
  public static int magnitude(long v) {
    final int l = numberOfLeadingZeros(v);
    if(l < 0) {
      return 0;
    }
    return Long.SIZE - l;
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
   * Compare two bitsets.
   * 
   * @param x First bitset
   * @param y Second bitset
   * @return Comparison result
   */
  public static int compare(long[] x, long[] y) {
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
      final long xp = x[p];
      final long yp = y[p];
      if(xp != yp) {
        if(xp < 0) {
          if(yp < 0) {
            return -Long.compare(xp, yp);
          }
          else {
            return +1;
          }
        }
        else {
          if(yp < 0) {
            return -1;
          }
          else {
            return Long.compare(xp, yp);
          }
        }
      }
    }
    return 0;
  }
}