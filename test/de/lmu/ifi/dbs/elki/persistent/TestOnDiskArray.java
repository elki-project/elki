package de.lmu.ifi.dbs.elki.persistent;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.persistent.OnDiskArray;

/**
 * Test to validate proper OnDiskArray operation.
 * @author Erich Schubert
 */
// TODO: also test with a static sample file.
public class TestOnDiskArray implements JUnit4Test {
  File file = new File("OnDiskArrayTestFile.test.dat");

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
   * Test the OnDiskArray class.
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
    array.writeExtraHeader(header);
    byte[] record1 = { 31, 41, 59 };
    byte[] record2 = { 26, 53, 58 };
    array.writeRecord(0, record1);
    array.writeRecord(1, record2);
    array.writeRecord(2, record2);
    array.writeRecord(3, record1);
    array.resizeFile(5);
    numrec = 5;
    array.writeRecord(4, record1);
    array.close();

    // validate file size
    Assert.assertEquals("File size doesn't match.", ODR_HEADER_SIZE + extraheadersize + recsize * numrec, file.length());

    OnDiskArray roarray = new OnDiskArray(file, 1, 2, 3, false);
    Assert.assertEquals("Number of records incorrect.", numrec, roarray.getNumRecords());
    
    Assert.assertArrayEquals("Header doesn't match.", header, roarray.readExtraHeader());
    Assert.assertArrayEquals("Record 0 doesn't match.", record1, roarray.readRecord(0));
    Assert.assertArrayEquals("Record 4 doesn't match.", record1, roarray.readRecord(4));
    Assert.assertArrayEquals("Record 1 doesn't match.", record2, roarray.readRecord(1));
    Assert.assertArrayEquals("Record 2 doesn't match.", record2, roarray.readRecord(2));
    Assert.assertArrayEquals("Record 3 doesn't match.", record1, roarray.readRecord(3));    
  }
}
