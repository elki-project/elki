/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2025
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
package elki.result;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Helper class to write a Numpy array to a file.
 * 
 * @author Andreas Lang
 */
public abstract class NumpyDumper {

  /**
   * Extension for numpy files
   */
  String extension = "npy";
  
  /**
   * Writes a NumPy array to a specified file, including the header information.
   *
   * @param file The FileChannel representing the file where the data will be
   *        written.
   * @param header A map containing key-value pairs for the metadata header of
   *        the NumPy array.
   *        This typically includes dtype(descr), fortran_order and shape.
   * @param array The ByteBuffer containing the actual data of the NumPy array.
   *        This buffer should be ordered with the same order as described in
   *        descr.
   *
   * @throws IOException If an I/O error occurs during writing the file.
   */
  public void writeNumpyArray(FileChannel file, Map<String,String> header, ByteBuffer array) throws IOException {
    ByteBuffer headerBuf = Charset.forName("UTF-8").encode(toString(header));
    int headerSize = headerBuf.limit();

    MappedByteBuffer buffer = file.map(MapMode.READ_WRITE, 0,12);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.put((byte) 0x93);
    buffer.put((byte) 'N');
    buffer.put((byte) 'U');
    buffer.put((byte) 'M');
    buffer.put((byte) 'P');
    buffer.put((byte) 'Y');

    // write version and header size
    buffer.put((byte) 0x3); // major version
    buffer.put((byte) 0x0); // minor version
    buffer.putInt(headerSize);

    // write Header
    buffer = file.map(MapMode.READ_WRITE, 12, headerSize);
    buffer.put(headerBuf);

    // write Array
    buffer = file.map(MapMode.READ_WRITE, 12 + headerSize, array.limit());
    buffer.put(array);
  }

  /**
   * Converts a map of header information to its string representation.
   *
   * @param header A map containing key-value pairs for the metadata header.
   *               This typically includes dtype(descr), fortran_order and shape.
   * @return The string representation of the header map.
   */
  public String toString(Map<String, String> header) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    for (Entry<String,String> entry : header.entrySet()){
      sb.append("'" + entry.getKey() + "'");
      sb.append(": ");
      sb.append(entry.getValue());
      sb.append(", ");
    }
    sb.append("}");
    return sb.toString();
  }
}
