package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.TreeSetDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.TreeSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.preprocessing.SharedNearestNeighborsPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * SharedNearestNeighborSimilarityFunction with a pattern defined
 * to accept Strings that define a non-negative Integer.
 * 
 * @author Arthur Zimek
 * @param <O> object type
 * @param <D> distance type
 */
// todo arthur comment class
public class FractionalSharedNearestNeighborSimilarityFunction<O extends DatabaseObject, D extends Distance<D>> extends AbstractPreprocessorBasedSimilarityFunction<O, SharedNearestNeighborsPreprocessor<O, D>, DoubleDistance> implements NormalizedSimilarityFunction<O, DoubleDistance> {
  private int cachesize = 100;
  
  /**
   * Cache for objects not handled by the preprocessor
   */
  private ArrayList<Pair<O,TreeSetDBIDs>> cache = new ArrayList<Pair<O,TreeSetDBIDs>>();
  
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
    super(config, DoubleDistance.FACTORY);
    final SharedNearestNeighborsPreprocessor<O, D> preprocessor = getPreprocessor();
    if (preprocessor != null) {
      numberOfNeighbors = preprocessor.getNumberOfNeighbors();
    }
  }

  public DoubleDistance similarity(DBID id1, DBID id2) {
    TreeSetDBIDs neighbors1 = getPreprocessor().get(id1);
    TreeSetDBIDs neighbors2 = getPreprocessor().get(id2);
    int intersection = SharedNearestNeighborSimilarityFunction.countSharedNeighbors(neighbors1, neighbors2);
    return new DoubleDistance((double)intersection / numberOfNeighbors);
  }

  /**
   * Wrapper to handle objects not preprocessed with a cache for performance.
   * 
   * @param obj query object
   * @return neighbors
   */
  private TreeSetDBIDs getNeighbors(O obj) {
    // try preprocessor first
    if (obj.getID() != null) {
      TreeSetDBIDs neighbors = getPreprocessor().get(obj.getID());
      if (neighbors != null) {
        return neighbors;
      }
    }
    // try cache second
    for (Pair<O, TreeSetDBIDs> item : cache) {
      if (item.getFirst() == obj) {
        return item.getSecond();
      }
    }
    TreeSetModifiableDBIDs neighbors = DBIDUtil.newTreeSet(numberOfNeighbors);
    List<DistanceResultPair<D>> kNN = getDatabase().kNNQueryForObject(obj, numberOfNeighbors, getPreprocessor().getDistanceFunction());
    for (int i = 1; i < kNN.size(); i++) {
        neighbors.add(kNN.get(i).getID());
    }
    // store in cache
    cache.add(0, new Pair<O,TreeSetDBIDs>(obj, neighbors));
    // limit cache size
    while (cache.size() > cachesize) {
      cache.remove(cachesize);
    }
    return neighbors;
  }

  @Override
  public DoubleDistance similarity(O o1, O o2) {
    TreeSetDBIDs neighbors1 = getNeighbors(o1);
    TreeSetDBIDs neighbors2 = getNeighbors(o2);
    int intersection = SharedNearestNeighborSimilarityFunction.countSharedNeighbors(neighbors1, neighbors2);
    return new DoubleDistance((double)intersection / numberOfNeighbors);
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
    // TODO: make dependant on preprocessor?
    return DatabaseObject.class;
  }
}