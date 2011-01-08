/**
 * 
 */
package experimentalcode.frankenb.partitioner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import experimentalcode.frankenb.model.PositionedPartition;
import experimentalcode.frankenb.model.ifaces.IPositionedPartition;

/**
 * This class creates partitions by separating the vector-space rather than
 * the data and therefore overcoming the dimensional curse. This implementation
 * just separates the space for each dimension and therefore doesn't scale the
 * amount of partitions created exponentially but rather linearly.
 * 
 * @author Florian Frankenberger
 */
@Reference(authors = "S. Blott and Roger Weber", title = "A Simple Vector-Approximation File for Similarity Search in High-Dimensional Vector Spaces", booktitle = "?")
public class VectorApproximationDimensionalPartitioner extends PositionedPartitionPairerPartitioner {

  private static final Logging LOG = Logging.getLogger(VectorApproximationDimensionalPartitioner.class);
  
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
  public VectorApproximationDimensionalPartitioner(Parameterization config) {
    super(config);
    LoggingConfiguration.setLevelFor(VectorApproximationDimensionalPartitioner.class.getCanonicalName(), Level.ALL.getName());
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.Partitioner#makePartitions(de.lmu.ifi.dbs.elki.database.Database, int)
   */
  @Override
  public List<IPositionedPartition> makePartitions(Database<NumberVector<?, ?>> dataBase, int packageQuantity, int partitionQuantity) throws UnableToComplyException {
    
    int partitionsPerDimension = partitionQuantity / dataBase.dimensionality();
    int itemsPerPartitionAndDimension = (int) Math.floor(dataBase.size() / (float) partitionsPerDimension);
    
    if (partitionsPerDimension < 2) {
      throw new UnableToComplyException("This dataset has " + dataBase.dimensionality() + " dimensions - so there will only be " + partitionsPerDimension + " partitions per dimension - that is too less. Please try increasing the partition quantity.");
    }
    
    int addItemsUntilPartition = dataBase.size() % partitionsPerDimension;
    int actualPartitionQuantity = partitionsPerDimension * dataBase.dimensionality(); // <- !
    
    LOG.log(Level.INFO, "\tPartitions that will be actually created: " + actualPartitionQuantity + " (" + partitionsPerDimension + " partitions per dimension" + ")");
    LOG.log(Level.INFO, "\tItems per partition and dimension (not only per partition!): " + itemsPerPartitionAndDimension);
    
    Map<PartitionPosition, IPositionedPartition> partitions = new HashMap<PartitionPosition, IPositionedPartition>();
    
    //create the empty partitions
    LOG.log(Level.INFO, "Creating empty partitions ...");
    for (int i = 0; i < dataBase.dimensionality(); ++i) {
      for (int j = 1; j <= partitionsPerDimension; ++j) {
        PartitionPosition partitionPosition = new PartitionPosition(dataBase.dimensionality());
        partitionPosition.setPosition(i, j);
        PositionedPartition positionedPartition = new PositionedPartition(partitionPosition.getPosition(), new BufferedDiskBackedPartition(dataBase.dimensionality()));
        partitions.put(partitionPosition, positionedPartition);
      }
    }    
    
    //set the cutting points
    LOG.log(Level.INFO, "Calculating the partitions dimensions and populating ...");
    Map<Integer, List<Double>> dimensionalCuttingPoints = new HashMap<Integer, List<Double>>();
    for (int j = 1; j <= dataBase.dimensionality(); ++j) {
      List<OrderItem> dimensionalOrderedItems = new ArrayList<OrderItem>(dataBase.size());
      for (DBID dbid : dataBase) {
        dimensionalOrderedItems.add(new OrderItem(dbid, dataBase.get(dbid).doubleValue(j)));
      }
      
      List<Double> cuttingPoints = new ArrayList<Double>();
      Collections.sort(dimensionalOrderedItems);
      int counter = 0;
      int position = 1;
      for (OrderItem orderItem : dimensionalOrderedItems) {
        if (++counter > itemsPerPartitionAndDimension + (cuttingPoints.size() < addItemsUntilPartition ? 1 : 0)) {
          cuttingPoints.add(orderItem.value);
          counter = 1;
          position++;
        }
        
        PartitionPosition entryPosition = new PartitionPosition(new int[dataBase.dimensionality()]);
        entryPosition.setPosition(j - 1, position);
        
        partitions.get(entryPosition).addVector(orderItem.dbid.getIntegerID(), dataBase.get(orderItem.dbid));
      }

      dimensionalCuttingPoints.put(j, cuttingPoints);
      System.out.println("Cutting points for dimension " + j + " are: " + cuttingPoints);
      
    }

    
    System.out.println("Actually created partitions: " + partitions.size());
    
    LOG.log(Level.INFO, "done.");
    
    return new ArrayList<IPositionedPartition>(partitions.values());
  }
  
}
