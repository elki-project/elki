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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.regex.Pattern;

import org.junit.Test;

/**
 * Simple unit test for testing the new tokenizer
 * 
 * TODO: add more test cases, refactor into input, expected-output pattern.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class TokenizerTest {
  Tokenizer t = new Tokenizer(Pattern.compile("\\s"), "\"'");

  @Test
  public void testSimple() {
    final String input = "1 -234 3.1415 - banana";
    final Object[] expect = { 1L, -234L, 3.1415, "-", "banana" };
    t.initialize(input, 0, input.length());
    tokenizerTest(expect);
  }

  @Test
  public void testQuotes() {
    final String input = "'this is' \"a test\" '123' '123 456' \"bana' na\"";
    final Object[] expect = { "this is", "a test", 123L, "123 456", "bana' na" };
    t.initialize(input, 0, input.length());
    tokenizerTest(expect);
  }

  @Test
  public void testSpecials() {
    final String input = "nan inf -∞ NaN infinity NA";
    final Object[] expect = { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY, Double.NaN };
    t.initialize(input, 0, input.length());
    tokenizerTest(expect);
  }

  @Test
  public void testEmpty() {
    final String input = "";
    final Object[] expect = {};
    t.initialize(input, 0, input.length());
    tokenizerTest(expect);
  }

  @Test
  public void testLineEnd() {
    final String input = "1 ";
    final Object[] expect = { 1L };
    t.initialize(input, 0, input.length());
    tokenizerTest(expect);
  }

  @Test
  public void testPartial() {
    final String input = "abc1def";
    final Object[] expect = { 1L };
    t.initialize(input, 3, 4);
    tokenizerTest(expect);
  }

  private void tokenizerTest(Object[] expect) {
    for(int i = 0; i < expect.length; i++, t.advance()) {
      assertTrue("Tokenizer stopped early.", t.valid());
      Object e = expect[i];
      // Negative tests first:
      if(e instanceof String || e instanceof Double) {
        try {
          long val = t.getLongBase10();
          fail("The value " + t.getSubstring() + " was expected to be not parseable as long integer, but returned: " + val);
        }
        catch(NumberFormatException ex) {
          // pass. this is expected to fail.
        }
      }
      if(e instanceof String) {
        try {
          double val = t.getDouble();
          fail("The value " + t.getSubstring() + " was expected to be not parseable as double, but returned: " + val);
        }
        catch(NumberFormatException ex) {
          // pass. this is expected to fail.
        }
      }
      // Positive tests:
      if(e instanceof Long) {
        assertEquals("Long parsing failed.", (long) e, t.getLongBase10());
      }
      if(e instanceof Double) {
        // Note: this also works for NaNs, they are treated special.
        assertEquals("Double parsing failed.", (double) e, t.getDouble(), Double.MIN_VALUE);
      }
      if(e instanceof String) {
        assertEquals("String parsing failed.", (String) e, t.getSubstring());
      }
    }
    if(t.valid()) {
      assertFalse("Spurous data after expected end: " + t.getSubstring(), t.valid());
    }
  }
}
