/**
 * 
 */
package experimentalcode.frankenb.main;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.application.StandAloneApplication;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.CPair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.frankenb.model.PackageDescriptor;
import experimentalcode.frankenb.model.Partition;

/**
 * This application divides a given database into
 * a given numbers of packages to calculate knn
 * on a distributed system like the sun cluster
 * <p />
 * Example usage:
 * <br />
 * <code>-dbc.parser DoubleVectorLabelParser -dbc.in /ELKI/data/synthetic/outlier-scenarios/3-gaussian-2d.csv -app.out D:/tmp/knnparts -packagequantity 10</code>
 * 
 * @author Florian Frankenberger
 */
public class KnnDataDivider extends StandAloneApplication {

  private static final Logging LOG = Logging.getLogger(KnnDataDivider.class);
  
  /**
   * OptionID for {@link #PACKAGES_PARAM}
   */
  public static final OptionID PACKAGES_ID = OptionID.getOrCreateOptionID("packagequantity", "");
  
  /**
   * Parameter that specifies the number of segments to create (= # of computers)
   * <p>
   * Key: {@code -packages}
   * </p>
   */
  private final IntParameter PACKAGES_PARAM = new IntParameter(PACKAGES_ID, false);

  private int packageQuantity = 0;
  private final DatabaseConnection<NumberVector<?, ?>> databaseConnection;
  
  /**
   * @param config
   */
  public KnnDataDivider(Parameterization config) {
    super(config);

    config = config.descend(this);
    PACKAGES_PARAM.setShortDescription(getPackagesDescription());
    if (config.grab(PACKAGES_PARAM)) {
      packageQuantity = PACKAGES_PARAM.getValue();      
    }
    
    databaseConnection = new FileBasedDatabaseConnection<NumberVector<?, ?>>(config);
  }

  /**
   * @return
   */
  private String getPackagesDescription() {
    // TODO Auto-generated method stub
    return "# of packages(computers) to split the data in";
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
      Database<NumberVector<?, ?>> database = databaseConnection.getDatabase(null);
      File outputDir = this.getOutput();
      
      if (outputDir.isFile()) 
        throw new UnableToComplyException("You need to specify an output directory not a file!");
      if (!outputDir.exists()) {
        if (!outputDir.mkdirs()) throw new UnableToComplyException("Could not create output directory");
      }
      
      int partitionQuantity = packagesQuantityToSegmentsQuantity(packageQuantity);
      int itemsPerPartition = (int) Math.floor(database.size() / partitionQuantity);
      List<DBID> ids = new ArrayList<DBID>(database.getIDs().asCollection());
      
      Random random = new Random(System.currentTimeMillis());
      
      //create and fill the partitions
      List<Partition> partitions = new ArrayList<Partition>();
      for (int i = 0; i < partitionQuantity; ++i) {
        int itemsToWrite = (i == partitionQuantity - 1 ? ids.size() : itemsPerPartition); // the last one gets the rest
        
        Partition partition = new Partition(database.dimensionality());
        partitions.add(partition);
        
        for (int j = 0; j < itemsToWrite; ++j) { 
          int id = random.nextInt(ids.size());
          DBID dbid = ids.remove(id);
          partition.addVector(dbid.getIntegerID(), database.get(dbid));
        }        
        
      }
      
      //create permutations
      Set<CPair<Integer, Integer>> partitionPermutations = permutatePartitions(partitionQuantity);
      
      System.out.println(partitionPermutations);
      
      int i = 0;
      for (CPair<Integer, Integer> segmentPermutation : partitionPermutations) {
        
        LOG.log(Level.INFO, String.format("Writing package %03d of %03d", i + 1, partitionPermutations.size()));
        String filenamePrefix = String.format("p%03d_", i);
        
        Partition partitionOne = partitions.get(segmentPermutation.getFirst());
        Partition partitionTwo = partitions.get(segmentPermutation.getSecond());
        
        List<Partition> selectedPartitions = new ArrayList<Partition>();
        selectedPartitions.add(partitionOne);
        if (!segmentPermutation.getFirst().equals(segmentPermutation.getSecond())) {
          selectedPartitions.add(partitionTwo);
        }
        
        List<File> partitionFiles = new ArrayList<File>();
        int partitionCounter = 0;
        for (Partition partition : selectedPartitions) {
          String partitionFilename = filenamePrefix + String.format("partition_%02d.dat", partitionCounter++);
          File partitionFile = new File(outputDir, partitionFilename);
          partitionFiles.add(partitionFile);

          LOG.log(Level.INFO, String.format("\tpartition %1d of %1d ... ", partitionCounter, selectedPartitions.size()));
          deleteAlreadyExistingFile(partitionFile);
          partition.copyToFile(partitionFile);
        }
        
        File packageDescriptorFile = new File(outputDir, filenamePrefix + "descriptor.xml");
        
        PackageDescriptor packageDescriptor = new PackageDescriptor(i);
        packageDescriptor.addPartitionPairing(
              new Pair<File, File>(
                  partitionFiles.get(0),
                  (partitionFiles.size() > 1 ? partitionFiles.get(1) : partitionFiles.get(0))
              )
            );
        packageDescriptor.setDimensionality(database.dimensionality());
        packageDescriptor.saveToFile(packageDescriptorFile);
        
        i++;
      }
      
    } catch (RuntimeException e) {
      throw e;
    } catch (UnableToComplyException e) {
      throw e;
    } catch (Exception e) {
      throw new UnableToComplyException(e);
    }
  }
  
  private static void deleteAlreadyExistingFile(File file) throws UnableToComplyException {
    if (file.exists()) {
      if (!file.delete()) throw new UnableToComplyException("File " + file.getName() + " already exists and could not be removed.");
    }
    
  }
  
  /**
   * Returns all possible permutations
   * 
   * @param i
   * @param segmentQuantity
   * @return
   */
  private static Set<CPair<Integer, Integer>> permutatePartitions(int segmentQuantity) {
    Set<CPair<Integer, Integer>> permutations = new HashSet<CPair<Integer, Integer>>();
    for (int i = 0; i < segmentQuantity; ++i) {
      for (int j = i; j < segmentQuantity; ++j) {
        permutations.add(new CPair<Integer, Integer>(i, j));
      }
    }
    
    return permutations;
  }
  
  /**
   * calculates the segments necessary to split the db into to calculate
   * the given number of packages
   * 
   * @return
   * @throws UnableToComplyException 
   */
  private static int packagesQuantityToSegmentsQuantity(int packageQuantity) throws UnableToComplyException {
    if (packageQuantity < 3) {
      throw new UnableToComplyException("Minimum is 3 packages");
    }
    return (int)Math.floor((Math.sqrt(1 + packageQuantity * 8) - 1) / 2.0);
  }
  
  public static void main(String[] args) {
    StandAloneApplication.runCLIApplication(KnnDataDivider.class, args);
  }



}
