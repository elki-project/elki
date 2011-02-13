/**
 * 
 */
package experimentalcode.frankenb.main;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;

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
import experimentalcode.frankenb.log.FileLogWriter;
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
    FileLogWriter logWriter = null;
    try {
      File outputDir = this.getOutput();
      
      if (outputDir.isFile()) 
        throw new UnableToComplyException("You need to specify an output directory not a file!");
      if (!outputDir.exists()) {
        if (!outputDir.mkdirs()) throw new UnableToComplyException("Could not create output directory");
      }
      
      Log.info();
      Log.info("cleaning output directory...");
      clearDirectory(outputDir);
      Log.info();
      
      logWriter = appendStatisticsWriter(outputDir);
      Log.info("knn data divider started @ " + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date(new Date().getTime() - Log.getElapsedTime())));
      
      Log.info("reading database ...");
      final Database<NumberVector<?, ?>> dataBase = databaseConnection.getDatabase(null);
      long totalCalculationsWithoutApproximation = Utils.sumFormular(dataBase.size() - 1);
      
      Log.info(String.format("DB Size: %,d (%d dimensions)", dataBase.size(), dataBase.dimensionality()));
      Log.info(String.format("Packages to create: %,8d", packageQuantity));
      Log.info(String.format("Algorithm used: %s", algorithm.getClass().getSimpleName()));
      Log.info();
      
      Log.info(String.format("Creating partitions (algorithm used: %s) ...", algorithm.getClass().getSimpleName()));
      
      List<PartitionPairing> pairings = this.algorithm.divide(dataBase, packageQuantity);
      
      Log.info(String.format("Total partition pairings: %,d", pairings.size()));
      

      Log.info("Counting calculations ...");
      long totalCalculations = 0;
      for (PartitionPairing pairing: pairings) {
        totalCalculations += pairing.getCalculations();
      }
      
      long calculationsPerPackage = (long) Math.ceil(totalCalculations / (double) packageQuantity);
      int expectedPairingsPerPackage = pairings.size() / packageQuantity;

      Log.info(String.format("Total calculations necessary: %,d (%.2f%% of cal. w/ apprx.)", totalCalculations, (totalCalculations / (float) totalCalculationsWithoutApproximation) * 100f));
      Log.info(String.format("Calculations per package: about %,d", calculationsPerPackage));
      
      Log.info("Sorting pairings ...");
      Collections.sort(pairings, new Comparator<PartitionPairing>() {

        @Override
        public int compare(PartitionPairing o1, PartitionPairing o2) {
          return Long.valueOf(o1.getCalculations()).compareTo(o2.getCalculations());
        }

        
      });
      
      Random random = new Random(System.currentTimeMillis()); 
      Log.info("Storing packages ...");
      for (int i = 0; i < packageQuantity && !pairings.isEmpty(); ++i) {
        long calculations = 0L;
        
        File targetDirectory = new File(outputDir, String.format("package%05d", i));
        targetDirectory.mkdirs();
        
        File packageDescriptorFile = new File(targetDirectory, String.format("package%05d_descriptor.dat", i));
        int bufferSize = expectedPairingsPerPackage * PackageDescriptor.PAIRING_DATA_SIZE + PackageDescriptor.HEADER_SIZE; 
        PackageDescriptor packageDescriptor = new PackageDescriptor(i + 1, dataBase.dimensionality(), new BufferedDiskBackedDataStorage(packageDescriptorFile, bufferSize));
        
        Log.info(String.format("Creating package %08d of max. %08d (%s)", i + 1, packageQuantity, packageDescriptorFile.toString()));
        
        while (!pairings.isEmpty() && calculations < calculationsPerPackage) {
          long calculationsToAdd = calculationsPerPackage - calculations;
          PartitionPairing pairingWithMostCalculations = pairings.get(pairings.size() - 1);
          PartitionPairing pairingToAdd = null;

          //if the necessary calculations for this package exceeds the biggest partition pairing
          //we choose one randomly - otherwise we choose the one that fits best
          if (calculationsToAdd > pairingWithMostCalculations.getCalculations()) {
            int index = random.nextInt(pairings.size());
            pairingToAdd = pairings.get(index);
          } else {
            for (int j = 1; j < pairings.size(); ++j) {
              PartitionPairing pairing = pairings.get(j);
              if (pairing.getCalculations() > calculationsToAdd) {
                pairingToAdd = pairings.get(j - 1);
                break;
              }
            }
            
            if (pairingToAdd == null) {
              pairingToAdd = pairings.get(pairings.size() - 1);
            }
          }
          pairings.remove(pairingToAdd);
          calculations += pairingToAdd.getCalculations();
          
          packageDescriptor.addPartitionPairing(pairingToAdd);
          Log.info(String.format("\tAdding %s\t%,16d calculations of at least %,16d", pairingToAdd.toString(), calculations, calculationsPerPackage));
        }
        
        Log.info(String.format("\tPackage %08d has now %,d calculations in %,d partitionPairings", i, calculations, packageDescriptor.getPairings()));
        packageDescriptor.close();
      }
      
      if (!pairings.isEmpty()) {
        throw new UnableToComplyException("Pairings was not empty - it contined " + pairings.size() + " pairings that have not been put into a package");
      }
      
      Log.info(String.format("Created %,d packages - done.", packageQuantity));
      
      if (logWriter != null) {
        logWriter.close();
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (UnableToComplyException e) {
      throw e;
    } catch (Exception e) {
      throw new UnableToComplyException(e);
    }
  }

  private FileLogWriter appendStatisticsWriter(File outputDir) throws IOException {
    File resultsFolder = new File(outputDir, "results");
    if (!resultsFolder.exists()) {
      resultsFolder.mkdirs();
    }
    File statisticsFile = new File(resultsFolder, "statistics.txt");
    Log.info("Storing statistics in file " + statisticsFile);
    FileLogWriter fileLogWriter = new FileLogWriter(statisticsFile);
    Log.addLogWriter(fileLogWriter);
    return fileLogWriter;
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
