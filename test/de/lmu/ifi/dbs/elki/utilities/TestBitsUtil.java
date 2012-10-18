package de.lmu.ifi.dbs.elki.utilities;

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

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.BitSet;
import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.utilities.BitsUtil;

public class TestBitsUtil implements JUnit4Test {
  @Test
  public void testAgainstBigInteger() {
    BigInteger bigint = new BigInteger("123");
    long[] bituti = BitsUtil.make(Long.SIZE, 123);
    assertEquals("Bit strings do not agree.", bigint.toString(2), BitsUtil.toString(bituti));

    bigint = bigint.shiftLeft(13);
    BitsUtil.shiftLeftI(bituti, 13);
    assertEquals("Bit strings do not agree.", bigint.toString(2), BitsUtil.toString(bituti));

    bigint = bigint.shiftRight(15);
    BitsUtil.shiftRightI(bituti, 15);
    assertEquals("Bit strings do not agree.", bigint.toString(2), BitsUtil.toString(bituti));
  }

  @Test
  public void testSimpleOperations() {
    long[] test = BitsUtil.zero(128);
    BitsUtil.setI(test, 5);
    BitsUtil.setI(test, 7);
    assertEquals(BitsUtil.toString(test), "10100000");
    assertEquals(BitsUtil.numberOfTrailingZerosSigned(test), 5);
    BitsUtil.truncateI(test, 7);
    assertEquals(BitsUtil.toString(test), "100000");
    assertEquals(BitsUtil.numberOfTrailingZerosSigned(test), 5);
    BitsUtil.setI(test, 7);
    assertEquals(BitsUtil.toString(test), "10100000");
    assertEquals(BitsUtil.numberOfTrailingZerosSigned(test), 5);
    BitsUtil.cycleRightI(test, 6, 8);
    assertEquals(BitsUtil.toString(test), "10000010");
    assertEquals(BitsUtil.numberOfTrailingZerosSigned(test), 1);
    assertEquals(BitsUtil.numberOfTrailingZeros(test), 1);

    BitsUtil.zeroI(test);
    BitsUtil.setI(test, 125);
    BitsUtil.setI(test, 60);
    BitsUtil.cycleRightI(test, 70, 128);
    assertTrue(BitsUtil.get(test, 55));
    assertTrue(BitsUtil.get(test, 118));
    assertEquals(BitsUtil.cardinality(test), 2);
    BitsUtil.cycleLeftI(test, 70, 128);
    assertTrue(BitsUtil.get(test, 125));
    assertTrue(BitsUtil.get(test, 60));
    assertEquals(BitsUtil.cardinality(test), 2);
  }

  @Test
  public void testSorting() {
    final Random r = new Random(0);
    final int cnt = 100;
    long[] rnds = new long[cnt];
    long[][] bits = new long[cnt][];
    for (int i = 0; i < cnt; i++) {
      rnds[i] = Math.abs(r.nextLong());
      bits[i] = BitsUtil.make(Long.SIZE, rnds[i]);
    }

    for (int i = 0; i < cnt; i++) {
      for (int j = 0; j < cnt; j++) {
        assertEquals(compare(rnds[i], rnds[j]), BitsUtil.compare(bits[i], bits[j]));
      }
    }

    for (int i = 0; i < cnt; i++) {
      long[] btmp = BitsUtil.copy(bits[i], 64 + r.nextInt(500));
      assertEquals(BitsUtil.compare(btmp, bits[i]), 0);
      for (int j = 0; j < cnt; j++) {
        assertEquals(compare(rnds[i], rnds[j]), BitsUtil.compare(btmp, bits[j]));
      }
    }

    for (int i = 0; i < cnt; i++) {
      long[] btmp = BitsUtil.truncateI(BitsUtil.copy(bits[i]), 47);
      for (int j = 0; j < cnt; j++) {
        assertEquals(compare(rnds[i] & ((1 << 48) - 1), rnds[j]), BitsUtil.compare(btmp, bits[j]));
      }
    }

    for (int i = 0; i < cnt; i++) {
      long[] btmp = BitsUtil.cycleRightI(BitsUtil.copy(bits[i]), 13, Long.SIZE - 32);
      long ltmp = BitsUtil.cycleRightC(rnds[i], 13, Long.SIZE - 32);
      for (int j = 0; j < cnt; j++) {
        assertEquals(compare(ltmp, rnds[j]), BitsUtil.compare(btmp, bits[j]));
      }
    }
  }

