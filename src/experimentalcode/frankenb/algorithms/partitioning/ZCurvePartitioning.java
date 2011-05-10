package experimentalcode.frankenb.algorithms.partitioning;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.frankenb.model.BufferedDiskBackedPartition;
import experimentalcode.frankenb.model.ifaces.IDataSet;
import experimentalcode.frankenb.model.ifaces.IPartition;
import experimentalcode.frankenb.utils.ZCurve;

/**
 * This class orders the items of the data set according to their z-curve value
 * and splits them in ascending order to their z-curve value into a given amount of partitions. 
 * 
 * @author Florian Frankenberger
 */
public class ZCurvePartitioning extends AbstractFixedAmountPartitioning {
  /**
   * Logger
   */
  private static final Logging logger = Logging.getLogger(ZCurvePartitioning.class);

  public ZCurvePartitioning(Parameterization config) {
    super(config);
  }
  
  @Override
  protected List<IPartition> makePartitions(IDataSet dataSet, int packageQuantity, int partitionQuantity) throws UnableToComplyException {
    try {
      List<Pair<DBID, BigInteger>> projection = ZCurve.projectToZCurve(dataSet);
      
      Collections.sort(projection, new Comparator<Pair<DBID, BigInteger>>() {

        @Override
        public int compare(Pair<DBID, BigInteger> o1, Pair<DBID, BigInteger> o2) {
          int result = o1.second.compareTo(o2.second);
          if (result == 0) {
            result = o1.first.compareTo(o2.first);
          }
          return result;
        }
        
      });

      int itemsPerPartition = dataSet.getSize() / partitionQuantity;
      int addItemsUntilPartition = dataSet.getSize() % partitionQuantity;
      
      logger.verbose(String.format("Items per partition about: %d", itemsPerPartition));
      
      Iterator<Pair<DBID, BigInteger>> projectionIterator = projection.iterator();
      List<IPartition> partitions = new ArrayList<IPartition>();
      for (int i = 0; i < partitionQuantity; ++i) {
        IPartition partition = new BufferedDiskBackedPartition(i, dataSet.getDimensionality());
        for (int j = 0; j < itemsPerPartition + (i + 1 < addItemsUntilPartition ? 1 : 0); ++j) {
          DBID id = projectionIterator.next().first;
          partition.addVector(id, dataSet.getOriginal().get(id));
        }
        logger.verbose(String.format("\tCreated partition %d with %d items.", partition.getId(), partition.getSize()));
        partitions.add(partition);
      }
      
      return partitions;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new UnableToComplyException(e);
    }
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}