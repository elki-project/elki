package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import java.util.Iterator;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.SetDBIDs;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.snn.SharedNearestNeighborIndex;
import de.lmu.ifi.dbs.elki.index.preprocessed.snn.SharedNearestNeighborPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * SharedNearestNeighborSimilarityFunction with a pattern defined to accept
 * Strings that define a non-negative Integer.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.has SharedNearestNeighborsPreprocessor
 * @apiviz.uses Instance oneway - - «create»
 * 
 * @param <O> object type
 * @param <D> distance type
 */
// todo arthur comment class
public class FractionalSharedNearestNeighborSimilarityFunction<O extends DatabaseObject, D extends Distance<D>> extends AbstractIndexBasedSimilarityFunction<O, SharedNearestNeighborIndex<O>, SetDBIDs, DoubleDistance> implements NormalizedSimilarityFunction<O, DoubleDistance> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public FractionalSharedNearestNeighborSimilarityFunction(Parameterization config) {
    super(config);
    config = config.descend(this);
  }

  @Override
  public Class<? super O> getInputDatatype() {
    return DatabaseObject.class;
  }

  @Override
  protected Class<?> getIndexFactoryRestriction() {
    return SharedNearestNeighborIndex.Factory.class;
  }

  @Override
  protected Class<?> getIndexFactoryDefaultClass() {
    return SharedNearestNeighborPreprocessor.Factory.class;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends O> Instance<T, D> instantiate(Database<T> database) {
    SharedNearestNeighborIndex<O> indexi = index.instantiate((Database<O>)database);
    return (Instance<T, D>) new Instance<O, D>((Database<O>)database, indexi, indexi.getNumberOfNeighbors());
  }

  /**
   * Actual instance for a dataset.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has de.lmu.ifi.dbs.elki.preprocessing.SharedNearestNeighborsPreprocessor.Instance
   * 
   * @param <O>
   * @param <D>
   */
  public static class Instance<O extends DatabaseObject, D extends Distance<D>> extends AbstractIndexBasedSimilarityFunction.Instance<O, SharedNearestNeighborIndex<O>, SetDBIDs, DoubleDistance> {
    // private int cachesize = 100;
    /**
     * Cache for objects not handled by the preprocessor
     */
    // private ArrayList<Pair<O,TreeSetDBIDs>> cache = new
    // ArrayList<Pair<O,TreeSetDBIDs>>();

    /**
     * Holds the number of nearest neighbors to be used.
     */
    private final int numberOfNeighbors;

    /**
     * Constructor.
     * 
     * @param database Database
     * @param preprocessor Preprocessor
     * @param numberOfNeighbors Neighborhood size
     */
    public Instance(Database<O> database, SharedNearestNeighborIndex<O> preprocessor, int numberOfNeighbors) {
      super(database, preprocessor);
      this.numberOfNeighbors = numberOfNeighbors;
    }

    static protected int countSharedNeighbors(SetDBIDs neighbors1, SetDBIDs neighbors2) {
      int intersection = 0;
      Iterator<DBID> iter1 = neighbors1.iterator();
      Iterator<DBID> iter2 = neighbors2.iterator();
      DBID neighbors1ID = null;
      DBID neighbors2ID = null;
      if(iter1.hasNext()) {
        neighbors1ID = iter1.next();
      }
      if(iter2.hasNext()) {
        neighbors2ID = iter2.next();
      }
      while((iter1.hasNext() || iter2.hasNext()) && neighbors1ID != null && neighbors2ID != null) {
        if(neighbors1ID.equals(neighbors2ID)) {
          intersection++;
          if(iter1.hasNext()) {
            neighbors1ID = iter1.next();
          }
          else {
            neighbors1ID = null;
          }
          if(iter2.hasNext()) {
            neighbors2ID = iter2.next();
          }
          else {
            neighbors2ID = null;
          }
        }
        else if(neighbors2ID.compareTo(neighbors1ID) < 0) {
          if(iter1.hasNext()) {
            neighbors1ID = iter1.next();
          }
          else {
            neighbors1ID = null;
          }
        }
        else // neighbors1ID > neighbors2ID
        {
          if(iter2.hasNext()) {
            neighbors2ID = iter2.next();
          }
          else {
            neighbors2ID = null;
          }
        }
      }
      return intersection;
    }

    @Override
    public DoubleDistance similarity(DBID id1, DBID id2) {
      SetDBIDs neighbors1 = index.getNearestNeighborSet(id1);
      SetDBIDs neighbors2 = index.getNearestNeighborSet(id2);
      int intersection = countSharedNeighbors(neighbors1, neighbors2);
      return new DoubleDistance((double) intersection / numberOfNeighbors);
    }

    /*
     * Wrapper to handle objects not preprocessed with a cache for performance.
     * 
     * @param obj query object
     * 
     * @return neighbors
     */
    /*
     * private TreeSetDBIDs getNeighbors(O obj) { // try preprocessor first if
     * (obj.getID() != null) { TreeSetDBIDs neighbors =
     * preprocessor.get(obj.getID()); if (neighbors != null) { return neighbors;
     * } } // try cache second for (Pair<O, TreeSetDBIDs> item : cache) { if
     * (item.getFirst() == obj) { return item.getSecond(); } }
     * TreeSetModifiableDBIDs neighbors =
     * DBIDUtil.newTreeSet(numberOfNeighbors); List<DistanceResultPair<D>> kNN =
     * database.kNNQueryForID(obj.getID(), numberOfNeighbors,
     * preprocessor.getDistanceFunction()); for (int i = 1; i < kNN.size(); i++)
     * { neighbors.add(kNN.get(i).getID()); } // store in cache cache.add(0, new
     * Pair<O,TreeSetDBIDs>(obj, neighbors)); // limit cache size while
     * (cache.size() > cachesize) { cache.remove(cachesize); } return neighbors;
     * }
     */

    /*
     * @Override public DoubleDistance similarity(O o1, O o2) { TreeSetDBIDs
     * neighbors1 = getNeighbors(o1); TreeSetDBIDs neighbors2 =
     * getNeighbors(o2); int intersection =
     * SharedNearestNeighborSimilarityFunction.countSharedNeighbors(neighbors1,
     * neighbors2); return new DoubleDistance((double)intersection /
     * numberOfNeighbors); }
     */

    @Override
    public DoubleDistance getDistanceFactory() {
      return DoubleDistance.FACTORY;
    }
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }
}