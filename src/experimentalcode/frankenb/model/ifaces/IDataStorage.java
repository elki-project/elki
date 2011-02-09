/**
 * 
 */
package experimentalcode.frankenb.model.ifaces;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public interface IDataStorage {

  /**
   * Returns a writeable bytebuffer of the current position
   * @return
   */
  //public ByteBuffer getByteBuffer(long size) throws IOException;
  
  /**
   * Returns a read only buffer
   */
  public ByteBuffer getReadOnlyByteBuffer(long size) throws IOException;
  
  /**
   * Writes the specified buffer at the current position and increments
   * the position about the amount of bytes in the buffer. The buffer
   * is rewinded before getting stored.
   * 
   * @param buffer
   * @throws IOException
   */
  public void writeBuffer(ByteBuffer buffer) throws IOException;
  
  public void seek(long position) throws IOException;
  
  public int readInt() throws IOException;
  
  public void writeInt(int i) throws IOException;
  
  public long readLong() throws IOException;
  
  public void writeLong(long l) throws IOException;
  
  public long getFilePointer() throws IOException;
  
  public boolean readBoolean() throws IOException;
  
  public void writeBoolean(boolean b) throws IOException;
  
  public void read(byte[] buffer) throws IOException;
  
  public void write(byte[] buffer) throws IOException;
  
  public void write(byte[] buffer, int off, int len) throws IOException;
  
  public void setLength(long length) throws IOException;
  
  public void close() throws IOException;
  
  public File getSource() throws IOException;
  
}
