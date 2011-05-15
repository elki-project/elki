package experimentalcode.frankenb.main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveNumberDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.frankenb.algorithms.partitioning.DBIDPartition;
import experimentalcode.frankenb.model.DistanceList;
import experimentalcode.frankenb.model.DistanceListSerializer;
import experimentalcode.frankenb.model.DynamicBPlusTree;
import experimentalcode.frankenb.model.PackageDescriptor;
import experimentalcode.frankenb.model.PartitionPairing;
import experimentalcode.frankenb.model.datastorage.BufferedDiskBackedDataStorage;
import experimentalcode.frankenb.model.datastorage.DiskBackedDataStorage;

/**
 * This class calculates the distances given in denoted package and creates a
 * result file containing the <code>maxk</code> neighbors of each point. This
 * file can in turn be merged to one index file using the KnnDataMerger.
 * <p />
 * This class has to be executed for all packages - normally this is done by
 * distributing the packages to different cluster nodes and then executing this
 * class on each of them.
 * <p />
 * Note that this implementation does not need access to the original data set
 * because all data has been stored in the same folder as the package.
 * <p />
 * Also note that this implementation supports multithreading with the optional
 * switch <code>-multithreading</code>.
 * 
 * <p />
 * Example usage: <br />
 * <code>-app.in /tmp/divided/package00004/package00004_descriptor.dat -knn.reachdistfunction  EuclideanDistanceFunction -maxk 100</code>
 * 
 * @author Florian Frankenberger
 */
public class KnnDataProcessor<V extends NumberVector<V, ?>> extends AbstractApplication {
  /**
   * The logger
   */
  static final Logging logger = Logging.getLogger(KnnDataProcessor.class);

  /**
   * Parameter that specifies the name of the input file.
   * <p>
   * Key: {@code -app.in}
   * </p>
   */
  public static final OptionID INPUT_ID = OptionID.getOrCreateOptionID("app.in", "");

  public static final OptionID MULTI_THREADING_ID = OptionID.getOrCreateOptionID("multithreading", "tells wether to use as much threads as cpus are available or not (default is false)");

  /**
   * Parameter that specifies the number of neighbors to keep with respect to
   * the definition of a k-nearest neighbor.
   * <p>
   * Key: {@code -k}
   * </p>
   */
  public static final OptionID MAXK_ID = OptionID.getOrCreateOptionID("maxk", "");

  /**
   * Holds the value of {@link #INPUT_PARAM}.
   */
  private File input;

  private int maxK;

  private boolean multiThreaded = false;

  /**
   * The distance function to determine the reachability distance between
   * database objects.
   */
  public static final OptionID REACHABILITY_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("knn.reachdistfunction", "Distance function to determine the reachability distance between database objects.");

  private final PrimitiveNumberDistanceFunction<V, DoubleDistance> distanceAlgorithm;

  private int totalTasks;

  private long totalItems;

  /**
   * Constructor.
   * 
   * @param verbose
   * @param input
   * @param maxK
   * @param distanceAlgorithm
   * @param multiThreaded
   */
  public KnnDataProcessor(boolean verbose, File input, int maxK, PrimitiveNumberDistanceFunction<V, DoubleDistance> distanceAlgorithm, boolean multiThreaded) {
    super(verbose);
    this.input = input;
    this.maxK = maxK;
    this.distanceAlgorithm = distanceAlgorithm;
    this.multiThreaded = multiThreaded;
  }

