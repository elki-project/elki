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

import org.junit.Assert;
import org.junit.Test;

/**
 * Test some of the varint functions.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class ByteArrayUtilTest {
  /**
   * Test the Varint functions
   */
  @Test
  public void dotestUnsignedVarint32() throws IOException {
    int[] testvals = { 0, 1, 127, 128, 16383, 16384, 2097151, 2097152, 268435455, 268435456, Integer.MAX_VALUE, 0xFFFFFFFF };
    int[] elen = { 1, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 5 };
    ByteBuffer buffer = ByteBuffer.allocate(100);
    Assert.assertEquals("Test incorrectly set up.", testvals.length, elen.length);

    // Fill the buffer
    int totallen = 0;
    for(int i = 0; i < testvals.length; i++) {
      ByteArrayUtil.writeUnsignedVarint(buffer, testvals[i]);
      totallen += elen[i];
      Assert.assertEquals("len(Varint(" + testvals[i] + ")) != " + elen[i], totallen, buffer.position());
    }
    // Seek and read again.
    buffer.position(0);
    totallen = 0;
    for(int i = 0; i < testvals.length; i++) {
      int read = ByteArrayUtil.readUnsignedVarint(buffer);
      Assert.assertEquals("Varint read failed.", testvals[i], read);
      totallen += elen[i];
      Assert.assertEquals("len(Varint(" + testvals[i] + ")) != " + elen[i], totallen, buffer.position());
    }
  }

  /**
   * Test the Varint functions
   */
  @Test
  public void dotestSignedVarint32() throws IOException {
    int[] testvals = { 0, 1, -1, 63, -64, 64, -65, 8191, -8192, 8192, -8193, 1048575, -1048576, 1048576, -1048577, 134217727, -134217728, 134217728, -134217729, Integer.MAX_VALUE, Integer.MIN_VALUE };
    int[] elen = { 1, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5 };
    ByteBuffer buffer = ByteBuffer.allocate(100);
    Assert.assertEquals("Test incorrectly set up.", testvals.length, elen.length);

    // Fill the buffer
    int totallen = 0;
    for(int i = 0; i < testvals.length; i++) {
      ByteArrayUtil.writeSignedVarint(buffer, testvals[i]);
      totallen += elen[i];
      Assert.assertEquals("len(Varint(" + testvals[i] + ")) != " + elen[i], totallen, buffer.position());
    }
    // Seek and read again.
    buffer.position(0);
    totallen = 0;
    for(int i = 0; i < testvals.length; i++) {
      int read = ByteArrayUtil.readSignedVarint(buffer);
      Assert.assertEquals("Varint read failed.", testvals[i], read);
      totallen += elen[i];
      Assert.assertEquals("len(Varint(" + testvals[i] + ")) != " + elen[i], totallen, buffer.position());
    }
  }

  /**
   * Official examples
   */
  @Test
  public void dotestVarintExamples() throws IOException {
    byte[] test = { 0x03, (byte) 0x8E, 0x02, (byte) 0x9E, (byte) 0xA7, 0x05, (byte) 0x96, 0x01 };
    int[] expect = { 3, 270, 86942, 150 };
    ByteBuffer buffer = ByteBuffer.wrap(test);
    for(int i = 0; i < expect.length; i++) {
      int read = ByteArrayUtil.readUnsignedVarint(buffer);
      Assert.assertEquals(expect[i], read);
    }
    Assert.assertEquals("Not all data processed.", 0, buffer.remaining());
  }

}
