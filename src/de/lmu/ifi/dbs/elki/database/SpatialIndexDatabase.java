package de.lmu.ifi.dbs.elki.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialIndex;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialNode;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * SpatialIndexDatabase is a database implementation which is supported by a
 * spatial index structure.
 * 
 * @author Elke Achtert
 * @param <O> the type of NumberVector as element of the database
 * @param <N> the type of SpatialNode stored in the index
 * @param <E> the type of SpatialEntry stored in the index
 */
public class SpatialIndexDatabase<O extends NumberVector<O, ?>, N extends SpatialNode<N, E>, E extends SpatialEntry> extends IndexDatabase<O> {

  /**
   * OptionID for {@link #INDEX_PARAM}
   */
  public static final OptionID INDEX_ID = OptionID.getOrCreateOptionID("spatialindexdb.index", "Spatial index class to use.");

  /**
   * Parameter to specify the spatial index to use.
   * <p>
   * Key: {@code -spatialindexdb.index}
   * </p>
   */
  private final ClassParameter<SpatialIndex<O, N, E>> INDEX_PARAM = new ClassParameter<SpatialIndex<O, N, E>>(INDEX_ID, SpatialIndex.class);

  /**
   * The index structure storing the data.
   */
  protected SpatialIndex<O, N, E> index;

  /**
   * Constructor.
   */
  public SpatialIndexDatabase() {
    super();
    addOption(INDEX_PARAM);
  }

  /**
   * Calls the super method and afterwards inserts the specified object into the
   * underlying index structure.
   */
  @Override
  public Integer insert(Pair<O, Associations> objectAndAssociations) throws UnableToComplyException {
    Integer id = super.insert(objectAndAssociations);
    O object = objectAndAssociations.getFirst();
    index.insert(object);
    return id;
  }

  /**
   * Calls the super method and afterwards inserts the specified objects into
   * the underlying index structure. If the option bulk load is enabled and the
   * index structure is empty, a bulk load will be performed. Otherwise the
   * objects will be inserted sequentially.
   */
  @Override
  public void insert(List<Pair<O, Associations>> objectsAndAssociationsList) throws UnableToComplyException {
    for(Pair<O, Associations> objectAndAssociations : objectsAndAssociationsList) {
      super.insert(objectAndAssociations);
    }
    index.insert(getObjects(objectsAndAssociationsList));
  }

  public <D extends Distance<D>> List<DistanceResultPair<D>> rangeQuery(Integer id, String epsilon, DistanceFunction<O, D> distanceFunction) {
    if(distanceFunction.isInfiniteDistance(distanceFunction.valueOf(epsilon))) {
      final List<DistanceResultPair<D>> result = new ArrayList<DistanceResultPair<D>>();
      for(Iterator<Integer> it = iterator(); it.hasNext();) {
        Integer next = it.next();
        result.add(new DistanceResultPair<D>(distanceFunction.distance(id, next), next));
      }
      Collections.sort(result);
      return result;
    }

    if(!(distanceFunction instanceof SpatialDistanceFunction)) {
      // TODO: why is this emulated here, but not for other queries.
      List<DistanceResultPair<D>> result = new ArrayList<DistanceResultPair<D>>();
      D distance = distanceFunction.valueOf(epsilon);
      for(Iterator<Integer> it = iterator(); it.hasNext();) {
        Integer next = it.next();
        D currentDistance = distanceFunction.distance(id, next);
        if(currentDistance.compareTo(distance) <= 0) {
          result.add(new DistanceResultPair<D>(currentDistance, next));
        }
      }
      Collections.sort(result);
      return result;
    }
    else {
      return index.rangeQuery(get(id), epsilon, (SpatialDistanceFunction<O, D>) distanceFunction);
    }
  }

  public <D extends Distance<D>> List<DistanceResultPair<D>> kNNQueryForObject(O queryObject, int k, DistanceFunction<O, D> distanceFunction) {
    if(!(distanceFunction instanceof SpatialDistanceFunction)) {
      throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");
    }
    return index.kNNQuery(queryObject, k, (SpatialDistanceFunction<O, D>) distanceFunction);
  }

  public <D extends Distance<D>> List<DistanceResultPair<D>> kNNQueryForID(Integer id, int k, DistanceFunction<O, D> distanceFunction) {
    if(!(distanceFunction instanceof SpatialDistanceFunction)) {
      throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");
    }
    return index.kNNQuery(get(id), k, (SpatialDistanceFunction<O, D>) distanceFunction);
  }

  public <D extends Distance<D>> List<List<DistanceResultPair<D>>> bulkKNNQueryForID(List<Integer> ids, int k, DistanceFunction<O, D> distanceFunction) {
    if(!(distanceFunction instanceof SpatialDistanceFunction)) {
      throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");
    }
    return index.bulkKNNQueryForIDs(ids, k, (SpatialDistanceFunction<O, D>) distanceFunction);
  }

  /**
   * Performs a reverse k-nearest neighbor query for the given object ID. The
   * query result is in ascending order to the distance to the query object.
   * 
   * @param id the ID of the query object
   * @param k the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances
   *        between the objects
   * @return a List of the query results
   */
  public <D extends Distance<D>> List<DistanceResultPair<D>> reverseKNNQuery(Integer id, int k, DistanceFunction<O, D> distanceFunction) {
    if(!(distanceFunction instanceof SpatialDistanceFunction)) {
      throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");
    }
    try {
      return index.reverseKNNQuery(get(id), k, (SpatialDistanceFunction<O, D>) distanceFunction);
    }
    catch(UnsupportedOperationException e) {
      List<DistanceResultPair<D>> result = new ArrayList<DistanceResultPair<D>>();
      for(Iterator<Integer> iter = iterator(); iter.hasNext();) {
        Integer candidateID = iter.next();
        List<DistanceResultPair<D>> knns = this.kNNQueryForID(candidateID, k, distanceFunction);
        for(DistanceResultPair<D> knn : knns) {
          if(knn.getID() == id) {
            result.add(new DistanceResultPair<D>(knn.getDistance(), candidateID));
          }
        }
      }
      Collections.sort(result);
      return result;
    }
  }

  /**
   * Returns a string representation of this database.
   * 
   * @return a string representation of this database.
   */
  @Override
  public String toString() {
    return index.toString();
  }

  /**
   * Sets the values for the parameter bulk. If the parameters is not specified
   * the default value is set.
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    index = INDEX_PARAM.instantiateClass();
    addParameterizable(index);

    remainingParameters = index.setParameters(remainingParameters);
    index.setDatabase(this);

    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Returns a list of the leaf nodes of the underlying spatial index of this
   * database.
   * 
   * @return a list of the leaf nodes of the underlying spatial index of this
   *         database
   */
  public List<E> getLeaves() {
    return index.getLeaves();
  }

  /**
   * Returns the id of the root of the underlying index.
   * 
   * @return the id of the root of the underlying index
   */
  public E getRootEntry() {
    return index.getRootEntry();
  }

  /**
   * Returns the index of this database.
   * 
   * @return the index of this database
   */
  @Override
  public SpatialIndex<O, N, E> getIndex() {
    return index;
  }

  /**
   * Returns a short description of the database. (Such as: efficiency in space
   * and time, index structure...)
   * 
   * @return a description of the database
   */
  @Override
  public String shortDescription() {
    StringBuffer description = new StringBuffer();
    description.append(this.getClass().getName());
    description.append(" holds all the data in a spatial index structure");
    description.append(" extending ").append(SpatialIndex.class.getName()).append(".\n");
    return description.toString();
  }
}