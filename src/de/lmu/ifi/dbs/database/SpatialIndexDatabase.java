package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.index.tree.spatial.SpatialDistanceFunction;
import de.lmu.ifi.dbs.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.index.tree.spatial.SpatialIndex;
import de.lmu.ifi.dbs.index.tree.spatial.SpatialNode;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * SpatialIndexDatabase is a database implementation which is supported by a
 * spatial index structure.
 *
 * @author Elke Achtert
 */
public class SpatialIndexDatabase<O extends NumberVector<O,?>, N extends SpatialNode<N, E>, E extends SpatialEntry> extends IndexDatabase<O> {

  /**
   * Option string for parameter index.
   */
  public static final String INDEX_P = "index";

  /**
   * Description for parameter index.
   */
  public static final String INDEX_D = "the spatial index to use " +
                                       Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(SpatialIndex.class) +
                                       ".";

  /**
   * The index structure storing the data.
   */
  protected SpatialIndex<O, N, E> index;

  public SpatialIndexDatabase() {
    super();
    ClassParameter<SpatialIndex<O,N,E>> spatIndex = new ClassParameter(INDEX_P, INDEX_D, SpatialIndex.class);
    optionHandler.put(spatIndex);
  }

  /**
   * Calls the super method and afterwards inserts the specified object into the underlying index structure.
   *
   * @see Database#insert(ObjectAndAssociations)
   */
  @Override
public Integer insert(ObjectAndAssociations<O> objectAndAssociations) throws UnableToComplyException {
    Integer id = super.insert(objectAndAssociations);
    O object = objectAndAssociations.getObject();
    index.insert(object);
    return id;
  }

  /**
   * Calls the super method and afterwards inserts the specified objects into the underlying index structure.
   * If the option bulk load is enabled and the index structure is empty, a bulk load will be performed.
   * Otherwise the objects will be inserted sequentially.
   *
   * @see Database#insert(java.util.List)
   */
  @Override
public void insert(List<ObjectAndAssociations<O>> objectsAndAssociationsList) throws UnableToComplyException {
    for (ObjectAndAssociations<O> objectAndAssociations : objectsAndAssociationsList) {
      super.insert(objectAndAssociations);
    }
    index.insert(getObjects(objectsAndAssociationsList));
  }

  /**
   * @see Database#rangeQuery(Integer, String, de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction)
   */
  public <D extends Distance<D>> List<QueryResult<D>> rangeQuery(Integer id, String epsilon, DistanceFunction<O, D> distanceFunction) {
    if (distanceFunction.isInfiniteDistance(distanceFunction.valueOf(epsilon))) {
      final List<QueryResult<D>> result = new ArrayList<QueryResult<D>>();
      for (Iterator<Integer> it = iterator(); it.hasNext();) {
        Integer next = it.next();
        result.add(new QueryResult<D>(next, distanceFunction.distance(id, next)));
      }
      Collections.sort(result);
      return result;
    }

    if (! (distanceFunction instanceof SpatialDistanceFunction)) {
      List<QueryResult<D>> result = new ArrayList<QueryResult<D>>();
      D distance = distanceFunction.valueOf(epsilon);
      for (Iterator<Integer> it = iterator(); it.hasNext();) {
        Integer next = it.next();
        D currentDistance = distanceFunction.distance(id, next);
        if (currentDistance.compareTo(distance) <= 0) {
          result.add(new QueryResult<D>(next, currentDistance));
        }
      }
      Collections.sort(result);
      return result;
    }

    return index.rangeQuery(get(id), epsilon, distanceFunction);
  }

  /**
   * @see Database#kNNQueryForObject(de.lmu.ifi.dbs.data.DatabaseObject, int, de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction)
   */
  public <D extends Distance<D>> List<QueryResult<D>> kNNQueryForObject(O queryObject, int k, DistanceFunction<O, D> distanceFunction) {
    if (!(distanceFunction instanceof SpatialDistanceFunction))
      throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");

    return index.kNNQuery(queryObject, k, distanceFunction);
  }

  /**
   * @see Database#kNNQueryForID(Integer, int, de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction)
   */
  public <D extends Distance<D>>List<QueryResult<D>> kNNQueryForID(Integer id, int k, DistanceFunction<O, D> distanceFunction) {
    if (!(distanceFunction instanceof SpatialDistanceFunction))
      throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");

    return index.kNNQuery(get(id), k, distanceFunction);
  }

  /**
   * @see Database#bulkKNNQueryForID(java.util.List, int, de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction)
   */
  public <D extends Distance<D>>List<List<QueryResult<D>>> bulkKNNQueryForID(List<Integer> ids, int k, DistanceFunction<O, D> distanceFunction) {
    if (!(distanceFunction instanceof SpatialDistanceFunction))
      throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");

    return index.bulkKNNQueryForIDs(ids, k, (SpatialDistanceFunction<O, D>) distanceFunction);
  }

  /**
   * Performs a reverse k-nearest neighbor query for the given object ID. The
   * query result is in ascending order to the distance to the query object.
   *
   * @param id               the ID of the query object
   * @param k                the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances beween the
   *                         objects
   * @return a List of the query results
   */
  public <D extends Distance<D>> List<QueryResult<D>> reverseKNNQuery(Integer id, int k, DistanceFunction<O, D> distanceFunction) {
    if (!(distanceFunction instanceof SpatialDistanceFunction))
      throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");

    try {
      return index.reverseKNNQuery(get(id), k, distanceFunction);
    }
    catch (UnsupportedOperationException e) {
      List<QueryResult<D>> result = new ArrayList<QueryResult<D>>();
      for (Iterator<Integer> iter = iterator(); iter.hasNext();) {
        Integer candidateID = iter.next();
        List<QueryResult<D>> knns = this.kNNQueryForID(candidateID, k, distanceFunction);
        for (QueryResult<D> knn : knns) {
          if (knn.getID() == id) {
            result.add(new QueryResult<D>(candidateID, knn.getDistance()));
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
   * Sets the values for the parameter bulk.
   * If the parameters is not specified the default value is set.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @Override
public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    String indexClass = (String) optionHandler.getOptionValue(INDEX_P);
    try {
      //noinspection unchecked
      index = Util.instantiate(SpatialIndex.class, indexClass);
    }
    catch (UnableToComplyException e) {
      throw new WrongParameterValueException(INDEX_P, indexClass, INDEX_D, e);
    }

    remainingParameters = index.setParameters(remainingParameters);
    setParameters(args, remainingParameters);
    index.setDatabase(this);
    return remainingParameters;
  }

  /**
   * Returns a list of the leaf nodes of the underlying spatial index of this database.
   *
   * @return a list of the leaf nodes of the underlying spatial index of this database
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
   * Returns a short description of the database.
   * (Such as: efficiency in space and time, index structure...)
   *
   * @return a description of the database
   */
  @Override
public String description() {
    StringBuffer description = new StringBuffer();
    description.append(this.getClass().getName());
    description.append(" holds all the data in a ");
    description.append(index.getClass().getName()).append(" index structure.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }
}