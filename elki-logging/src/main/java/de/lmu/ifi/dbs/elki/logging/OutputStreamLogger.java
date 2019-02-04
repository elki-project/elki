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
 * @since 0.2
 */
public class OutputStreamLogger extends OutputStreamWriter {
  /**
   * Flag to signal if we have had a newline recently.
   */
  private int charsSinceNewline = 0;

  /**
   * Carriage return character.
   */
  protected static final char CARRIAGE_RETURN = '\r';

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
   * Whitespace.
   */
  public static final String WHITESPACE = "                                                                                ";

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
   * Count the tailing non-newline characters.
   * 
   * @param cbuf Character buffer
   * @param off Offset
   * @param len Range
   * @return number of tailing non-newline character
   */
  private int tailingNonNewline(char[] cbuf, int off, int len) {
    for(int cnt = 0; cnt < len; cnt++) {
      final int pos = off + (len - 1) - cnt;
      if(cbuf[pos] == UNIX_NEWLINE) {
        return cnt;
      }
      if(cbuf[pos] == CARRIAGE_RETURN) {
        return cnt;
      }
      // TODO: need to compare to NEWLINEC, too?
    }
    return len;
  }

  /**
   * Count the tailing non-newline characters.
   * 
   * @param str String
   * @param off Offset
   * @param len Range
   * @return number of tailing non-newline character
   */
  private int tailingNonNewline(String str, int off, int len) {
    for(int cnt = 0; cnt < len; cnt++) {
      final int pos = off + (len - 1) - cnt;
      if(str.charAt(pos) == UNIX_NEWLINE) {
        return cnt;
      }
      if(str.charAt(pos) == CARRIAGE_RETURN) {
        return cnt;
      }
      // TODO: need to compare to NEWLINE, too?
    }
    return len;
  }

  /**
   * Count the number of non-newline characters before first newline in the string.
   * 
   * @param cbuf character buffer
   * @param off offset
   * @param len length
   * @return number of non-newline characters
   */
  private int countNonNewline(char[] cbuf, int off, int len) {
    for(int cnt = 0; cnt < len; cnt++) {
      final int pos = off + cnt;
      if(cbuf[pos] == UNIX_NEWLINE) {
        return cnt;
      }
      if(cbuf[pos] == CARRIAGE_RETURN) {
        return cnt;
      }
    }
    return len;
  }

  /**
   * Count the number of non-newline characters before first newline in the string.
   * 
   * @param str String
   * @param off offset
   * @param len length
   * @return number of non-newline characters
   */
  private int countNonNewline(String str, int off, int len) {
    for(int cnt = 0; cnt < len; cnt++) {
      final int pos = off + cnt;
      if(str.charAt(pos) == UNIX_NEWLINE) {
        return cnt;
      }
      if(str.charAt(pos) == CARRIAGE_RETURN) {
        return cnt;
      }
    }
    return len;
  }

  /**
   * Writer that keeps track of when it hasn't seen a newline yet, will
   * auto-insert newlines except when lines start with a carriage return.
   */
  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    if(len <= 0) {
      return;
    }
    // if we havn't last seen a newline, and don't get a CR, insert a newline.
    if(charsSinceNewline > 0) {
      if(cbuf[off] != CARRIAGE_RETURN) {
        super.write(NEWLINEC, 0, NEWLINEC.length);
        charsSinceNewline = 0;
      }
      else {
        // length of this line:
        int nonnl = countNonNewline(cbuf, off + 1, len - 1);
        // clear the existing chars.
        if(nonnl < charsSinceNewline) {
          super.write(CARRIAGE_RETURN);
          while(charsSinceNewline > 0) {
            final int n = Math.min(charsSinceNewline, WHITESPACE.length());
            super.write(WHITESPACE, 0, n);
            charsSinceNewline -= n;
          }
        }
        else {
          charsSinceNewline = 0;
        }
      }
    }
    charsSinceNewline = tailingNonNewline(cbuf, off, len);
    super.write(cbuf, off, len);
    flush();
  }

  /**
   * Writer that keeps track of when it hasn't seen a newline yet, will
   * auto-insert newlines except when lines start with a carriage return.
   */
  @Override
  public void write(String str, int off, int len) throws IOException {
    if(len <= 0) {
      return;
    }
    // if we havn't last seen a newline, and don't get a CR, insert a newline.
    if(charsSinceNewline > 0) {
      if(str.charAt(off) != CARRIAGE_RETURN) {
        super.write(NEWLINEC, 0, NEWLINEC.length);
        charsSinceNewline = 0;
      }
      else {
        // length of this line:
        int nonnl = countNonNewline(str, off + 1, len - 1);
        // clear the existing chars.
        if(nonnl < charsSinceNewline) {
          super.write(CARRIAGE_RETURN);
          while(charsSinceNewline > 0) {
            final int n = Math.min(charsSinceNewline, WHITESPACE.length());
            super.write(WHITESPACE, 0, n);
            charsSinceNewline -= n;
          }
        }
        else {
          charsSinceNewline = 0;
        }
      }
    }
    charsSinceNewline = tailingNonNewline(str, off, len);
    super.write(str, off, len);
    flush();
  }
}