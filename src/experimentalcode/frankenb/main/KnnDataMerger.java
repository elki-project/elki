package experimentalcode.frankenb.main;

import java.io.File;
import java.io.FilenameFilter;

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.frankenb.model.ConstantSizeIntegerDBIDSerializer;
import experimentalcode.frankenb.model.DistanceList;
import experimentalcode.frankenb.model.DistanceListSerializer;
import experimentalcode.frankenb.model.DynamicBPlusTree;
import experimentalcode.frankenb.model.PackageDescriptor;
import experimentalcode.frankenb.model.PartitionPairing;
import experimentalcode.frankenb.model.PrecalculatedKnnIndex;
import experimentalcode.frankenb.model.datastorage.BufferedDiskBackedDataStorage;
import experimentalcode.frankenb.model.datastorage.DiskBackedDataStorage;

/**
 * This class merges the results precalculated on the cluster network to a
 * single precalculated knn file with a given k neighbors for each point. The
 * result files are written to <code>app.out</code> and can be used as an kNN
 * index using the {@link PrecalculatedKnnIndex} class in ELKI.
 * <p/>
 * Note that there is an optional <code>-inmemory</code> switch which forces the
 * algorithm to hold the resulting b-plus-tree in memory while merging - this
 * speeds up the merging significantly but it also can consume all memory when
 * having huge data sets with a high amount of k leading to an
 * OutOfMemoryException.
 * <p/>
 * Usage: <br/>
 * <code>-dbc.parser DoubleVectorLabelParser -dbc.in dataset.csv -app.out /tmp/index -app.in /tmp/divided -k 10</code>
 * 
 * @author Florian Frankenberger
 */
public class KnnDataMerger extends AbstractApplication {
  /**
   * The logger
   */
  private static final Logging logger = Logging.getLogger(KnnDataMerger.class);

  /**
   * Parameter that specifies the number of neighbors to keep with respect to
   * the definition of a k-nearest neighbor.
   * <p>
   * Key: {@code -k}
   * </p>
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("k", "");

  public static final OptionID IN_MEMORY_ID = OptionID.getOrCreateOptionID("inmemory", "tells wether the resulting tree data should be buffered in memory or not. This can increase performance but can also lead to OutOfMemoryExceptions!");

  private int k;

  private boolean inMemory = false;

  /**
   * The input directory
   */
  private File indir;

  /**
   * The output directory
   */
  private File outdir;

  /**
   * Constructor.
   * 
   * @param verbose
   * @param indir
   * @param outdir
   * @param k
   * @param inMemory
   */
  public KnnDataMerger(boolean verbose, File indir, File outdir, int k, boolean inMemory) {
    super(verbose);
    this.indir = indir;
    this.outdir = outdir;
    this.k = k;
    this.inMemory = inMemory;
  }

  @Override
  public void run() throws UnableToComplyException {
    try {
      logger.verbose("Start merging data");
      logger.verbose("using inMemory strategy: " + Boolean.toString(inMemory));
      logger.verbose("maximum k to calculate: " + k);

      File[] packageDirectories = indir.listFiles(new FilenameFilter() {

        @Override
        public boolean accept(File dir, String name) {
          return name.matches("^package[0-9]{5}$");
        }

      });

      File resultDirectory = new File(outdir, "result.dir");
      if(resultDirectory.exists())
        resultDirectory.delete();
      File resultData = new File(outdir, "result.dat");
      if(resultData.exists())
        resultData.delete();

      DynamicBPlusTree<DBID, DistanceList> resultTree = new DynamicBPlusTree<DBID, DistanceList>(new BufferedDiskBackedDataStorage(resultDirectory), (inMemory ? new BufferedDiskBackedDataStorage(resultData) : new DiskBackedDataStorage(resultData)), new ConstantSizeIntegerDBIDSerializer(), new DistanceListSerializer(), 8);

      // open all result files
      HashSetModifiableDBIDs testSet = DBIDUtil.newHashSet();
      for(File packageDirectory : packageDirectories) {
        File[] packageDescriptorCandidates = packageDirectory.listFiles(new FilenameFilter() {

          @Override
          public boolean accept(File dir, String name) {
            return name.matches("^package[0-9]{5}_descriptor.dat$");
          }

        });

        if(packageDescriptorCandidates.length > 0) {
          logger.verbose("Opening result of " + packageDirectory.getName() + " ...");
          File packageDescriptorFile = packageDescriptorCandidates[0];
          PackageDescriptor packageDescriptor = PackageDescriptor.readFromStorage(new BufferedDiskBackedDataStorage(packageDescriptorFile));

          int counter = 0;
          for(PartitionPairing pairing : packageDescriptor) {
            if(!pairing.hasResult()) {
              throw new UnableToComplyException("Package " + packageDescriptorFile + "/pairing " + pairing + " has no results!");
            }
            DynamicBPlusTree<DBID, DistanceList> result = packageDescriptor.getResultTreeFor(pairing);
            logger.verbose(String.format("\tprocessing result of pairing %05d of %05d (%s) - %6.2f%% ...", ++counter, packageDescriptor.getPairings(), pairing, (counter / (float) packageDescriptor.getPairings()) * 100f));

            for(Pair<DBID, DistanceList> resultEntry : result) {
              DistanceList distanceList = resultTree.get(resultEntry.first);
              if(distanceList == null) {
                distanceList = new DistanceList(resultEntry.first, k);
              }
              testSet.add(resultEntry.first);
              distanceList.addAll(resultEntry.second);
              resultTree.put(resultEntry.first, distanceList);
            }
          }
        }
        else {
          logger.warning("Skipping directory " + packageDirectory + " because the package descriptor could not be found.");
        }
      }

      logger.verbose("result tree items: " + resultTree.getSize());
      resultTree.close();
      logger.verbose("TestSet has " + testSet.size() + " items");
      logger.verbose("Created result tree with " + resultTree.getSize() + " entries.");
    }
    catch(Exception e) {
      logger.error("Could not merge data", e);
    }

  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractApplication.Parameterizer {
    private File indir = null;

    private File outdir = null;

    private int k = 0;

    private boolean inMemory = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      indir = getParameterInputFile(config, "Input directory.");
      outdir = getParameterOutputFile(config, "Output directory.");

      final IntParameter K_PARAM = new IntParameter(K_ID, false);
      if(config.grab(K_PARAM)) {
        k = K_PARAM.getValue();
      }

      final Flag IN_MEMORY_PARAM = new Flag(IN_MEMORY_ID);
      if(config.grab(IN_MEMORY_PARAM)) {
        inMemory = IN_MEMORY_PARAM.getValue();
      }
    }

    @Override
    protected KnnDataMerger makeInstance() {
      return new KnnDataMerger(verbose, indir, outdir, k, inMemory);
    }
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    AbstractApplication.runCLIApplication(KnnDataMerger.class, args);
  }
}