/**
 * 
 */
package experimentalcode.frankenb.model.ifaces;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import experimentalcode.frankenb.model.PartitionPairing;

/**
 * The implementor of this class should generate partitions and arrange their pairing. Normally
 * this is done by an additional <code>IPartitionPairer</code> that is used by the implementor.
 * 
 * @author Florian Frankenberger
 */
public interface IPartitioner {

  /**
   * Creates the partitions according to the implemented algorithm.
   * @param dataBase
   * @param packageQuantity
   * @return a list of partitions to be paired by a ParitionPairer
   * @throws UnableToComplyException
   */
  public List<PartitionPairing> makePartitionPairings(Database<NumberVector<?, ?>> dataBase, int packageQuantity) throws UnableToComplyException;
  
}
