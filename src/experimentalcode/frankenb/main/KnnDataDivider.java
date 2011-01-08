/**
 * 
 */
package experimentalcode.frankenb.main;

import java.io.File;
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
import experimentalcode.frankenb.model.PartitionPairing;
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
      Database<NumberVector<?, ?>> dataBase = databaseConnection.getDatabase(null);
      File outputDir = this.getOutput();
      
      if (outputDir.isFile()) 
        throw new UnableToComplyException("You need to specify an output directory not a file!");
      if (!outputDir.exists()) {
        if (!outputDir.mkdirs()) throw new UnableToComplyException("Could not create output directory");
      }
      clearDirectory(outputDir);

      LOG.log(Level.INFO, String.format("Packages to create: %d", packageQuantity));
      
      LOG.log(Level.INFO, String.format("Creating partitions (%s) ...", partitioner.getClass().getSimpleName()));
      List<PartitionPairing> partitionPairings = this.partitioner.makePartitionPairings(dataBase, packageQuantity);
      
      LOG.log(Level.INFO, String.format("Pairings created: %d", partitionPairings.size()));
      
      long calculations = 0;
      for (PartitionPairing pairing : partitionPairings) {
        calculations += pairing.getPartitionOne().getSize() * pairing.getPartitionTwo().getSize();
      }
      LOG.log(Level.INFO, String.format("Calculations total (about): %d", calculations));
      LOG.log(Level.INFO, String.format("Calculations per package (about): %d", calculations / packageQuantity));
      
      int partitionPairingsPerPackage = (int)Math.ceil(partitionPairings.size() / (float)packageQuantity);
      LOG.log(Level.INFO, String.format("Max Pairings per Package: %d", partitionPairingsPerPackage));
      
      for (int i = 0; i < packageQuantity; ++i) {
        PackageDescriptor packageDescriptor = new PackageDescriptor(i);
        for (int j = 0; j < partitionPairingsPerPackage; ++j) {
          if (partitionPairings.size() == 0) break;
          packageDescriptor.addPartitionPairing(partitionPairings.remove(0));
        }
        if (packageDescriptor.getPartitionPairings().size() > 0) {
          LOG.log(Level.INFO, String.format("persisting packageDescriptor %05d ...", i));
          File targetDirectory = new File(this.getOutput(), String.format("package%05d", i));
          File packageDescriptorFile = new File(targetDirectory, String.format("package%05d_descriptor.xml", i));
          targetDirectory.mkdirs();
          
          packageDescriptor.setDimensionality(dataBase.dimensionality());
          packageDescriptor.saveToFile(packageDescriptorFile);
        }
      }
      
      LOG.log(Level.INFO, "done.");
      
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
