/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.persistent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

/**
 * Defines the requirements for a header of a persistent page file. A header
 * must at least store the size of a page in Bytes.
 *
 * @author Elke Achtert
 * @since 0.1
 */
public interface PageHeader {
  /**
   * Returns the size of this header in Bytes.
   *
   * @return the size of this header in Bytes
   */
  int size();

  /**
   * Read the header from an input file.
   *
   * @param file File to read from
   * @throws IOException
   */
  default void readHeader(FileChannel file) throws IOException {
    readHeader(file.map(MapMode.READ_ONLY, 0, size()));
  }

  /**
   * Initializes this header from the specified file.
   *
   * @param data byte array with the page data.
   */
  void readHeader(ByteBuffer data);

  /**
   * Read the header from an input file.
   *
   * @param file File to read from
   * @throws IOException
   */
  default void writeHeader(FileChannel file) throws IOException {
    writeHeader(file.map(MapMode.READ_WRITE, 0, size()));
  }

  /**
   * Writes this header to the specified file.
   *
   * @param buffer Buffer to write to
   * @throws IOException IOException if an I/O-error occurs during writing
   */
  void writeHeader(ByteBuffer buffer) throws IOException;

  /**
   * Returns the size of a page in Bytes.
   *
   * @return the size of a page in Bytes
   */
  int getPageSize();

  /**
   * Returns the number of pages necessary for the header
   *
   * @return the number of pages
   */
  int getReservedPages();
}
