/**
 * 
 */
package experimentalcode.frankenb.algorithms;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import experimentalcode.frankenb.log.Log;
import experimentalcode.frankenb.model.DataBaseDataSet;
import experimentalcode.frankenb.model.ifaces.IDataSet;
import experimentalcode.frankenb.model.ifaces.IDividerAlgorithm;
import experimentalcode.frankenb.model.ifaces.IPartition;
import experimentalcode.frankenb.model.ifaces.IPartitionPairing;
import experimentalcode.frankenb.model.ifaces.IPartitionPairingStorage;
import experimentalcode.frankenb.model.ifaces.IPartitioning;
import experimentalcode.frankenb.model.ifaces.IProjection;

/**
 * This is a simple abstract class that manages the following:
 * <p/>
 * <code>DB -> Projection1 -> ... -> ProjectionN -> Partitioning -> Pairing -> Result</code>
 * <p/>
 * Note: Projections can be empty.
 * 
 * @author Florian Frankenberger
 */
public abstract class AbstractDividerAlgorithm implements IDividerAlgorithm {

  private List<IProjection> projections = new ArrayList<IProjection>();
  private IPartitioning partitioning = null;
  private IPartitionPairing pairing = null;
  
  protected void addProjection(IProjection projection) {
    this.projections.add(projection);
  }
  
  protected void setPartitioning(IPartitioning partitioning) {
    this.partitioning = partitioning;
  }
  
  protected void setPairing(IPartitionPairing pairing) {
    this.pairing = pairing;
  }
  
  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.IDividerAlgorithm#divide(de.lmu.ifi.dbs.elki.database.Database, experimentalcode.frankenb.model.ifaces.IPartitionPairingStorage, int)
   */
  @Override
  public void divide(Database<NumberVector<?, ?>> dataBase, IPartitionPairingStorage partitionPairingStorage, int packageQuantity) throws UnableToComplyException {
    /*if (partitioning == null) throw new UnableToComplyException("No partitioning strategy has been selected.");
    if (pairing == null) throw new UnableToComplyException("No partition pairing strategy has been selected.");*/

    Log.info("1. projection phase");
    IDataSet dataSet = new DataBaseDataSet(dataBase);
    for (IProjection projection : projections) {
      Log.info("projection " + projection.getClass().getSimpleName() + " ...");
      dataSet = projection.project(dataSet);
      Log.info("dimension reduced to: " + dataSet.getDimensionality());
      Log.info();
    }
    
    Log.info("2. partitioning phase");
    Log.info("partitioning " + partitioning.getClass().getSimpleName());
    List<IPartition> partitions = partitioning.makePartitions(dataSet, packageQuantity);
    Log.info();
    
    Log.info("3. pairing phase");
    pairing.makePairings(dataSet, partitions, partitionPairingStorage, packageQuantity);
  }

}
