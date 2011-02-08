/**
 * 
 */
package experimentalcode.frankenb.main;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.Set;

import de.lmu.ifi.dbs.elki.application.StandAloneApplication;
import de.lmu.ifi.dbs.elki.application.StandAloneInputApplication;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
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

/**
 * This class merges the results precalculated on the cluster network
 * to a single precalculated knn file with a given max k neighbors.
 * <p/>
 * Usage:
 * <br/>
 * <code>-dbc.parser DoubleVectorLabelParser -dbc.in D:\Coding\Projects\ELKI\data\synthetic\outlier-scenarios\3-gaussian-2d.csv -app.out D:/temp/knnparts -app.in D:/temp/knnparts -k 10</code>
 * @author Florian Frankenberger
 */
public class KnnDataMerger extends StandAloneInputApplication {

  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("k", "");
  public static final OptionID IN_MEMORY_ID = OptionID.getOrCreateOptionID("inmemory", "tells wether the resulting tree data should be buffered in memory or not. This can increase performance but can also lead to OutOfMemoryExceptions!");
  
  /**
   * Parameter that specifies the number of neighbors to keep with respect
   * to the definition of a k-nearest neighbor.
   * <p>
   * Key: {@code -k}
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, false);
  private final Flag IN_MEMORY_PARAM = new Flag(IN_MEMORY_ID);
  
  
  private int k;
  private boolean inMemory = false;
  
  /**
   * @param config
   */
  public KnnDataMerger(Parameterization config) {
    super(config);
    
    Log.setLogFormatter(new TraceLevelLogFormatter());
    Log.addLogWriter(new StdOutLogWriter());
    Log.setFilter(LogLevel.INFO);
    
    if (config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }
    
    if (config.grab(IN_MEMORY_PARAM)) {
      inMemory = IN_MEMORY_PARAM.getValue();
    }
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.application.StandAloneInputApplication#getInputDescription()
   */
  @Override
  public String getInputDescription() {
    // TODO Auto-generated method stub
    return null;
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
      Log.info("Start merging data");
      Log.info();
      Log.info("using inMemory strategy: " + Boolean.toString(inMemory));
      Log.info("maximum k to calculate: " + k);
      Log.info();

      File[] packageDirectories = getInput().listFiles(new FilenameFilter() {
  
        @Override
        public boolean accept(File dir, String name) {
          return name.matches("^package[0-9]{5}$");
        }
        
      });
      
      
      File resultDirectory = new File(this.getOutput(), "result.dir");
      if (resultDirectory.exists())
        resultDirectory.delete();
      File resultData = new File(this.getOutput(), "result.dat");
      if (resultData.exists())
        resultData.delete();
      
      DynamicBPlusTree<Integer, DistanceList> resultTree = new DynamicBPlusTree<Integer, DistanceList>(
          new BufferedDiskBackedDataStorage(resultDirectory),
          (inMemory ? new BufferedDiskBackedDataStorage(resultData) : new DiskBackedDataStorage(resultData)),
          new ConstantSizeIntegerSerializer(),
          new DistanceListSerializer(),
          100
      );      
      
      //open all result files
      Set<Integer> testSet = new HashSet<Integer>();
      for (File packageDirectory : packageDirectories) {
        File[] packageDescriptorCandidates = packageDirectory.listFiles(new FilenameFilter() {

          @Override
          public boolean accept(File dir, String name) {
            return name.matches("^package[0-9]{5}_descriptor.dat$");
          }

        });

        if (packageDescriptorCandidates.length > 0) {
          Log.info("Opening result of " + packageDirectory.getName() + " ...");
          File packageDescriptorFile = packageDescriptorCandidates[0];
          PackageDescriptor packageDescriptor = PackageDescriptor.readFromStorage(new BufferedDiskBackedDataStorage(packageDescriptorFile));
          
          int counter = 0;
          for (PartitionPairing pairing : packageDescriptor) {
            if (!packageDescriptor.hasResult(pairing)) throw new UnableToComplyException("Package " + packageDescriptorFile + "/pairing " + pairing + " has no results!");
            DynamicBPlusTree<Integer, DistanceList> result = packageDescriptor.getResultTreeFor(pairing);
            Log.info(String.format("\tprocessing result of pairing %05d of %05d (%s) - %6.2f%% ...", ++counter, packageDescriptor.getPairings(), pairing, (counter / (float) packageDescriptor.getPairings()) * 100f));
            
            for (Pair<Integer, DistanceList> resultEntry : result) {
              DistanceList distanceList = resultTree.get(resultEntry.first);
              if (distanceList == null) {
                distanceList = new DistanceList(resultEntry.first, k);
              }
              testSet.add(resultEntry.first);
              distanceList.addAll(resultEntry.second);
              resultTree.put(resultEntry.first, distanceList);
            }
          }
        } else {
          Log.warn("Skipping directory " + packageDirectory + " because the package descriptor could not be found.");
        }
      }
      
      Log.info("result tree items: " + resultTree.getSize());
      resultTree.close();
      Log.info("TestSet has " + testSet.size() + " items");
      Log.info("Created result tree with " + resultTree.getSize() + " entries.");
    } catch (Exception e) {
      Log.error("Could not merge data", e);
    }
    
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    StandAloneApplication.runCLIApplication(KnnDataMerger.class, args);
  }

}
