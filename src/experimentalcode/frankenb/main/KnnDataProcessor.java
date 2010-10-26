/**
 * 
 */
package experimentalcode.frankenb.main;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.application.StandAloneApplication;
import de.lmu.ifi.dbs.elki.application.StandAloneInputApplication;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.RawDoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.frankenb.model.ConstantSizeIntegerSerializer;
import experimentalcode.frankenb.model.DistanceList;
import experimentalcode.frankenb.model.DistanceListSerializer;
import experimentalcode.frankenb.model.DynamicBPlusTree;
import experimentalcode.frankenb.model.PackageDescriptor;
import experimentalcode.frankenb.model.PackageDescriptor.Pairing;
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
      
      for (Pairing pairing : packageDescriptor.getPartitionPairings()) {
        LOG.log(Level.INFO, String.format("Processing pairing %03d of %03d", counter+1, packageDescriptor.getPartitionPairings().size()));
        
        Partition partitionOne = Partition.loadFromFile(packageDescriptor.getDimensionality(), pairing.getFirstPartitionFile());
        Partition partitionTwo = partitionOne;
        if (!pairing.getFirstPartitionFile().equals(pairing.getSecondPartitionFile())) {
          partitionTwo = Partition.loadFromFile(packageDescriptor.getDimensionality(), pairing.getSecondPartitionFile());
        }
        
        
        Map<Integer, Map<Integer, Double>> distances = new HashMap<Integer, Map<Integer, Double>>();
        
        LOG.log(Level.INFO, "PartitionOne entries: " + partitionOne.getSize());
        LOG.log(Level.INFO, "PartitionTwo entries: " + partitionTwo.getSize());
        
        int pairingCounter = 0;
        for (Pair<Integer, NumberVector<?, ?>> entryOne : partitionOne) {
          
          for (Pair<Integer, NumberVector<?, ?>> entryTwo : partitionTwo) {
            double aDistance = distance.doubleDistance(entryOne.second, entryTwo.second);
            
            // A vs B in list as A -> map(B, distance)
            Map<Integer, Double> map = distances.get(entryOne.getFirst());
            if (map == null) {
              map = new HashMap<Integer, Double>();
              distances.put(entryOne.getFirst(), map);
            }
            map.put(entryTwo.getFirst(), aDistance);
            
            // A vs B in list as B -> map(A, distance)
            map = distances.get(entryTwo.getFirst());
            if (map == null) {
              map = new HashMap<Integer, Double>();
              distances.put(entryTwo.getFirst(), map);
            }
            map.put(entryOne.getFirst(), aDistance);
            
            items++;
            
          }
          
          if (pairingCounter++ % 10 == 0) {
            LOG.log(Level.INFO, "\tPairing " + pairingCounter + " ...");
          }
        }
        
        LOG.log(Level.INFO, "Storing (" + distances.size() + " entries) ...");
        
        //now we store everything from memory into an efficient data structure (b+ tree) for quick
        //merging in reduction step
        File resultFileDir = new File(this.getOutput(), String.format("p%03d_result_%02d.dir", packageDescriptor.getId(), counter));
        if (resultFileDir.exists()) resultFileDir.delete();
        File resultFileDat = new File(this.getOutput(), String.format("p%03d_result_%02d.dat", packageDescriptor.getId(), counter++));
        if (resultFileDat.exists()) resultFileDat.delete();
        
        DynamicBPlusTree<Integer, DistanceList> bPlusTree = new DynamicBPlusTree<Integer, DistanceList>(
            resultFileDir,
            resultFileDat,
            new ConstantSizeIntegerSerializer(),
            new DistanceListSerializer(),
            100
        );
        
        for (Entry<Integer, Map<Integer, Double>> entry : distances.entrySet()) {
          DistanceList distanceList = new DistanceList(entry.getKey());
          for (Entry<Integer, Double> aDistanceEntry : entry.getValue().entrySet()) {
            distanceList.addDistance(aDistanceEntry.getKey(), aDistanceEntry.getValue());
          }
          bPlusTree.put(entry.getKey(), distanceList);
        }
        pairing.setResultFile(resultFileDir, resultFileDat);
      }
      
      packageDescriptor.saveToFile(getInput());
      LOG.log(Level.INFO, String.format("Calculated and stored %d distances in %d seconds", items, (System.currentTimeMillis() - startTime) / 1000));
      System.out.println(String.format("Calculated and stored %d distances in %d seconds", items, (System.currentTimeMillis() - startTime) / 1000));
      
      
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
