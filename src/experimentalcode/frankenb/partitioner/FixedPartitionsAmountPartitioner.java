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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import experimentalcode.frankenb.model.PartitionPairing;
import experimentalcode.frankenb.model.ifaces.Partitioner;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public abstract class FixedPartitionsAmountPartitioner implements Partitioner {

  private static final Logging LOG = Logging.getLogger(FixedPartitionsAmountPartitioner.class);
  
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
  
  public FixedPartitionsAmountPartitioner(Parameterization config) {
    LoggingConfiguration.setLevelFor(FixedPartitionsAmountPartitioner.class.getCanonicalName(), Level.ALL.getName());
    
    if (config.grab(PARTITIONS_PARAM)) {
      partitionQuantity = PARTITIONS_PARAM.getValue();
    }    
  }
  
  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.Partitioner#makePairings(de.lmu.ifi.dbs.elki.database.Database, int)
   */
  @Override
  public final List<PartitionPairing> makePairings(Database<NumberVector<?, ?>> dataBase, int packageQuantity) throws UnableToComplyException {
    LOG.log(Level.INFO, "\tCreating partitions: " + partitionQuantity);
    
    return makePairings(dataBase, packageQuantity, partitionQuantity);
  }
  
  public abstract List<PartitionPairing> makePairings(Database<NumberVector<?, ?>> dataBase, int packageQuantity, int partitionQuantity) throws UnableToComplyException;

}
