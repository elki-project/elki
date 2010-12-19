/**
 * 
 */
package experimentalcode.frankenb.partitioner;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import experimentalcode.frankenb.model.Partition;
import experimentalcode.frankenb.model.PartitionPairing;
import experimentalcode.frankenb.model.ifaces.Partitioner;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public abstract class CrossPairingPartitioner implements Partitioner {

  private static final Logging LOG = Logging.getLogger(CrossPairingPartitioner.class);
  
  public CrossPairingPartitioner() {
    LoggingConfiguration.setLevelFor(CrossPairingPartitioner.class.getCanonicalName(), Level.ALL.getName());
  }
  
  
  public final List<PartitionPairing> makePairings(Database<NumberVector<?, ?>> dataBase, int packageQuantity) throws UnableToComplyException {
    final int partitionQuantity = packagesQuantityToPartitionsQuantity(packageQuantity);
    List<Partition> partitions = makePartitions(dataBase, partitionQuantity);
    
    float deviationPercent = 0.5f;
    int deviationMax = (int) (Math.max(1, deviationPercent * (float) partitions.size()) - 1);
    LOG.fine("DeviationMax: " + deviationMax);
    
    List<PartitionPairing> pairings = new ArrayList<PartitionPairing>();
    
    for (int i = 0; i < partitions.size(); ++i) {
      for (int j = i; j >= Math.max(0, i - deviationMax); --j) {
        LOG.fine("Pairing " + i + " vs " + j);
        pairings.add(new PartitionPairing(partitions.get(i), partitions.get(j)));
      }
    }
    
    return pairings;
    
  }  
  
  protected abstract List<Partition> makePartitions(Database<NumberVector<?, ?>> dataBase, int partitionQuantity) throws UnableToComplyException;
  
  
  /**
   * calculates the quantity of partitions so that around the given quantity of packages
   * result
   * 
   * @return
   * @throws UnableToComplyException 
   */
  private static int packagesQuantityToPartitionsQuantity(int packageQuantity) throws UnableToComplyException {
    if (packageQuantity < 3) {
      throw new UnableToComplyException("Minimum is 3 packages");
    }
    return (int)Math.floor((Math.sqrt(1 + packageQuantity * 8) - 1) / 2.0);
  }  
  
}
