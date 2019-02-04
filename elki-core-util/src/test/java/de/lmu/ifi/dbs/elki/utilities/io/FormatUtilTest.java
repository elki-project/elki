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
package de.lmu.ifi.dbs.elki.utilities.io;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Test formatting code.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class FormatUtilTest {
  @Test
  public void testStringSize() {
    long[] specialL = new long[] { 0L, 999L, 1001L, Long.MIN_VALUE, Long.MAX_VALUE };
    int[] specialI = new int[] { 0, 99999, 100001, Integer.MIN_VALUE, Integer.MAX_VALUE };

    // Positives
    for(long x = 1L; x > 0; x *= 2) {
      assertEquals("String length does not match for " + x, String.valueOf(x).length(), FormatUtil.stringSize(x));
    }
    // Negatives
    for(long x = -1L; x < 0; x *= 2) {
      assertEquals("String length does not match for " + x, String.valueOf(x).length(), FormatUtil.stringSize(x));
    }
    // Specials
    for(long x : specialL) {
      assertEquals("String length does not match for " + x, String.valueOf(x).length(), FormatUtil.stringSize(x));
    }
    for(int x : specialI) {
      assertEquals("String length does not match for " + x, String.valueOf(x).length(), FormatUtil.stringSize(x));
    }
  }

  @Test
  public void testFormatVector() {
    double[] v1 = { 1, 2, 3, Double.POSITIVE_INFINITY, Double.NaN };
    String expect = "1.0, 2.0, 3.0, Infinity, NaN";
    assertEquals("Vector not formatted as expected.", expect, FormatUtil.format(v1));
    double[] v2 = { 1, 2, 3 };
    String expect2 = "1.000,2.000,3.000";
    assertEquals("Vector not formatted as expected.", expect2, FormatUtil.format(v2, ",", FormatUtil.NF3));
  }

  @Test
  public void testFormatMatrix() {
    double[][] m = { { 1, 2 }, { 3, 11 } };
    String expect = "[\n [1.00, 2.00]\n [3.00, 11.00]\n]";
    assertEquals("Matrix not formatted as expected.", expect, FormatUtil.format(m));
    String expect2 = "[  1.00,  2.00][  3.00, 11.00]";
    assertEquals("Matrix not formatted as expected.", expect2, FormatUtil.format(m, 6, 2, "[", "]", ","));
  }
}
