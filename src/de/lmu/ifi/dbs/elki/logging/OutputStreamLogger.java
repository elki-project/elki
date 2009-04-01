/**
 * 
 */
package de.lmu.ifi.dbs.elki.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

/**
 * Class to write to Output Streams, IGNORING {@link #close()}, with a special
 * newline handling and always flushing.
 * 
 * This is meant to wrap logging output to the console.
 * 
 * @author Erich Schubert
 */
public class OutputStreamLogger extends OutputStreamWriter {
  /**
   * Flag to signal if we have had a newline recently.
   */
  private boolean hadNewline = true;

  /**
   * Carriage return character.
   */
  public static final char CARRIAGE_RETURN = '\r';

  /**
   * Unix newline
   */
  public static final char UNIX_NEWLINE = '\n';

  /**
   * Newline string.
   */
  public static final String NEWLINE = System.getProperty("line.separator");

  /**
   * Newline as char array.
   */
  public static final char[] NEWLINEC = NEWLINE.toCharArray();

  /**
   * Constructor.
   * 
   * @param out Output stream
   */
  public OutputStreamLogger(OutputStream out) {
    super(out);
  }

  /**
   * Constructor.
   * 
   * @param out Output stream
   * @param charsetName Character set name
   * @throws UnsupportedEncodingException thrown on unknown character sets
   */
  public OutputStreamLogger(OutputStream out, String charsetName) throws UnsupportedEncodingException {
    super(out, charsetName);
  }

  /**
   * Constructor.
   * 
   * @param out Output Stream
   * @param cs Character set to use
   */
  public OutputStreamLogger(OutputStream out, Charset cs) {
    super(out, cs);
  }

  /**
   * Constructor.
   * 
   * @param out Output Stream
   * @param enc Charset encoder
   */
  public OutputStreamLogger(OutputStream out, CharsetEncoder enc) {
    super(out, enc);
  }

  /**
   * Close command - will be IGNORED.
   */
  @Override
  public void close() {
    // IGNORE any close command.
  }

  /**
   * Compare two char array ranges for identical characters.
   * 
   * @param a1 First array
   * @param off1 Offset in array
   * @param a2 Second array
   * @param off2 Offset in array
   * @param len Length to compare.
   * @return
   */
  private static final boolean compareArray(char[] a1, int off1, char[] a2, int off2, int len) {
    for(int i = 0; i < len; i++) {
      if(a1[off1 + i] != a2[off2 + i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Compare a string and a char array ranges for identical characters.
   * 
   * @param a1 First String
   * @param off1 Offset in array
   * @param a2 Second array
   * @param off2 Offset in array
   * @param len Length to compare.
   * @return
   */
  private static final boolean compareString(String a1, int off1, char[] a2, int off2, int len) {
    for(int i = 0; i < len; i++) {
      if(a1.charAt(off1 + i) != a2[off2 + i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Writer that keeps track of when it hasn't seen a newline yet, will auto-insert newlines
   * except when lines start with a carriage return.
   */
  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    if(len <= 0) {
      return;
    }
    // if we havn't last seen a newline, and don't get a CR, insert a newline.
    if(!hadNewline && cbuf[off] != CARRIAGE_RETURN) {
      super.write(NEWLINEC, 0, NEWLINEC.length);
    }
    // check if we write a trailing newline.
    if(len >= NEWLINEC.length && compareArray(cbuf, off + len - NEWLINEC.length, NEWLINEC, 0, NEWLINEC.length)) {
      hadNewline = true;
    }
    else if(cbuf[off + len - 1] == UNIX_NEWLINE) {
      hadNewline = true;
    }
    else {
      hadNewline = false;
    }
    super.write(cbuf, off, len);
    flush();
  }

  /**
   * Writer that keeps track of when it hasn't seen a newline yet, will auto-insert newlines
   * except when lines start with a carriage return.
   */
  @Override
  public void write(String str, int off, int len) throws IOException {
    if(len <= 0) {
      return;
    }
    // if we havn't last seen a newline, and don't get a CR, insert a newline.
    if(!hadNewline && str.charAt(off) != CARRIAGE_RETURN) {
      super.write(NEWLINEC, 0, NEWLINEC.length);
    }
    // check if we write a trailing newline.
    if(len >= NEWLINEC.length && compareString(str, off + len - NEWLINEC.length, NEWLINEC, 0, NEWLINEC.length)) {
      hadNewline = true;
    }
    else if(str.charAt(off + len - 1) == UNIX_NEWLINE) {
      hadNewline = true;
    }
    else {
      hadNewline = false;
    }
    super.write(str, off, len);
    flush();
  }
}
