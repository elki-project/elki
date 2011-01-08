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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import experimentalcode.frankenb.model.DiskBackedPartition;
import experimentalcode.frankenb.model.ifaces.IPartition;

/**
 * This class partitions the data
 * @author Florian
 *
 */
public class RandomPartitioner extends PartitionPairerPartitioner {

  private static final Logging LOG = Logging.getLogger(RandomPartitioner.class);
  
  public RandomPartitioner(Parameterization config) {
    super(config);
    LoggingConfiguration.setLevelFor(RandomPartitioner.class.getCanonicalName(), Level.ALL.getName());
  }
  
  @Override
  public List<IPartition> makePartitions(Database<NumberVector<?, ?>> dataBase, int packageQuantity, int partitionQuantity) throws UnableToComplyException {
    try {
      int dataEntriesPerPartition = (int)Math.ceil(dataBase.size() / (float)partitionQuantity);
      
      LOG.log(Level.INFO, "\tEach contains about items:" + dataEntriesPerPartition);
      
      Random random = new Random(System.currentTimeMillis());
      List<DBID> candidates = new ArrayList<DBID>();
      for (DBID dbid : dataBase) {
        candidates.add(dbid);
      }
      
      List<IPartition> partitions = new ArrayList<IPartition>();
      for (int i = 0; i < partitionQuantity; ++i) {
        IPartition partition = new DiskBackedPartition(dataBase.dimensionality());
        for (int j = 0; j < dataEntriesPerPartition; ++j) {
          if (candidates.size() == 0) break;
          DBID candidate = candidates.remove(random.nextInt(candidates.size()));
          partition.addVector(candidate.getIntegerID(), dataBase.get(candidate));
        }
        partitions.add(partition);
      }
      
      return partitions;
      
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new UnableToComplyException(e);
    }
    
  }


}
