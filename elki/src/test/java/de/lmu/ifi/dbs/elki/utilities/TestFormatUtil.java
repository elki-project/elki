package de.lmu.ifi.dbs.elki.utilities;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;

public class TestFormatUtil implements JUnit4Test {
  @Test
  public void testParseDouble() {
    assertEquals(0., FormatUtil.parseDouble("0"), 0.);
    assertEquals(0., FormatUtil.parseDouble("0.0"), 0.);
    assertEquals(0., FormatUtil.parseDouble("0."), 0.);
    assertEquals(0., FormatUtil.parseDouble("0e10"), 0.);
    assertEquals(0., FormatUtil.parseDouble("0E10"), 0.);
    assertEquals(0., FormatUtil.parseDouble("0e-10"), 0.);
    assertEquals(0., FormatUtil.parseDouble("0E-10"), 0.);
    assertEquals(1., FormatUtil.parseDouble("1"), 0.);
    assertEquals(1., FormatUtil.parseDouble("1.0"), 0.);
    assertEquals(1., FormatUtil.parseDouble("1."), 0.);
    assertEquals(1., FormatUtil.parseDouble("1e0"), 0.);
    assertEquals(1., FormatUtil.parseDouble("1E0"), 0.);
    assertEquals(1., FormatUtil.parseDouble("1e-0"), 0.);
    assertEquals(1., FormatUtil.parseDouble("1E-0"), 0.);
    assertEquals(2., FormatUtil.parseDouble("2"), 0.);
    assertEquals(2., FormatUtil.parseDouble("2.0"), 0.);
    assertEquals(2., FormatUtil.parseDouble("2."), 0.);
    assertEquals(2., FormatUtil.parseDouble("2e0"), 0.);
    assertEquals(2., FormatUtil.parseDouble("2E0"), 0.);
    assertEquals(2., FormatUtil.parseDouble("2e-0"), 0.);
    assertEquals(2., FormatUtil.parseDouble("2E-0"), 0.);
    assertEquals(-1., FormatUtil.parseDouble("-1"), 0.);
    assertEquals(-1., FormatUtil.parseDouble("-1.0"), 0.);
    assertEquals(.2, FormatUtil.parseDouble("0.2"), 0.);
    assertEquals(-.2, FormatUtil.parseDouble("-0.2"), 0.);
    assertEquals(.2, FormatUtil.parseDouble(".2"), 0.);
    assertEquals(-.2, FormatUtil.parseDouble("-.2"), 0.);
    assertEquals(2000., FormatUtil.parseDouble("2.0e3"), 0.);
    assertEquals(2000., FormatUtil.parseDouble("2.0E3"), 0.);
    assertEquals(-2000., FormatUtil.parseDouble("-2.0e3"), 0.);
    assertEquals(-2000., FormatUtil.parseDouble("-2.0E3"), 0.);
    assertEquals(.002, FormatUtil.parseDouble("2.0e-3"), 0.);
    assertEquals(.002, FormatUtil.parseDouble("2.0E-3"), 0.);
    assertEquals(-.002, FormatUtil.parseDouble("-2.0e-3"), 0.);
    assertEquals(-.002, FormatUtil.parseDouble("-2.0E-3"), 0.);

    // Case where the JDK had a serious bug, in a few variations
    assertEquals(2.2250738585072012e-308, FormatUtil.parseDouble("2.2250738585072012e-308"), 0.);
    assertEquals(0.00022250738585072012e-304, FormatUtil.parseDouble("0.00022250738585072012e-304"), 0.);
    assertEquals(00000000002.2250738585072012e-308, FormatUtil.parseDouble("00000000002.2250738585072012e-308"), 0.);
    assertEquals(2.2250738585072012e-00308, FormatUtil.parseDouble("2.2250738585072012e-00308"), 0.);

    assertTrue(Double.POSITIVE_INFINITY == FormatUtil.parseDouble("inf"));
    assertTrue(Double.NEGATIVE_INFINITY == FormatUtil.parseDouble("-inf"));
    assertTrue(Double.POSITIVE_INFINITY == FormatUtil.parseDouble("∞"));
    assertTrue(Double.NEGATIVE_INFINITY == FormatUtil.parseDouble("-∞"));
    assertTrue(Double.isNaN(FormatUtil.parseDouble("nan")));

    assertEquals(1, FormatUtil.parseDouble("+1"), 0.);
  }

  @Test(expected = NumberFormatException.class)
  public void textOnlyPlus() {
    FormatUtil.parseDouble("+");
  }

  @Test(expected = NumberFormatException.class)
  public void textExtraCharacer() {
    FormatUtil.parseDouble("123Banana");
  }

  @Test(expected = NumberFormatException.class)
  public void textTooManyDigits() {
    FormatUtil.parseDouble("123456789012345678901234567890");
  }

  @Test(expected = NumberFormatException.class)
  public void textNoExponent() {
    FormatUtil.parseDouble("1e");
  }

  @Test(expected = NumberFormatException.class)
  public void textNoExponentMinus() {
    FormatUtil.parseDouble("1e-");
  }

  @Test(expected = NumberFormatException.class)
  public void textEmptyString() {
    FormatUtil.parseDouble("");
  }

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
}
