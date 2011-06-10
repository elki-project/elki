package de.lmu.ifi.dbs.elki.persistent;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;

/**
 * Test the on-disk OnDiskUpperTriangleMatrix class.
 * @author Erich Schubert
 *
 */
// TODO: also test with a static sample file.
public class TestOnDiskUpperTriangleMatrix implements JUnit4Test {
  static File file = new File("UpperTriangleTestFile.test.dat");

  /**
   * Check that we don't overwrite any file.
   * @throws Exception on errors.
   */
  @Before
  public void safetyCheck() throws Exception {
    if(file.exists()) {
      Assert.fail("Could not run test - test file already exists.");
    }
  }

  /**
   * Clean up afterwards
   * @throws Exception on errors.
   */
  @After
  public void cleanup() throws Exception {
    if(file != null && file.exists()) {
      if(!file.delete()) {
        Assert.fail("Error cleaning up: can't remove test file.");
      }
    }
  }

  /**
   * Test the ondisk triangle matrix
   * @throws IOException on errors.
   */
  @Test
  public void testUpperTriangleMatrix() throws IOException {
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
    Assert.assertEquals("File size doesn't match.", ODR_HEADER_SIZE + extraheadersize + recsize * matsize * (matsize + 1) / 2, file.length());

    OnDiskUpperTriangleMatrix roarray = new OnDiskUpperTriangleMatrix(file, 1, extraheadersize, recsize, false);
    Assert.assertEquals("Number of records incorrect.", matsize, roarray.getMatrixSize());
    
    byte[] buf = new byte[recsize];
    roarray.getRecordBuffer(0,0).get(buf);
    Assert.assertArrayEquals("Record 0,0 doesn't match.", record1, buf);
    roarray.getRecordBuffer(0,1).get(buf);
    Assert.assertArrayEquals("Record 0,1 doesn't match.", record2, buf);
    roarray.getRecordBuffer(1,1).get(buf);
    Assert.assertArrayEquals("Record 1,1 doesn't match.", record3, buf);
    roarray.getRecordBuffer(1,0).get(buf);
    Assert.assertArrayEquals("Record 1,0 doesn't match.", record2, buf);
    roarray.getRecordBuffer(0,2).get(buf);
    Assert.assertArrayEquals("Record 0,2 doesn't match.", record3, buf);
    roarray.getRecordBuffer(1,2).get(buf);
    Assert.assertArrayEquals("Record 1,2 doesn't match.", record2, buf);
    roarray.getRecordBuffer(2,2).get(buf);
    Assert.assertArrayEquals("Record 2,2 doesn't match.", record1, buf);
  }
}
