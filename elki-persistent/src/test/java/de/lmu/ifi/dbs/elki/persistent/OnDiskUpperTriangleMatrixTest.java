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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

/**
 * Test the on-disk OnDiskUpperTriangleMatrix class.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
// TODO: also test with a static sample file.
public class OnDiskUpperTriangleMatrixTest {
  /**
   * Test the ondisk triangle matrix
   *
   * @throws IOException on errors.
   */
  @Test
  public void testUpperTriangleMatrix() throws IOException {
    File file = File.createTempFile("ELKIUnitTest", null);
    file.deleteOnExit();

    final int extraheadersize = 2;
    final int recsize = 3;
    int matsize = 2;
    // Only applicable to the version we are testing.
    final int ODR_HEADER_SIZE = 4 * 4 + 4;
    OnDiskUpperTriangleMatrix array = new OnDiskUpperTriangleMatrix(file, 1, extraheadersize, recsize, matsize);
    byte[] record1 = { 31, 41, 59 };
    byte[] record2 = { 26, 53, 58 };
    byte[] record3 = { 97, 93, 1 };
    array.getRecordBuffer(0, 0).put(record1);
    array.getRecordBuffer(0, 1).put(record2);
    array.getRecordBuffer(1, 1).put(record3);
    // test resizing.
    matsize = 3;
    array.resizeMatrix(3);
    array.getRecordBuffer(0, 2).put(record3);
    array.getRecordBuffer(1, 2).put(record2);
    array.getRecordBuffer(2, 2).put(record1);
    array.close();

    // validate file size
    assertEquals("File size doesn't match.", ODR_HEADER_SIZE + extraheadersize + recsize * matsize * (matsize + 1) / 2, file.length());

    OnDiskUpperTriangleMatrix roarray = new OnDiskUpperTriangleMatrix(file, 1, extraheadersize, recsize, false);
    assertEquals("Number of records incorrect.", matsize, roarray.getMatrixSize());

    byte[] buf = new byte[recsize];
    roarray.getRecordBuffer(0, 0).get(buf);
    assertArrayEquals("Record 0,0 doesn't match.", record1, buf);
    roarray.getRecordBuffer(0, 1).get(buf);
    assertArrayEquals("Record 0,1 doesn't match.", record2, buf);
    roarray.getRecordBuffer(1, 1).get(buf);
    assertArrayEquals("Record 1,1 doesn't match.", record3, buf);
    roarray.getRecordBuffer(1, 0).get(buf);
    assertArrayEquals("Record 1,0 doesn't match.", record2, buf);
    roarray.getRecordBuffer(0, 2).get(buf);
    assertArrayEquals("Record 0,2 doesn't match.", record3, buf);
    roarray.getRecordBuffer(1, 2).get(buf);
    assertArrayEquals("Record 1,2 doesn't match.", record2, buf);
    roarray.getRecordBuffer(2, 2).get(buf);
    assertArrayEquals("Record 2,2 doesn't match.", record1, buf);
    roarray.close();

    file.delete(); // Note: probably fails on Windows.
    // We cannot reliably delete mmaped files on Windows, apparently.
  }
}
