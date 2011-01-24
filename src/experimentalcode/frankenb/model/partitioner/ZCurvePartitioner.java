/**
 * 
 */
package experimentalcode.frankenb.model.partitioner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.frankenb.log.Log;
import experimentalcode.frankenb.model.DiskBackedPartition;
import experimentalcode.frankenb.model.ifaces.IDataSet;
import experimentalcode.frankenb.model.ifaces.IPartition;
import experimentalcode.frankenb.model.ifaces.IPartitioning;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class ZCurvePartitioner implements IPartitioning {

  public static final OptionID BITS_PER_DIMENSION_ID = OptionID.getOrCreateOptionID("bitsperdimension", "how many bits per dimension should be used (gives 2^bitsperdimension partitions per dimension)");
  private final IntParameter BITS_PER_DIMENSION_PARAM = new IntParameter(BITS_PER_DIMENSION_ID, 3);
  
  private int bitsPerDimension;
  
  public ZCurvePartitioner(Parameterization config) {
    if (config.grab(BITS_PER_DIMENSION_PARAM)) {
      bitsPerDimension = BITS_PER_DIMENSION_PARAM.getValue();
    }
  }
  
  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.IPartitioning#makePartitions(experimentalcode.frankenb.model.ifaces.IDataSet, int)
   */
  @Override
  public List<IPartition> makePartitions(IDataSet dataSet, int packageQuantity) throws UnableToComplyException {
    int segmentsPerDimension = (int) Math.pow(2, this.bitsPerDimension);
    long totalPartitions = (long) Math.pow(segmentsPerDimension, dataSet.getDimensionality());
    
    if (totalPartitions > Integer.MAX_VALUE) throw new RuntimeException("Too many segments!");
    try {
      //analyse the extends in every dimmension
      Log.info("partitions to create: " + totalPartitions);
      Log.info("analysing data space ...");
      
      List<Pair<Double, Double>> minMaxList = new ArrayList<Pair<Double, Double>>(dataSet.getDimensionality());
      for (int id : dataSet.getIDs()) {
        NumberVector<?, ?> vector = dataSet.get(id);
        for (int dim = 1; dim <= vector.getDimensionality(); ++dim) {
          double component = vector.doubleValue(dim);
          
          if (minMaxList.size() < dim) {
            minMaxList.add(new Pair<Double, Double>(0.0, 0.0));
          }
          
          Pair<Double, Double> minMax = minMaxList.get(dim - 1);
          if (component < minMax.first) {
            minMax.first = component;
          } else
            if (component > minMax.second) {
              minMax.second = component;
            }
        }
      }
      
      
      Log.info("space extents in all dimensions:");
      int counter = 0;
      List<Double> dimPartWidths = new ArrayList<Double>();
      for (Pair<Double, Double> minMax : minMaxList) {
        Log.info("Dimension " + (++counter) + ": [" + minMax.first + " - " + minMax.second + "]");
        dimPartWidths.add((minMax.second - minMax.first) / (double) segmentsPerDimension);
      }
      
      Map<Integer, IPartition> partitions = new LinkedHashMap<Integer, IPartition>();
      
      Log.info("generating " + totalPartitions + " partitions ...");
      
      for (int i = 0; i < totalPartitions; ++i) {
        partitions.put(i, new DiskBackedPartition(i, dataSet.getDimensionality()));
      }
      
      counter = 0;
      int tenPercent = dataSet.getSize() / 10;
      for (int id : dataSet.getIDs()) {
        if (counter++ % tenPercent == 0) {
          Log.info(" processed " + counter + " items of " + dataSet.getSize() + " ...");
        }
        NumberVector<?, ?> vector = dataSet.get(id);
        int position = 0;
        for (int dim = 1; dim <= dataSet.getDimensionality(); ++dim) {
          Pair<Double, Double> minMax = minMaxList.get(dim - 1);
          double value = vector.doubleValue(dim) - minMax.first;
          double dimPartWidth = dimPartWidths.get(dim - 1);
          int dimPosition = (int) Math.floor(value / dimPartWidth);
          
          //for the only case where the max item for that dimension is calculated
          if (dimPosition > segmentsPerDimension - 1) {
            dimPosition = segmentsPerDimension - 1;
          }
          
          dimPosition = (dimPosition << ((dim - 1) * this.bitsPerDimension));
          position = position | dimPosition;
        }
        
        IPartition partition = partitions.get(position);
        partition.addVector(id, vector);
      }
      
      return new ArrayList<IPartition>(partitions.values());
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new UnableToComplyException(e);
    }
  }
  
}
