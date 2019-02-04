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
package de.lmu.ifi.dbs.elki.math;

import static de.lmu.ifi.dbs.elki.math.MathUtil.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

/**
 * Unit test for some basic math functions.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class MathUtilTest {
  @Test
  public void testBitMath() {
    assertEquals("Bit math issues", 1024, nextPow2Int(912));
    assertEquals("Bit math issues", 8, nextPow2Int(5));
    assertEquals("Bit math issues", 4, nextPow2Int(4));
    assertEquals("Bit math issues", 4, nextPow2Int(3));
    assertEquals("Bit math issues", 2, nextPow2Int(2));
    assertEquals("Bit math issues", 1, nextPow2Int(1));
    assertEquals("Bit math issues", 0, nextPow2Int(0));
    assertEquals("Bit math issues", 1024L, nextPow2Long(912L));
    assertEquals("Bit math issues", 0, nextPow2Int(-1));
    assertEquals("Bit math issues", 0, nextPow2Int(-2));
    assertEquals("Bit math issues", 0, nextPow2Int(-99));
    assertEquals("Bit math issues", 15, nextAllOnesInt(8));
    assertEquals("Bit math issues", 7, nextAllOnesInt(4));
    assertEquals("Bit math issues", 3, nextAllOnesInt(3));
    assertEquals("Bit math issues", 3, nextAllOnesInt(2));
    assertEquals("Bit math issues", 1, nextAllOnesInt(1));
    assertEquals("Bit math issues", 0, nextAllOnesInt(0));
    assertEquals("Bit math issues", -1, nextAllOnesInt(-1));
    assertEquals("Bit math issues", 0, 0 >>> 1);
    assertEquals("Bit math issues", 15, nextAllOnesLong(8));
  }

  @Test
  public void testFloatToDouble() {
    Random r = new Random(1l);
    for(int i = 0; i < 10000; i++) {
      final double dbl = Double.longBitsToDouble(r.nextLong());
      final float flt = (float) dbl;
      final double uppd = floatToDoubleUpper(flt);
      final float uppf = (float) uppd;
      final double lowd = floatToDoubleLower(flt);
      final float lowf = (float) lowd;
      assertTrue("Expected value to become larger, but " + uppd + " < " + dbl, uppd >= dbl || Double.isNaN(dbl));
      assertTrue("Expected value to round to the same float.", flt == uppf || Double.isNaN(flt));
      assertTrue("Expected value to become smaller, but " + lowd + " > " + dbl, lowd <= dbl || Double.isNaN(dbl));
      assertTrue("Expected value to round to the same float.", flt == lowf || Double.isNaN(flt));
    }
  }

  @Test
  public void testPowi() {
    assertEquals("Power incorrect", 0.01, powi(0.1, 2), 1e-17);
    assertEquals("Power incorrect", 0.001, powi(0.1, 3), 1e-18);
    assertEquals("Power incorrect", 0.0001, powi(0.1, 4), 1e-19);
    assertEquals("Power incorrect", 0.00001, powi(0.1, 5), 1e-20);
    assertEquals("Power incorrect", 9, ipowi(3, 2));
    assertEquals("Power incorrect", 27, ipowi(3, 3));
    assertEquals("Power incorrect", 81, ipowi(3, 4));
    assertEquals("Power incorrect", 243, ipowi(3, 5));
    assertEquals("Power incorrect", 729, ipowi(3, 6));
    assertEquals("Power incorrect", 2187, ipowi(3, 7));
    assertEquals("Power incorrect", 125, ipowi(5, 3));
    assertEquals("Power incorrect", 256, ipowi(16, 2));
    assertEquals("Power incorrect", 0x10000, ipowi(16, 4));
    assertEquals("Power incorrect", 0x10000, powi(16., 4), 0);
  }

  @Test
  public void testRad2deg() {
    assertEquals(Math.PI / 2, deg2rad(90), 0.);
    assertEquals(180, rad2deg(Math.PI), 0.);
    assertEquals(12.34, rad2deg(deg2rad(12.34)), 0);
    assertEquals(1.7 * Math.PI, normAngle(3.7 * Math.PI), 0.);
    assertEquals(1.7 * Math.PI, normAngle(-.3 * Math.PI), 0.);
  }

  @Test
  public void testSequence() {
    int[] s1 = sequence(4, 7);
    assertEquals(7 - 4, s1.length);
    for(int i = 0; i < s1.length; i++) {
      assertEquals(i + 4, s1[i]);
    }
    assertEquals(0, sequence(5, 1).length);
  }

  @Test
  public void minMax() {
    for(int i = 0; i < 256; i++) {
      int[] a = { i & 3, (i >> 2) & 3, (i >> 4) & 3, (i >> 8) & 3 };
      assertEquals(Math.max(a[0], a[1]), max(a[0], a[1]));
      assertEquals(Math.min(a[0], a[1]), min(a[0], a[1]));
      assertEquals(Math.max(Math.max(a[0], a[1]), a[2]), max(a[0], a[1], a[2]));
      assertEquals(Math.min(Math.min(a[0], a[1]), a[2]), min(a[0], a[1], a[2]));
      assertEquals(Math.max(Math.max(a[0], a[1]), Math.max(a[2], a[3])), max(a[0], a[1], a[2], a[3]));
      assertEquals(Math.min(Math.min(a[0], a[1]), Math.min(a[2], a[3])), min(a[0], a[1], a[2], a[3]));
    }
  }

  @Test
  public void minMaxDouble() {
    for(int i = 0; i < 256; i++) {
      double[] a = { i & 3, (i >> 2) & 3, (i >> 4) & 3, (i >> 8) & 3 };
      assertEquals(Math.max(a[0], a[1]), max(a[0], a[1]), 0.);
      assertEquals(Math.min(a[0], a[1]), min(a[0], a[1]), 0.);
      assertEquals(Math.max(Math.max(a[0], a[1]), a[2]), max(a[0], a[1], a[2]), 0.);
      assertEquals(Math.min(Math.min(a[0], a[1]), a[2]), min(a[0], a[1], a[2]), 0.);
      assertEquals(Math.max(Math.max(a[0], a[1]), Math.max(a[2], a[3])), max(a[0], a[1], a[2], a[3]), 0.);
      assertEquals(Math.min(Math.min(a[0], a[1]), Math.min(a[2], a[3])), min(a[0], a[1], a[2], a[3]), 0.);
    }
  }

  @Test
  public void testSumFirstIntegers() {
    assertEquals(5050, sumFirstIntegers(100)); // Famous Gauss example
  }

  @Test
  public void testFactorial() {
    assertEquals(6, factorial(3));
    assertEquals(6, approximateFactorial(3), 0.);
    assertEquals(87178291200L, factorial(14));
    assertEquals(87178291200., approximateFactorial(14), 0.);
    assertEquals(2432902008176640000L, factorial(20));
    assertEquals(2432902008176640000., approximateFactorial(20), 0.);
  }

  @Test
  public void testBinomialCoefficient() {
    int[] eight = new int[] { 1, 8, 28, 56, 70 };
    for(int i = 0; i < eight.length; i++) {
      assertEquals(eight[i], binomialCoefficient(8, i));
      assertEquals(eight[i], approximateBinomialCoefficient(8, i), 0.);
    }
  }
}
