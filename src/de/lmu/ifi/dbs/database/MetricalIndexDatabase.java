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
  private DistanceFunction<O, D> distanceFunction;

  /**
   * The metrical index storing the data.
   */
  MetricalIndex<O, D> index;

  public MetricalIndexDatabase() {
    super();
    parameterToDescription.put(DISTANCE_FUNCTION_P + OptionHandler.EXPECTS_VALUE, DISTANCE_FUNCTION_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * Calls the super method and afterwards inserts the specified object into the underlying index structure.
   *
   * @see Database#insert(ObjectAndAssociations)
   */
  public Integer insert(ObjectAndAssociations<O> objectAndAssociations) throws UnableToComplyException {
    Integer id = super.insert(objectAndAssociations);

    O object = objectAndAssociations.getObject();
    if (this.index == null) {
      index = createMetricalIndex();
    }
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
  public void insert(List<ObjectAndAssociations<O>> objectsAndAssociationsList) throws UnableToComplyException {
    if (this.index == null) {
      super.insert(objectsAndAssociationsList);
      this.index = createMetricalIndex(getObjects(objectsAndAssociationsList));
    }
    else {
      for (ObjectAndAssociations<O> objectAndAssociations : objectsAndAssociationsList) {
        insert(objectAndAssociations);
      }
    }
  }

  /**
   * @see Database#rangeQuery(Integer, String, de.lmu.ifi.dbs.distance.DistanceFunction)
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
   * @see Database#kNNQueryForObject(DatabaseObject, int, de.lmu.ifi.dbs.distance.DistanceFunction)
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
   * @see Database#kNNQueryForID(Integer, int, de.lmu.ifi.dbs.distance.DistanceFunction)
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
   * @see Database#reverseKNNQuery(Integer, int, de.lmu.ifi.dbs.distance.DistanceFunction)
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