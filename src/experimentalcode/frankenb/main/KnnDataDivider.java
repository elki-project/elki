/**
 * 
 */
package experimentalcode.frankenb.main;

import java.io.File;
import java.util.List;

import de.lmu.ifi.dbs.elki.application.StandAloneApplication;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import experimentalcode.frankenb.log.Log;
import experimentalcode.frankenb.log.LogLevel;
import experimentalcode.frankenb.log.StdOutLogWriter;
import experimentalcode.frankenb.log.TraceLevelLogFormatter;
import experimentalcode.frankenb.model.PackageDescriptor;
import experimentalcode.frankenb.model.PartitionPairing;
import experimentalcode.frankenb.model.datastorage.BufferedDiskBackedDataStorage;
import experimentalcode.frankenb.model.ifaces.IDividerAlgorithm;
import experimentalcode.frankenb.utils.Utils;

/**
 * This application divides a given database into
 * a given numbers of packages to calculate knn
 * on a distributed system like the sun cluster
 * <p />
 * Example usage:
 * <br />
 * <code>-dbc.parser DoubleVectorLabelParser -dbc.in /ELKI/data/synthetic/outlier-scenarios/3-gaussian-2d.csv -app.out D:/tmp/knnparts -packages 3 -partitioner xxx</code>
 * 
 * @author Florian Frankenberger
 */
public class KnnDataDivider extends StandAloneApplication {

  public static final OptionID DIVIDER_ALGORITHM_ID = OptionID.getOrCreateOptionID("algorithm", "A divider algorithm to use");
  /**
   * OptionID for {@link #PACKAGES_PARAM}
   */
  public static final OptionID PACKAGES_ID = OptionID.getOrCreateOptionID("packages", "");
  
  /**
   * Parameter that specifies the number of segments to create (= # of computers)
   * <p>
   * Key: {@code -packages}
   * </p>
   */
  private final IntParameter PACKAGES_PARAM = new IntParameter(PACKAGES_ID, false);
  private int packageQuantity = 0;
  
  
  private final DatabaseConnection<NumberVector<?, ?>> databaseConnection;
  private IDividerAlgorithm algorithm;

  /**
   * @param config
   */
  public KnnDataDivider(Parameterization config) {
    super(config);
    Log.setLogFormatter(new TraceLevelLogFormatter());
    Log.addLogWriter(new StdOutLogWriter());
    Log.setFilter(LogLevel.DEBUG);
    
    config = config.descend(this);
    if (config.grab(PACKAGES_PARAM)) {
      packageQuantity = PACKAGES_PARAM.getValue();      
    }
        
    final ObjectParameter<IDividerAlgorithm> paramPartitioner = new ObjectParameter<IDividerAlgorithm>(DIVIDER_ALGORITHM_ID, IDividerAlgorithm.class, false);
    if(config.grab(paramPartitioner)) {
      this.algorithm = paramPartitioner.instantiateClass(config);
    }
    
    databaseConnection = FileBasedDatabaseConnection.parameterize(config);
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
      Log.info("knn data divider started");
      Log.info("reading database ...");
      final Database<NumberVector<?, ?>> dataBase = databaseConnection.getDatabase(null);
      long totalCalculationsWithoutApproximation = Utils.sumFormular(dataBase.size());
      
      File outputDir = this.getOutput();
      
      if (outputDir.isFile()) 
        throw new UnableToComplyException("You need to specify an output directory not a file!");
      if (!outputDir.exists()) {
        if (!outputDir.mkdirs()) throw new UnableToComplyException("Could not create output directory");
      }
      Log.info(String.format("%d items in db (%d dimensions)", dataBase.size(), dataBase.dimensionality()));
      
      Log.info();
      Log.info("cleaning output directory...");
      clearDirectory(outputDir);
      Log.info();
      
      Log.info(String.format("Packages to create: %,8d", packageQuantity));
      Log.info(String.format("Creating partitions (algorithm used: %s) ...", algorithm.getClass().getSimpleName()));
      
      List<PartitionPairing> pairings = this.algorithm.divide(dataBase, packageQuantity);
      
      int pairingsPerPackage = pairings.size() / packageQuantity;
      int addPairingsToPackageUntil = pairings.size() % packageQuantity;
      
      Log.info(String.format("Total partition pairings: %,d", pairings.size()));
      Log.info("Pairings per package: " + String.format("%,d", pairingsPerPackage) + (addPairingsToPackageUntil > 0 ? "-" + String.format("%,d", pairingsPerPackage + 1) : ""));
      Log.info("Storing packages ...");

      long totalCalculations = 0;
      int persistedPairings = 0;
      int packageCounter = -1;
      PackageDescriptor packageDescriptor = null;
      for (PartitionPairing pairing : pairings) {
        int maxPairingsForCurrentPackage = pairingsPerPackage + (packageCounter < addPairingsToPackageUntil ? 1 : 0);
        if (packageDescriptor == null || persistedPairings >= maxPairingsForCurrentPackage) {
          if (packageDescriptor != null) {
            packageDescriptor.close();
          }
          
          packageCounter++;
          File targetDirectory = new File(outputDir, String.format("package%05d", packageCounter));
          targetDirectory.mkdirs();
          File packageDescriptorFile = new File(targetDirectory, String.format("package%05d_descriptor.dat", packageCounter));
          int bufferSize = maxPairingsForCurrentPackage * PackageDescriptor.PAIRING_DATA_SIZE + PackageDescriptor.HEADER_SIZE; 
          packageDescriptor = new PackageDescriptor(packageCounter + 1, dataBase.dimensionality(), new BufferedDiskBackedDataStorage(packageDescriptorFile, bufferSize));
          
          persistedPairings = 0;
          Log.info(String.format("Creating package %,8d of %,8d", packageCounter + 1, packageQuantity));
        }
        
        packageDescriptor.addPartitionPairing(pairing);
        totalCalculations += Utils.sumFormular(pairing.getPartitionOne().getSize() + pairing.getPartitionTwo().getSize());
        persistedPairings++;
        
        if (persistedPairings % 100 == 0 || persistedPairings == maxPairingsForCurrentPackage) {
          Log.info(String.format("\t%6.2f%% partition pairings persisted (%,10d partition pairings of %,10d) ...", 
              (persistedPairings / (float)maxPairingsForCurrentPackage) * 100, 
              persistedPairings, 
              maxPairingsForCurrentPackage
              ));            
        }        
        
      }
      
      if (packageDescriptor != null) {
        packageDescriptor.close();
      }
      
      Log.info(String.format("Created %,d packages containing %,d calculations (%.2f%% of cal. w/ apprx.) in %,d partition pairings", 
          packageQuantity, totalCalculations, (totalCalculations / (float) totalCalculationsWithoutApproximation) * 100f, pairings.size()));
      
    } catch (RuntimeException e) {
      throw e;
    } catch (UnableToComplyException e) {
      throw e;
    } catch (Exception e) {
      throw new UnableToComplyException(e);
    }
  }
  
  public static void main(String[] args) {
    StandAloneApplication.runCLIApplication(KnnDataDivider.class, args);
  }

  private static void clearDirectory(File directory) throws UnableToComplyException {
    for (File file : directory.listFiles()) {
      if (file.equals(directory) || file.equals(directory.getParentFile())) continue;
      if (file.isDirectory()) {
        clearDirectory(file);
      }
      if (!file.delete()) throw new UnableToComplyException("Could not delete " + file + ".");
    }
  }

}
