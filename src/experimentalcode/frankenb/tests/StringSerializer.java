package experimentalcode.frankenb.tests;

import java.io.IOException;
import java.nio.ByteBuffer;

import de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class StringSerializer implements ByteBufferSerializer<String> {
  @Override
  public String fromByteBuffer(ByteBuffer data) throws IOException, UnsupportedOperationException {
    int size = data.getInt();
    byte[] buffer = new byte[size];
    data.get(buffer);
    
    return new String(buffer);
  }

  @Override
  public void toByteBuffer(ByteBuffer buffer, String obj) throws IOException, UnsupportedOperationException {
    byte[] aBuffer = obj.getBytes();
    buffer.putInt(aBuffer.length);
    buffer.put(aBuffer);
  }

  @Override
  public int getByteSize(String obj) throws IOException, UnsupportedOperationException {
    return obj.getBytes().length + Integer.SIZE / 8;
  }
}