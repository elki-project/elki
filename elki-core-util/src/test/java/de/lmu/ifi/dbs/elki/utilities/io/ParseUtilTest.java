package de.lmu.ifi.dbs.elki.utilities.io;

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

/**
 * Test parser functionality.
 * 
 * @author Erich Schubert
 * @since 0.7.1
 */
public class ParseUtilTest implements JUnit4Test {
  @Test
  public void testParseDouble() {
    assertEquals(0., ParseUtil.parseDouble("0"), 0.);
    assertEquals(0., ParseUtil.parseDouble("0.0"), 0.);
    assertEquals(0., ParseUtil.parseDouble("0."), 0.);
    assertEquals(0., ParseUtil.parseDouble("0e10"), 0.);
    assertEquals(0., ParseUtil.parseDouble("0E10"), 0.);
    assertEquals(0., ParseUtil.parseDouble("0e-10"), 0.);
    assertEquals(0., ParseUtil.parseDouble("0E-10"), 0.);
    assertEquals(1., ParseUtil.parseDouble("1"), 0.);
    assertEquals(1., ParseUtil.parseDouble("1.0"), 0.);
    assertEquals(1., ParseUtil.parseDouble("1."), 0.);
    assertEquals(1., ParseUtil.parseDouble("1e0"), 0.);
    assertEquals(1., ParseUtil.parseDouble("1E0"), 0.);
    assertEquals(1., ParseUtil.parseDouble("1e-0"), 0.);
    assertEquals(1., ParseUtil.parseDouble("1E-0"), 0.);
    assertEquals(2., ParseUtil.parseDouble("2"), 0.);
    assertEquals(2., ParseUtil.parseDouble("2.0"), 0.);
    assertEquals(2., ParseUtil.parseDouble("2."), 0.);
    assertEquals(2., ParseUtil.parseDouble("2e0"), 0.);
    assertEquals(2., ParseUtil.parseDouble("2E0"), 0.);
    assertEquals(2., ParseUtil.parseDouble("2e-0"), 0.);
    assertEquals(2., ParseUtil.parseDouble("2E-0"), 0.);
    assertEquals(-1., ParseUtil.parseDouble("-1"), 0.);
    assertEquals(-1., ParseUtil.parseDouble("-1.0"), 0.);
    assertEquals(.2, ParseUtil.parseDouble("0.2"), 0.);
    assertEquals(-.2, ParseUtil.parseDouble("-0.2"), 0.);
    assertEquals(.2, ParseUtil.parseDouble(".2"), 0.);
    assertEquals(-.2, ParseUtil.parseDouble("-.2"), 0.);
    assertEquals(2000., ParseUtil.parseDouble("2.0e3"), 0.);
    assertEquals(2000., ParseUtil.parseDouble("2.0E3"), 0.);
    assertEquals(-2000., ParseUtil.parseDouble("-2.0e3"), 0.);
    assertEquals(-2000., ParseUtil.parseDouble("-2.0E3"), 0.);
    assertEquals(.002, ParseUtil.parseDouble("2.0e-3"), 0.);
    assertEquals(.002, ParseUtil.parseDouble("2.0E-3"), 0.);
    assertEquals(-.002, ParseUtil.parseDouble("-2.0e-3"), 0.);
    assertEquals(-.002, ParseUtil.parseDouble("-2.0E-3"), 0.);

    // Case where the JDK had a serious bug, in a few variations
    assertEquals(2.2250738585072012e-308, ParseUtil.parseDouble("2.2250738585072012e-308"), 0.);
    assertEquals(0.00022250738585072012e-304, ParseUtil.parseDouble("0.00022250738585072012e-304"), 0.);
    assertEquals(00000000002.2250738585072012e-308, ParseUtil.parseDouble("00000000002.2250738585072012e-308"), 0.);
    assertEquals(2.2250738585072012e-00308, ParseUtil.parseDouble("2.2250738585072012e-00308"), 0.);

    assertTrue(Double.POSITIVE_INFINITY == ParseUtil.parseDouble("inf"));
    assertTrue(Double.NEGATIVE_INFINITY == ParseUtil.parseDouble("-inf"));
    assertTrue(Double.POSITIVE_INFINITY == ParseUtil.parseDouble("∞"));
    assertTrue(Double.NEGATIVE_INFINITY == ParseUtil.parseDouble("-∞"));
    assertTrue(Double.isNaN(ParseUtil.parseDouble("nan")));

    assertEquals(1, ParseUtil.parseDouble("+1"), 0.);
  }

  @Test(expected = NumberFormatException.class)
  public void textOnlyPlus() {
    ParseUtil.parseDouble("+");
  }

  @Test(expected = NumberFormatException.class)
  public void textExtraCharacer() {
    ParseUtil.parseDouble("123Banana");
  }

  @Test(expected = NumberFormatException.class)
  public void textTooManyDigits() {
    ParseUtil.parseDouble("123456789012345678901234567890");
  }

  @Test(expected = NumberFormatException.class)
  public void textNoExponent() {
    ParseUtil.parseDouble("1e");
  }

  @Test(expected = NumberFormatException.class)
  public void textNoExponentMinus() {
    ParseUtil.parseDouble("1e-");
  }

  @Test(expected = NumberFormatException.class)
  public void textEmptyString() {
    ParseUtil.parseDouble("");
  }
}
