/**
 * 
 */
package experimentalcode.frankenb.main;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.application.StandAloneApplication;
import de.lmu.ifi.dbs.elki.application.StandAloneInputApplication;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.RawDoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.OnDiskArray;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.frankenb.model.PackageDescriptor;
import experimentalcode.frankenb.model.Partition;

/**
 * This class calculates the distances between the given packages and creates
 * a result file that can be used to aggregate a list of precalculated knn
 * values
 * 
 * @author Florian Frankenberger
 */
public class KnnDataProcessor extends StandAloneInputApplication {

  private static final Logging LOG = Logging.getLogger(KnnDataProcessor.class);
  
  private static final int RESULT_MAGIC = 830831;
  
  /**
   * The distance function to determine the reachability distance between
   * database objects.
   */
  public static final OptionID REACHABILITY_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("knn.reachdistfunction", "Distance function to determine the reachability distance between database objects.");
  
  private final RawDoubleDistance<NumberVector<?, ?>> distance;
  private final Comparator<Pair<Integer, Double>> resultComparator = new Comparator<Pair<Integer, Double>>() {

    @Override
    public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
      return o1.getSecond().compareTo(o2.getSecond());
    }
    
  };
  
  /**
   * @param config
   */
  public KnnDataProcessor(Parameterization config) {
    super(config);
    config = config.descend(this);
    distance = getParameterReachabilityDistanceFunction(config);
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.application.StandAloneApplication#getOutputDescription()
   */
  @Override
  public String getOutputDescription() {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.application.AbstractApplication#run()
   */
  @Override
  public void run() throws UnableToComplyException {
    try {
      
      PackageDescriptor packageDescriptor = PackageDescriptor.loadFromFile(getInput());
      
      int counter = 0;
      long items = 0;
      long startTime = System.currentTimeMillis();
      
      for (Pair<File, File> pairing : packageDescriptor.getPartitionPairings()) {
        LOG.log(Level.INFO, String.format("Processing pairing %03d of %03d", counter+1, packageDescriptor.getPartitionPairings().size()));
        
        Partition partitionOne = Partition.loadFromFile(packageDescriptor.getDimensionality(), pairing.first);
        Partition partitionTwo = Partition.loadFromFile(packageDescriptor.getDimensionality(), pairing.second);
        OnDiskArray result = new OnDiskArray(
              new File(this.getOutput(), String.format("p%03d_result_%02d.dat", packageDescriptor.getId(), counter++)), 
              RESULT_MAGIC, 
              2 * (Integer.SIZE / 8), //we store the partitionOne and partitionTwo sizes
              //one int to store the id of the other data set and one double for the distance and one to store the partitionOne's id
              partitionTwo.getSize() * (Integer.SIZE / 8 + Double.SIZE / 8) + Integer.SIZE / 8, 
              partitionOne.getSize()
            );
        
        ByteBuffer header = result.getExtraHeader();
        header.putInt(partitionOne.getSize());
        header.putInt(partitionTwo.getSize());
        
        int bufferCounter = 0;
        for (Pair<Integer, NumberVector<?, ?>> entryOne : partitionOne) {
          
          List<Pair<Integer, Double>> results = new ArrayList<Pair<Integer, Double>>();
          for (Pair<Integer, NumberVector<?, ?>> entryTwo : partitionTwo) {
            double aDistance = distance.doubleDistance(entryOne.second, entryTwo.second);
            results.add(new Pair<Integer, Double>(entryTwo.first, aDistance));
            items++;
          }
          
          if (bufferCounter % 10 == 0) {
            LOG.log(Level.INFO, "\tBuffer " + bufferCounter + " ...");
          }
          
          //store to result buffer
          Collections.sort(results, resultComparator);
          ByteBuffer buffer = result.getRecordBuffer(bufferCounter++);
          
          //store id of partition one
          buffer.putInt(entryOne.first);
          
          for (Pair<Integer, Double> resultPair : results) {
            buffer.putInt(resultPair.first);
            buffer.putDouble(resultPair.second);
          }
          
        }
        
      }
      
      LOG.log(Level.INFO, String.format("Calculated %d distances in %d seconds", items, (System.currentTimeMillis() - startTime) / 1000));
      
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new UnableToComplyException(e);
    }

  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.application.StandAloneInputApplication#getInputDescription()
   */
  @Override
  public String getInputDescription() {
    // TODO Auto-generated method stub
    return "The package descriptor (usually an .xml-file)";
  }
  
  /**
   * Grab the reachability distance configuration option.
   * 
   * @param <F> distance function type
   * @param config Parameterization
   * @return Parameter value or null.
   */
  protected static <F extends RawDoubleDistance<NumberVector<?, ?>>> F getParameterReachabilityDistanceFunction(Parameterization config) {
    final ObjectParameter<F> param = new ObjectParameter<F>(REACHABILITY_DISTANCE_FUNCTION_ID, RawDoubleDistance.class, true);
    if(config.grab(param)) {
      return param.instantiateClass(config);
    }
    return null;
  }  
  
  public static void main(String[] args) {
    StandAloneApplication.runCLIApplication(KnnDataProcessor.class, args);
  }  

}
