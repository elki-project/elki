package experimentalcode.frankenb.tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.ByteBuffer;

import org.junit.Test;

import experimentalcode.frankenb.model.datastorage.BufferedDiskBackedDataStorage;


/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class BufferedDiskBackedDataStorageTest {

  @Test
  public void longTest() throws Exception {
    File file = File.createTempFile("test", ".dat");
    file.deleteOnExit();
    
    BufferedDiskBackedDataStorage storage = new BufferedDiskBackedDataStorage(file);
    
    for (long l = 0; l < 10000; ++l) {
      storage.setLength((Long.SIZE / 8) * (l + 1));
      storage.writeLong(l);
    }
    
    storage.close();

    storage = new BufferedDiskBackedDataStorage(file);
    storage.seek(0L);
    for (long l = 0; l < 10000; ++l) {
      long aL = storage.readLong();
      assertEquals(l, aL);
    }
    
    storage.close();
    
  }
  
  @Test
  public void bufferTest() throws Exception {
    File file = File.createTempFile("test", ".dat");
    file.deleteOnExit();
    
    BufferedDiskBackedDataStorage storage = new BufferedDiskBackedDataStorage(file);
    storage.seek(0);
    
    int capacity = 25 * Long.SIZE / 8;
    storage.setLength(capacity);
    for (long l = 0; l < 10; ++l) {
      storage.writeLong(l);
    }
    
    ByteBuffer buffer = ByteBuffer.allocate(5 * Long.SIZE / 8);
    for (long l = 0; l < 5; ++l) {
      buffer.putLong(l);
    }
    storage.writeBuffer(buffer);

    for (long l = 0; l < 10; ++l) {
      storage.writeLong(l);
    }
    
    storage.close();

    storage = new BufferedDiskBackedDataStorage(file);
    storage.seek(0);
    for (long l = 0; l < 10; ++l) {
      long aL = storage.readLong();
      assertEquals(l, aL);
    }
    
    buffer = storage.getReadOnlyByteBuffer(5 * Long.SIZE / 8);
    for (long l = 0; l < 5; ++l) {
      assertEquals(l, buffer.getLong());
    }
    storage.seek(storage.getFilePointer() + 5 * Long.SIZE / 8);
    
    for (long l = 0; l < 10; ++l) {
      long aL = storage.readLong();
      assertEquals(l, aL);
    }
    
    
    storage.close();
    
  }  
}
