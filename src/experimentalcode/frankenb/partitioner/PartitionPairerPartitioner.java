/**
 * 
 */
package experimentalcode.frankenb.partitioner;

import java.util.List;
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import experimentalcode.frankenb.model.ifaces.IPartition;
import experimentalcode.frankenb.model.ifaces.IPartitionPairer;
import experimentalcode.frankenb.model.ifaces.IPartitionPairingStorage;

/**
 * Abstract Partitioner class
 * 
 * @author Florian Frankenberger
 */
public abstract class PartitionPairerPartitioner extends FixedAmountPartitioner {

  private static final Logging LOG = Logging.getLogger(PartitionPairerPartitioner.class);
  public static final OptionID PARTITIO_PAIRER_ID = OptionID.getOrCreateOptionID("partitionpairer", "A partition pairer");

  private IPartitionPairer partitionPairer;
  
  public PartitionPairerPartitioner(Parameterization config) {
    super(config);
    
    LoggingConfiguration.setLevelFor(PartitionPairerPartitioner.class.getCanonicalName(), Level.ALL.getName());
    final ObjectParameter<IPartitionPairer> paramPartitionPairer = new ObjectParameter<IPartitionPairer>(PARTITIO_PAIRER_ID, IPartitionPairer.class, false);
    if(config.grab(paramPartitionPairer)) {
      this.partitionPairer = paramPartitionPairer.instantiateClass(config);
    }    
  }
  
  protected void makePartitionPairings(Database<NumberVector<?, ?>> dataBase, IPartitionPairingStorage partitionPairingStorage, int partitionQuantity, int packageQuantity) throws UnableToComplyException {
    List<IPartition> partitions = makePartitions(dataBase, partitionQuantity, packageQuantity);
    
    LOG.log(Level.INFO, String.format("Creating partition pairings (%s) ...", partitionPairer.getClass().getSimpleName()));
    this.partitionPairer.makePairings(dataBase, partitions, partitionPairingStorage, packageQuantity);
  }

  
  protected abstract List<IPartition> makePartitions(Database<NumberVector<?, ?>> dataBase, int partitionQuantity, int packageQuantity) throws UnableToComplyException;

  
}
