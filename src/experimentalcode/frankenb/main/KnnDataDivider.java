package experimentalcode.frankenb.main;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.HashmapDatabase;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import experimentalcode.frankenb.model.PackageDescriptor;
import experimentalcode.frankenb.model.PartitionPairing;
import experimentalcode.frankenb.model.datastorage.BufferedDiskBackedDataStorage;
import experimentalcode.frankenb.model.ifaces.IDividerAlgorithm;

/**
 * This application divides a given database into a given numbers of packages to
 * calculate knn on a distributed system like the sun grid engine
 * <p />
 * Example usage: <br />
 * <code>-dbc.parser DoubleVectorLabelParser -dbc.in dataset.csv -app.out /tmp/divided -packages 10 -algorithm experimentalcode.frankenb.algorithms.RandomProjectionVectorApproximationCrossPairingDividerAlgorithm -partitions 100 -crosspairings 0.0</code>
 * 
 * @author Florian Frankenberger
 */
public class KnnDataDivider<V extends NumberVector<V, ?>> extends AbstractApplication {
  /**
   * The logger
   */
  private static final Logging logger = Logging.getLogger(KnnDataDivider.class);

  public static final OptionID DIVIDER_ALGORITHM_ID = OptionID.getOrCreateOptionID("algorithm", "A divider algorithm to use");

  /**
   * Parameter that specifies the number of segments to create (= # of
   * computers)
   * <p>
   * Key: {@code -packages}
   * </p>
   */
  public static final OptionID PACKAGES_ID = OptionID.getOrCreateOptionID("packages", "");

  private int packageQuantity = 0;

  private final Database database;

  private IDividerAlgorithm<V> algorithm;

  File outputDir;

  /**
   * @param verbose
   * @param outputDir
   * @param packageQuantity
   * @param database
   * @param algorithm
   */
  public KnnDataDivider(boolean verbose, File outputDir, int packageQuantity, Database database, IDividerAlgorithm<V> algorithm) {
    super(verbose);
    this.outputDir = outputDir;
    this.packageQuantity = packageQuantity;
    this.database = database;
    this.algorithm = algorithm;
  }

