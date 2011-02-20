/**
 * 
 */
package experimentalcode.frankenb.algorithms.pairing;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import experimentalcode.frankenb.algorithms.partitioning.ZCurvePartitioning;
import experimentalcode.frankenb.log.Log;
import experimentalcode.frankenb.model.PartitionPairing;
import experimentalcode.frankenb.model.ifaces.IDataSet;
import experimentalcode.frankenb.model.ifaces.IPartition;
import experimentalcode.frankenb.model.ifaces.IPartitionPairing;

/**
 * This class implements a sliding window pairing. That means that all partitions get paired with
 * the partitions with a distance of n around each partition. This implementation assumes that all
 * partitions are from a one dimensional space (e.g. {@link ZCurvePartitioning}).
 * <p/>
 * Consider this simple example with a window size of 1. In this example partition 2 would be paired 
 * with partition 1 and partition 3 and of course with itself. 
 * 
 * <pre>
 * -----------------------------
 * | 0 | 1 | 2 | 3 | 4 | 5 | 6 |
 * -----------------------------
 *     |     ^     |
 *     -------------
 * </pre>
 * 
 * @author Florian Frankenberger
 */
public class SlidingWindowPartitionPairing implements IPartitionPairing {

  public static final OptionID WINDOW_SIZE_ID = OptionID.getOrCreateOptionID("windowsize", "size of the sliding window (look ahead window)");
  private final IntParameter WINDOW_SIZE_PARAM = new IntParameter(WINDOW_SIZE_ID);

  private int windowSize = 0;
  
  public SlidingWindowPartitionPairing(Parameterization config) {
    if (config.grab(WINDOW_SIZE_PARAM)) {
      windowSize = WINDOW_SIZE_PARAM.getValue();
    }
  }
  
  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.IPartitionPairing#makePairings(experimentalcode.frankenb.model.ifaces.IDataSet, java.util.List, experimentalcode.frankenb.model.ifaces.IPartitionPairingStorage, int)
   */
  @Override
  public List<PartitionPairing> makePairings(IDataSet dataSet, List<IPartition> partitions, int packageQuantity) throws UnableToComplyException {
    Log.info("sliding window size: " + windowSize);
    
    int totalPairings = ((windowSize + 1) * (partitions.size() - windowSize)) + ((windowSize * (windowSize + 1)) / 2); 
    Log.info("amount of pairings to create: " + totalPairings);
    
    List<PartitionPairing> result = new ArrayList<PartitionPairing>(totalPairings);
    
    for (int i = 0; i < partitions.size(); ++i) {
      IPartition basePartition = partitions.get(i);
      for (int j = i; j < Math.min(i + windowSize + 1, partitions.size()); ++j) {
        IPartition pairPartition = partitions.get(j);
        result.add(new PartitionPairing(basePartition, pairPartition));
      }
    }
    
    return result;
  }

}