  /**
   * Not jet in Java 6. To come in JDK7 as Long.copmare
   * 
   * @param x
   * @param y
   * @return
   */
  public static int compare(long x, long y) {
    return (x < y) ? -1 : ((x == y) ? 0 : 1);
  }

  @Test
  public void testAgainstBitSet() {
    BitSet bitset = new BitSet();
    long[] bituti = BitsUtil.zero(Long.SIZE);
    for (int i = 0; i >= 0;) {
      assertEquals("Bit strings do not agree.", bitset.nextSetBit(i), BitsUtil.nextSetBit(bituti, i));
      i = bitset.nextSetBit(i + 1);
    }
    // Java 7:
    // assertEquals("Bit strings do not agree.",
    // BitsUtil.toString(bitset.toLongArray()), BitsUtil.toString(bituti));

    bitset.set(4);
    BitsUtil.setI(bituti, 4);
    for (int i = 0; i >= 0;) {
      assertEquals("Bit strings do not agree.", bitset.nextSetBit(i), BitsUtil.nextSetBit(bituti, i));
      i = bitset.nextSetBit(i + 1);
    }
    // Java 7:
    // assertEquals("Bit strings do not agree.",
    // BitsUtil.toString(bitset.toLongArray()), BitsUtil.toString(bituti));

    bitset.set(15);
    BitsUtil.setI(bituti, 15);
    for (int i = 0; i >= 0;) {
      assertEquals("Bit strings do not agree.", bitset.nextSetBit(i), BitsUtil.nextSetBit(bituti, i));
      i = bitset.nextSetBit(i + 1);
    }
    // Java 7:
    // assertEquals("Bit strings do not agree.",
    // BitsUtil.toString(bitset.toLongArray()), BitsUtil.toString(bituti));

    assertEquals(bitset.nextSetBit(0), BitsUtil.nextSetBit(bituti, 0));
    assertEquals(bitset.nextSetBit(4), BitsUtil.nextSetBit(bituti, 4));
    assertEquals(bitset.nextSetBit(5), BitsUtil.nextSetBit(bituti, 5));
    // previousSetBit is not in JDK6.
    // assertEquals(bitset.previousSetBit(64), BitsUtil.previousSetBit(bituti,
    // 64));
    // assertEquals(bitset.previousSetBit(15), BitsUtil.previousSetBit(bituti,
    // 15));
    // assertEquals(bitset.previousSetBit(14), BitsUtil.previousSetBit(bituti,
    // 14));
  }

  @Test
  public void testGrayCoding() {
    long[] bits = BitsUtil.zero(123);
    long[] ones = BitsUtil.ones(123);
    BitsUtil.flipI(bits, 122);
    BitsUtil.invgrayI(bits);
    BitsUtil.xorI(bits, ones);
    assertTrue(BitsUtil.isZero(bits));
    BitsUtil.xorI(bits, ones);
    BitsUtil.grayI(bits);
    assertTrue(BitsUtil.get(bits, 122));
    assertEquals(1, BitsUtil.cardinality(bits));
  }

  @Test
  public void testLeadingTrailing() {
    int[] testi = new int[] { 0x7, 0x12345678, 0x23456789, 0x45678900, 0x89000000, 0xFFFF0000 };
    int[] truli = new int[] { 29, 3, 2, 1, 0, 0 };
    int[] truti = new int[] { 0, 3, 0, 8, 24, 16 };
    for (int i = 0; i < testi.length; i++) {
      assertEquals("Leading zeros don't agree for " + BitsUtil.toString(testi[i]), truli[i], BitsUtil.numberOfLeadingZeros(testi[i]));
      assertEquals("Trailing zeros don't agree for " + BitsUtil.toString(testi[i]), truti[i], BitsUtil.numberOfTrailingZeros(testi[i]));
    }

    long[] testl = new long[] { 0x7L, 0x12345678L, 0x23456789L, 0x45678900L, 0x89000000L, 0x1FFFF0000L, 0x123456789ABCDEFL, 0x0011001188008800L };
    int[] trull = new int[] { 61, 35, 34, 33, 32, 31, 7, 11 };
    int[] trutl = new int[] { 0, 3, 0, 8, 24, 16, 0, 11 };
    for (int i = 0; i < testl.length; i++) {
      assertEquals("Leading zeros don't agree for " + BitsUtil.toString(testl[i]), trull[i], BitsUtil.numberOfLeadingZeros(testl[i]));
      assertEquals("Trailing zeros don't agree for " + BitsUtil.toString(testl[i]), trutl[i], BitsUtil.numberOfTrailingZeros(testl[i]));
    }
  }
}
