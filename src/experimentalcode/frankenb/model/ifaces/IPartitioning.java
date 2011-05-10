package experimentalcode.frankenb.model.ifaces;

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;

/**
 * The implementor of this class should generate partitions and arrange their pairing. Normally
 * this is done by an additional <code>IPartitionPairer</code> that is used by the implementor.
 * 
 * @author Florian Frankenberger
 */
public interface IPartitioning {

  /**
   * Creates the partitions
   * 
   * @param dataSet
   * @param packageQuantity
   * @return a list of partitions
   * @throws UnableToComplyException
   */
  public List<IPartition> makePartitions(IDataSet dataSet, int packageQuantity) throws UnableToComplyException;
  
}
