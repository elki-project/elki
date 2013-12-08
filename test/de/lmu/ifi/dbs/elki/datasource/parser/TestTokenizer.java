package de.lmu.ifi.dbs.elki.datasource.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.regex.Pattern;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;

/**
 * Simple unit test for testing the new tokenizer
 * 
 * @author Erich Schubert
 */
public class TestTokenizer implements JUnit4Test {
  @Test
  public void simpleLines() {
    Tokenizer t = new Tokenizer(Pattern.compile("\\s"), '"');
    // A simple line test.
    t.initialize("1 234 3.1415 banana\n");
    assertTrue(t.valid());
    assertEquals("Expected value does not match.", 1L, t.getLongBase10());
    t.advance();
    assertTrue(t.valid());
    assertEquals("Expected value does not match.", 234L, t.getLongBase10());
    t.advance();
    assertTrue(t.valid());
    try {
      t.getLongBase10();
      fail("The value is a double, but not a long.");
    }
    catch(Exception e) {
      // pass.
    }
    assertEquals("Expected value does not match.", 3.1415, t.getDouble(), 1e-15);
    t.advance();
    assertTrue(t.valid());
    try {
      t.getDouble();
      fail("The value is a string, but not a long.");
    }
    catch(Exception e) {
      // pass.
    }
    try {
      t.getLongBase10();
      fail("The value is a string, but not a long.");
    }
    catch(Exception e) {
      // pass.
    }
    assertEquals("Expected value does not match.", "banana", t.getSubstring());
    t.advance();
    assertTrue(!t.valid());
  }
}
