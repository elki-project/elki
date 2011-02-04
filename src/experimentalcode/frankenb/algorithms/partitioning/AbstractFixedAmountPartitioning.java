/**
 * 
 */
package experimentalcode.frankenb.algorithms.partitioning;

import java.util.List;
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import experimentalcode.frankenb.log.Log;
import experimentalcode.frankenb.model.ifaces.IDataSet;
import experimentalcode.frankenb.model.ifaces.IPartition;
import experimentalcode.frankenb.model.ifaces.IPartitioning;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public abstract class AbstractFixedAmountPartitioning implements IPartitioning {

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
  
  public AbstractFixedAmountPartitioning(Parameterization config) {
    LoggingConfiguration.setLevelFor(AbstractFixedAmountPartitioning.class.getCanonicalName(), Level.ALL.getName());
    
    if (config.grab(PARTITIONS_PARAM)) {
      partitionQuantity = PARTITIONS_PARAM.getValue();
    }    
  }
  
  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.Partitioner#makePairings(de.lmu.ifi.dbs.elki.database.Database, int)
   */
  @Override
  public List<IPartition> makePartitions(IDataSet dataSet, int packageQuantity) throws UnableToComplyException {
    Log.info("partition quantity: " + partitionQuantity);
    
    return makePartitions(dataSet, packageQuantity, partitionQuantity);
  }
  
  protected abstract List<IPartition> makePartitions(IDataSet dataSet, int packageQuantity, int partitionQuantity) throws UnableToComplyException;

}
