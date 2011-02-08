/**
 * 
 */
package experimentalcode.frankenb.main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.application.StandAloneApplication;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.RawDoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.frankenb.log.Log;
import experimentalcode.frankenb.log.LogLevel;
import experimentalcode.frankenb.log.StdOutLogWriter;
import experimentalcode.frankenb.log.TraceLevelLogFormatter;
import experimentalcode.frankenb.model.DistanceList;
import experimentalcode.frankenb.model.DynamicBPlusTree;
import experimentalcode.frankenb.model.PackageDescriptor;
import experimentalcode.frankenb.model.PartitionPairing;
import experimentalcode.frankenb.model.datastorage.DiskBackedDataStorage;
import experimentalcode.frankenb.model.ifaces.IPartition;
import experimentalcode.frankenb.utils.Utils;

/**
 * This class calculates the distances between the given packages and creates
 * a result file that can be used to aggregate a list of precalculated knn
 * values
 * <p />
 * Example usage:
 * <br />
 * <code>-app.in D:/temp/knnparts/packagep00000/package00000_descriptor.xml -knn.reachdistfunction  EuclideanDistanceFunction</code>
 * 
 * @author Florian Frankenberger
 */
public class KnnDataProcessor extends AbstractApplication {

  /**
   * OptionID for {@link #INPUT_PARAM}
   */
  public static final OptionID INPUT_ID = OptionID.getOrCreateOptionID("app.in", "");
  public static final OptionID MULTI_THREADING_ID = OptionID.getOrCreateOptionID("multithreading", "tells wether to use as much threads as cpus are available or not (default is false)");

  /**
   * Parameter that specifies the name of the input file.
   * <p>
   * Key: {@code -app.in}
   * </p>
   */
  private final FileParameter INPUT_PARAM = new FileParameter(INPUT_ID, FileParameter.FileType.INPUT_FILE);
  private final Flag MULTI_THREAD_PARAM = new Flag(MULTI_THREADING_ID);
  
  /**
   * OptionID for {@link #MAXK_PARAM}
   */
  public static final OptionID MAXK_ID = OptionID.getOrCreateOptionID("maxk", "");
  
  /**
   * Parameter that specifies the number of neighbors to keep with respect
   * to the definition of a k-nearest neighbor.
   * <p>
   * Key: {@code -k}
   * </p>
   */
  private final IntParameter MAXK_PARAM = new IntParameter(MAXK_ID, false);  
  
  /**
   * Holds the value of {@link #INPUT_PARAM}.
   */
  private File input;
  
  private int maxK;
  private boolean multiThreaded = false;
  
  /**
   * The distance function to determine the reachability distance between
   * database objects.
   */
  public static final OptionID REACHABILITY_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("knn.reachdistfunction", "Distance function to determine the reachability distance between database objects.");
  
  private final RawDoubleDistance<NumberVector<?, ?>> distance;
  
  /**
   * @param config
   */
  public KnnDataProcessor(Parameterization config) {
    super(config);

    Log.setLogFormatter(new TraceLevelLogFormatter());
    Log.addLogWriter(new StdOutLogWriter());
    Log.setFilter(LogLevel.INFO);
    
    config = config.descend(this);
    INPUT_PARAM.setShortDescription(getInputDescription());
    if (config.grab(INPUT_PARAM)) {
      input = INPUT_PARAM.getValue();
    }
    
    if (config.grab(MAXK_PARAM)) {
      maxK = MAXK_PARAM.getValue();
    }
    
    if (config.grab(MULTI_THREAD_PARAM)) {
      multiThreaded = MULTI_THREAD_PARAM.getValue();
    }
    
    distance = getParameterReachabilityDistanceFunction(config);
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.application.AbstractApplication#run()
   */
  @Override
  public void run() throws UnableToComplyException {
    try {
      
      Log.info("started processing");
      Log.info("multithreaded: " + Boolean.valueOf(multiThreaded));
      Log.info("maximum k to calculate: " + maxK);
      Log.info();
      Log.info(String.format("opening package %s ...", input));
      final PackageDescriptor packageDescriptor = PackageDescriptor.readFromStorage(new DiskBackedDataStorage(input));
      
      int counter = 0;
      long items = 0;

      //create a threadpool with that many processes that there are processors available
      Runtime runtime = Runtime.getRuntime();
      ExecutorService threadPool = Executors.newFixedThreadPool((multiThreaded ? runtime.availableProcessors() : 1));
      
      List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
     
      for (final PartitionPairing pairing : packageDescriptor) {
        if (!packageDescriptor.hasResult(pairing)) {
          final int taskId = ++counter;
          final DynamicBPlusTree<Integer, DistanceList> resultTree = packageDescriptor.getResultTreeFor(pairing);
          
          items += Utils.sumFormular(pairing.getPartitionOne().getSize() + pairing.getPartitionTwo().getSize());
          
          Callable<Boolean> task = new Callable<Boolean>() {
  
            @Override
            public Boolean call() throws Exception {
              try {
              Log.info(String.format("Processing pairing %010d of %010d...", taskId, packageDescriptor.getPairings()));
                
                //LOG.log(Level.INFO, String.format("Processing pairing %03d of %03d", counter+1, packageDescriptor.getPartitionPairings().size()));
                
                IPartition partitionOne = pairing.getPartitionOne();
                IPartition partitionTwo = pairing.getPartitionTwo();
                

                //one pass forward and one backward (each from partitionOne vs all of partitionTwo AND each from partitionTwo vs all of partitionOne) 
                for (int i = 0; i < 2; ++i) {
                  IPartition[] partitionsToCalculate = (i == 0 ? new IPartition[] { partitionOne, partitionTwo } : new IPartition[] { partitionTwo, partitionOne });
                  if (i == 1 && partitionOne.equals(partitionTwo)) continue;
                  
                  Log.info(String.format("\tPairing %010d: partition%05d (%d items) with partition%05d (%d items)", taskId, partitionsToCalculate[0].getId(), partitionsToCalculate[0].getSize(), partitionsToCalculate[1].getId(), partitionsToCalculate[1].getSize()));
                  for (Pair<Integer, NumberVector<?, ?>> entryOne : partitionsToCalculate[0]) {
                    DistanceList distanceList = resultTree.get(entryOne.first);
                    if (distanceList == null) {
                      distanceList = new DistanceList(entryOne.first, maxK);
                    }
                    for (Pair<Integer, NumberVector<?, ?>> entryTwo : partitionsToCalculate[1]) {
                      double aDistance = distance.doubleDistance(entryOne.second, entryTwo.second);
                      distanceList.addDistance(entryTwo.first, aDistance);
                    }
                    resultTree.put(entryOne.first, distanceList);
                  }
                  
                  
                }
                
                resultTree.close();
                
              } catch (Exception e) {
                Log.error(String.format("Problem in pairing %s: %s",pairing, e.getMessage()), e);
                return false;
              } finally {
                Log.info(String.format("Pairing %d done.", taskId));
              }
              return true;
            }
            
          };
          
          futures.add(threadPool.submit(task));
        }
      }
      
      //wait for all tasks to finish
      for (Future<Boolean> future : futures) {
        future.get();
      }
      
      threadPool.shutdown();
      
      if (futures.size() > 0) {
        //packageDescriptor.saveToFile(input);
        Log.info(String.format("Calculated and stored %d distances.", items));
      } else {
        Log.info("Nothing to do - all results have already been calculated");
      }
      
      
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new UnableToComplyException(e);
    }

  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.application.StandAloneInputApplication#getInputDescription()
   */
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