  @Override
  public void run() throws UnableToComplyException {
    Runtime runtime = Runtime.getRuntime();
    final ExecutorService threadPool = Executors.newFixedThreadPool((multiThreaded ? runtime.availableProcessors() : 1));

    try {
      logger.verbose("started processing");
      logger.verbose("multithreaded: " + Boolean.valueOf(multiThreaded));
      logger.verbose("maximum k to calculate: " + maxK);
      logger.verbose(String.format("opening package %s ...", input));
      final PackageDescriptor<?> packageDescriptor = PackageDescriptor.readFromStorage(new DiskBackedDataStorage(input));

      logger.verbose("Verifying package ...");
      packageDescriptor.verify();

      totalTasks = 0;
      totalItems = 0;

      // create a thread pool with that many processes that there are processors
      // available

      List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();

      logger.verbose("Creating tasks ...");
      List<Callable<Boolean>> tasks = new ArrayList<Callable<Boolean>>();
      for(final PartitionPairing pairing : packageDescriptor) {
        if(pairing.getPartitionOne().getSize() < 1 || pairing.getPartitionTwo().getSize() < 1) {
          throw new UnableToComplyException("Pairing " + pairing + " has 0 items");
        }

        if(pairing.hasResult()) {
          // logger.verbose(String.format("Skipping pairing of partition%05d with partition%05d - as it already contains a result",
          // pairing.getPartitionOne().getId(),
          // pairing.getPartitionTwo().getId()));
          continue;
        }

        final int taskId = ++totalTasks;

        Callable<Boolean> task = new Callable<Boolean>() {
          @Override
          public Boolean call() throws Exception {
            try {
              logger.verbose(String.format("Processing pairing %010d of %010d (%010d in package)...", taskId, totalTasks, packageDescriptor.getPairings()));

              // heuristic to determine the bucket size based on a tree height
              // of about 21
              int maxKeysPerBucket = (int) Math.max(5, Math.floor(Math.pow(pairing.getEstimatedUniqueIdsAmount(), 1f / 20f)));
              logger.verbose(String.format("maxKeysPerBucket in tree are: %,d for %,d items", maxKeysPerBucket, pairing.getEstimatedUniqueIdsAmount()));

              File tmpDirFile = File.createTempFile("pairing" + taskId, ".dir");
              File tmpDataFile = File.createTempFile("pairing" + taskId, ".dat");

              DynamicBPlusTree<DBID, DistanceList> resultTree = new DynamicBPlusTree<DBID, DistanceList>(new BufferedDiskBackedDataStorage(tmpDirFile), new DiskBackedDataStorage(tmpDataFile), DBIDFactory.FACTORY.getDBIDSerializerStatic(), new DistanceListSerializer(), maxKeysPerBucket);
              ModifiableDBIDs processedIds = DBIDUtil.newHashSet();

              //logger.verbose(String.format("\tPairing %010d: partition%05d (%,d items) with partition%05d (%,d items)", taskId, pairing.getPartitionOne().getId(), pairing.getPartitionOne().getSize(), pairing.getPartitionTwo().getId(), pairing.getPartitionTwo().getSize()));

              List<Pair<DBIDPartition, DBIDPartition>> partitionsToProcess = new ArrayList<Pair<DBIDPartition, DBIDPartition>>(2);
              partitionsToProcess.add(new Pair<DBIDPartition, DBIDPartition>(pairing.getPartitionOne(), pairing.getPartitionTwo()));
              partitionsToProcess.add(new Pair<DBIDPartition, DBIDPartition>(pairing.getPartitionTwo(), pairing.getPartitionOne()));

              // we make two passes here as the calculation is much faster than
              // deserializing the distanceLists all the
              // time to check if we already processed that result. Furthermore
              // a hash list with the processed pairs has also
              // the drawback of using too much memory - especially when more
              // threads run simultaneously - the memory usage is n^2 per thread
              for(int i = 0; i < (pairing.isSelfPairing() ? 1 : partitionsToProcess.size()); ++i) {
                Pair<DBIDPartition, DBIDPartition> partitions = partitionsToProcess.get(i);
                int counter = 0;
                for(Pair<DBID, V> pointOne : partitions.first) {
                  if(counter++ % 50 == 0) {
                    logger.verbose(String.format("\t\tPairing %010d: Processed %,d of %,d items ...", taskId, counter, partitions.first.getSize()));
                  }

                  for(Pair<DBID, V> pointTwo : partitions.second) {
                    double distance = distanceAlgorithm.doubleDistance(pointOne.getSecond(), pointTwo.getSecond());
                    persistDistance(resultTree, processedIds, pointOne, pointTwo, distance);
                  }
                }
              }

              packageDescriptor.setResultFor(pairing, resultTree);

              addToTotalItems(pairing.getEstimatedUniqueIdsAmount());

              tmpDirFile.delete();
              tmpDataFile.delete();

              resultTree.close();
            }
            catch(Exception e) {
              logger.error(String.format("Problem in pairing %s: %s", pairing, e.getMessage()), e);
              return false;
            }
            finally {
              logger.verbose(String.format("Pairing %d done.", taskId));
            }
            return true;
          }
        };

        tasks.add(task);
      }

      // add all tasks
      logger.verbose("Adding all tasks ...");
      for(Callable<Boolean> task : tasks) {
        futures.add(threadPool.submit(task));
      }

      // wait for all tasks to finish
      logger.verbose("Waiting for all tasks to finish ...");
      for(Future<Boolean> future : futures) {
        future.get();
      }

      if(futures.size() > 0) {
        logger.verbose(String.format("Calculated and stored %,d distances.", totalItems));
      }
      else {
        logger.verbose("Nothing to do - all results have already been calculated");
      }

    }
    catch(RuntimeException e) {
      logger.error("Runtime Exception: " + e.getMessage(), e);
      throw e;
    }
    catch(Exception e) {
      logger.error("Exception: " + e.getMessage(), e);
      throw new UnableToComplyException(e);
    }
    finally {
      logger.verbose("Shutting down thread pool ...");
      Thread terminationThread = new Thread() {
        public void run() {
          try {
            Thread.sleep(10000);
          }
          catch(InterruptedException e) {
          }
          logger.verbose("Exiting.");
          System.exit(0);
        }
      };
      terminationThread.start();
      threadPool.shutdownNow();
    }

  }

