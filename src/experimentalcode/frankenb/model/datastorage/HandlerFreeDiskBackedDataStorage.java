package experimentalcode.frankenb.model.datastorage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import experimentalcode.frankenb.model.ifaces.IDataStorage;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class HandlerFreeDiskBackedDataStorage implements IDataStorage {

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

  @Override
  public ByteBuffer getReadOnlyByteBuffer(final long size) throws IOException {
    return randomAccess(
        new RandomAccess<ByteBuffer>() {

          @Override
          public ByteBuffer call(RandomAccessFile randomAccessFile) throws IOException{
            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            randomAccessFile.getChannel().read(buffer);
            buffer.rewind();
            return buffer;
          }
          
        }
    );
  }
  
  @Override
  public void writeBuffer(final ByteBuffer buffer) throws IOException {
    randomAccess(
        new RandomAccess<Void>() {

          @Override
          public Void call(RandomAccessFile randomAccessFile) throws IOException{
            buffer.rewind();
            randomAccessFile.getChannel().write(buffer);
            buffer.rewind();
            position += buffer.remaining();
            return null;
          }
          
        }
    );
  }

  @Override
  public void seek(long position) throws IOException {
    if (position > this.source.length()) throw new IOException("Can't seek beyond the end of file");
    this.position = position;
  }

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

  @Override
  public long getFilePointer() throws IOException {
    return this.position;
  }

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

  @Override
  public void read(byte[] buffer) throws IOException {
    throw new UnsupportedOperationException();
  }

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
  

  @Override
  public void write(final byte[] buffer, final int off, final int len) throws IOException {
    randomAccess(
        new RandomAccess<Void>() {

          @Override
          public Void call(RandomAccessFile randomAccessFile) throws IOException{
            randomAccessFile.write(buffer, off, len);
            return null;
          }
          
        }
    );  
  }  

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

  @Override
  public void close() throws IOException {
    //not necessary
  }

  @Override
  public File getSource() throws IOException {
    return this.source;
  }


}
