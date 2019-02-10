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

import static de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.BitSet;
import java.util.Random;

import org.junit.Test;

/**
 * Test bit manipulation code.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class BitsUtilTest {
  @Test
  public void testAgainstBigInteger() {
    BigInteger bigint = new BigInteger("123");
    long[] bituti = make(Long.SIZE, 123);
    assertEquals("Bit strings do not agree.", bigint.toString(2), BitsUtil.toString(bituti));

    bigint = bigint.shiftLeft(13);
    shiftLeftI(bituti, 13);
    assertEquals("Bit strings do not agree.", bigint.toString(2), BitsUtil.toString(bituti));

    bigint = bigint.shiftRight(15);
    shiftRightI(bituti, 15);
    assertEquals("Bit strings do not agree.", bigint.toString(2), BitsUtil.toString(bituti));
  }

  @Test
  public void testSimpleOperations() {
    long[] test = zero(128);
    assertFalse(get(test, 0) || get(test, 31271));
    long tmp = setC(test[0], 5);
    assertTrue(get(tmp, 5) && !get(tmp, 4));
    assertEquals(clearC(tmp, 5), 0L);
    setI(test, 5);
    assertEquals(tmp, test[0]);
    tmp = setC(test[0], 7);
    setI(test, 7);
    assertEquals(tmp, test[0]);
    assertEquals(BitsUtil.toString(test), "10100000");
    assertEquals(BitsUtil.toStringLow(test), "00000101");
    assertEquals(BitsUtil.toString(tmp), "10100000");
    assertEquals(BitsUtil.toStringLow(tmp), "00000101");
    assertEquals(5, numberOfTrailingZerosSigned(test));
    truncateI(test, 7);
    assertEquals(BitsUtil.toString(test), "100000");
    assertEquals(BitsUtil.toStringLow(test), "000001");
    assertEquals(5, numberOfTrailingZerosSigned(test));
    setI(test, 7);
    assertEquals(BitsUtil.toString(test), "10100000");
    assertEquals(BitsUtil.toStringLow(test), "00000101");
    assertEquals(5, numberOfTrailingZerosSigned(test));
    tmp = cycleRightC(test[0], 6, 8);
    cycleRightI(test, 6, 8);
    assertEquals(tmp, test[0]);
    assertEquals(BitsUtil.toString(test), "10000010");
    assertEquals(BitsUtil.toStringLow(test), "01000001");
    assertEquals(1, numberOfTrailingZerosSigned(test));
    assertEquals(1, numberOfTrailingZeros(test));
    assertEquals(1, numberOfTrailingZerosSigned(tmp));
    assertEquals(1, numberOfTrailingZeros(tmp));
    assertEquals(2, cardinality(test));
    assertEquals(2, cardinality(test[0]));

    tmp = cycleLeftC(test[0], 6, 8);
    cycleLeftI(test, 6, 8);
    assertEquals(tmp, test[0]);
    assertEquals(BitsUtil.toString(test), "10100000");
    assertEquals(BitsUtil.toStringLow(test), "00000101");

    // Longer tests
    zeroI(test);
    setI(test, 125);
    setI(test, 60);
    cycleRightI(test, 70, 128);
    assertTrue(get(test, 55));
    assertTrue(get(test, 118));
    assertEquals(2, cardinality(test));
    assertEquals(1, cardinality(test[0]));
    cycleLeftI(test, 70, 128);
    assertTrue(get(test, 125));
    assertTrue(get(test, 60));
    assertTrue(get(test[0], 60));
    assertEquals(2, cardinality(test));
    assertEquals(1, cardinality(test[0]));

    long[] test2 = ones(327);
    assertEquals(327, cardinality(test2));
    invertI(test2);
    assertEquals(capacity(test2) - 327, cardinality(test2));

    onesI(test2, 327);
    orI(test2, test2, -128);
    assertEquals(327, cardinality(test2));

    onesI(test2, 327);
    xorI(test2, test2, -128);
    assertEquals(199, cardinality(test2));

    onesI(test2, 327);
    xorI(test2, test2, 128);
    assertEquals(capacity(test2) - 327 + 128, cardinality(test2));

    onesI(test2, 327);
    andI(test2, test2, -128);
    assertEquals(213, cardinality(test2));

    onesI(test2, 327);
    andI(test2, test2, 128);
    assertEquals(199, cardinality(test2));

    onesI(test2, 327);
    nandI(test2, test2);
    assertTrue(isZero(test2));

    // Word shifts
    onesI(test2, 327);
    shiftRightI(test2, 64);
    assertEquals(327 - 64, cardinality(test2));

    onesI(test2, 327);
    shiftLeftI(test2, 64);
    assertEquals(capacity(test2) - 64, cardinality(test2));

    // Non-divisible shifts
    onesI(test2, 327);
    shiftRightI(test2, 72);
    assertEquals(327 - 72, cardinality(test2));

    onesI(test2, 327);
    shiftLeftI(test2, 72);
    assertEquals(capacity(test2) - 72, cardinality(test2));

    // Negative offsets
    onesI(test2, 327);
    shiftLeftI(test2, -65);
    assertEquals(327 - 65, cardinality(test2));

    onesI(test2, 327);
    shiftRightI(test2, -65);
    assertEquals(capacity(test2) - 65, cardinality(test2));
  }

  @Test
  public void testToString() {
    assertEquals("null", BitsUtil.toString(null));
    assertEquals("0", BitsUtil.toString(0L));
    assertEquals("0", BitsUtil.toString(zero(5)));
    assertEquals("0", BitsUtil.toString(zero(65)));
    assertEquals("0000", BitsUtil.toString(zero(5), 4));
    assertEquals("0000", BitsUtil.toString(zero(65), 4));
    assertEquals("1100", BitsUtil.toString(make(65, 12)));
    assertEquals("0001100", BitsUtil.toString(make(65, 12), 7));
    assertEquals("2 3", BitsUtil.toString(make(65, 12), " ", 0));
    assertEquals("3 4", BitsUtil.toString(make(65, 12), " ", 1));
    // low endian
    assertEquals("null", BitsUtil.toStringLow(null));
    assertEquals("0", BitsUtil.toStringLow(0L));
    assertEquals("0", BitsUtil.toStringLow(zero(5)));
    assertEquals("0", BitsUtil.toStringLow(zero(65)));
    assertEquals("0000", BitsUtil.toStringLow(zero(5), 4));
    assertEquals("0000", BitsUtil.toStringLow(zero(65), 4));
    assertEquals("0011", BitsUtil.toStringLow(make(65, 12)));
    assertEquals("0011000", BitsUtil.toStringLow(make(65, 12), 7));
  }

  @Test
  public void testSorting() {
    final Random r = new Random(0);
    final int cnt = 100;
    long[] rnds = new long[cnt];
    long[][] bits = new long[cnt][];
    for(int i = 0; i < cnt; i++) {
      rnds[i] = Math.abs(r.nextLong());
      bits[i] = make(Long.SIZE, rnds[i]);
    }

    for(int i = 0; i < cnt; i++) {
      for(int j = 0; j < cnt; j++) {
        assertEquals(Long.compare(rnds[i], rnds[j]), compare(bits[i], bits[j]));
      }
    }

    for(int i = 0; i < cnt; i++) {
      long[] btmp = copy(bits[i], 64 + r.nextInt(500));
      assertEquals(0, compare(btmp, bits[i]));
      for(int j = 0; j < cnt; j++) {
        assertEquals(Long.compare(rnds[i], rnds[j]), compare(btmp, bits[j]));
      }
    }

    for(int i = 0; i < cnt; i++) {
      long[] btmp = truncateI(copy(bits[i]), 47);
      for(int j = 0; j < cnt; j++) {
        assertEquals(Long.compare(rnds[i] & ((1 << 48) - 1), rnds[j]), compare(btmp, bits[j]));
      }
    }

    for(int i = 0; i < cnt; i++) {
      long[] btmp = cycleRightI(copy(bits[i]), 13, Long.SIZE - 32);
      long ltmp = cycleRightC(rnds[i], 13, Long.SIZE - 32);
      for(int j = 0; j < cnt; j++) {
        assertEquals(Long.compare(ltmp, rnds[j]), compare(btmp, bits[j]));
      }
    }
  }

  @Test
  public void testAgainstBitSet() {
    BitSet bitset = new BitSet();
    long[] bituti = zero(Long.SIZE);
    for(int i = 0; i >= 0;) {
      assertEquals("Bit strings do not agree.", bitset.nextSetBit(i), nextSetBit(bituti, i));
      i = bitset.nextSetBit(i + 1);
    }
    assertEquals("Bit strings do not agree.", BitsUtil.toString(bitset.toLongArray()), BitsUtil.toString(bituti));

    bitset.set(4);
    setI(bituti, 4);
    for(int i = 0; i >= 0;) {
      assertEquals("Bit strings do not agree.", bitset.nextSetBit(i), nextSetBit(bituti, i));
      i = bitset.nextSetBit(i + 1);
    }
    assertEquals("Bit strings do not agree.", BitsUtil.toString(bitset.toLongArray()), BitsUtil.toString(bituti));

    bitset.set(15);
    setI(bituti, 15);
    for(int i = 0; i >= 0;) {
      assertEquals("Bit strings do not agree.", bitset.nextSetBit(i), nextSetBit(bituti, i));
      i = bitset.nextSetBit(i + 1);
    }
    assertEquals("Bit strings do not agree.", BitsUtil.toString(bitset.toLongArray()), BitsUtil.toString(bituti));

    assertEquals(bitset.nextSetBit(0), nextSetBit(bituti, 0));
    assertEquals(bitset.nextSetBit(4), nextSetBit(bituti, 4));
    assertEquals(bitset.nextSetBit(5), nextSetBit(bituti, 5));
    assertEquals(bitset.nextSetBit(999), nextSetBit(bituti, 999));
    assertEquals(bitset.previousSetBit(64), previousSetBit(bituti, 64));
    assertEquals(bitset.previousSetBit(15), previousSetBit(bituti, 15));
    assertEquals(bitset.previousSetBit(14), previousSetBit(bituti, 14));
    assertEquals(bitset.previousSetBit(-1), previousSetBit(bituti, -1));
    assertEquals(1, nextSetBit(2L, -1));
    assertEquals(-1, nextSetBit(1L, 99));
    assertEquals(-1, previousSetBit(1L, -1));
    assertEquals(1, previousSetBit(2L, 99));
  }

  @Test
  public void testIteration() {
    // All zero
    assertEquals(-1, nextSetBit(0, 0));
    assertEquals(-1, previousSetBit(0, 0));
    assertEquals(-1, nextSetBit(0, 13));
    assertEquals(-1, previousSetBit(0, 13));
    assertEquals(0, nextClearBit(0, 0));
    assertEquals(0, previousClearBit(0, 0));
    assertEquals(13, nextClearBit(0, 13));
    assertEquals(13, previousClearBit(0, 13));

    // All one
    assertEquals(0, nextSetBit(~0, 0));
    assertEquals(0, previousSetBit(~0, 0));
    assertEquals(13, nextSetBit(~0, 13));
    assertEquals(13, previousSetBit(~0, 13));
    assertEquals(-1, nextClearBit(~0, 0));
    assertEquals(-1, previousClearBit(~0, 0));
    assertEquals(-1, nextClearBit(~0, 13));
    assertEquals(-1, previousClearBit(~0, 13));

    // Two bits set.
    long two = (1L << 42) | (1L << 13);
    assertEquals(13, nextSetBit(two, 0));
    assertEquals(13, nextSetBit(two, 13));
    assertEquals(42, nextSetBit(two, 14));
    assertEquals(42, nextSetBit(two, 42));
    assertEquals(-1, nextSetBit(two, 43));
    assertEquals(13, nextClearBit(~two, 12));
    assertEquals(13, nextClearBit(~two, 13));
    assertEquals(42, nextClearBit(~two, 14));
    assertEquals(42, nextClearBit(~two, 42));
    assertEquals(-1, nextClearBit(~two, 43));
    assertEquals(-1, previousSetBit(two, 0));
    assertEquals(-1, previousSetBit(two, 12));
    assertEquals(13, previousSetBit(two, 13));
    assertEquals(13, previousSetBit(two, 14));
    assertEquals(13, previousSetBit(two, 41));
    assertEquals(42, previousSetBit(two, 42));
    assertEquals(42, previousSetBit(two, 43));
    assertEquals(-1, previousClearBit(~two, 12));
    assertEquals(13, previousClearBit(~two, 13));
    assertEquals(13, previousClearBit(~two, 14));
    assertEquals(13, previousClearBit(~two, 41));
    assertEquals(42, previousClearBit(~two, 42));
    assertEquals(42, previousClearBit(~two, 43));

    long[] zero = zero(512), ones = ones(512);
    // All zeros
    assertEquals(-1, nextSetBit(zero, 0));
    assertEquals(-1, nextSetBit(zero, 71));
    assertEquals(0, nextClearBit(zero, 0));
    assertEquals(71, nextClearBit(zero, 71));
    assertEquals(-1, previousSetBit(zero, 0));
    assertEquals(-1, previousSetBit(zero, 71));
    assertEquals(0, previousClearBit(zero, 0));
    assertEquals(71, previousClearBit(zero, 71));

    // All ones
    assertEquals(0, nextSetBit(ones, 0));
    assertEquals(71, nextSetBit(ones, 71));
    assertEquals(-1, nextClearBit(ones, 0));
    assertEquals(-1, nextClearBit(ones, 71));
    assertEquals(0, previousSetBit(ones, 0));
    assertEquals(71, previousSetBit(ones, 71));
    assertEquals(-1, previousClearBit(ones, 0));
    assertEquals(-1, previousClearBit(ones, 71));

    // One bit set
    long[] set = zero(512);
    setI(set, 391);
    assertEquals(391, nextSetBit(set, 0));
    assertEquals(391, nextSetBit(set, 391));
    assertEquals(-1, nextSetBit(set, 392));
    assertEquals(-1, previousSetBit(set, 0));
    assertEquals(-1, previousSetBit(set, 390));
    assertEquals(391, previousSetBit(set, 391));
    assertEquals(391, previousSetBit(set, 511));
    assertEquals(0, nextClearBit(set, 0));
    assertEquals(390, nextClearBit(set, 390));
    assertEquals(392, nextClearBit(set, 391));
    assertEquals(392, nextClearBit(set, 392));
    assertEquals(0, previousClearBit(set, 0));
    assertEquals(390, previousClearBit(set, 390));
    assertEquals(390, previousClearBit(set, 391));
    assertEquals(392, previousClearBit(set, 392));
  }

  @Test
  public void testGrayCoding() {
    // Single long version
    long sbits = flipC(0L, 61);
    assertEquals(invgrayC(sbits), 0x3FFF_FFFF_FFFF_FFFFL);
    assertEquals(invgrayC(grayC(sbits)), sbits);
    // Full version
    long[] bits = zero(123), ones = zero(123);
    flipI(bits, 122); // initialize
    onesI(ones, 123); // initialize
    invgrayI(bits);
    assertTrue(equal(bits, ones));
    grayI(bits);
    assertTrue(get(bits, 122));
    assertEquals(1, cardinality(bits));
  }

  @Test
  public void testLeadingTrailing() {
    int[] testi = new int[] { 0x7, 0x12345678, 0x23456789, 0x45678900, 0x89000000, 0xFFFF0000, -1, 0 };
    int[] truli = new int[] { 29, 3, 2, 1, 0, 0, 0, 32 };
    int[] truti = new int[] { 0, 3, 0, 8, 24, 16, 0, 32 };
    for(int i = 0; i < testi.length; i++) {
      assertEquals("Leading zeros don't agree for " + BitsUtil.toString(testi[i]), truli[i], numberOfLeadingZeros(testi[i]));
      assertEquals("Trailing zeros don't agree for " + BitsUtil.toString(testi[i]), truti[i], numberOfTrailingZeros(testi[i]));
      assertEquals("Leading zeros don't agree for " + BitsUtil.toString(testi[i]), truli[i] == 32 ? -1 : truli[i], numberOfLeadingZerosSigned(testi[i]));
      assertEquals("Trailing zeros don't agree for " + BitsUtil.toString(testi[i]), truti[i] == 32 ? -1 : truti[i], numberOfTrailingZerosSigned(testi[i]));
    }

    long[] testl = new long[] { 0x7L, 0x12345678L, 0x23456789L, 0x45678900L, 0x89000000L, 0x1FFFF0000L, 0x123456789ABCDEFL, 0x0011001188008800L, -1, 0 };
    int[] trull = new int[] { 61, 35, 34, 33, 32, 31, 7, 11, 0, 64 };
    int[] trutl = new int[] { 0, 3, 0, 8, 24, 16, 0, 11, 0, 64 };
    for(int i = 0; i < testl.length; i++) {
      assertEquals("Leading zeros don't agree for " + BitsUtil.toString(testl[i]), trull[i], numberOfLeadingZeros(testl[i]));
      assertEquals("Trailing zeros don't agree for " + BitsUtil.toString(testl[i]), trutl[i], numberOfTrailingZeros(testl[i]));
      assertEquals("Leading zeros don't agree for " + BitsUtil.toString(testl[i]), trull[i] == 64 ? -1 : trull[i], numberOfLeadingZerosSigned(testl[i]));
      assertEquals("Trailing zeros don't agree for " + BitsUtil.toString(testl[i]), trutl[i] == 64 ? -1 : trutl[i], numberOfTrailingZerosSigned(testl[i]));
    }

    long[][] testll = new long[testl.length][];
    int[] trulll = new int[testl.length], trutll = new int[testl.length];
    for(int i = 0; i < testl.length; i++) {
      testll[i] = make(128, testl[i]);
      shiftLeftI(testll[i], i + 1);
      trulll[i] = testl[i] == 0 ? 128 : trull[i] + 63 - i;
      trutll[i] = testl[i] == 0 ? 128 : trutl[i] + 1 + i;
    }
    for(int i = 0; i < testll.length; i++) {
      assertEquals("Leading zeros don't agree for " + BitsUtil.toString(testll[i]), trulll[i], numberOfLeadingZeros(testll[i]));
      assertEquals("Trailing zeros don't agree for " + BitsUtil.toString(testll[i]), trutll[i], numberOfTrailingZeros(testll[i]));
      assertEquals("Leading zeros don't agree for " + BitsUtil.toString(testll[i]), trulll[i] == 128 ? -1 : trulll[i], numberOfLeadingZerosSigned(testll[i]));
      assertEquals("Trailing zeros don't agree for " + BitsUtil.toString(testll[i]), trutll[i] == 128 ? -1 : trutll[i], numberOfTrailingZerosSigned(testll[i]));
    }
  }

  @Test
  public void testCardinality() {
    long[] ones = ones(128);
    assertEquals("Ones not correct", 2, ones.length);
    assertEquals("Ones not correct", -1L, ones[0]);
    assertEquals("Ones not correct", -1L, ones[1]);
    assertEquals("Cardinality not correct.", 128, cardinality(ones));
  }

  @Test
  public void testRandomBitset() {
    for(int card : new int[] { 0, 1, 7, 13, 63, 110, 126, 128 }) {
      for(long seed = 0; seed < 5; seed++) {
        long[] set = random(card, 128, new Random(seed));
        assertEquals("Bitset too large", 2, set.length);
        assertEquals("Wrong cardinality", card, cardinality(set));
      }
    }
  }
}
