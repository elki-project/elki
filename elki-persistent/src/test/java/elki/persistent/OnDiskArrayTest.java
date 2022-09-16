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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test to validate proper OnDiskArray operation.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
// TODO: also test with a static sample file.
public class OnDiskArrayTest {
  /**
   * File we are using.
   */
  Path file;

  /**
   * Set up the temp file for testing.
   */
  @Before
  public void setup() throws IOException {
    file = Files.createTempFile("ELKIUnitTest", null);
    file.toFile().deleteOnExit();
  }

  /**
   * Delete the file after the test.
   */
  @After
  public void cleanup() {
    System.gc(); // maybe helps unmap the file
    try {
      Files.delete(file); // Note: probably fails on Windows.
    }
    catch(IOException e) {
      // We cannot reliably delete mmaped files on Windows, apparently.
      elki.logging.LoggingUtil.exception(e);
    }
  }

  /**
   * Test the OnDiskArray class.
   *
   * @throws IOException on errors.
   */
  @Test
  public void dotestOnDiskArray() throws IOException {
    final int extraheadersize = 2;
    final int recsize = 3;
    int numrec = 4;
    // Only applicable to the version we are testing.
    final int ODR_HEADER_SIZE = 4 * 4;
    OnDiskArray array = new OnDiskArray(file, 1, extraheadersize, recsize, numrec);
    byte[] header = { 42, 23 };
    array.getExtraHeader().put(header);
    byte[] record1 = { 31, 41, 59 };
    byte[] record2 = { 26, 53, 58 };
    array.getRecordBuffer(0).put(record1);
    array.getRecordBuffer(1).put(record2);
    array.getRecordBuffer(2).put(record2);
    array.getRecordBuffer(3).put(record1);
    array.resizeFile(5);
    numrec = 5;
    array.getRecordBuffer(4).put(record1);
    array.close();

    // validate file size
    assertEquals("File size doesn't match.", ODR_HEADER_SIZE + extraheadersize + recsize * numrec, Files.size(file));

    OnDiskArray roarray = new OnDiskArray(file, 1, 2, 3, false);
    assertEquals("Number of records incorrect.", numrec, roarray.getNumRecords());

    byte[] buf = new byte[recsize];
    ByteBuffer hbuf = roarray.getExtraHeader();
    for(int i = 0; i < header.length; i++) {
      assertEquals("Header doesn't match.", header[i], hbuf.get());
    }
    roarray.getRecordBuffer(0).get(buf);
    assertArrayEquals("Record 0 doesn't match.", record1, buf);
    roarray.getRecordBuffer(4).get(buf);
    assertArrayEquals("Record 4 doesn't match.", record1, buf);
    roarray.getRecordBuffer(1).get(buf);
    assertArrayEquals("Record 1 doesn't match.", record2, buf);
    roarray.getRecordBuffer(2).get(buf);
    assertArrayEquals("Record 2 doesn't match.", record2, buf);
    roarray.getRecordBuffer(3).get(buf);
    assertArrayEquals("Record 3 doesn't match.", record1, buf);
    roarray.close();
  }
}
