package de.lmu.ifi.dbs.elki.index.preprocessed.knn;

import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * A preprocessor for annotation of the k nearest neighbors (and their
 * distances) to each database object.
 * 
 * Used for example by {@link de.lmu.ifi.dbs.elki.algorithm.outlier.LOF}.
 * 
 * @author Erich Schubert
 * 
 * @param <O> the type of database objects the preprocessor can be applied to
 * @param <D> the type of distance the used distance function will return
 */
@Title("Partitioning Approximate kNN Preprocessor")
@Description("Caterializes the (approximate) k nearest neighbors of objects of a database by partitioning and only computing kNN within each partition.")
public class PartitionApproximationMaterializeKNNPreprocessor<O, D extends Distance<D>> extends AbstractMaterializeKNNPreprocessor<O, D> {
  // TODO: randomize/shuffle?

  /**
   * Logger to use
   */
  private static final Logging logger = Logging.getLogger(PartitionApproximationMaterializeKNNPreprocessor.class);

  /**
   * Number of partitions to use.
   */
  private final int partitions;

  /**
   * Constructor
   * 
   * @param representation Representation to process
   * @param distanceFunction the distance function to use
   * @param k query k
   * @param partitions Number of partitions
   */
  public PartitionApproximationMaterializeKNNPreprocessor(Relation<O> representation, DistanceFunction<? super O, D> distanceFunction, int k, int partitions) {
    super(representation, distanceFunction, k);
    this.partitions = partitions;
    // preprocess now
    preprocess();
  }

  @Override
  protected void preprocess() {
    DistanceQuery<O, D> distanceQuery = rep.getDatabase().getDistanceQuery(rep, distanceFunction);
    storage = DataStoreUtil.makeStorage(rep.getDBIDs(), DataStoreFactory.HINT_STATIC, List.class);
    MeanVariance ksize = new MeanVariance();
    if(logger.isVerbose()) {
      logger.verbose("Approximating nearest neighbor lists to database objects");
    }

    ArrayDBIDs aids = DBIDUtil.ensureArray(rep.getDBIDs());
    int minsize = (int) Math.floor(aids.size() / partitions);

    FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Processing partitions.", partitions, logger) : null;
    for(int part = 0; part < partitions; part++) {
      int size = (partitions * minsize + part >= aids.size()) ? minsize : minsize + 1;
      // Collect the ids in this node.
      ArrayModifiableDBIDs ids = DBIDUtil.newArray(size);
      for(int i = 0; i < size; i++) {
        assert (size * partitions + part < aids.size());
        ids.add(aids.get(i * partitions + part));
      }
      HashMap<DBIDPair, D> cache = new HashMap<DBIDPair, D>(size * size * 3 / 8);
      for(DBID id : ids) {
        KNNHeap<D> kNN = new KNNHeap<D>(k, distanceQuery.infiniteDistance());
        for(DBID id2 : ids) {
          DBIDPair key = DBIDUtil.newPair(id, id2);
          D d = cache.remove(key);
          if(d != null) {
            // consume the previous result.
            kNN.add(new DistanceResultPair<D>(d, id2));
          }
          else {
            // compute new and store the previous result.
            d = distanceQuery.distance(id, id2);
            kNN.add(new DistanceResultPair<D>(d, id2));
            // put it into the cache, but with the keys reversed
            key = DBIDUtil.newPair(id2, id);
            cache.put(key, d);
          }
        }
        ksize.put(kNN.size());
        storage.put(id, kNN.toSortedArrayList());
      }
      if(logger.isDebugging()) {
        if(cache.size() > 0) {
          logger.warning("Cache should be empty after each run, but still has " + cache.size() + " elements.");
        }
      }
      if(progress != null) {
        progress.incrementProcessed(logger);
      }
    }
    if(progress != null) {
      progress.ensureCompleted(logger);
    }
    if(logger.isVerbose()) {
      logger.verbose("On average, " + ksize.getMean() + " +- " + ksize.getSampleStddev() + " neighbors returned.");
    }
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public String getLongName() {
    return "Random partition kNN approximation";
  }

  @Override
  public String getShortName() {
    return "random-partition-knn";
  }

  /**
   * The parameterizable factory.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses PartitionApproximationMaterializeKNNPreprocessor oneway - -
   *              «create»
   * 
   * @param <O> The object type
   * @param <D> The distance type
   */
  public static class Factory<O, D extends Distance<D>> extends AbstractMaterializeKNNPreprocessor.Factory<O, D> {
    /**
     * Parameter to specify the number of partitions to use for materializing
     * the kNN. Must be an integer greater than 1.
     * <p>
     * Key: {@code -partknn.p}
     * </p>
     */
    public static final OptionID PARTITIONS_ID = OptionID.getOrCreateOptionID("partknn.p", "The number of partitions to use for approximate kNN.");

    /**
     * The number of partitions to use
     */
    int partitions;

    /**
     * Constructor.
     * 
     * @param k k
     * @param distanceFunction distance function
     * @param partitions number of partitions
     */
    public Factory(int k, DistanceFunction<? super O, D> distanceFunction, int partitions) {
      super(k, distanceFunction);
      this.partitions = partitions;
    }

    @Override
    public PartitionApproximationMaterializeKNNPreprocessor<O, D> instantiate(Relation<O> representation) {
      PartitionApproximationMaterializeKNNPreprocessor<O, D> instance = new PartitionApproximationMaterializeKNNPreprocessor<O, D>(representation, distanceFunction, k, partitions);
      return instance;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer<O, D extends Distance<D>> extends AbstractMaterializeKNNPreprocessor.Factory.Parameterizer<O, D> {
      protected int partitions = 0;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        final IntParameter partitionsP = new IntParameter(PARTITIONS_ID, new GreaterConstraint(1));
        if(config.grab(partitionsP)) {
          partitions = partitionsP.getValue();
        }
      }

      @Override
      protected Factory<O, D> makeInstance() {
        return new Factory<O, D>(k, distanceFunction, partitions);
      }
    }
  }
}