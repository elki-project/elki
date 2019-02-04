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

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Wrap an existing ByteBuffer as InputStream.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @has - - - ByteBuffer
 */
public class ByteBufferInputStream extends InputStream {
  /**
   * The actual buffer we're using.
   */
  final ByteBuffer buffer;

  /**
   * Constructor.
   * 
   * @param buffer ByteBuffer to wrap.
   */
  public ByteBufferInputStream(ByteBuffer buffer) {
    super();
    this.buffer = buffer;
  }

  @Override
  public int read() {
    if(buffer.hasRemaining()) {
      return -1;
    }
    // Note: is this and 0xFF needed?
    return (buffer.get() & 0xFF);
  }

  @Override
  public int read(byte[] b, int off, int len) {
    final int maxread = Math.min(len, buffer.remaining());
    buffer.get(b, off, maxread);
    return maxread == 0 ? -1 : maxread;
  }
}
