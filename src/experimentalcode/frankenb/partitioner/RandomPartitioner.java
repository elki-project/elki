package experimentalcode.frankenb.partitioner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import experimentalcode.frankenb.model.Partition;
import experimentalcode.frankenb.model.PartitionPairing;
import experimentalcode.frankenb.model.ifaces.Partitioner;

/**
 * This class partitions the data
 * @author Florian
 *
 */
public class RandomPartitioner implements Partitioner {

  private static final Logging LOG = Logging.getLogger(RandomPartitioner.class);
  
  public RandomPartitioner() {
    LoggingConfiguration.setLevelFor(RandomPartitioner.class.getCanonicalName(), Level.ALL.getName());
    
  }
  
  @Override
  public List<PartitionPairing> makePairings(Database<NumberVector<?, ?>> dataBase, int packageQuantity) throws UnableToComplyException {
    try {
      int partitionQuantity = packagesQuantityToPartitionsQuantity(packageQuantity);
      int dataEntriesPerPartition = (int)Math.ceil(dataBase.size() / (float)partitionQuantity);
      
      LOG.debug("Creating " + partitionQuantity + " partitions");
      
      Random random = new Random(System.currentTimeMillis());
      List<DBID> candidates = new ArrayList<DBID>();
      for (DBID dbid : dataBase) {
        candidates.add(dbid);
      }
      
      List<Partition> partitions = new ArrayList<Partition>();
      for (int i = 0; i < partitionQuantity; ++i) {
        Partition partition = new Partition(dataBase.dimensionality());
        for (int j = 0; j < dataEntriesPerPartition; ++j) {
          if (candidates.size() == 0) break;
          DBID candidate = candidates.remove(random.nextInt(candidates.size()));
          partition.addVector(candidate.getIntegerID(), dataBase.get(candidate));
        }
        partitions.add(partition);
      }
      
      List<PartitionPairing> pairings = new ArrayList<PartitionPairing>();
      
      for (int i = 0; i < partitionQuantity; ++i) {
        for (int j = 0; j <= i; ++j) {
          LOG.debug("Pairing " + i + " vs " + j);
          pairings.add(new PartitionPairing(partitions.get(i), partitions.get(j)));
        }
      }
      
      return pairings;
      
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new UnableToComplyException(e);
    }
    
  }
  
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
