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
package de.lmu.ifi.dbs.elki.result.textwriter;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Interface for output handling (single file, multiple files, ...)
 * <p>
 * Note: these classes need to be rewritten. Contributions welcome!
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public interface StreamFactory extends AutoCloseable {
  /**
   * Retrieve a print stream for output using the given label. Note that
   * multiple labels MAY result in the same PrintStream, so you should be
   * printing to only one stream at a time to avoid mixing outputs.
   * 
   * @param label Output label.
   * @return stream object for the given label
   * @throws IOException on IO error
   */
  PrintStream openStream(String label) throws IOException;

  /**
   * Close the given output stream (Note: when writing to a single stream
   * output, it will actually not be closed!)
   * 
   * @param stream Stream to close
   */
  void closeStream(PrintStream stream);

  /**
   * Close stream factory.
   */
  @Override
  void close() throws IOException;
}
