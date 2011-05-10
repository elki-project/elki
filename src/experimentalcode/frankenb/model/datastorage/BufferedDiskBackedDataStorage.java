package experimentalcode.frankenb.model.datastorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import de.lmu.ifi.dbs.elki.logging.Logging;
import experimentalcode.frankenb.log.Log;
import experimentalcode.frankenb.model.ifaces.IDataStorage;

/**
 * A Buffered Implementation of DataStore which bufferes all content in direct memory. This
 * class should be used with DynamicBPlusTree to buffer the directory file in memory and therefore
 * prevent a lot of random access to disk.
 * <p/>
 * <b>Be aware that this implementation has a capacity limit of 2^31-1 bytes</b>
 * 
 * @author Florian Frankenberger
 */
public class BufferedDiskBackedDataStorage implements IDataStorage {
  /**
   * Logger
   */
  private static final Logging logger = Logging.getLogger(BufferedDiskBackedDataStorage.class);

  private ByteBuffer buffer; 
  private final File source;
  
  public BufferedDiskBackedDataStorage(File source) throws IOException {
    this(source, 0);
  }
  
  public BufferedDiskBackedDataStorage(File source, int bufferSize) throws IOException {
    this.source = source;
    if (source.exists() && source.length() > 0) {
      logger.debug(String.format("source file exists: %s", source));
      if (source.length() > Integer.MAX_VALUE) throw new IllegalStateException("This class only supports to buffer files up to " + Integer.MAX_VALUE + " bytes");
      readEntireFile();
    } else {
      logger.debug(String.format("creating new buffer for %s with size %d", source, bufferSize));
      buffer = ByteBuffer.allocate(bufferSize);
    }
  }
  
  private void readEntireFile() throws IOException {
    logger.debug(String.format("copying %d bytes from source file %s into memory ...", source.length(), this.getSource()));
    
    buffer = ByteBuffer.allocate((int) source.length());
    
    InputStream in = null;
    byte[] localBuffer = new byte[4096];
    int bytesRead = 0;
    
    try {
      in = new FileInputStream(this.source);
      do {
        bytesRead = in.read(localBuffer);
        if (bytesRead > 0) {
          buffer.put(localBuffer, 0, bytesRead);
        }
      } while (bytesRead > -1);
    } finally {
      if (in != null) in.close();
    }
  }

  @Override
  public ByteBuffer getReadOnlyByteBuffer(long size) throws IOException {
    return buffer.duplicate().asReadOnlyBuffer();
  }
  
  @Override
  public void writeBuffer(ByteBuffer src) throws IOException {
    src.rewind();
    this.buffer.put(src);
  }

  @Override
  public void seek(long position) throws IOException {
    buffer.position((int) position);
  }

  @Override
  public int readInt() throws IOException {
    return buffer.getInt();
  }

  @Override
  public void writeInt(int i) throws IOException {
    buffer.putInt(i);
  }

  @Override
  public long readLong() throws IOException {
    return buffer.getLong();
  }

  @Override
  public void writeLong(long l) throws IOException {
    buffer.putLong(l);
  }

  @Override
  public long getFilePointer() throws IOException {
    return buffer.position();
  }

  @Override
  public boolean readBoolean() throws IOException {
    return buffer.get() > 0;
  }

  @Override
  public void writeBoolean(boolean b) throws IOException {
    buffer.put((byte) (b ? 1 : 0));
  }

  @Override
  public void read(byte[] aBuffer) throws IOException {
    buffer.get(aBuffer);
  }

  @Override
  public void write(byte[] aBuffer) throws IOException {
    buffer.put(aBuffer);
  }

  @Override
  public void write(byte[] aBuffer, int off, int len) throws IOException {
    buffer.put(aBuffer, off, len);
  }
  
  @Override
  public void setLength(long length) throws IOException {
    if (length > Integer.MAX_VALUE) throw new IllegalStateException("This buffered implementation can only hold " + Integer.MAX_VALUE + " items in memory");
    if (length > buffer.capacity()) {
      ByteBuffer newBuffer = ByteBuffer.allocate((int) length);
      byte[] localBuffer = new byte[2048];
      int position = buffer.position();
      buffer.rewind();
      int toWrite = 0;
      do {
        toWrite = Math.min(localBuffer.length, buffer.remaining());
        if (toWrite > 0) {
          buffer.get(localBuffer, 0, toWrite);
          newBuffer.put(localBuffer, 0, toWrite);
        }
      } while (toWrite > 0);
      newBuffer.position(position);
      this.buffer = newBuffer;
    } else {
      buffer.limit((int) length);
    }
  }

  @Override
  public void close() throws IOException {
    Log.debug(String.format("Storing %d bytes to source file %s ...", buffer.limit(), this.getSource()));
    OutputStream out = null;
    byte[] localBuffer = new byte[2048];
    
    buffer.rewind();
    try {
      out = new FileOutputStream(this.source);
      int toWrite = 0;
      do {
        toWrite = Math.min(localBuffer.length, buffer.remaining());
        if (toWrite > 0) {
          buffer.get(localBuffer, 0, toWrite);
          out.write(localBuffer, 0, toWrite);
        }
      } while (toWrite > 0);
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }

  @Override
  public File getSource() throws IOException {
    return this.source;
  }
}