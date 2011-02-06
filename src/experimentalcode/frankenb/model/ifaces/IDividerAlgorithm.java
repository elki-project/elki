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
 * Every algorithm that implements this interface
 * can be used to divide the data into pairings
 * 
 * @author Florian Frankenberger
 */
public interface IDividerAlgorithm {

  public List<PartitionPairing> divide(Database<NumberVector<?, ?>> dataBase, int packageQuantity) throws UnableToComplyException;
  
}
