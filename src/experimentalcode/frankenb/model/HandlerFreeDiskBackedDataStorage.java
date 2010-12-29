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
public class HandlerFreeDiskBackedDataStorage implements DataStorage {

  private final File source;
  private long position;
  
  private static interface RandomAccess<T> {
    T call(RandomAccessFile randomAccessFile) throws IOException;
  }
  
  public HandlerFreeDiskBackedDataStorage(File source) throws IOException {
    position = 0;
    this.source = source;
  }
  
  private <T> T randomAccess(RandomAccess<T> randomAccess) throws IOException {
    RandomAccessFile randomAccessFile = null;
    try {
      randomAccessFile = new RandomAccessFile(this.source, "rw");
      randomAccessFile.seek(position);
      
      T result = randomAccess.call(randomAccessFile);
      
      this.position = randomAccessFile.getFilePointer();
      return result;
    } finally {
      if (randomAccessFile != null) {
        randomAccessFile.close();
      }
    }
  }
  
  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#getByteBuffer(long)
   */
  @Override
  public ByteBuffer getByteBuffer(final long size) throws IOException {
    return randomAccess(
        new RandomAccess<ByteBuffer>() {

          @Override
          public ByteBuffer call(RandomAccessFile randomAccessFile) throws IOException{
            return randomAccessFile.getChannel().map(MapMode.READ_WRITE, randomAccessFile.getFilePointer(), size);
          }
          
        }
    );
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#getReadOnlyByteBuffer(long)
   */
  @Override
  public ByteBuffer getReadOnlyByteBuffer(final long size) throws IOException {
    return randomAccess(
        new RandomAccess<ByteBuffer>() {

          @Override
          public ByteBuffer call(RandomAccessFile randomAccessFile) throws IOException{
            return randomAccessFile.getChannel().map(MapMode.READ_ONLY, randomAccessFile.getFilePointer(), size);
          }
          
        }
    );
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#seek(long)
   */
  @Override
  public void seek(long position) throws IOException {
    if (position > this.source.length()) throw new IOException("Can't seek beyond the end of file");
    this.position = position;
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#readInt()
   */
  @Override
  public int readInt() throws IOException {
    return randomAccess(
        new RandomAccess<Integer>() {

          @Override
          public Integer call(RandomAccessFile randomAccessFile) throws IOException{
            return randomAccessFile.readInt();
          }
          
        }
    );
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#writeInt(int)
   */
  @Override
  public void writeInt(final int i) throws IOException {
    randomAccess(
        new RandomAccess<Void>() {

          @Override
          public Void call(RandomAccessFile randomAccessFile) throws IOException{
            randomAccessFile.writeInt(i);
            return null;
          }
          
        }
    );  
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#readLong()
   */
  @Override
  public long readLong() throws IOException {
    return randomAccess(
        new RandomAccess<Long>() {

          @Override
          public Long call(RandomAccessFile randomAccessFile) throws IOException{
            return randomAccessFile.readLong();
          }
          
        }
    );  
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#writeLong(long)
   */
  @Override
  public void writeLong(final long l) throws IOException {
    randomAccess(
        new RandomAccess<Void>() {

          @Override
          public Void call(RandomAccessFile randomAccessFile) throws IOException{
            randomAccessFile.writeLong(l);
            return null;
          }
          
        }
    );
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#getFilePointer()
   */
  @Override
  public long getFilePointer() throws IOException {
    return this.position;
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#readBoolean()
   */
  @Override
  public boolean readBoolean() throws IOException {
    return randomAccess(
        new RandomAccess<Boolean>() {

          @Override
          public Boolean call(RandomAccessFile randomAccessFile) throws IOException{
            return randomAccessFile.readBoolean();
          }
          
        }
    );  
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#writeBoolean(boolean)
   */
  @Override
  public void writeBoolean(final boolean b) throws IOException {
    randomAccess(
        new RandomAccess<Void>() {

          @Override
          public Void call(RandomAccessFile randomAccessFile) throws IOException{
            randomAccessFile.writeBoolean(b);
            return null;
          }
          
        }
    );
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#read(byte[])
   */
  @Override
  public void read(byte[] buffer) throws IOException {
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#write(byte[])
   */
  @Override
  public void write(final byte[] buffer) throws IOException {
    randomAccess(
        new RandomAccess<Void>() {

          @Override
          public Void call(RandomAccessFile randomAccessFile) throws IOException{
            randomAccessFile.write(buffer);
            return null;
          }
          
        }
    );  
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#setLength(long)
   */
  @Override
  public void setLength(final long length) throws IOException {
    randomAccess(
        new RandomAccess<Void>() {

          @Override
          public Void call(RandomAccessFile randomAccessFile) throws IOException{
            randomAccessFile.setLength(length);
            return null;
          }
          
        }
    );   
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#close()
   */
  @Override
  public void close() throws IOException {
    //not necessary
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.DataStorage#getSource()
   */
  @Override
  public File getSource() throws IOException {
    return this.source;
  }

}
