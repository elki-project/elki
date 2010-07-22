package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import java.util.Iterator;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.TreeSetDBIDs;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.preprocessing.SharedNearestNeighborsPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * SharedNearestNeighborSimilarityFunction with a pattern defined to accept
 * Strings that define a non-negative Integer.
 * 
 * @author Arthur Zimek
 * @param <O> object type
 * @param <D> distance type
 */
// todo arthur comment class
public class FractionalSharedNearestNeighborSimilarityFunction<O extends DatabaseObject, D extends Distance<D>> extends AbstractPreprocessorBasedSimilarityFunction<O, SharedNearestNeighborsPreprocessor<O, D>, DoubleDistance> implements NormalizedSimilarityFunction<O, DoubleDistance> {
  /**
   * Holds the number of nearest neighbors to be used.
   */
  private int numberOfNeighbors;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public FractionalSharedNearestNeighborSimilarityFunction(Parameterization config) {
    super(config);
    final SharedNearestNeighborsPreprocessor<O, D> preprocessor = getPreprocessor();
    if(preprocessor != null) {
      numberOfNeighbors = preprocessor.getNumberOfNeighbors();
    }
  }

  /**
   * @return the name of the default preprocessor, which is
   *         {@link SharedNearestNeighborsPreprocessor}
   */
  @Override
  public Class<?> getDefaultPreprocessorClass() {
    return SharedNearestNeighborsPreprocessor.class;
  }

  @Override
  public String getPreprocessorDescription() {
    return "The Classname of the preprocessor to determine the neighbors of the objects.";
  }

  /**
   * @return the super class for the preprocessor, which is
   *         {@link SharedNearestNeighborsPreprocessor}
   */
  @Override
  public Class<SharedNearestNeighborsPreprocessor<O, D>> getPreprocessorSuperClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(SharedNearestNeighborsPreprocessor.class);
  }

  @Override
  public Class<? super O> getInputDatatype() {
    return DatabaseObject.class;
  }

  @Override
  public DatabaseSimilarityFunction<O, DoubleDistance> preprocess(Database<O> database) {
    return new Instance<O, D>(database, getPreprocessor(), numberOfNeighbors);
  }

  public static class Instance<O extends DatabaseObject, D extends Distance<D>> extends AbstractPreprocessorBasedSimilarityFunction.Instance<O, SharedNearestNeighborsPreprocessor<O, D>, DoubleDistance> implements NormalizedSimilarityFunction<O, DoubleDistance> {
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

    public Instance(Database<O> database, SharedNearestNeighborsPreprocessor<O, D> preprocessor, int numberOfNeighbors) {
      super(database, preprocessor);
      this.numberOfNeighbors = numberOfNeighbors;
    }

    static protected int countSharedNeighbors(TreeSetDBIDs neighbors1, TreeSetDBIDs neighbors2) {
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

    public DoubleDistance similarity(DBID id1, DBID id2) {
      TreeSetDBIDs neighbors1 = preprocessor.get(id1);
      TreeSetDBIDs neighbors2 = preprocessor.get(id2);
      int intersection = SharedNearestNeighborSimilarityFunction.countSharedNeighbors(neighbors1, neighbors2);
      return new DoubleDistance((double) intersection / numberOfNeighbors);
    }

    /**
     * Wrapper to handle objects not preprocessed with a cache for performance.
     * 
     * @param obj query object
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
    public Class<? super O> getInputDatatype() {
      return DatabaseObject.class;
    }

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