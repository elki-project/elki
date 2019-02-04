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
import java.io.Reader;

/**
 * Fast class to read a file, line per line.
 *
 * Lines must be split using Unix newlines <code>\n</code>, linefeeds
 * <code>\r</code> are ignored.
 *
 * This is a rather minimal implementation, which supposedly pays off in
 * performance. In particular, this class allows recycling the buffer, which
 * will yield less object allocations and thus less garbage collection.
 *
 * Usage example:
 *
 * <pre>
 * StringBuilder buf = new StringBuilder();
 * LineReader reader = new LineReader(inputStream);
 * // Clear buffer, and append next line.
 * while(reader.readLine(buf.setLength(0))) {
 *   // process string in buffer.
 * }
 * </pre>
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class LineReader implements AutoCloseable {
  /**
   * Default buffer size to use
   */
  final static int BUFFER_SIZE = 4096;

  /**
   * Input stream to read from
   */
  Reader in;

  /**
   * Character buffer
   */
  char[] buffer;

  /**
   * Current position, and length of buffer
   */
  int pos = 0, end = 0;

  /**
   * Constructor
   *
   * @param in Stream
   */
  public LineReader(InputStream in) {
    this(new InputStreamReader(in));
  }

  /**
   * Constructor
   *
   * @param in Reader
   */
  public LineReader(Reader in) {
    this(in, BUFFER_SIZE);
  }

  /**
   * Constructor
   *
   * @param in Reader
   * @param buffersize Buffer size
   */
  public LineReader(InputStream in, int buffersize) {
    this(new InputStreamReader(in), buffersize);
  }

  /**
   * Constructor
   *
   * @param in Reader
   * @param buffersize Buffer size
   */
  public LineReader(Reader in, int buffersize) {
    this.in = in;
    this.buffer = new char[buffersize];
  }

  /**
   * Read a line into the given buffer.
   *
   * @param buf Buffer.
   * @return {@code true} if some characters have been read.
   */
  public boolean readLine(Appendable buf) throws IOException {
    boolean success = false;
    while(true) {
      // Process buffer:
      while(pos < end) {
        success = true;
        final char c = buffer[pos++];
        if(c == '\n') {
          return success;
        }
        if(c == '\r') {
          continue;
        }
        buf.append(c);
      }
      // Refill buffer:
      assert (pos >= end) : "Buffer wasn't empty when refilling!";
      end = in.read(buffer, 0, buffer.length);
      pos = 0;
      if(end < 0) { // End of stream.
        return success;
      }
    }
  }

  @Override
  public void close() throws IOException {
    if(in != null) {
      in.close();
    }
  }
}
