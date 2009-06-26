package de.lmu.ifi.dbs.elki.test.logging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;

import junit.framework.TestCase;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.logging.OutputStreamLogger;

/**
 * Small experiment to assert the console output logger works as expected.
 * 
 * @author Erich Schubert
 * 
 */
public class TestOutputStreamLogger extends TestCase {
  /**
   * Write a couple of messages to the console output writer and compare the
   * resulting characters.
   * 
   * @throws IOException
   */
  @Test
  public final void testWriteString() throws IOException {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    Writer wri = new OutputStreamLogger(buf);
    wri.write("Test." + OutputStreamLogger.NEWLINE);
    wri.write("\r123");
    wri.write("\r23");
    wri.write("\r3");
    wri.write("Test.");
    String should = "Test." + OutputStreamLogger.NEWLINE + "\r123\r   \r23\r  \r3" + OutputStreamLogger.NEWLINE + "Test.";
    assertEquals("Output doesn't match requirements.", should, buf.toString());
  }
}
