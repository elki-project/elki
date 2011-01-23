/**
 * 
 */
package experimentalcode.frankenb.model.ifaces;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;

/**
 * Every algorithm that implements this interface
 * can be used to divide the data into pairings
 * 
 * @author Florian Frankenberger
 */
public interface IDividerAlgorithm {

  public void divide(Database<NumberVector<?, ?>> dataBase, IPartitionPairingStorage partitionPairingStorage, int packageQuantity) throws UnableToComplyException;
  
}
