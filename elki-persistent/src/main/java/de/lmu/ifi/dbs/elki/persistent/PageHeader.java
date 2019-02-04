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
package de.lmu.ifi.dbs.elki.persistent;

import java.io.IOException;
import java.io.RandomAccessFile;

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
   * Initializes this header from the specified file.
   * 
   * @param file the file to which this header belongs
   * @throws IOException if an I/O-error occurs during reading
   */
  void readHeader(RandomAccessFile file) throws IOException;

  /**
   * Initializes this header from the specified file.
   * 
   * @param data byte array with the page data.
   */
  void readHeader(byte[] data);

  /**
   * Writes this header to the specified file.
   * 
   * @param file the file to which this header belongs
   * @throws IOException IOException if an I/O-error occurs during writing
   */
  void writeHeader(RandomAccessFile file) throws IOException;

  /**
   * Return the header as byte array
   * 
   * @return header as byte array
   */
  byte[] asByteArray();

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
