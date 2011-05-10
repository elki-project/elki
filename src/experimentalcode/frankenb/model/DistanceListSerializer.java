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

  @Override
  public DistanceList fromByteBuffer(ByteBuffer data) throws IOException, UnsupportedOperationException {
    int id = data.getInt();
    int k = data.getInt();
    DistanceList distanceList = new DistanceList(id, k);

    int items = data.getInt();

    for (int i = 0; i < items; ++i) {
      distanceList.addDistance(data.getInt(), data.getDouble());
    }
    
    return distanceList;
  }

  @Override
  public void toByteBuffer(ByteBuffer buffer, DistanceList distanceList) throws IOException, UnsupportedOperationException {
    if (buffer.remaining() < getByteSize(distanceList)) {
      throw new IOException("Can't store DistanceList to file because blocksize is too small");
    }
    
    buffer.putInt(distanceList.getId());
    buffer.putInt(distanceList.getK());
    buffer.putInt(distanceList.getDistances().size());
    
    for (Pair<Integer, Double> entry : distanceList) {
      buffer.putInt(entry.getFirst());
      buffer.putDouble(entry.getSecond());
    }
  }

  @Override
  public int getByteSize(DistanceList distanceList) throws IOException, UnsupportedOperationException {
    //for update purpose we reserve more bytes here then necessary because we know
    //that later on this list will most likely have at least k entries
    
    return getSizeForNEntries(Math.max(distanceList.getDistances().size(), distanceList.getK()));
  }
  
  public static int getSizeForNEntries(int n) {
    return n * ((Integer.SIZE + Double.SIZE) / 8) + 3 * (Integer.SIZE / 8);
  }
  
}
