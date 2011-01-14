/**
 * 
 */
package experimentalcode.frankenb.partitioner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import experimentalcode.frankenb.model.BufferedDiskBackedPartition;
import experimentalcode.frankenb.model.ifaces.IPartition;

/**
 * This class creates partitions by separating the vector-space rather than
 * the data and therefore overcoming the dimensional curse.
 * 
 * @author Florian Frankenberger
 */
@Reference(authors = "S. Blott and Roger Weber", title = "A Simple Vector-Approximation File for Similarity Search in High-Dimensional Vector Spaces", booktitle = "?")
public class VectorApproximationPartitioner extends PartitionPairerPartitioner {

  private static final Logging LOG = Logging.getLogger(VectorApproximationPartitioner.class);
  
  private static class OrderItem implements Comparable<OrderItem>{
    
    DBID dbid;
    double value;
    
    public OrderItem(DBID dbid, double value) {
      this.dbid = dbid;
      this.value = value;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(OrderItem o) {
      if (this.value > o.value) {
        return -1;
      } else 
      if (this.value < o.value) {
        return +1;
      }
      return this.dbid.compareTo(o.dbid);
    }
  }
  
  /**
   * @param config
   */
  public VectorApproximationPartitioner(Parameterization config) {
    super(config);
    LoggingConfiguration.setLevelFor(VectorApproximationPartitioner.class.getCanonicalName(), Level.ALL.getName());
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.Partitioner#makePartitions(de.lmu.ifi.dbs.elki.database.Database, int)
   */
  @Override
  public List<IPartition> makePartitions(Database<NumberVector<?, ?>> dataBase, int packageQuantity, int partitionQuantity) throws UnableToComplyException {
    
    int partitionsPerDimension = (int) Math.floor(Math.pow(partitionQuantity, 1 / (float) dataBase.dimensionality()));
    int itemsPerPartitionAndDimension = (int) Math.floor(dataBase.size() / (float) partitionsPerDimension);
    
    if (partitionsPerDimension < 2) {
      throw new UnableToComplyException("This dataset has " + dataBase.dimensionality() + " dimensions - so there will only be " + partitionsPerDimension + " partitions per dimension - that is too less. Please try increasing the partition quantity.");
    }
    
    int addItemsUntilPartition = dataBase.size() % partitionsPerDimension;
    int actualPartitionQuantity = (int) Math.pow(partitionsPerDimension, dataBase.dimensionality());
    LOG.log(Level.INFO, "\tPartitions that will be actually created: " + actualPartitionQuantity);
    LOG.log(Level.INFO, "\tItems per partition and dimension (not only per partition!): " + itemsPerPartitionAndDimension);
    
    Map<PartitionPosition, IPartition> partitions = new HashMap<PartitionPosition, IPartition>();
    //create the empty partitions
    /*LOG.log(Level.INFO, "Creating empty partitions ...");
    for (int i = 0; i < actualPartitionQuantity; ++i) {
      int[] position = new int[dataBase.dimensionality()];
      int acPosition = i;
      for (int j = position.length - 1; j >= 0; --j) {
        int partitionsDivider = (int) Math.pow(partitionsPerDimension, j);
        
        position[j] = (int) Math.floor(acPosition / (float) partitionsDivider);
        acPosition = acPosition % partitionsDivider;
      }
      
      partitions.put(new PartitionPosition(position), new BufferedDiskBackedPartition(dataBase.dimensionality()));
    } */   
    
    //set the cutting points
    LOG.log(Level.INFO, "Calculating the partitions dimensions ...");
    Map<DBID, PartitionPosition> dbEntriesPositions = new HashMap<DBID, PartitionPosition>();
    Map<Integer, List<Double>> dimensionalCuttingPoints = new HashMap<Integer, List<Double>>();
    for (int j = 1; j <= dataBase.dimensionality(); ++j) {
      List<OrderItem> dimensionalOrderedItems = new ArrayList<OrderItem>(dataBase.size());
      for (DBID dbid : dataBase) {
        dimensionalOrderedItems.add(new OrderItem(dbid, dataBase.get(dbid).doubleValue(j)));
      }
      
      List<Double> cuttingPoints = new ArrayList<Double>();
      Collections.sort(dimensionalOrderedItems);
      int counter = 0;
      int position = 0;
      for (OrderItem orderItem : dimensionalOrderedItems) {
        if (++counter > itemsPerPartitionAndDimension + (cuttingPoints.size() < addItemsUntilPartition ? 1 : 0)) {
          cuttingPoints.add(orderItem.value);
          counter = 1;
          position++;
        }
        PartitionPosition entryPosition = dbEntriesPositions.get(orderItem.dbid);
        if (entryPosition == null) {
          entryPosition = new PartitionPosition(new int[dataBase.dimensionality()]);
          dbEntriesPositions.put(orderItem.dbid, entryPosition);
        }
        entryPosition.setPosition(j - 1, position);
      }

      dimensionalCuttingPoints.put(j, cuttingPoints);
      System.out.println("Cutting points for dimension " + j + " are: " + cuttingPoints);
      
    }

    LOG.log(Level.INFO, "Now populating ...");
    //now we populate the partitions
    for (Entry<DBID, PartitionPosition> entry: dbEntriesPositions.entrySet()) {
      NumberVector<?, ?> vector = dataBase.get(entry.getKey());
      IPartition partition = partitions.get(entry.getValue());
      if (partition == null) {
        partition = new BufferedDiskBackedPartition(partitions.size(), dataBase.dimensionality());
        partitions.put(entry.getValue(), partition);
      }
      partition.addVector(entry.getKey().getIntegerID(), vector);
//      System.out.println(entry.getKey() + " was added to partition " + entry.getValue());
    }
    
    System.out.println("Actually created partitions: " + partitions.size());
//    for (Entry<PartitionPosition, Partition> entry : partitions.entrySet()) {
//      System.out.println(entry.getKey() + ": " + entry.getValue().getSize() + " items");
//    }
    
    LOG.log(Level.INFO, "done.");
    
    return new ArrayList<IPartition>(partitions.values());
  }
  
}
