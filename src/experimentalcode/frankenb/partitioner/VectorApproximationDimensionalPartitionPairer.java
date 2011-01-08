/**
 * 
 */
package experimentalcode.frankenb.partitioner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import experimentalcode.frankenb.model.PartitionPairing;
import experimentalcode.frankenb.model.ifaces.IPositionedPartition;
import experimentalcode.frankenb.model.ifaces.IPositionedPartitionPairer;

/**
 * This class represents the pairer for the linear scaling partitioning algorithm
 * <code>VectorApproximationDimensionalPartitioner</code>. This implementation normally
 * just makes sense to use with this class.
 * 
 * @author Florian Frankenberger
 */
public class VectorApproximationDimensionalPartitionPairer implements IPositionedPartitionPairer {

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.IPartitionPairer#makePairings(de.lmu.ifi.dbs.elki.database.Database, java.util.List, int)
   */
  @Override
  public List<PartitionPairing> makePairings(Database<NumberVector<?, ?>> dataBase, List<IPositionedPartition> partitions, int packageQuantity) throws UnableToComplyException {
    Collections.sort(partitions, new Comparator<IPositionedPartition>() {

      @Override
      public int compare(IPositionedPartition o1, IPositionedPartition o2) {
        for (int i = o1.getDimensionality(); i >= 1; --i) {
          if (o1.getPosition(i) != o2.getPosition(i)) {
            return Integer.valueOf(o1.getPosition(i)).compareTo(o2.getPosition(i));
          }
        }
        return Integer.valueOf(o1.hashCode()).compareTo(o2.hashCode());
      }
      
    });

    List<PartitionPairing> pairings = new ArrayList<PartitionPairing>();
    int partitionsPerDimension = partitions.size() / dataBase.dimensionality();
    
    int radius = 1;
    for (int i = 0; i < partitions.size(); ++i) {
      int dimension = i / partitionsPerDimension;
      IPositionedPartition partition = partitions.get(i);
      int position = i % partitionsPerDimension;
      if (partition.getPosition(dimension + 1) != position +  1) throw new UnableToComplyException("This class should only be used with VectorApproximationDimensionalPartitioner");
      
      // we just view at the partitions within the radius in one direction because the other
      // pairing has already been added by our predecessor
      for (int j = 0; j <= radius; ++j) {
        if (position + j >= partitionsPerDimension) continue;
        pairings.add(new PartitionPairing(partition, partitions.get(i + j)));
      }
    }
    
    return pairings;
  }

}
