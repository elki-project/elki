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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Class for buffered IO, avoiding some of the overheads of the Java API.
 *
 * The main difference to the standard Java API is that this implementation will
 * reuse the buffer. <b>After a call to {@code nextLine()}, the buffer will be
 * overwitten!</b>
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class BufferedLineReader implements AutoCloseable {
  /**
   * Line reader.
   */
  private LineReader reader = null;

  /**
   * The buffer we read the data into.
   */
  protected StringBuilder buf = new StringBuilder(1024);

  /**
   * Current line number.
   */
  protected int lineNumber = 0;

  /**
   * Constructor. Use {@link #reset} to assign an input stream.
   */
  public BufferedLineReader() {
    super();
    this.reader = null;
  }

  /**
   * Constructor.
   *
   * @param in Line reader
   */
  public BufferedLineReader(LineReader in) {
    super();
    reader = in;
  }

  /**
   * Constructor.
   *
   * @param in Input stream
   */
  public BufferedLineReader(InputStream in) {
    this(new LineReader(new InputStreamReader(in)));
  }

  /**
   * Constructor.
   *
   * @param in Input stream reader
   */
  public BufferedLineReader(InputStreamReader in) {
    this(new LineReader(in));
  }

  /**
   * Reset the current reader (but do <em>not</em> close the stream).
   */
  public void reset() {
    reader = null;
    buf.setLength(0);
    lineNumber = 0;
  }

  /**
   * Reset to a new line reader.
   *
   * <b>A previous reader will not be closed automatically!</b>
   *
   * @param r New reader
   */
  public void reset(LineReader r) {
    reset();
    this.reader = r;
  }

  /**
   * Reset to a new line reader.
   *
   * <b>A previous stream will not be closed automatically!</b>
   *
   * @param in New input stream reader
   */
  public void reset(InputStream in) {
    reset();
    this.reader = new LineReader(in);
  }

  /**
   * Reset to a new line reader.
   *
   * <b>A previous stream will not be closed automatically!</b>
   *
   * @param in New input stream reader
   */
  public void reset(InputStreamReader in) {
    reset();
    this.reader = new LineReader(in);
  }

  /**
   * Get the reader buffer.
   *
   * <b>After a call to {@code nextLine()}, the buffer will be overwitten!</b>
   *
   * @return Buffer.
   */
  public CharSequence getBuffer() {
    return buf;
  }

  /**
   * Get the current line number.
   *
   * @return Current line number
   */
  public int getLineNumber() {
    return lineNumber;
  }

  /**
   * Read the next line.
   *
   * @return {@code true} if another line was read successfully.
   * @throws IOException on IO errors.
   */
  public boolean nextLine() throws IOException {
    while(reader.readLine(buf.delete(0, buf.length()))) {
      ++lineNumber;
      if(lengthWithoutLinefeed(buf) > 0) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void close() throws IOException {
    if(reader != null) {
      reader.close();
    }
    buf.setLength(0);
    buf.trimToSize();
  }

  /**
   * Get the length of the string, not taking trailing linefeeds into account.
   *
   * @param line Input line
   * @return Length
   */
  public static int lengthWithoutLinefeed(CharSequence line) {
    int length = line.length();
    while(length > 0) {
      char last = line.charAt(length - 1);
      if(last != '\n' && last != '\r') {
        break;
      }
      --length;
    }
    return length;
  }
}