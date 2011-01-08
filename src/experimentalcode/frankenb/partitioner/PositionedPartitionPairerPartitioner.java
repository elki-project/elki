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
import experimentalcode.frankenb.model.PartitionPairing;
import experimentalcode.frankenb.model.ifaces.IPositionedPartition;
import experimentalcode.frankenb.model.ifaces.IPositionedPartitionPairer;

/**
 * Abstract Partitioner class that generates positioned partitions and therefore need
 * an partition pairer that is capable of dealing with them.
 * 
 * @author Florian Frankenberger
 */
public abstract class PositionedPartitionPairerPartitioner extends FixedAmountPartitioner {

  private static final Logging LOG = Logging.getLogger(PositionedPartitionPairerPartitioner.class);
  public static final OptionID PARTITIO_PAIRER_ID = OptionID.getOrCreateOptionID("partitionpairer", "A partition pairer");

  private IPositionedPartitionPairer partitionPairer;
  
  public PositionedPartitionPairerPartitioner(Parameterization config) {
    super(config);
    
    LoggingConfiguration.setLevelFor(PositionedPartitionPairerPartitioner.class.getCanonicalName(), Level.ALL.getName());
    final ObjectParameter<IPositionedPartitionPairer> paramPartitionPairer = new ObjectParameter<IPositionedPartitionPairer>(PARTITIO_PAIRER_ID, IPositionedPartitionPairer.class, false);
    if(config.grab(paramPartitionPairer)) {
      this.partitionPairer = paramPartitionPairer.instantiateClass(config);
    }    
  }
  
  protected List<PartitionPairing> makePartitionPairings(Database<NumberVector<?, ?>> dataBase, int partitionQuantity, int packageQuantity) throws UnableToComplyException {
    List<IPositionedPartition> partitions = makePartitions(dataBase, partitionQuantity, packageQuantity);
    
    LOG.log(Level.INFO, String.format("Creating partition pairings (%s) ...", partitionPairer.getClass().getSimpleName()));
    return this.partitionPairer.makePairings(dataBase, partitions, packageQuantity);
  }

  
  protected abstract List<IPositionedPartition> makePartitions(Database<NumberVector<?, ?>> dataBase, int partitionQuantity, int packageQuantity) throws UnableToComplyException;

  
}
