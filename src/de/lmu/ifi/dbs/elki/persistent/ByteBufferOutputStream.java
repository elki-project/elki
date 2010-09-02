package de.lmu.ifi.dbs.elki.persistent;

import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Wrap an existing ByteBuffer as OutputStream.
 * 
 * @author Erich Schubert
 */
public class ByteBufferOutputStream extends OutputStream {
  /**
   * The actual buffer we're using.
   */
  final ByteBuffer buffer;

  /**
   * Constructor.
   * 
   * @param buffer ByteBuffer to wrap.
   */
  public ByteBufferOutputStream(ByteBuffer buffer) {
    super();
    this.buffer = buffer;
  }
  
  @Override
  public void write(int b) {
    buffer.put((byte) b);
  }

  @Override
  public void write(byte[] b, int off, int len) {
    buffer.put(b, off, len);
  }
}