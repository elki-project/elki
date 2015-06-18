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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import de.lmu.ifi.dbs.elki.datasource.parser.AbstractStreamingParser;

/**
 * Class for buffered IO, avoiding some of the overheads of the Java API.
 * 
 * The main difference to the standard Java API is that this implementation will
 * reuse the buffer. <b>After a call to {@code nextLine()}, the buffer will be
 * overwitten!</b>
 * 
 * @author Erich Schubert
 */
public class BufferedLineReader implements AutoCloseable {
  /**
   * Line reader.
   */
  private LineReader reader;

  /**
   * The buffer we read the data into.
   */
  private StringBuilder buf = new StringBuilder();

  /**
   * Current line number.
   */
  private int lineNumber = 0;

  /**
   * Constructor.
   *
   * @param in Input stream
   */
  public BufferedLineReader(InputStream in) {
    this(new InputStreamReader(in));
  }

  /**
   * Constructor.
   *
   * @param in Input stream reader
   */
  public BufferedLineReader(InputStreamReader in) {
    reader = new LineReader(in);
  }

  /**
   * Get the reader buffer.
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
      final int len = AbstractStreamingParser.lengthWithoutLinefeed(buf);
      if(len > 0) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void close() throws IOException {
    reader.close();
    buf.setLength(0);
    buf.trimToSize();
  }
}