  private synchronized void addToTotalItems(long items) {
    totalItems += items;
  }

  private void persistDistance(DynamicBPlusTree<DBID, DistanceList> resultTree, ModifiableDBIDs processedIds, Pair<DBID, V> fromPoint, Pair<DBID, V> toPoint, double distance) throws IOException {
    DistanceList distanceList = null;
    if(processedIds.contains(fromPoint.getFirst())) {
      distanceList = resultTree.get(fromPoint.getFirst());
    }
    else {
      distanceList = new DistanceList(fromPoint.getFirst(), maxK);
      processedIds.add(fromPoint.getFirst());
    }
    distanceList.addDistance(toPoint.getFirst(), distance);
    resultTree.put(fromPoint.getFirst(), distanceList);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<V, ?>> extends AbstractApplication.Parameterizer {
    File input = null;

    int maxK = 0;

    boolean multiThreaded = false;

    private PrimitiveNumberDistanceFunction<V, DoubleDistance> distanceAlgorithm = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final FileParameter inputP = new FileParameter(INPUT_ID, FileParameter.FileType.INPUT_FILE);
      inputP.setShortDescription("The package descriptor (usually an .xml-file)");
      if(config.grab(inputP)) {
        input = inputP.getValue();
      }

      final IntParameter maxKP = new IntParameter(MAXK_ID, false);
      if(config.grab(maxKP)) {
        maxK = maxKP.getValue();
      }

      final Flag multiThreadedP = new Flag(MULTI_THREADING_ID);
      if(config.grab(multiThreadedP)) {
        multiThreaded = multiThreadedP.getValue();
      }

      configParameterReachabilityDistanceFunction(config);
    }

    /**
     * Grab the reachability distance configuration option.
     * 
     * @param config Parameterization
     * @return Parameter value or null.
     */
    protected void configParameterReachabilityDistanceFunction(Parameterization config) {
      final ObjectParameter<PrimitiveNumberDistanceFunction<V, DoubleDistance>> param = new ObjectParameter<PrimitiveNumberDistanceFunction<V, DoubleDistance>>(REACHABILITY_DISTANCE_FUNCTION_ID, PrimitiveNumberDistanceFunction.class, true);
      if(config.grab(param)) {
        distanceAlgorithm = param.instantiateClass(config);
      }
    }

    @Override
    protected KnnDataProcessor<V> makeInstance() {
      return new KnnDataProcessor<V>(verbose, input, maxK, distanceAlgorithm, multiThreaded);
    }
  }

  public static void main(String[] args) {
    AbstractApplication.runCLIApplication(KnnDataProcessor.class, args);
  }
}