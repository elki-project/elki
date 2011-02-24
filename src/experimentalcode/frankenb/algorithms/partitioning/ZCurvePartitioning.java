/**
 * 
 */
package experimentalcode.frankenb.algorithms.partitioning;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.frankenb.log.Log;
import experimentalcode.frankenb.model.BufferedDiskBackedPartition;
import experimentalcode.frankenb.model.ifaces.IDataSet;
import experimentalcode.frankenb.model.ifaces.IPartition;
import experimentalcode.frankenb.utils.ZCurve;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class ZCurvePartitioning extends AbstractFixedAmountPartitioning {

  public ZCurvePartitioning(Parameterization config) {
    super(config);
  }
  
  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.IPartitioning#makePartitions(experimentalcode.frankenb.model.ifaces.IDataSet, int)
   */
  @Override
  protected List<IPartition> makePartitions(IDataSet dataSet, int packageQuantity, int partitionQuantity) throws UnableToComplyException {
    try {
      List<Pair<Integer, BigInteger>> projection = ZCurve.projectToZCurve(dataSet);
      
      Collections.sort(projection, new Comparator<Pair<Integer, BigInteger>>() {

        @Override
        public int compare(Pair<Integer, BigInteger> o1, Pair<Integer, BigInteger> o2) {
          int result = o1.second.compareTo(o2.second);
          if (result == 0) {
            result = o1.first.compareTo(o2.first);
          }
          return result;
        }
        
      });

      int itemsPerPartition = dataSet.getSize() / partitionQuantity;
      int addItemsUntilPartition = dataSet.getSize() % partitionQuantity;
      
      Log.info(String.format("Items per partition about: %d", itemsPerPartition));
      
      Iterator<Pair<Integer, BigInteger>> projectionIterator = projection.iterator();
      List<IPartition> partitions = new ArrayList<IPartition>();
      for (int i = 0; i < partitionQuantity; ++i) {
        IPartition partition = new BufferedDiskBackedPartition(i, dataSet.getDimensionality());
        for (int j = 0; j < itemsPerPartition + (i + 1 < addItemsUntilPartition ? 1 : 0); ++j) {
          int id = projectionIterator.next().first;
          partition.addVector(id, dataSet.getOriginal().get(id));
        }
        Log.info(String.format("\tCreated partition %d with %d items.", partition.getId(), partition.getSize()));
        partitions.add(partition);
      }
      
      return partitions;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new UnableToComplyException(e);
    }
  }

  
}
