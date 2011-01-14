/**
 * 
 */
package experimentalcode.frankenb.partitioner;

import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import experimentalcode.frankenb.model.ifaces.IPartitionPairingStorage;
import experimentalcode.frankenb.model.ifaces.IPartitioner;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public abstract class FixedAmountPartitioner implements IPartitioner {

  private static final Logging LOG = Logging.getLogger(FixedAmountPartitioner.class);
  
  /**
   * OptionID for {@link #PARTITIONS_PARAM}
   */
  public static final OptionID PARTITIONS_ID = OptionID.getOrCreateOptionID("partitions", "Amount of partitions to create");
  
  /**
   * Parameter that specifies the percentage of deviations
   * <p>
   * Key: {@code -packages}
   * </p>
   */
  private final IntParameter PARTITIONS_PARAM = new IntParameter(PARTITIONS_ID, false);  
  
  private int partitionQuantity;
  
  public FixedAmountPartitioner(Parameterization config) {
    LoggingConfiguration.setLevelFor(FixedAmountPartitioner.class.getCanonicalName(), Level.ALL.getName());
    
    if (config.grab(PARTITIONS_PARAM)) {
      partitionQuantity = PARTITIONS_PARAM.getValue();
    }    
  }
  
  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.Partitioner#makePairings(de.lmu.ifi.dbs.elki.database.Database, int)
   */
  @Override
  public final void makePartitionPairings(Database<NumberVector<?, ?>> dataBase, IPartitionPairingStorage partitionPairingStorage, int packageQuantity) throws UnableToComplyException {
    LOG.log(Level.INFO, "\tCreating partitions: " + partitionQuantity);
    
    makePartitionPairings(dataBase, partitionPairingStorage, packageQuantity, partitionQuantity);
  }
  
  protected abstract void makePartitionPairings(Database<NumberVector<?, ?>> dataBase, IPartitionPairingStorage partitionPairingStorage, int packageQuantity, int partitionQuantity) throws UnableToComplyException;

}
