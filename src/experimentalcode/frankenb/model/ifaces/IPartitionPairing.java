package experimentalcode.frankenb.model.ifaces;

import java.util.List;

import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import experimentalcode.frankenb.algorithms.partitioning.DBIDPartition;
import experimentalcode.frankenb.model.PartitionPairing;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public interface IPartitionPairing<V> {
  /**
   * When called requires you to return a list of PartitionPairings that should
   * be calculated on the cluster. The partitions get automatically stored at
   * the right directories. The package quantity is just a hint for this
   * algorithm and can be ignored if not needed. If less than packageQuantity
   * PartitionPairings are returned, only as many as there are PartitionPairings
   * packages get generated.
   * 
   * @param dataSet
   * @param partitions
   * @param packageQuantity
   * @return
   */
  public List<PartitionPairing> makePairings(Relation<? extends V> dataSet, List<DBIDPartition> partitions, int packageQuantity) throws UnableToComplyException;
}
