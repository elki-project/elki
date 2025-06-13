package elki.result;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;

public abstract class NumpyDumper {
  
  public void writeNumpyArray(FileChannel file, Map<String,String> header, ByteBuffer array) throws IOException{
    ByteBuffer headerBuf = Charset.forName("UTF-8").encode(toString(header));
    int headerSize = headerBuf.limit();

    MappedByteBuffer buffer = file.map(MapMode.READ_WRITE, 0,12);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.put((byte) 0x93);
    buffer.put((byte) 'N');
    buffer.put((byte) 'U');
    buffer.put((byte) 'M');
    buffer.put((byte) 'P');
    buffer.put((byte) 'Y');

    // write version and header size
    buffer.put((byte) 0x3); // major version
    buffer.put((byte) 0x0); // minor version
    buffer.putInt(headerSize);

    // write Header
    buffer = file.map(MapMode.READ_WRITE, 12, headerSize);
    buffer.put(headerBuf);

    // write Array
    buffer = file.map(MapMode.READ_WRITE, 12 + headerSize, array.limit());
    buffer.put(array);
  }

  public String toString(Map<String,String> header){
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    for (Entry<String,String> entry : header.entrySet()){
      sb.append("'" + entry.getKey() + "'");
      sb.append(": ");
      sb.append(entry.getValue());
      sb.append(", ");
    }
    sb.append("}");
    return sb.toString();
  }
}
