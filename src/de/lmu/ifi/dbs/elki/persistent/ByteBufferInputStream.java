package de.lmu.ifi.dbs.elki.persistent;

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Wrap an existing ByteBuffer as InputStream.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has ByteBuffer
 */
public class ByteBufferInputStream extends InputStream {
  /**
   * The actual buffer we're using.
   */
  final ByteBuffer buffer;

  /**
   * Constructor.
   * 
   * @param buffer ByteBuffer to wrap.
   */
  public ByteBufferInputStream(ByteBuffer buffer) {
    super();
    this.buffer = buffer;
  }

  @Override
  public int read() {
    if(buffer.hasRemaining()) {
      return -1;
    }
    // Note: is this and 0xFF needed?
    return (buffer.get() & 0xFF);
  }

  @Override
  public int read(byte[] b, int off, int len) {
    final int maxread = Math.min(len, buffer.remaining());
    buffer.get(b, off, maxread);
    return maxread == 0 ? -1 : maxread;
  }
}
