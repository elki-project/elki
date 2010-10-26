/**
 * 
 */
package experimentalcode.frankenb.model;

import java.io.IOException;
import java.nio.ByteBuffer;

import de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class DistanceListSerializer implements ByteBufferSerializer<DistanceList> {

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer#fromByteBuffer(java.nio.ByteBuffer)
   */
  @Override
  public DistanceList fromByteBuffer(ByteBuffer data) throws IOException, UnsupportedOperationException {
    int id = data.getInt();
    DistanceList distanceList = new DistanceList(id);

    int items = data.getInt();
    distanceList.setSorted(data.get() > 0);
    
    for (int i = 0; i < items; ++i) {
      distanceList.addDistance(data.getInt(), data.getDouble());
    }
    
    return distanceList;
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer#toByteBuffer(java.nio.ByteBuffer, java.lang.Object)
   */
  @Override
  public void toByteBuffer(ByteBuffer buffer, DistanceList distanceList) throws IOException, UnsupportedOperationException {
    if (buffer.remaining() < getByteSize(distanceList)) {
      throw new IOException("Can't store DistanceList to file because blocksize is too small");
    }
    
    buffer.putInt(distanceList.getId());
    buffer.putInt(distanceList.getDistances().size());
    buffer.put((byte) (distanceList.isSorted() ? 1 : 0));
    
    for (Pair<Integer, Double> entry : distanceList) {
      buffer.putInt(entry.getFirst());
      buffer.putDouble(entry.getSecond());
    }
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer#getByteSize(java.lang.Object)
   */
  @Override
  public int getByteSize(DistanceList distanceList) throws IOException, UnsupportedOperationException {
    return getSizeForNEntries(distanceList.getDistances().size());
  }
  
  public static int getSizeForNEntries(int n) {
    return n * ((Integer.SIZE + Double.SIZE) / 8) + 2 * (Integer.SIZE / 8) + Short.SIZE / 8;
  }
  
}
