/**
 * 
 */
package experimentalcode.frankenb.main;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.application.StandAloneApplication;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import experimentalcode.frankenb.model.PackageDescriptor;
import experimentalcode.frankenb.model.PackageDescriptorOLD;
import experimentalcode.frankenb.model.PartitionPairing;
import experimentalcode.frankenb.model.datastorage.BufferedDiskBackedDataStorage;
import experimentalcode.frankenb.model.datastorage.DiskBackedDataStorage;
import experimentalcode.frankenb.model.ifaces.IPartitionPairingStorage;
import experimentalcode.frankenb.model.ifaces.IPartitioner;

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

  private static final Logging LOG = Logging.getLogger(KnnDataDivider.class);
  public static final OptionID PARTITIONER_ID = OptionID.getOrCreateOptionID("partitioner", "A partitioner");
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
  private IPartitioner partitioner;

  private int partitionPairings = 0;
  private int partitionPairingsCounter = 0;
  private int packageCounter = 0;
  
  /**
   * @param config
   */
  public KnnDataDivider(Parameterization config) {
    super(config);
    LoggingConfiguration.setLevelFor(KnnDataDivider.class.getCanonicalName(), Level.ALL.getName());

    config = config.descend(this);
    if (config.grab(PACKAGES_PARAM)) {
      packageQuantity = PACKAGES_PARAM.getValue();      
    }
        
    final ObjectParameter<IPartitioner> paramPartitioner = new ObjectParameter<IPartitioner>(PARTITIONER_ID, IPartitioner.class, false);
    if(config.grab(paramPartitioner)) {
      this.partitioner = paramPartitioner.instantiateClass(config);
    }
    
    databaseConnection = new FileBasedDatabaseConnection<NumberVector<?, ?>>(config);
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
      final Database<NumberVector<?, ?>> dataBase = databaseConnection.getDatabase(null);
      File outputDir = this.getOutput();
      
      if (outputDir.isFile()) 
        throw new UnableToComplyException("You need to specify an output directory not a file!");
      if (!outputDir.exists()) {
        if (!outputDir.mkdirs()) throw new UnableToComplyException("Could not create output directory");
      }
      clearDirectory(outputDir);

      long time = System.currentTimeMillis();
      IPartitionPairingStorage storage = new IPartitionPairingStorage() {

        private boolean set = false;
        private int partitionPairingsPerPackage = 0;
        private int additionalPairings = 0;
        
        private PackageDescriptor packageDescriptor;
        
        @Override
        public void setPartitionPairings(int partitionPairings) {
          KnnDataDivider.this.partitionPairings = partitionPairings;
          partitionPairingsPerPackage = partitionPairings / packageQuantity;
          additionalPairings = partitionPairings % packageQuantity;
          set = true;
        }

        @Override
        public void add(PartitionPairing partitionPairing) {
          try {
            if (!set) {
              throw new RuntimeException("You need to set the amount of partition pairings first!");
            }
            
            if (packageDescriptor == null) {
              File targetDirectory = new File(getOutput(), String.format("package%05d", packageCounter));
              targetDirectory.mkdirs();
              File packageDescriptorFile = new File(targetDirectory, String.format("package%05d_descriptor.dat", packageCounter));
              int bufferSize = (partitionPairingsPerPackage + (packageCounter <= additionalPairings ? 1 : 0)) * PackageDescriptor.PAIRING_DATA_SIZE + PackageDescriptor.HEADER_SIZE; 
              packageDescriptor = new PackageDescriptor(packageCounter, dataBase.dimensionality(), new BufferedDiskBackedDataStorage(packageDescriptorFile, bufferSize));
              LOG.log(Level.INFO, String.format("new PackageDescriptor %05d ...", packageCounter));
            }
            
            packageDescriptor.addPartitionPairing(partitionPairing);
            
            if (partitionPairingsCounter % 100000 == 0) {
              LOG.log(Level.INFO, String.format("\t%5.2f%% partition pairings persisted (%10d partition pairings of %10d) ...", 
                  ((partitionPairingsCounter + 1) / (float)(partitionPairingsPerPackage + (packageCounter <= additionalPairings ? 1 : 0))) * 100, 
                  (partitionPairingsCounter + 1), 
                  partitionPairingsPerPackage + (packageCounter <= additionalPairings ? 1 : 0)
                  ));
            }
            
            if (partitionPairingsCounter++ >= (partitionPairingsPerPackage - 1) + (packageCounter <= additionalPairings ? 1 : 0)) {
              partitionPairingsCounter = 0;
              
              packageDescriptor.close();
              
              packageDescriptor = null;
              packageCounter++;
            }
          } catch (IOException e) {
            throw new RuntimeException("Could not persist package descriptor", e);
          }
        }
        
      };
      
      LOG.log(Level.INFO, String.format("Packages to create: %d", packageQuantity));
      
      LOG.log(Level.INFO, String.format("Creating partitions (%s) ...", partitioner.getClass().getSimpleName()));
      this.partitioner.makePartitionPairings(dataBase, storage, packageQuantity);
      
      LOG.log(Level.INFO, String.format("Created %010d packages containing %010d partition pairings in %d seconds", this.packageCounter, this.partitionPairings, (System.currentTimeMillis() - time) / 1000));
      
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
