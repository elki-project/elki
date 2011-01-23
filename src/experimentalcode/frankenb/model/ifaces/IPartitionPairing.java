/**
 * 
 */
package experimentalcode.frankenb.model.ifaces;

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public interface IPartitionPairing {

  /**
   * When called requires you to return a list of PartitionPairings that should be 
   * calculated on the cluster. The partitions get automatically stored at the
   * right directories. The package quantity is just a hint for this algorithm and
   * can be ignored if not needed. If less than packageQuantity PartitionPairings are
   * returned, only as many as there are PartitionPairings packages get generated.
   * 
   * @param dataSet
   * @param partitions
   * @param packageQuantity
   * @return
   */
  public void makePairings(IDataSet dataSet, List<IPartition> partitions, IPartitionPairingStorage partitionPairingStorage, int packageQuantity) throws UnableToComplyException;
  
}
