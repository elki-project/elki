package de.lmu.ifi.dbs.elki.index.preprocessed.knn;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialIndex;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialNode;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * A preprocessor for annotation of the k nearest neighbors (and their
 * distances) to each database object.
 * 
 * Used for example by {@link de.lmu.ifi.dbs.elki.algorithm.outlier.LOF}.
 * 
 * TODO correct handling of datastore events
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has SpatialIndex
 * 
 * @param <D> the type of distance the used distance function will return
 * @param <N> the type of spatial nodes in the spatial index
 * @param <E> the type of spatial entries in the spatial index
 */
@Title("Spatial Approximation Materialize kNN Preprocessor")
@Description("Caterializes the (approximate) k nearest neighbors of objects of a database using a spatial approximation.")
public class SpatialApproximationMaterializeKNNPreprocessor<O extends NumberVector<?, ?>, D extends Distance<D>, N extends SpatialNode<N, E>, E extends SpatialEntry> extends MaterializeKNNPreprocessor<O, D> {
  /**
   * Logger to use
   */
  private static final Logging logger = Logging.getLogger(SpatialApproximationMaterializeKNNPreprocessor.class);

  /**
   * Constructor
   * 
   * @param database database to preprocess
   * @param distanceFunction the distance function to use
   * @param k query k
   */
  public SpatialApproximationMaterializeKNNPreprocessor(Database<O> database, DistanceFunction<? super O, D> distanceFunction, int k) {
    super(database, distanceFunction, k);
  }

  @Override
  protected void preprocess() {
    DistanceQuery<O, D> distanceQuery = database.getDistanceQuery(distanceFunction);

    Collection<SpatialIndex<O, N, E>> indexes = ResultUtil.filterResults(database, SpatialIndex.class);
    if(indexes.size() != 1) {
      throw new AbortException(SpatialApproximationMaterializeKNNPreprocessor.class.getSimpleName() + " found " + indexes.size() + " spatial indexes, expected exactly one.");
    }
    SpatialIndex<O, N, E> index = indexes.iterator().next();

    storage = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, List.class);
    MeanVariance pagesize = new MeanVariance();
    MeanVariance ksize = new MeanVariance();
    if(getLogger().isVerbose()) {
      getLogger().verbose("Approximating nearest neighbor lists to database objects");
    }

    List<E> leaves = index.getLeaves();
    FiniteProgress progress = getLogger().isVerbose() ? new FiniteProgress("Processing leaf nodes.", leaves.size(), getLogger()) : null;
    for(E leaf : leaves) {
      N node = index.getNode(leaf);
      int size = node.getNumEntries();
      pagesize.put(size);
      if(getLogger().isDebuggingFinest()) {
        getLogger().debugFinest("NumEntires = " + size);
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
        storage.put(id, kNN.toSortedArrayList());
      }
      if(getLogger().isDebugging()) {
        if(cache.size() > 0) {
          getLogger().warning("Cache should be empty after each run, but still has " + cache.size() + " elements.");
        }
      }
      if(progress != null) {
        progress.incrementProcessed(getLogger());
      }
    }
    if(progress != null) {
      progress.ensureCompleted(getLogger());
    }
    if(getLogger().isVerbose()) {
      getLogger().verbose("Average page size = " + pagesize.getMean() + " +- " + pagesize.getStddev());
      getLogger().verbose("On average, " + ksize.getMean() + " +- " + ksize.getStddev() + " neighbors returned.");
    }
  }

  @SuppressWarnings("unused")
  @Override
  public void insert(List<O> objects) {
    throw new UnsupportedOperationException("The preprocessor " + getClass().getSimpleName() + " does currently not allow dynamic updates.");
  }

  @SuppressWarnings("unused")
  @Override
  public boolean delete(O object) {
    throw new UnsupportedOperationException("The preprocessor " + getClass().getSimpleName() + " does currently not allow dynamic updates.");
  }
  
  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * The actual preprocessor instance.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses SpatialApproximationMaterializeKNNPreprocessor oneway - - «create»
   * 
   * @param <D> the type of distance the used distance function will return
   * @param <N> the type of spatial nodes in the spatial index
   * @param <E> the type of spatial entries in the spatial index
   */
  public static class Factory<D extends Distance<D>, N extends SpatialNode<N, E>, E extends SpatialEntry> extends MaterializeKNNPreprocessor.Factory<NumberVector<?, ?>, D> {
    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     * 
     * @param config Parameterization
     */
    public Factory(Parameterization config) {
      super(config);
      config = config.descend(this);
    }

    @Override
    public SpatialApproximationMaterializeKNNPreprocessor<NumberVector<?, ?>, D, N, E> instantiate(Database<NumberVector<?, ?>> database) {
      SpatialApproximationMaterializeKNNPreprocessor<NumberVector<?, ?>, D, N, E> instance = new SpatialApproximationMaterializeKNNPreprocessor<NumberVector<?, ?>, D, N, E>(database, distanceFunction, k);
      if(database.size() > 0) {
        instance.preprocess();
      }
      return instance;
    }
  }
}