/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.utilities.io;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

/**
 * Test parser functionality.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class ParseUtilTest {
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

    assertEquals(Double.POSITIVE_INFINITY, ParseUtil.parseDouble("inf"), 0.);
    assertEquals(Double.NEGATIVE_INFINITY, ParseUtil.parseDouble("-inf"), 0.);
    assertEquals(Double.POSITIVE_INFINITY, ParseUtil.parseDouble("INF"), 0.);
    assertEquals(Double.NEGATIVE_INFINITY, ParseUtil.parseDouble("-INF"), 0.);
    assertEquals(Double.POSITIVE_INFINITY, ParseUtil.parseDouble("infINITY"), 0.);
    assertEquals(Double.NEGATIVE_INFINITY, ParseUtil.parseDouble("-infINITY"), 0.);
    assertEquals(Double.POSITIVE_INFINITY, ParseUtil.parseDouble("\u221E"), 0.);
    assertEquals(Double.NEGATIVE_INFINITY, ParseUtil.parseDouble("-\u221E"), 0.);
    assertTrue(Double.isNaN(ParseUtil.parseDouble("nan")));
    assertTrue(Double.isNaN(ParseUtil.parseDouble("NaN")));
    assertTrue(Double.isNaN(ParseUtil.parseDouble("NA")));

    assertEquals(1, ParseUtil.parseDouble("+1"), 0.);
  }

  @Test
  public void testBytes() throws IOException {
    assertEquals(0., parseBytes("0"), 0.);
    assertEquals(0., parseBytes("0.0"), 0.);
    assertEquals(0., parseBytes("0."), 0.);
    assertEquals(0., parseBytes("0e10"), 0.);
    assertEquals(0., parseBytes("0E10"), 0.);
    assertEquals(0., parseBytes("0e-10"), 0.);
    assertEquals(0., parseBytes("0E-10"), 0.);
    assertEquals(1., parseBytes("1"), 0.);
    assertEquals(1., parseBytes("1.0"), 0.);
    assertEquals(1., parseBytes("1."), 0.);
    assertEquals(1., parseBytes("1e0"), 0.);
    assertEquals(1., parseBytes("1E0"), 0.);
    assertEquals(1., parseBytes("1e-0"), 0.);
    assertEquals(1., parseBytes("1E-0"), 0.);
    assertEquals(2., parseBytes("2"), 0.);
    assertEquals(2., parseBytes("2.0"), 0.);
    assertEquals(2., parseBytes("2."), 0.);
    assertEquals(2., parseBytes("2e0"), 0.);
    assertEquals(2., parseBytes("2E0"), 0.);
    assertEquals(2., parseBytes("2e-0"), 0.);
    assertEquals(2., parseBytes("2E-0"), 0.);
    assertEquals(-1., parseBytes("-1"), 0.);
    assertEquals(-1., parseBytes("-1.0"), 0.);
    assertEquals(.2, parseBytes("0.2"), 0.);
    assertEquals(-.2, parseBytes("-0.2"), 0.);
    assertEquals(.2, parseBytes(".2"), 0.);
    assertEquals(-.2, parseBytes("-.2"), 0.);
    assertEquals(2000., parseBytes("2.0e3"), 0.);
    assertEquals(2000., parseBytes("2.0E3"), 0.);
    assertEquals(-2000., parseBytes("-2.0e3"), 0.);
    assertEquals(-2000., parseBytes("-2.0E3"), 0.);
    assertEquals(.002, parseBytes("2.0e-3"), 0.);
    assertEquals(.002, parseBytes("2.0E-3"), 0.);
    assertEquals(-.002, parseBytes("-2.0e-3"), 0.);
    assertEquals(-.002, parseBytes("-2.0E-3"), 0.);

    // Case where the JDK had a serious bug, in a few variations
    assertEquals(2.2250738585072012e-308, parseBytes("2.2250738585072012e-308"), 0.);
    assertEquals(0.00022250738585072012e-304, parseBytes("0.00022250738585072012e-304"), 0.);
    assertEquals(00000000002.2250738585072012e-308, parseBytes("00000000002.2250738585072012e-308"), 0.);
    assertEquals(2.2250738585072012e-00308, parseBytes("2.2250738585072012e-00308"), 0.);

    assertEquals(Double.POSITIVE_INFINITY, parseBytes("inf"), 0.);
    assertEquals(Double.NEGATIVE_INFINITY, parseBytes("-inf"), 0.);
    assertEquals(Double.POSITIVE_INFINITY, parseBytes("INF"), 0.);
    assertEquals(Double.NEGATIVE_INFINITY, parseBytes("-INF"), 0.);
    assertEquals(Double.POSITIVE_INFINITY, parseBytes("infINITY"), 0.);
    assertEquals(Double.NEGATIVE_INFINITY, parseBytes("-infINITY"), 0.);
    assertEquals(Double.POSITIVE_INFINITY, parseBytes("\u221E"), 0.);
    assertEquals(Double.NEGATIVE_INFINITY, parseBytes("-\u221E"), 0.);
    assertTrue(Double.isNaN(parseBytes("nan")));
    assertTrue(Double.isNaN(parseBytes("NaN")));
    assertTrue(Double.isNaN(parseBytes("NA")));

    assertEquals(1, parseBytes("+1"), 0.);
  }

  private static double parseBytes(String string) throws IOException {
    byte[] bytes = string.getBytes("UTF-8");
    return ParseUtil.parseDouble(bytes, 0, bytes.length);
  }

  static String[] BAD_FLOATS = {
      // Incomplete:
      "", "+", "+.", "-", "-.", "1..1", "1e", "1e-", "-.e0", "1e 1", "-1e 1",
      // bad letters
      "A", "1A", "1eA", "1e01A", "-A", "+A", "-.A", "infAnity", "Nana", "non", "naX",
      // value range
      "9223372036854775808", "1e1024" };

  @Test
  public void testExceptions() {
    for(String bad : BAD_FLOATS) {
      try {
        ParseUtil.parseDouble(bad);
        fail("No exception on '" + bad + "'");
      }
      catch(NumberFormatException e) {
        // Good
      }
    }
  }

  @Test
  public void testExceptionsBytes() throws IOException {
    for(String bad : BAD_FLOATS) {
      try {
        parseBytes(bad);
        fail("No exception on '" + bad + "'");
      }
      catch(NumberFormatException e) {
        // Good
      }
    }
  }

  @Test
  public void testInteger() {
    assertEquals(0, ParseUtil.parseIntBase10("0"));
    assertEquals(42, ParseUtil.parseIntBase10("42"));
    assertEquals(-31415, ParseUtil.parseIntBase10("-31415"));
    assertEquals(Integer.MAX_VALUE, ParseUtil.parseIntBase10(Integer.toString(Integer.MAX_VALUE)));
    assertEquals(Integer.MAX_VALUE - 1, ParseUtil.parseIntBase10(Integer.toString(Integer.MAX_VALUE - 1)));
    assertEquals(Integer.MIN_VALUE + 1, ParseUtil.parseIntBase10(Integer.toString(Integer.MIN_VALUE + 1)));
    assertEquals(Integer.MIN_VALUE, ParseUtil.parseIntBase10(Integer.toString(Integer.MIN_VALUE)));
  }

  @Test
  public void testLong() {
    assertEquals(0L, ParseUtil.parseLongBase10("0"));
    assertEquals(42L, ParseUtil.parseLongBase10("42"));
    assertEquals(-31415L, ParseUtil.parseLongBase10("-31415"));
    assertEquals(Long.MAX_VALUE, ParseUtil.parseLongBase10(Long.toString(Long.MAX_VALUE)));
    assertEquals(Long.MAX_VALUE - 1, ParseUtil.parseLongBase10(Long.toString(Long.MAX_VALUE - 1)));
    assertEquals(Long.MIN_VALUE + 1, ParseUtil.parseLongBase10(Long.toString(Long.MIN_VALUE + 1)));
    assertEquals(Long.MIN_VALUE, ParseUtil.parseLongBase10(Long.toString(Long.MIN_VALUE)));
  }

  static String[] BAD_INTEGERS = {
      // Incomplete:
      "", "+", "-", "1.0", "1e0",
      // bad letters
      "A", "1A", "-A", "+A", "inf", "nan",
      // value range
      "9223372036854775808", "-9223372036854775809" };

  @Test
  public void testBadIntegers() {
    for(String bad : BAD_INTEGERS) {
      try {
        ParseUtil.parseIntBase10(bad);
        fail("No exception on '" + bad + "'");
      }
      catch(NumberFormatException e) {
        // Good
      }
    }
    try {
      ParseUtil.parseIntBase10(Integer.toString(Integer.MIN_VALUE).substring(1));
      fail("No exception on '" + Integer.toString(Integer.MIN_VALUE).substring(1) + "'");
    }
    catch(NumberFormatException e) {
      // Good
    }
    try {
      ParseUtil.parseIntBase10(Long.toString(Integer.MIN_VALUE - 1L));
      fail("No exception on '" + Long.toString(Integer.MIN_VALUE - 1L) + "'");
    }
    catch(NumberFormatException e) {
      // Good
    }
  }

  @Test
  public void testBadLongs() {
    for(String bad : BAD_INTEGERS) {
      try {
        ParseUtil.parseLongBase10(bad);
        fail("No exception on '" + bad + "'");
      }
      catch(NumberFormatException e) {
        // Good
      }
    }
    try {
      ParseUtil.parseLongBase10(Long.toString(Long.MIN_VALUE).substring(1));
      fail("No exception on '" + Long.toString(Long.MIN_VALUE).substring(1) + "'");
    }
    catch(NumberFormatException e) {
      // Good
    }
  }
}
