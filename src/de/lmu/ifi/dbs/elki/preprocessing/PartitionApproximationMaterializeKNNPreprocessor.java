package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * A preprocessor for annotation of the k nearest neighbors (and their
 * distances) to each database object.
 * 
 * Used for example by {@link de.lmu.ifi.dbs.elki.algorithm.outlier.LOF}.
 * 
 * @author Erich Schubert
 * @param <O> the type of database objects the preprocessor can be applied to
 * @param <D> the type of distance the used distance function will return
 */
@Title("Partitioning Approximate kNN Preprocessor")
@Description("Caterializes the (approximate) k nearest neighbors of objects of a database by partitioning and only computing kNN within each partition.")
public class PartitionApproximationMaterializeKNNPreprocessor<O extends DatabaseObject, D extends Distance<D>> extends MaterializeKNNPreprocessor<O, D> {
  /**
   * OptionID for {@link #PARTITIONS_PARAM}
   */
  public static final OptionID PARTITIONS_ID = OptionID.getOrCreateOptionID("partknn.p", "The number of partitions to use for approximate kNN.");

  /**
   * Parameter to specify the number of partitions to use for materializing the
   * kNN. Must be an integer greater than 1.
   * <p>
   * Key: {@code -partknn.p}
   * </p>
   */
  private final IntParameter PARTITIONS_PARAM = new IntParameter(PARTITIONS_ID, new GreaterConstraint(1));

  /**
   * The number of partitions to use
   */
  int partitions;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public PartitionApproximationMaterializeKNNPreprocessor(Parameterization config) {
    super(config);
    if(config.grab(PARTITIONS_PARAM)) {
      partitions = PARTITIONS_PARAM.getValue();
    }
  }

  @Override
  public <T extends O> Instance<T, D> instantiate(Database<T> database) {
    return new Instance<T, D>(database, distanceFunction, k, partitions);
  }

  /**
   * The actual preprocessor instance.
   * 
   * @author Erich Schubert
   * 
   * @param <O> The object type
   * @param <D> The distance type
   */
  public static class Instance<O extends DatabaseObject, D extends Distance<D>> extends MaterializeKNNPreprocessor.Instance<O, D> {
    /**
     * Logger to use
     */
    private static Logging logger = Logging.getLogger(PartitionApproximationMaterializeKNNPreprocessor.class);
    
    /**
     * Number of partitions to use.
     */
    private final int partitions;

    /**
     * Constructor
     * 
     * @param database Database to preprocess
     * @param distanceFunction The distance function to use.
     * @param k query k
     * @param partitions Number of partitions
     */
    public Instance(Database<O> database, DistanceFunction<? super O, D> distanceFunction, int k, int partitions) {
      super(database, distanceFunction, k);
      this.partitions = partitions;
    }

    @Override
    protected void preprocess(Database<O> database, DistanceFunction<? super O, D> distanceFunction) {
      DistanceQuery<O, D> distanceQuery = database.getDistanceQuery(distanceFunction);
      materialized = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, List.class);
      MeanVariance ksize = new MeanVariance();
      if(logger.isVerbose()) {
        logger.verbose("Approximating nearest neighbor lists to database objects");
      }

      ArrayDBIDs aids = DBIDUtil.ensureArray(database.getIDs());
      int minsize = (int) Math.floor(aids.size() / partitions);

      FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Processing partitions.", partitions, logger) : null;
      for(int part = 0; part < partitions; part++) {
        int size = (partitions * minsize + part >= aids.size()) ? minsize : minsize + 1;
        // Collect the ids in this node.
        DBID[] ids = new DBID[size];
        for(int i = 0; i < size; i++) {
          assert (size * partitions < aids.size());
          ids[i] = aids.get(part + i * partitions);
        }
        HashMap<Pair<DBID, DBID>, D> cache = new HashMap<Pair<DBID, DBID>, D>(size * size * 3 / 8);
        for(DBID id : ids) {
          KNNHeap<D> kNN = new KNNHeap<D>(k, distanceQuery.infiniteDistance());
          for(DBID id2 : ids) {
            if(id.compareTo(id2) == 0) {
              kNN.add(new DistanceResultPair<D>(distanceQuery.distance(id, id2), id2));
            }
            else {
              Pair<DBID, DBID> key = new Pair<DBID, DBID>(id, id2);
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
                key.first = id2;
                key.second = id;
                cache.put(key, d);
              }
            }
          }
          ksize.put(kNN.size());
          materialized.put(id, kNN.toSortedArrayList());
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
        logger.verbose("On average, " + ksize.getMean() + " +- " + ksize.getStddev() + " neighbors returned.");
      }
    }

    @Override
    public List<DistanceResultPair<D>> get(DBID id) {
      return materialized.get(id);
    }
  }
}