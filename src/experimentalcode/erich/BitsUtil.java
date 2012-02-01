package experimentalcode.erich;

import java.util.Arrays;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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
  private static final int LONG_LOG2_SIZE = 5;

  /**
   * Masking for long shifts.
   */
  private static final int LONG_LOG2_MASK = 0x3f; // 6 bits

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
    int wordindex = off >>> LONG_LOG2_SIZE;
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
    int wordindex = off >>> LONG_LOG2_SIZE;
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
    int wordindex = off >>> LONG_LOG2_SIZE;
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
    int wordindex = off >>> LONG_LOG2_SIZE;
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
}