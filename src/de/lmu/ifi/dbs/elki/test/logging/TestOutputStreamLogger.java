package de.lmu.ifi.dbs.elki.test.logging;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.logging.OutputStreamLogger;

public class TestOutputStreamLogger {
  @Test
  public final void testWriteString() throws IOException {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    Writer wri = new OutputStreamLogger(buf);
    wri.write("Test."+OutputStreamLogger.NEWLINE);
    wri.write("\r1");
    wri.write("\r2");
    wri.write("\r3");
    wri.write("Test.");
    String should = "Test."+OutputStreamLogger.NEWLINE+"\r1\r2\r3"+OutputStreamLogger.NEWLINE+"Test.";
    assertEquals("Output doesn't match requirements.", should, buf.toString());
  }
}
