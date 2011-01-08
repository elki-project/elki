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
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.application.StandAloneApplication;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.RawDoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.frankenb.model.BufferedDiskBackedDataStorage;
import experimentalcode.frankenb.model.ConstantSizeIntegerSerializer;
import experimentalcode.frankenb.model.DistanceList;
import experimentalcode.frankenb.model.DistanceListSerializer;
import experimentalcode.frankenb.model.DynamicBPlusTree;
import experimentalcode.frankenb.model.PackageDescriptor;
import experimentalcode.frankenb.model.PartitionPairing;
import experimentalcode.frankenb.model.RandomAccessFileDataStorage;
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

  /**
   * Parameter that specifies the name of the input file.
   * <p>
   * Key: {@code -app.in}
   * </p>
   */
  private final FileParameter INPUT_PARAM = new FileParameter(INPUT_ID, FileParameter.FileType.INPUT_FILE);

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
  
  
  private static final Logging LOG = Logging.getLogger(KnnDataProcessor.class);
  
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

    LoggingConfiguration.setLevelFor(KnnDataProcessor.class.getCanonicalName(), Level.ALL.getName());
    
    config = config.descend(this);
    INPUT_PARAM.setShortDescription(getInputDescription());
    if (config.grab(INPUT_PARAM)) {
      input = INPUT_PARAM.getValue();
    }
    
    if (config.grab(MAXK_PARAM)) {
      maxK = MAXK_PARAM.getValue();
    }
    
    distance = getParameterReachabilityDistanceFunction(config);
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.application.AbstractApplication#run()
   */
  @Override
  public void run() throws UnableToComplyException {
    try {
      
      LOG.log(Level.INFO, "Opening package ...");
      final PackageDescriptor packageDescriptor = PackageDescriptor.loadFromFile(input);
      
      int counter = 0;
      long items = 0;
      long startTime = System.currentTimeMillis();

      //create a threadpool with that many processes that there are processors available
      Runtime runtime = Runtime.getRuntime();
      ExecutorService threadPool = Executors.newFixedThreadPool(runtime.availableProcessors() * 4);
      
      List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
      
      for (final PartitionPairing pairing : packageDescriptor.getPartitionPairings()) {
        if (!pairing.hasResult()) {
          final int taskId = counter++;
          items += pairing.getPartitionOne().getSize() * pairing.getPartitionTwo().getSize();
          
          Callable<Boolean> task = new Callable<Boolean>() {
  
            @Override
            public Boolean call() throws Exception {
              try {
              LOG.log(Level.INFO, String.format("Processing pairing %04d ...", taskId));
                
                //LOG.log(Level.INFO, String.format("Processing pairing %03d of %03d", counter+1, packageDescriptor.getPartitionPairings().size()));
                
                IPartition partitionOne = pairing.getPartitionOne();
                IPartition partitionTwo = pairing.getPartitionTwo();
                
                File packageDirectory = input.getParentFile();
                File resultFileDir = new File(packageDirectory, String.format("package%05d_result%02d.dir", packageDescriptor.getId(), taskId));
                if (resultFileDir.exists()) resultFileDir.delete();
                File resultFileDat = new File(packageDirectory, String.format("package%05d_result%02d.dat", packageDescriptor.getId(), taskId));
                if (resultFileDat.exists()) resultFileDat.delete();
                
                DynamicBPlusTree<Integer, DistanceList> resultTree = new DynamicBPlusTree<Integer, DistanceList>(
                    new BufferedDiskBackedDataStorage(resultFileDir),
                    new RandomAccessFileDataStorage(resultFileDat),
                    new ConstantSizeIntegerSerializer(),
                    new DistanceListSerializer(),
                    100
                );
                
                for (int i = 0; i < 2; ++i) {
                  IPartition[] partitionsToCalculate = (i == 0 ? new IPartition[] { partitionOne, partitionTwo } : new IPartition[] { partitionTwo, partitionOne });
                  if (i == 1 && partitionOne.equals(partitionTwo)) continue;
                  
                  LOG.log(Level.INFO, String.format("\tPairing %04d: partition1 (%d items) with partition2 (%d items)", taskId, partitionsToCalculate[0].getSize(), partitionsToCalculate[1].getSize()));
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
                    if (resultTree.getSize() % 100 == 0) {
                      LOG.log(Level.INFO, String.format("\tPairing %04d: resultTree items: %d",taskId ,resultTree.getSize()));
                      		
                      System.gc();
                    }
                  }
                  
                  
                }
                
                pairing.setResult(resultTree);
                resultTree.close();
                
              } catch (Exception e) {
                LOG.log(Level.WARNING, "Problem in pairing " + pairing + ": " + e, e);
                return false;
              } finally {
                LOG.log(Level.INFO, "Pairing " + taskId + " done.");
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
        packageDescriptor.saveToFile(input);
        LOG.log(Level.INFO, String.format("Calculated and stored %d distances in %d seconds", items, (System.currentTimeMillis() - startTime) / 1000));
      } else {
        LOG.log(Level.INFO, "Nothing to do - all results have already been calculated");
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