  @Override
  public void run() throws UnableToComplyException {
    // FileLogWriter logWriter = null;
    try {
      if(outputDir.isFile()) {
        throw new UnableToComplyException("You need to specify an output directory not a file!");
      }
      if(!outputDir.exists()) {
        if(!outputDir.mkdirs()) {
          throw new UnableToComplyException("Could not create output directory");
        }
      }

      logger.verbose("cleaning output directory...");
      clearDirectory(outputDir);

      // logWriter = appendStatisticsWriter(outputDir);
      // logger.verbose("knn data divider started @ " + new
      // SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date(new
      // Date().getTime() - Log.getElapsedTime())));

      logger.verbose("reading database ...");
      database.initialize();
      Relation<V> relation = database.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);
      int dim = DatabaseUtil.assumeVectorField(relation).dimensionality();
      long totalCalculationsWithoutApproximation = MathUtil.sumFirstIntegers(relation.size() - 1);

      logger.verbose(String.format("DB Size: %,d (%d dimensions)", relation.size(), dim));
      logger.verbose(String.format("Packages to create: %,8d", packageQuantity));
      logger.verbose(String.format("Algorithm used: %s", algorithm.getClass().getSimpleName()));

      logger.verbose(String.format("Creating partitions (algorithm used: %s) ...", algorithm.getClass().getSimpleName()));

      List<PartitionPairing> pairings = this.algorithm.divide(relation, packageQuantity);

      logger.verbose(String.format("Total partition pairings: %,d", pairings.size()));

      logger.verbose("Counting calculations ...");
      long totalCalculations = 0;
      for(PartitionPairing pairing : pairings) {
        totalCalculations += pairing.getCalculations();
      }

      long calculationsPerPackage = (long) Math.ceil(totalCalculations / (double) packageQuantity);
      int expectedPairingsPerPackage = pairings.size() / packageQuantity;

      logger.verbose(String.format("Total calculations necessary: %,d (%.2f%% of cal. w/ apprx.)", totalCalculations, (totalCalculations / (float) totalCalculationsWithoutApproximation) * 100f));
      logger.verbose(String.format("Calculations per package: about %,d", calculationsPerPackage));

      logger.verbose("Sorting pairings ...");
      Collections.sort(pairings, new Comparator<PartitionPairing>() {
        @Override
        public int compare(PartitionPairing o1, PartitionPairing o2) {
          return Long.valueOf(o1.getCalculations()).compareTo(o2.getCalculations());
        }
      });

      Random random = new Random(System.currentTimeMillis());
      logger.verbose("Storing packages ...");
      for(int i = 0; i < packageQuantity && !pairings.isEmpty(); ++i) {
        long calculations = 0L;

        File targetDirectory = new File(outputDir, String.format("package%05d", i));
        targetDirectory.mkdirs();

        File packageDescriptorFile = new File(targetDirectory, String.format("package%05d_descriptor.dat", i));
        int bufferSize = expectedPairingsPerPackage * PackageDescriptor.PAIRING_DATA_SIZE + PackageDescriptor.HEADER_SIZE;
        PackageDescriptor<V> packageDescriptor = new PackageDescriptor<V>(i + 1, dim, new BufferedDiskBackedDataStorage(packageDescriptorFile, bufferSize));

        logger.verbose(String.format("Creating package %08d of max. %08d (%s)", i + 1, packageQuantity, packageDescriptorFile.toString()));

        while(!pairings.isEmpty() && calculations < calculationsPerPackage) {
          long calculationsToAdd = calculationsPerPackage - calculations;
          PartitionPairing pairingWithMostCalculations = pairings.get(pairings.size() - 1);
          PartitionPairing pairingToAdd = null;

          // if the necessary calculations for this package exceeds the biggest
          // partition pairing
          // we choose one randomly - otherwise we choose the one that fits best
          if(calculationsToAdd > pairingWithMostCalculations.getCalculations()) {
            int index = random.nextInt(pairings.size());
            pairingToAdd = pairings.get(index);
          }
          else {
            for(int j = 1; j < pairings.size(); ++j) {
              PartitionPairing pairing = pairings.get(j);
              if(pairing.getCalculations() > calculationsToAdd) {
                pairingToAdd = pairings.get(j - 1);
                break;
              }
            }

            if(pairingToAdd == null) {
              pairingToAdd = pairings.get(pairings.size() - 1);
            }
          }
          pairings.remove(pairingToAdd);
          calculations += pairingToAdd.getCalculations();

          packageDescriptor.addPartitionPairing(relation, pairingToAdd);
          logger.verbose(String.format("\tAdding %s\t%,16d calculations of at least %,16d", pairingToAdd.toString(), calculations, calculationsPerPackage));
        }

        logger.verbose(String.format("\tPackage %08d has now %,d calculations in %,d partitionPairings", i, calculations, packageDescriptor.getPairings()));
        packageDescriptor.close();
      }

      if(!pairings.isEmpty()) {
        throw new UnableToComplyException("Pairings was not empty - it contined " + pairings.size() + " pairings that have not been put into a package");
      }

      logger.verbose(String.format("Created %,d packages - done.", packageQuantity));

      /*
       * if (logWriter != null) { logWriter.close(); }
       */
    }
    catch(RuntimeException e) {
      throw e;
    }
    catch(UnableToComplyException e) {
      throw e;
    }
    catch(Exception e) {
      throw new UnableToComplyException(e);
    }
  }

  /*
   * private FileLogWriter appendStatisticsWriter(File outputDir) throws
   * IOException { File resultsFolder = new File(outputDir, "results"); if
   * (!resultsFolder.exists()) { resultsFolder.mkdirs(); } File statisticsFile =
   * new File(resultsFolder, "statistics.txt");
   * logger.verbose("Storing statistics in file " + statisticsFile);
   * FileLogWriter fileLogWriter = new FileLogWriter(statisticsFile);
   * Log.addLogWriter(fileLogWriter); return fileLogWriter; }
   */

  private static void clearDirectory(File directory) throws UnableToComplyException {
    for(File file : directory.listFiles()) {
      if(file.equals(directory) || file.equals(directory.getParentFile())) {
        continue;
      }
      if(file.isDirectory()) {
        clearDirectory(file);
      }
      if(!file.delete()) {
        throw new UnableToComplyException("Could not delete " + file + ".");
      }
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<V, ?>> extends AbstractApplication.Parameterizer {
    private int packageQuantity = 0;

    private Database database = null;

    private IDividerAlgorithm<V> algorithm = null;

    File outputDir;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      outputDir = getParameterOutputFile(config, "Output directory.");

      final IntParameter PACKAGES_PARAM = new IntParameter(PACKAGES_ID, false);
      if(config.grab(PACKAGES_PARAM)) {
        packageQuantity = PACKAGES_PARAM.getValue();
      }

      final ObjectParameter<IDividerAlgorithm<V>> paramPartitioner = new ObjectParameter<IDividerAlgorithm<V>>(DIVIDER_ALGORITHM_ID, IDividerAlgorithm.class, false);
      if(config.grab(paramPartitioner)) {
        algorithm = paramPartitioner.instantiateClass(config);
      }

      database = config.tryInstantiate(HashmapDatabase.class);
    }

    @Override
    protected KnnDataDivider<V> makeInstance() {
      return new KnnDataDivider<V>(verbose, outputDir, packageQuantity, database, algorithm);
    }
  }

  public static void main(String[] args) {
    AbstractApplication.runCLIApplication(KnnDataDivider.class, args);
  }
}