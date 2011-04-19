/**
 * 
 */
package experimentalcode.frankenb.model.datastorage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import experimentalcode.frankenb.model.ifaces.IDataStorage;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class DiskBackedDataStorage implements IDataStorage {

  private final File source;
  private FileChannel channel;

  private RandomAccessFile randomAccessFile;

  public DiskBackedDataStorage(File source) throws IOException {
    this.source = source;
    randomAccessFile = new RandomAccessFile(source, "rw");
    channel = randomAccessFile.getChannel();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * experimentalcode.frankenb.model.ifaces.DataStorage#getReadOnlyByteBuffer
   * (long)
   */
  @Override
  public ByteBuffer getReadOnlyByteBuffer(final long size) throws IOException {
    ByteBuffer buffer = ByteBuffer.allocate((int) size);
    channel.read(buffer);
    
    buffer.rewind();
    return buffer;
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.IDataStorage#writeBuffer(java.nio.ByteBuffer)
   */
  @Override
  public void writeBuffer(ByteBuffer buffer) throws IOException {
    long position = this.getFilePointer();
    buffer.rewind();
    channel.write(buffer);
    buffer.rewind();
    randomAccessFile.seek(position + buffer.remaining());
  }
  
  @Override
  public void seek(long position) throws IOException {
    randomAccessFile.seek(position);
  }

  @Override
  public int readInt() throws IOException {
    return randomAccessFile.readInt();
  }

  @Override
  public void writeInt(final int i) throws IOException {
    randomAccessFile.writeInt(i);
  }

  @Override
  public long readLong() throws IOException {
    return randomAccessFile.readLong();
  }

  @Override
  public void writeLong(final long l) throws IOException {
    randomAccessFile.writeLong(l);
  }

  @Override
  public long getFilePointer() throws IOException {
    return randomAccessFile.getFilePointer();
  }

  @Override
  public boolean readBoolean() throws IOException {
    return randomAccessFile.readBoolean();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * experimentalcode.frankenb.model.ifaces.DataStorage#writeBoolean(boolean)
   */
  @Override
  public void writeBoolean(final boolean b) throws IOException {
    randomAccessFile.writeBoolean(b);
  }

  @Override
  public void read(byte[] buffer) throws IOException {
    randomAccessFile.read(buffer);
  }

  @Override
  public void write(final byte[] buffer) throws IOException {
    randomAccessFile.write(buffer);
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.IDataStorage#write(byte[], int, int)
   */
  @Override
  public void write(byte[] buffer, int off, int len) throws IOException {
    randomAccessFile.write(buffer, off, len);
  }
  
  @Override
  public void setLength(final long length) throws IOException {
    randomAccessFile.setLength(length);
  }

  @Override
  public void close() throws IOException {
    channel.close();
    randomAccessFile.close();
  }

  @Override
  public File getSource() throws IOException {
    return this.source;
  }


}
