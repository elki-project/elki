package experimentalcode.frankenb.algorithms.partitioning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import experimentalcode.frankenb.model.BufferedDiskBackedPartition;
import experimentalcode.frankenb.model.PositionedPartition;
import experimentalcode.frankenb.model.ifaces.IDataSet;
import experimentalcode.frankenb.model.ifaces.IPartition;
import experimentalcode.frankenb.model.ifaces.IPartitioning;

/**
 * This class creates partitions by separating the vector-space rather than the
 * data. The whole vector-space is splitted into grid cells which then form
 * the partitions. If a grid cell remains empty it will not be returned by this
 * algorithm.
 * <p/>
 * Note: This implementation returns {@link PositionedPartition}s which provide additional
 * information to the pairing algorithm if needed.
 * 
 * @author Florian Frankenberger
 */
public class GridPartitioning implements IPartitioning {
  /**
   * Logger
   */
  private static final Logging logger = Logging.getLogger(GridPartitioning.class);

  public static final OptionID SECTORS_ID = OptionID.getOrCreateOptionID("sectorsperdimension", "Amount of sectors per dimension");
  
  private final IntParameter SECTORS_PARAM = new IntParameter(SECTORS_ID, false);    
  
  private static class OrderItem implements Comparable<OrderItem> {

    int dbid;

    double value;

    public OrderItem(int dbid, double value) {
      this.dbid = dbid;
      this.value = value;
    }

    @Override
    public int compareTo(OrderItem o) {
      if(this.value > o.value) {
        return -1;
      }
      else if(this.value < o.value) {
        return +1;
      }
      return Integer.valueOf(dbid).compareTo(o.dbid);
    }
  }

  private static class PartitionPosition {
    private final int[] position;

    private int hashCode;

    private boolean tainted = true;

    /*
     * public PartitionPosition(int dimensonality) { this.position = new
     * int[dimensonality]; for (int i = 0; i < this.position.length; ++i) {
     * this.position[i] = 0; } }
     */

    public PartitionPosition(int[] position) {
      this.position = position;
    }

    public void setPosition(int dimension, int position) {
      this.position[dimension] = position;
      tainted = true;
    }

    @Override
    public int hashCode() {
      if(tainted) {
        hashCode = 0;
        for(int i = 0; i < position.length; ++i) {
          hashCode ^= this.position[i];
        }
        tainted = false;
      }
      return hashCode;
    }

    @Override
    public boolean equals(Object o) {
      if(!(o instanceof PartitionPosition)) {
        return false;
      }
      PartitionPosition other = (PartitionPosition) o;
      if(this.hashCode() != other.hashCode)
        return false;
      if(this.position.length != other.position.length)
        return false;
      for(int i = 0; i < this.position.length; ++i) {
        if(this.position[i] != other.position[i])
          return false;
      }
      return true;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("position [");
      boolean first = true;
      for(int aPosition : position) {
        if(first) {
          first = false;
        }
        else {
          sb.append(", ");
        }
        sb.append(aPosition);
      }
      sb.append("]");
      return sb.toString();
    }

    /**
     * @return
     */
    public int[] getPosition() {
      return position;
    }
  }

  private int sectors = 0;
  
  /**
   * @param config
   */
  public GridPartitioning(Parameterization config) {
    if (config.grab(SECTORS_PARAM)) {
      this.sectors = SECTORS_PARAM.getValue();
      if (sectors < 1) throw new RuntimeException("Sectors need to be > 0");
    }
  }

  @Override
  public List<IPartition> makePartitions(IDataSet dataSet, int packageQuantity) throws UnableToComplyException {

    int itemsPerSectorAndDimension = (int) Math.floor(dataSet.getSize() / (float) sectors);

    int addItemsUntilPartition = dataSet.getSize() % sectors;
    int actualPartitionQuantity = (int) Math.pow(sectors, dataSet.getDimensionality());
    logger.verbose("Partitions that will be actually created: " + actualPartitionQuantity);
    logger.verbose("Items per partition and dimension (not only per partition!): " + itemsPerSectorAndDimension);

    // set the cutting points
    logger.verbose("Calculating the partitions dimensions ...");
    Map<Integer, PartitionPosition> dbEntriesPositions = new HashMap<Integer, PartitionPosition>();
    Map<Integer, List<Double>> dimensionalCuttingPoints = new HashMap<Integer, List<Double>>();
    for(int dim = 1; dim <= dataSet.getDimensionality(); ++dim) {
      List<OrderItem> dimensionalOrderedItems = new ArrayList<OrderItem>(dataSet.getSize());
      for(int dbid : dataSet.getIDs()) {
        dimensionalOrderedItems.add(new OrderItem(dbid, dataSet.get(dbid).doubleValue(dim)));
      }

      List<Double> cuttingPoints = new ArrayList<Double>();
      Collections.sort(dimensionalOrderedItems);
      int counter = 0;
      int position = 0;
      for(OrderItem orderItem : dimensionalOrderedItems) {
        if(++counter > itemsPerSectorAndDimension + (cuttingPoints.size() < addItemsUntilPartition ? 1 : 0)) {
          cuttingPoints.add(orderItem.value);
          counter = 1;
          position++;
        }
        PartitionPosition entryPosition = dbEntriesPositions.get(orderItem.dbid);
        if(entryPosition == null) {
          entryPosition = new PartitionPosition(new int[dataSet.getDimensionality()]);
          dbEntriesPositions.put(orderItem.dbid, entryPosition);
        }
        entryPosition.setPosition(dim - 1, position);
      }

      dimensionalCuttingPoints.put(dim, cuttingPoints);
      logger.debug("Cutting points for dimension " + dim + " are: " + cuttingPoints);

    }

    Map<PartitionPosition, IPartition> partitions = new HashMap<PartitionPosition, IPartition>();

    logger.verbose("Now populating the partitions ...");
    // now we populate the partitions
    for(Entry<Integer, PartitionPosition> entry : dbEntriesPositions.entrySet()) {
      NumberVector<?, ?> vector = dataSet.getOriginal().get(entry.getKey());
      IPartition partition = partitions.get(entry.getValue());
      if(partition == null) {
        partition = new PositionedPartition(entry.getValue().getPosition(), new BufferedDiskBackedPartition(partitions.size(), dataSet.getDimensionality()));
        partitions.put(entry.getValue(), partition);
      }
      partition.addVector(entry.getKey(), vector);
    }

    logger.verbose("Actually created partitions: " + partitions.size());
    return new ArrayList<IPartition>(partitions.values());
  }

}
