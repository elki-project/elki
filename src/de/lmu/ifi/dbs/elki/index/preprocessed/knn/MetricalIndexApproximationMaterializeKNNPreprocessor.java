package de.lmu.ifi.dbs.elki.index.preprocessed.knn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
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
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

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
 * @apiviz.has MetricalIndex
 * 
 * @param <O> the type of database objects the preprocessor can be applied to
 * @param <D> the type of distance the used distance function will return
 * @param <N> the type of spatial nodes in the spatial index
 * @param <E> the type of spatial entries in the spatial index
 */
@Title("Spatial Approximation Materialize kNN Preprocessor")
@Description("Caterializes the (approximate) k nearest neighbors of objects of a database using a spatial approximation.")
public class MetricalIndexApproximationMaterializeKNNPreprocessor<O extends NumberVector<? super O, ?>, D extends Distance<D>, N extends MetricalNode<N, E>, E extends MTreeEntry<D>> extends AbstractMaterializeKNNPreprocessor<O, D> {
  /**
   * Logger to use
   */
  private static final Logging logger = Logging.getLogger(MetricalIndexApproximationMaterializeKNNPreprocessor.class);

  /**
   * Constructor
   * 
   * @param representation Representation to preprocess
   * @param distanceFunction the distance function to use
   * @param k query k
   */
  public MetricalIndexApproximationMaterializeKNNPreprocessor(Relation<O> representation, DistanceFunction<? super O, D> distanceFunction, int k) {
    super(representation, distanceFunction, k);
  }

  @Override
  protected void preprocess() {
    DistanceQuery<O, D> distanceQuery = rep.getDatabase().getDistanceQuery(rep, distanceFunction);

    MetricalIndex<O, D, N, E> index = getMetricalIndex(rep);

    storage = DataStoreUtil.makeStorage(rep.getDBIDs(), DataStoreFactory.HINT_STATIC, List.class);
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
      getLogger().verbose("Average page size = " + pagesize.getMean() + " +- " + pagesize.getSampleStddev());
      getLogger().verbose("On average, " + ksize.getMean() + " +- " + ksize.getSampleStddev() + " neighbors returned.");
    }
  }

  /**
   * Do some (limited) type checking, then cast the database into a spatial
   * database.
   * 
   * @param rep Database
   * @return Spatial database.
   * @throws IllegalStateException when the cast fails.
   */
  private MetricalIndex<O, D, N, E> getMetricalIndex(Relation<O> rep) throws IllegalStateException {
    Class<MetricalIndex<O, D, N, E>> mcls = ClassGenericsUtil.uglyCastIntoSubclass(MetricalIndex.class);
    ArrayList<MetricalIndex<O, D, N, E>> indexes = ResultUtil.filterResults(rep.getDatabase(), mcls);
    // FIXME: check we got the right the representation
    if(indexes.size() == 1) {
      return indexes.get(0);
    }
    if(indexes.size() > 1) {
      throw new IllegalStateException("More than one metrical index found - this is not supported!");
    }
    throw new IllegalStateException("No metrical index found!");
  }
  
  @Override
  public String getLongName() {
    return "Metrical index knn approximation";
  }

  @Override
  public String getShortName() {
    return "metrical-knn-approximation";
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * The parameterizable factory.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses MetricalIndexApproximationMaterializeKNNPreprocessor oneway -
   *              - «create»
   * 
   * @param <O> the type of database objects the preprocessor can be applied to
   * @param <D> the type of distance the used distance function will return
   * @param <N> the type of spatial nodes in the spatial index
   * @param <E> the type of spatial entries in the spatial index
   */
  public static class Factory<O extends NumberVector<? super O, ?>, D extends Distance<D>, N extends MetricalNode<N, E>, E extends MTreeEntry<D>> extends AbstractMaterializeKNNPreprocessor.Factory<O, D> {
    /**
     * Constructor.
     * 
     * @param k k
     * @param distanceFunction distance function
     */
    public Factory(int k, DistanceFunction<? super O, D> distanceFunction) {
      super(k, distanceFunction);
    }

    @Override
    public MetricalIndexApproximationMaterializeKNNPreprocessor<O, D, N, E> instantiate(Relation<O> representation) {
      MetricalIndexApproximationMaterializeKNNPreprocessor<O, D, N, E> instance = new MetricalIndexApproximationMaterializeKNNPreprocessor<O, D, N, E>(representation, distanceFunction, k);
      return instance;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer<O extends NumberVector<? super O, ?>, D extends Distance<D>, N extends MetricalNode<N, E>, E extends MTreeEntry<D>> extends AbstractMaterializeKNNPreprocessor.Factory.Parameterizer<O, D> {
      @Override
      protected Factory<O, D, N, E> makeInstance() {
        return new Factory<O, D, N, E>(k, distanceFunction);
      }
    }
  }
}