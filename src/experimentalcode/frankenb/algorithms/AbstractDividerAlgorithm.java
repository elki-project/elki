package experimentalcode.frankenb.algorithms;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import experimentalcode.frankenb.model.DataBaseDataSet;
import experimentalcode.frankenb.model.PartitionPairing;
import experimentalcode.frankenb.model.ifaces.IDataSet;
import experimentalcode.frankenb.model.ifaces.IDividerAlgorithm;
import experimentalcode.frankenb.model.ifaces.IPartition;
import experimentalcode.frankenb.model.ifaces.IPartitionPairing;
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

  @Override
  public List<PartitionPairing> divide(Relation<? extends NumberVector<?, ?>> dataBase, int packageQuantity) throws UnableToComplyException {
    if(partitioning == null) {
      throw new UnableToComplyException("No partitioning strategy has been selected.");
    }
    if(pairing == null) {
      throw new UnableToComplyException("No partition pairing strategy has been selected.");
    }
    if(getLogger().isVerbose()) {
      getLogger().verbose("1. projection phase\n");
    }
    IDataSet dataSet = new DataBaseDataSet(dataBase);
    for(IProjection projection : projections) {
      if(getLogger().isVerbose()) {
        getLogger().verbose("projection using [" + projection.getClass().getSimpleName() + "]\n");
      }
      dataSet = projection.project(dataSet);
      if(getLogger().isVerbose()) {
        getLogger().verbose("dimension reduced to: " + dataSet.getDimensionality() + "\n");
        getLogger().verbose("\n");
      }
    }

    if(getLogger().isVerbose()) {
      getLogger().verbose("2. partitioning phase\n");
      getLogger().verbose("\tpartitioning using [" + partitioning.getClass().getSimpleName() + "]\n");
    }
    List<IPartition> partitions = partitioning.makePartitions(dataSet, packageQuantity);
    if(getLogger().isVerbose()) {
      getLogger().verbose("\n");
    }

    if(getLogger().isVerbose()) {
      getLogger().verbose("3. pairing phase\n");
      getLogger().verbose("\tpairing using [" + pairing.getClass().getSimpleName() + "]\n");
    }
    return pairing.makePairings(dataSet, partitions, packageQuantity);
  }

  abstract Logging getLogger();
}