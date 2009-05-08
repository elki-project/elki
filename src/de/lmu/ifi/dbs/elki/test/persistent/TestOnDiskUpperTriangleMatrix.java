package de.lmu.ifi.dbs.elki.test.persistent;

import java.io.File;
import java.io.IOException;

import org.junit.*;

import de.lmu.ifi.dbs.elki.persistent.OnDiskUpperTriangleMatrix;

/**
 * Test the on-disk OnDiskUpperTriangleMatrix class.
 * @author Erich Schubert
 *
 */
// TODO: also test with a static sample file.
public class TestOnDiskUpperTriangleMatrix {
  File file = new File("UpperTriangleTestFile.test.dat");

  /**
   * Check that we don't overwrite any file.
   */
  @Before
  public void checkTestFile() {
    if(file.exists()) {
      Assert.fail("Could not run test - test file already exists.");
    }
  }

  /**
   * Clean up afterwards
   */
  @After
  public void removeTestFile() {
    if(file.exists()) {
      if(!file.delete()) {
        Assert.fail("Error cleaning up: can't remove test file.");
      }
    }
  }

  /**
   * Test the ondisk triangle matrix
   * @throws IOException
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
    array.writeRecord(0, 0, record1);
    array.writeRecord(0, 1, record2);
    array.writeRecord(1, 1, record3);
    // test resizing.
    matsize = 3;
    array.resizeMatrix(3);
    array.writeRecord(0, 2, record3);
    array.writeRecord(1, 2, record2);
    array.writeRecord(2, 2, record1);
    array.close();

    // validate file size
    Assert.assertEquals("File size doesn't match.", ODR_HEADER_SIZE + extraheadersize + recsize * matsize * (matsize + 1) / 2, file.length());

    OnDiskUpperTriangleMatrix roarray = new OnDiskUpperTriangleMatrix(file, 1, extraheadersize, recsize, false);
    Assert.assertEquals("Number of records incorrect.", matsize, roarray.getMatrixSize());
    
    Assert.assertArrayEquals("Record 0,0 doesn't match.", record1, roarray.readRecord(0,0));
    Assert.assertArrayEquals("Record 0,1 doesn't match.", record2, roarray.readRecord(0,1));
    Assert.assertArrayEquals("Record 1,1 doesn't match.", record3, roarray.readRecord(1,1));
    Assert.assertArrayEquals("Record 1,0 doesn't match.", record2, roarray.readRecord(1,0));
    Assert.assertArrayEquals("Record 0,2 doesn't match.", record3, roarray.readRecord(0,2));
    Assert.assertArrayEquals("Record 1,2 doesn't match.", record2, roarray.readRecord(1,2));
    Assert.assertArrayEquals("Record 2,2 doesn't match.", record1, roarray.readRecord(2,2));
  }
}
