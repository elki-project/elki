package experimentalcode.frankenb.algorithms.partitioning;

import java.util.List;

import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;

/**
 * The implementor of this class should generate partitions and arrange their
 * pairing. Normally this is done by an additional <code>IPartitionPairer</code>
 * that is used by the implementor.
 * 
 * @author Florian Frankenberger
 */
public interface IPartitioning<V> {
  /**
   * Creates the partitions
   * 
   * @param dataSet
   * @return a list of partitions
   * @throws UnableToComplyException
   */
  public List<DBIDPartition> makePartitions(Relation<? extends V> dataSet) throws UnableToComplyException;
}