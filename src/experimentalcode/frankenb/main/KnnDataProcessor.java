/**
 * 
 */
package experimentalcode.frankenb.main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import experimentalcode.frankenb.model.ConstantSizeIntegerSerializer;
import experimentalcode.frankenb.model.DistanceList;
import experimentalcode.frankenb.model.DistanceListSerializer;
import experimentalcode.frankenb.model.DynamicBPlusTree;
import experimentalcode.frankenb.model.PackageDescriptor;
import experimentalcode.frankenb.model.PartitionPairing;
import experimentalcode.frankenb.model.datastorage.BufferedDiskBackedDataStorage;
import experimentalcode.frankenb.model.datastorage.DiskBackedDataStorage;
import experimentalcode.frankenb.model.ifaces.IPartition;

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
  
  private final RawDoubleDistance<NumberVector<?, ?>> distanceAlgorithm;
  private int totalTasks;
  private long totalItems;
  
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
    
    distanceAlgorithm = getParameterReachabilityDistanceFunction(config);
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.application.AbstractApplication#run()
   */
  @Override
  public void run() throws UnableToComplyException {
    Runtime runtime = Runtime.getRuntime();
    final ExecutorService threadPool = Executors.newFixedThreadPool((multiThreaded ? runtime.availableProcessors() : 1));
    
    try {
      Log.info("started processing");
      Log.info("multithreaded: " + Boolean.valueOf(multiThreaded));
      Log.info("maximum k to calculate: " + maxK);
      Log.info();
      Log.info(String.format("opening package %s ...", input));
      final PackageDescriptor packageDescriptor = PackageDescriptor.readFromStorage(new DiskBackedDataStorage(input));
      
      totalTasks = 0;
      totalItems = 0;

      //create a thread pool with that many processes that there are processors available
      
      List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
     
      for (final PartitionPairing pairing : packageDescriptor) {
        if (pairing.getPartitionOne().getSize() < 1 || pairing.getPartitionTwo().getSize() < 1) {
          throw new UnableToComplyException("Pairing " + pairing + " has 0 items");
        }
        
        if (pairing.hasResult()) {
          Log.info(String.format("Skipping pairing of partition%05d with partition%05d - as it already contains a result", pairing.getPartitionOne().getId(), pairing.getPartitionTwo().getId()));
          continue;
        }
        final int taskId = ++totalTasks;
       
        Callable<Boolean> task = new Callable<Boolean>() {

          @Override
          public Boolean call() throws Exception {
            try {
              Log.info(String.format("Processing pairing %010d of %010d (%010d in package)...", taskId, totalTasks, packageDescriptor.getPairings()));
              
              // heuristic to determine the bucket size based on a tree height of about 21
              int maxKeysPerBucket = (int) Math.max(5, Math.floor(Math.pow(pairing.getEstimatedUniqueIdsAmount(), 1f / 20f)));
              Log.info(String.format("maxKeysPerBucket in tree are: %,d for %,d items", maxKeysPerBucket, pairing.getEstimatedUniqueIdsAmount()));
              
              File tmpDirFile = File.createTempFile("pairing" + taskId, ".dir");
              File tmpDataFile = File.createTempFile("pairing" + taskId, ".dat");
              tmpDirFile.deleteOnExit();
              tmpDataFile.deleteOnExit();
              
              DynamicBPlusTree<Integer, DistanceList> resultTree = new DynamicBPlusTree<Integer, DistanceList>(
                  new BufferedDiskBackedDataStorage(tmpDirFile),
                  new DiskBackedDataStorage(tmpDataFile),
                  new ConstantSizeIntegerSerializer(),
                  new DistanceListSerializer(),
                  maxKeysPerBucket
              );
              Set<Integer> processedIds = new HashSet<Integer>();
              
              Log.info(String.format("\tPairing %010d: partition%05d (%,d items) with partition%05d (%,d items)", taskId, pairing.getPartitionOne().getId(), pairing.getPartitionOne().getSize(), 
                  pairing.getPartitionTwo().getId(), pairing.getPartitionTwo().getSize()));

              IPartition[][] partitionsToProcess = new IPartition[][] { 
                    new IPartition[] { pairing.getPartitionOne(), pairing.getPartitionTwo() },
                    new IPartition[] { pairing.getPartitionTwo(), pairing.getPartitionOne() }
                  };
              
              //we make two passes here as the calculation is much faster than deserializing the distanceLists all the
              //time to check if we already processed that result. Furthermore a hash list with the processed pairs has also 
              //the drawback of using too much memory - especially when more threads run simultaneously - the memory usage is n^2 per thread
              for (int i = 0; i < (pairing.isSelfPairing() ? 1 : partitionsToProcess.length); ++i) {
                IPartition[] partitions = partitionsToProcess[i];
                int counter = 0;
                for (Pair<Integer, NumberVector<?, ?>> pointOne : partitions[0]) {
                  if (counter++ % 50 == 0) {
                    Log.info(String.format("\t\tPairing %010d: Processed %,d of %,d items ...", taskId, counter, partitions[0].getSize()));
                  }
                  
                  for (Pair<Integer, NumberVector<?, ?>> pointTwo : partitions[1]) {
                    double distance = distanceAlgorithm.doubleDistance(pointOne.getSecond(), pointTwo.getSecond());
                    persistDistance(resultTree, processedIds, pointOne, pointTwo, distance);
                  }
                }
              }
              
//              int counter = 0;
//              for (Pair<Integer, NumberVector<?, ?>> pointOne : pairing.getPartitionOne()) {
//                if (counter++ % 50 == 0) {
//                  Log.info(String.format("\t\tPairing %010d: Processed %,d of %,d items ...", taskId, counter, pairing.getPartitionOne().getSize()));
//                }
//                
//                for (Pair<Integer, NumberVector<?, ?>> pointTwo : pairing.getPartitionTwo()) {
//                  double distance = distanceAlgorithm.doubleDistance(pointOne.getSecond(), pointTwo.getSecond());
//
//                  //persist distance for both items
//                  persistDistance(resultTree, processedIds, pointOne, pointTwo, distance);
//                  persistDistance(resultTree, processedIds, pointTwo, pointOne, distance);
//                }
//              }
              
              packageDescriptor.setResultFor(pairing, resultTree);
              
              addToTotalItems(pairing.getEstimatedUniqueIdsAmount());

              resultTree.close();
            } catch (Exception e) {
              Log.error(String.format("Problem in pairing %s: %s", pairing, e.getMessage()), e);
              return false;
            } finally {
              Log.info(String.format("Pairing %d done.", taskId));
            }
            return true;
          }
          
        };
        
        futures.add(threadPool.submit(task));
      }
      
      //wait for all tasks to finish
      for (Future<Boolean> future : futures) {
        future.get();
      }
      
      
      
      if (futures.size() > 0) {
        Log.info(String.format("Calculated and stored %,d distances.", totalItems));
      } else {
        Log.info("Nothing to do - all results have already been calculated");
      }
      
      
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new UnableToComplyException(e);
    } finally {
      Log.info("Shutting down thread pool ...");
      Thread terminationThread = new Thread() {
        public void run() {
          try {
            Thread.sleep(10000);
          } catch (InterruptedException e) {}
          Log.info("Exiting.");
          System.exit(0);
        }
      };
      terminationThread.start();
      threadPool.shutdownNow();
    }

  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.application.StandAloneInputApplication#getInputDescription()
   */
  public String getInputDescription() {
    // TODO Auto-generated method stub
    return "The package descriptor (usually an .xml-file)";
  }
  
  private synchronized void addToTotalItems(long items) {
    totalItems += items;
  }
  
  private void persistDistance(DynamicBPlusTree<Integer, DistanceList> resultTree, Set<Integer> processedIds, Pair<Integer, NumberVector<?, ?>> fromPoint, Pair<Integer, NumberVector<?, ?>> toPoint, double distance) throws IOException {
    DistanceList distanceList = null;
    if (processedIds.contains(fromPoint.getFirst())) {
      distanceList = resultTree.get(fromPoint.getFirst());
    } else {
      distanceList = new DistanceList(fromPoint.getFirst(), maxK);
      processedIds.add(fromPoint.getFirst());
    }
    distanceList.addDistance(toPoint.getFirst(), distance);
    resultTree.put(fromPoint.getFirst(), distanceList);
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
