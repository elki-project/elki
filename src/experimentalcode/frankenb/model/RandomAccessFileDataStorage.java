/**
 * 
 */
package experimentalcode.frankenb.model;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel.MapMode;

import experimentalcode.frankenb.model.ifaces.DataStorage;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class RandomAccessFileDataStorage implements DataStorage {

  private final RandomAccessFile randomAccess;
  private final File source;
  
  public RandomAccessFileDataStorage(File source) throws IOException {
    randomAccess = new RandomAccessFile(source, "rw");
    this.source = source;
  }
  
  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#getByteBuffer(long)
   */
  @Override
  public ByteBuffer getByteBuffer(long size) throws IOException {
    return randomAccess.getChannel().map(MapMode.READ_WRITE, randomAccess.getFilePointer(), size);
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#getReadOnlyByteBuffer(long)
   */
  @Override
  public ByteBuffer getReadOnlyByteBuffer(long size) throws IOException {
    return randomAccess.getChannel().map(MapMode.READ_ONLY, randomAccess.getFilePointer(), size);
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#seek(long)
   */
  @Override
  public void seek(long position) throws IOException {
    randomAccess.seek(position);
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#readInt()
   */
  @Override
  public int readInt() throws IOException {
    return randomAccess.readInt();
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#writeInt(int)
   */
  @Override
  public void writeInt(int i) throws IOException {
    randomAccess.writeInt(i);
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#readLong()
   */
  @Override
  public long readLong() throws IOException {
    return randomAccess.readLong();
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#writeLong(long)
   */
  @Override
  public void writeLong(long l) throws IOException {
    randomAccess.writeLong(l);
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#getFilePointer()
   */
  @Override
  public long getFilePointer() throws IOException {
    return randomAccess.getFilePointer();
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#readBoolean()
   */
  @Override
  public boolean readBoolean() throws IOException {
    return randomAccess.readBoolean();
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#writeBoolean(boolean)
   */
  @Override
  public void writeBoolean(boolean b) throws IOException {
    randomAccess.writeBoolean(b);
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#read(byte[])
   */
  @Override
  public void read(byte[] buffer) throws IOException {
    randomAccess.read(buffer);
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#write(byte[])
   */
  @Override
  public void write(byte[] buffer) throws IOException {
    randomAccess.write(buffer);
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#setLength(long)
   */
  @Override
  public void setLength(long length) throws IOException {
    randomAccess.setLength(length);
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#close()
   */
  @Override
  public void close() throws IOException {
    randomAccess.close();
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#getSource()
   */
  @Override
  public File getSource() throws IOException {
    return this.source;
  }

}
