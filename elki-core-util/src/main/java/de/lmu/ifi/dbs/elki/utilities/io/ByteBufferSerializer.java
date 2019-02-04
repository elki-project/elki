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
import java.nio.ByteBuffer;

/**
 * Class to convert from and to byte arrays (in index structures).
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @assoc - "serializes to/from" - ByteBuffer
 * 
 * @param <T> Object type processed
 */
public interface ByteBufferSerializer<T> {
  /**
   * Deserialize an object from a byte buffer (e.g. disk)
   * 
   * @param buffer Data array to process
   * @return Deserialized object
   * @throws IOException on IO errors
   * @throws UnsupportedOperationException When functionality not implemented or
   *         available
   */
  T fromByteBuffer(ByteBuffer buffer) throws IOException, UnsupportedOperationException;

  /**
   * Serialize the object to a byte array (e.g. disk)
   * 
   * @param buffer Buffer to serialize to
   * @param object Object to serialize
   * @throws IOException on IO errors
   * @throws UnsupportedOperationException When functionality not implemented or
   *         available
   */
  void toByteBuffer(ByteBuffer buffer, T object) throws IOException, UnsupportedOperationException;

  /**
   * Get the size of the object in bytes.
   * 
   * @param object Object to serialize
   * @return maximum size in serialized form
   * @throws IOException on IO errors
   * @throws UnsupportedOperationException When functionality not implemented or
   *         available
   */
  int getByteSize(T object) throws IOException, UnsupportedOperationException;
}
