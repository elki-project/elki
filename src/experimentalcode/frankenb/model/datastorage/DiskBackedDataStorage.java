/**
 * 
 */
package experimentalcode.frankenb.model.datastorage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel.MapMode;

import experimentalcode.frankenb.model.ifaces.IDataStorage;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class DiskBackedDataStorage implements IDataStorage {

  private final File source;

  private RandomAccessFile randomAccessFile;

  public DiskBackedDataStorage(File source) throws IOException {
    this.source = source;
    randomAccessFile = new RandomAccessFile(source, "rw");
  }

  /*
   * (non-Javadoc)
   * 
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#getByteBuffer(long)
   */
  @Override
  public ByteBuffer getByteBuffer(final long size) throws IOException {
    return randomAccessFile.getChannel().map(MapMode.READ_WRITE, randomAccessFile.getFilePointer(), size);
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
    return randomAccessFile.getChannel().map(MapMode.READ_ONLY, randomAccessFile.getFilePointer(), size);
  }

  /*
   * (non-Javadoc)
   * 
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#seek(long)
   */
  @Override
  public void seek(long position) throws IOException {
    randomAccessFile.seek(position);
  }

  /*
   * (non-Javadoc)
   * 
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#readInt()
   */
  @Override
  public int readInt() throws IOException {
    return randomAccessFile.readInt();
  }

  /*
   * (non-Javadoc)
   * 
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#writeInt(int)
   */
  @Override
  public void writeInt(final int i) throws IOException {
    randomAccessFile.writeInt(i);
  }

  /*
   * (non-Javadoc)
   * 
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#readLong()
   */
  @Override
  public long readLong() throws IOException {
    return randomAccessFile.readLong();
  }

  /*
   * (non-Javadoc)
   * 
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#writeLong(long)
   */
  @Override
  public void writeLong(final long l) throws IOException {
    randomAccessFile.writeLong(l);
  }

  /*
   * (non-Javadoc)
   * 
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#getFilePointer()
   */
  @Override
  public long getFilePointer() throws IOException {
    return randomAccessFile.getFilePointer();
  }

  /*
   * (non-Javadoc)
   * 
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#readBoolean()
   */
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

  /*
   * (non-Javadoc)
   * 
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#read(byte[])
   */
  @Override
  public void read(byte[] buffer) throws IOException {
    throw new UnsupportedOperationException();
  }

  /*
   * (non-Javadoc)
   * 
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#write(byte[])
   */
  @Override
  public void write(final byte[] buffer) throws IOException {
    randomAccessFile.write(buffer);
  }

  /*
   * (non-Javadoc)
   * 
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#setLength(long)
   */
  @Override
  public void setLength(final long length) throws IOException {
    randomAccessFile.setLength(length);
  }

  /*
   * (non-Javadoc)
   * 
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#close()
   */
  @Override
  public void close() throws IOException {
    randomAccessFile.close();
  }

  /*
   * (non-Javadoc)
   * 
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#getSource()
   */
  @Override
  public File getSource() throws IOException {
    return this.source;
  }

}
