package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.MetricalIndex;
import de.lmu.ifi.dbs.elki.index.tree.metrical.MetricalNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * A preprocessor for annotation of the k nearest neighbors (and their
 * distances) to each database object.
 * 
 * Used for example by {@link de.lmu.ifi.dbs.elki.algorithm.outlier.LOF}.
 * 
 * @author Erich Schubert
 * @param <O> the type of database objects the preprocessor can be applied to
 * @param <D> the type of distance the used distance function will return
 * @param <N> the type of spatial nodes in the spatial index
 * @param <E> the type of spatial entries in the spatial index
 * 
 *        TODO correct handling of datastore events
 */
@Title("Spatial Approximation Materialize kNN Preprocessor")
@Description("Caterializes the (approximate) k nearest neighbors of objects of a database using a spatial approximation.")
public class MetricalIndexApproximationMaterializeKNNPreprocessor<O extends NumberVector<? super O, ?>, D extends Distance<D>, N extends MetricalNode<N, E>, E extends MTreeEntry<D>> extends MaterializeKNNPreprocessor<O, D> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public MetricalIndexApproximationMaterializeKNNPreprocessor(Parameterization config) {
    super(config);
    config = config.descend(this);
  }

  @Override
  public <T extends O> Instance<T, D, N, E> instantiate(Database<T> database) {
    Instance<T, D, N, E> instance = new Instance<T, D, N, E>(database, distanceFunction, k);
    instance.preprocess();
    return instance;
  }

  /**
   * The actual preprocessor instance.
   * 
   * @author Erich Schubert
   * 
   * @param <O> Database object type
   * @param <D> Distance type
   */
  public static class Instance<O extends NumberVector<? super O, ?>, D extends Distance<D>, N extends MetricalNode<N, E>, E extends MTreeEntry<D>> extends MaterializeKNNPreprocessor.Instance<O, D> {
    /**
     * Logger to use
     */
    private static final Logging logger = Logging.getLogger(MetricalIndexApproximationMaterializeKNNPreprocessor.class);

    /**
     * Constructor
     * 
     * @param database database to preprocess
     * @param distanceFunction the distance function to use
     * @param k query k
     */
    public Instance(Database<O> database, DistanceFunction<? super O, D> distanceFunction, int k) {
      super(database, distanceFunction, k);
    }

    @Override
    protected void preprocess() {
      DistanceQuery<O, D> distanceQuery = database.getDistanceQuery(distanceFunction);

      MetricalIndex<O, D, N, E> index = getMetricalIndex(database);

      materialized = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, List.class);
      MeanVariance pagesize = new MeanVariance();
      MeanVariance ksize = new MeanVariance();
      if(logger.isVerbose()) {
        logger.verbose("Approximating nearest neighbor lists to database objects");
      }

      List<E> leaves = index.getLeaves();
      FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Processing leaf nodes.", leaves.size(), logger) : null;
      for(E leaf : leaves) {
        N node = index.getNode(leaf);
        int size = node.getNumEntries();
        pagesize.put(size);
        if(logger.isDebuggingFinest()) {
          logger.debugFinest("NumEntires = " + size);
        }
        // Collect the ids in this node.
        DBID[] ids = new DBID[size];
        for(int i = 0; i < size; i++) {
          ids[i] = ((LeafEntry) node.getEntry(i)).getDBID();
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
        logger.verbose("Average page size = " + pagesize.getMean() + " +- " + pagesize.getStddev());
        logger.verbose("On average, " + ksize.getMean() + " +- " + ksize.getStddev() + " neighbors returned.");
      }
    }

    /**
     * Do some (limited) type checking, then cast the database into a spatial
     * database.
     * 
     * @param database Database
     * @return Spatial database.
     * @throws IllegalStateException when the cast fails.
     */
    private MetricalIndex<O, D, N, E> getMetricalIndex(Database<O> database) throws IllegalStateException {
      Class<MetricalIndex<O, D, N, E>> mcls = ClassGenericsUtil.uglyCastIntoSubclass(MetricalIndex.class);
      ArrayList<MetricalIndex<O, D, N, E>> indexes = ResultUtil.filterResults(database, mcls);
      if (indexes.size() == 1) {
        return indexes.get(0);
      }
      if (indexes.size() > 1) {
        throw new IllegalStateException("More than one metrical index found - this is not supported!");
      }
      throw new IllegalStateException("No metrical index found!");
    }

    @Override
    public void contentChanged(DataStoreEvent<O> e) {
      // TODO
      throw new UnsupportedOperationException("TODO " + e);
    }
  }
}