package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.index.metrical.MetricalIndex;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MetricalIndexDatabase is a database implementation which is supported by a
 * metrical index structure.
 *
 * @author Elke Achtert(<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class MetricalIndexDatabase<O extends DatabaseObject, D extends Distance> extends IndexDatabase<O> {
  /**
   * The default distance function.
   */
  public static final String DEFAULT_DISTANCE_FUNCTION = EuklideanDistanceFunction.class.getName();

  /**
   * Parameter for distance function.
   */
  public static final String DISTANCE_FUNCTION_P = "distancefunction";

  /**
   * Description for parameter distance function.
   */
  public static final String DISTANCE_FUNCTION_D = "<classname>the distance function to determine the distance between database objects - must implement " + DistanceFunction.class.getName() + ". (Default: " + DEFAULT_DISTANCE_FUNCTION + ").";

  /**
   * The distance function.
   */
  private DistanceFunction<O,D> distanceFunction;

  /**
   * The metrical index storing the data.
   */
  MetricalIndex<O, D> index;

  public MetricalIndexDatabase() {
    super();
    parameterToDescription.put(DISTANCE_FUNCTION_P + OptionHandler.EXPECTS_VALUE, DISTANCE_FUNCTION_D);
  }

  /**
   * Inserts the given object into the database.
   *
   * @param object the object to be inserted
   * @return the ID assigned to the inserted object
   * @throws de.lmu.ifi.dbs.utilities.UnableToComplyException
   *          if insertion is not possible
   */
  public Integer insert(O object) throws UnableToComplyException {
    Integer id = super.putToContent(object);

    if (this.index == null) {
      index = createMetricalIndex();
    }
    index.insert(object);
    return id;
  }

  /**
   * @see Database#insert(de.lmu.ifi.dbs.data.DatabaseObject, java.util.Map)
   */
  public Integer insert(O object, Map<AssociationID, Object> associations) throws UnableToComplyException {
    Integer id = insert(object);
    setAssociations(id, associations);
    return id;
  }

  /**
   * Initializes the databases by inserting the specified objects into the
   * database.
   *
   * @param objects the list of objects to be inserted
   * @throws de.lmu.ifi.dbs.utilities.UnableToComplyException
   *          if initialization is not possible
   */
  public void insert(List<O> objects) throws UnableToComplyException {
    if (this.index == null) {
      for (O object : objects) {
        putToContent(object);
      }
      this.index = createMetricalIndex(objects);
    }

    else {
      for (O object : objects) {
        insert(object);
      }
    }
  }

  /**
   * @see Database#insert(java.util.List, java.util.List)
   */
  public void insert(List<O> objects, List<Map<AssociationID, Object>> associations) throws UnableToComplyException {
    if (objects.size() != associations.size()) {
      throw new UnableToComplyException("List of objects and list of associations differ in length.");
    }

    if (this.index == null) {
      for (int i = 0; i < objects.size(); i++) {
        Integer id = putToContent(objects.get(i));
        setAssociations(id, associations.get(i));
      }

      this.index = createMetricalIndex(objects);
    }
    else {
      for (int i = 0; i < objects.size(); i++) {
        insert(objects.get(i), associations.get(i));
      }
    }
  }

  /**
   * Removes the object with the given id from the database.
   *
   * @param id the id of an object to be removed from the database
   */
  public void delete(Integer id) {
    O object = removeFromContent(id);
    index.delete(object);
    restoreID(id);
    deleteAssociations(id);
  }

  /**
   * Performs a range query for the given object ID with the given epsilon
   * range and the according distance function. The query result is in
   * ascending order to the distance to the query object.
   *
   * @param id               the ID of the query object
   * @param epsilon          the string representation of the query range
   * @param distanceFunction the distance function that computes the distances beween the
   *                         objects
   * @return a List of the query results
   */
  public <T extends Distance<T>> List<QueryResult<T>> rangeQuery(Integer id, String epsilon, DistanceFunction<O, T> distanceFunction) {
    if (! distanceFunction.getClass().equals(this.distanceFunction.getClass()))
      throw new IllegalArgumentException("Parameter distanceFunction must be an instance of " +
                                         this.distanceFunction.getClass());

    List<QueryResult<D>> rangeQuery = index.rangeQuery(get(id), epsilon);

    List<QueryResult<T>> result = new ArrayList<QueryResult<T>>();
    for (QueryResult<D> qr : rangeQuery) {
      //noinspection unchecked
      result.add((QueryResult<T>) qr);
    }

    return result;
  }

  /**
   * Performs a k-nearest neighbor query for the given object ID. The query
   * result is in ascending order to the distance to the query object.
   *
   * @param queryObject      the query object
   * @param k                the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances beween the
   *                         objects
   * @return a List of the query results
   */
  public <T extends Distance<T>> List<QueryResult<T>> kNNQueryForObject(O queryObject, int k, DistanceFunction<O, T> distanceFunction) {
    if (! distanceFunction.getClass().equals(this.distanceFunction.getClass()))
      throw new IllegalArgumentException("Parameter distanceFunction must be an instance of " +
                                         this.distanceFunction.getClass());

    List<QueryResult<D>> knnQuery = index.kNNQuery(queryObject, k);

    List<QueryResult<T>> result = new ArrayList<QueryResult<T>>();
    for (QueryResult<D> qr : knnQuery) {
      //noinspection unchecked
      result.add((QueryResult<T>) qr);
    }

    return result;
  }

  /**
   * Performs a k-nearest neighbor query for the given object ID. The query
   * result is in ascending order to the distance to the query object.
   *
   * @param id               the ID of the query object
   * @param k                the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances beween the
   *                         objects
   * @return a List of the query results
   */
  public <T extends Distance<T>>List<QueryResult<T>> kNNQueryForID(Integer id, int k, DistanceFunction<O, T> distanceFunction) {
    if (! distanceFunction.getClass().equals(this.distanceFunction.getClass()))
      throw new IllegalArgumentException("Parameter distanceFunction must be an instance of " +
                                         this.distanceFunction.getClass());

    List<QueryResult<D>> knnQuery = index.kNNQuery(get(id), k);

    List<QueryResult<T>> result = new ArrayList<QueryResult<T>>();
    for (QueryResult<D> qr : knnQuery) {
      //noinspection unchecked
      result.add((QueryResult<T>) qr);
      
    }

    return result;
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
  public <T extends Distance> List<QueryResult<T>> reverseKNNQuery(Integer id, int k, DistanceFunction<O, T> distanceFunction) {
    if (! distanceFunction.getClass().equals(this.distanceFunction.getClass()))
      throw new IllegalArgumentException("Parameter distanceFunction must be an instance of " +
                                         this.distanceFunction.getClass());

    List<QueryResult<D>> rknnQuery = index.reverseKNNQuery(get(id), k);

    List<QueryResult<T>> result = new ArrayList<QueryResult<T>>();
    for (QueryResult<D> qr : rknnQuery) {
      //noinspection unchecked
      result.add((QueryResult<T>) qr);
    }

    return result;
  }

  /**
   * Returns a string representation of this database.
   *
   * @return a string representation of this database.
   */
  public String toString() {
    return index.toString();
  }

  /**
   * Sets the values for the parameter bulk.
   * If the parameters is not specified the default value is set.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = super.setParameters(args);

    if (optionHandler.isSet(DISTANCE_FUNCTION_P)) {
      String className = optionHandler.getOptionValue(DISTANCE_FUNCTION_P);
      //noinspection unchecked
      distanceFunction = Util.instantiate(DistanceFunction.class, className);
    }
    else {
      //noinspection unchecked
      distanceFunction = Util.instantiate(DistanceFunction.class, DEFAULT_DISTANCE_FUNCTION);
    }

    distanceFunction.setDatabase(this, false);
    return distanceFunction.setParameters(remainingParameters);
  }

  /**
   * Returns the I/O-Access of this database.
   *
   * @return the I/O-Access of this database
   */
  public long getIOAccess() {
    return index.getIOAccess();
  }

  /**
   * Resets the I/O-Access of this database.
   */
  public void resetIOAccess() {
    index.resetIOAccess();
  }

  /**
   * Returns the index of this database.
   *
   * @return the index of this database
   */
  public MetricalIndex<O, D> getIndex() {
    return index;
  }

  /**
   * Returns the distance function.
   *
   * @return the distance function
   */
  public DistanceFunction<O, D> getDistanceFunction() {
    return distanceFunction;
  }

  /**
   * Creates a metrical index object for this database.
   */
  public abstract MetricalIndex<O, D> createMetricalIndex();

  /**
   * Creates a metrical index object for this database.
   *
   * @param objects the objects to be indexed
   */
  public abstract MetricalIndex<O, D> createMetricalIndex(List<O> objects);
}