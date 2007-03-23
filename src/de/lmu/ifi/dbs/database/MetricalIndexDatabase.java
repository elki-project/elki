package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.index.tree.metrical.MetricalIndex;
import de.lmu.ifi.dbs.index.tree.metrical.MetricalNode;
import de.lmu.ifi.dbs.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.ArrayList;
import java.util.List;

/**
 * MetricalIndexDatabase is a database implementation which is supported by a
 * metrical index structure.
 *
 * @author Elke Achtert(<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MetricalIndexDatabase<O extends DatabaseObject, D extends Distance<D>, N extends MetricalNode<N, E>, E extends MTreeEntry<D>>
    extends IndexDatabase<O> {
  /**
   * Option string for parameter index.
   */
  public static final String INDEX_P = "index";

  /**
   * Description for parameter index.
   */
  public static final String INDEX_D = "the metrical index to use " +
                                       Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(MetricalIndex.class) +
                                       ".";

  /**
   * The metrical index storing the data.
   */
  MetricalIndex<O, D, N, E> index;

  public MetricalIndexDatabase() {
    super();
    optionHandler.put(new ClassParameter<MetricalIndex>(INDEX_P, INDEX_D, MetricalIndex.class));
  }

  /**
   * Calls the super method and afterwards inserts the specified object into
   * the underlying index structure.
   *
   * @see Database#insert(ObjectAndAssociations)
   */
  public Integer insert(ObjectAndAssociations<O> objectAndAssociations) throws UnableToComplyException {
    Integer id = super.insert(objectAndAssociations);
    O object = objectAndAssociations.getObject();
    index.insert(object);
    return id;
  }

  /**
   * Calls the super method and afterwards inserts the specified objects into
   * the underlying index structure. If the option bulk load is enabled and
   * the index structure is empty, a bulk load will be performed. Otherwise
   * the objects will be inserted sequentially.
   *
   * @see Database#insert(java.util.List)
   */
  public void insert(List<ObjectAndAssociations<O>> objectsAndAssociationsList) throws UnableToComplyException {
    for (ObjectAndAssociations<O> objectAndAssociations : objectsAndAssociationsList) {
      super.insert(objectAndAssociations);
    }
    this.index.insert(getObjects(objectsAndAssociationsList));
  }

  /**
   * @see Database#rangeQuery(Integer, String, de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction)
   */
  public <T extends Distance<T>> List<QueryResult<T>> rangeQuery(Integer id,
                                                                 String epsilon,
                                                                 DistanceFunction<O, T> distanceFunction) {
    if (!distanceFunction.getClass().equals(index.getDistanceFunction().getClass()))
      throw new IllegalArgumentException("Parameter distanceFunction must be an instance of " +
                                         index.getDistanceFunction().getClass());

    List<QueryResult<D>> rangeQuery = index.rangeQuery(get(id), epsilon);

    List<QueryResult<T>> result = new ArrayList<QueryResult<T>>();
    for (QueryResult<D> qr : rangeQuery) {
      // noinspection unchecked
      result.add((QueryResult<T>) qr);
    }

    return result;
  }

  /**
   * @see Database#kNNQueryForObject(DatabaseObject, int,de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction)
   */
  public <T extends Distance<T>> List<QueryResult<T>> kNNQueryForObject(
      O queryObject, int k, DistanceFunction<O, T> distanceFunction) {
    if (!distanceFunction.getClass().equals(index.getDistanceFunction().getClass()))
      throw new IllegalArgumentException("Parameter distanceFunction must be an instance of "
                                         + index.getDistanceFunction().getClass());

    List<QueryResult<D>> knnQuery = index.kNNQuery(queryObject, k);

    List<QueryResult<T>> result = new ArrayList<QueryResult<T>>();
    for (QueryResult<D> qr : knnQuery) {
      // noinspection unchecked
      result.add((QueryResult<T>) qr);
    }

    return result;
  }

  /**
   * @see Database#kNNQueryForID(Integer, int,
   *      de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction)
   */
  public <T extends Distance<T>> List<QueryResult<T>> kNNQueryForID(Integer id, int k, DistanceFunction<O, T> distanceFunction) {

    if (!distanceFunction.getClass().equals(index.getDistanceFunction().getClass()))
      throw new IllegalArgumentException("Parameter distanceFunction must be an instance of "
                                         + index.getDistanceFunction().getClass());

    List<QueryResult<D>> knnQuery = index.kNNQuery(get(id), k);

    List<QueryResult<T>> result = new ArrayList<QueryResult<T>>();
    for (QueryResult<D> qr : knnQuery) {
      // noinspection unchecked
      result.add((QueryResult<T>) qr);
    }

    return result;
  }

  /**
   * @see Database#bulkKNNQueryForID(java.util.List, int, de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction)
   */
  public <D extends Distance<D>>List<List<QueryResult<D>>> bulkKNNQueryForID(List<Integer> ids, int k, DistanceFunction<O, D> distanceFunction) {
    throw new UnsupportedOperationException("Not yet supported!");
  }


  /**
   * @see Database#reverseKNNQuery(Integer, int,
   *      de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction)
   */
  public <T extends Distance> List<QueryResult<T>> reverseKNNQuery(Integer id, int k, DistanceFunction<O, T> distanceFunction) {

    if (!distanceFunction.getClass().equals(index.getDistanceFunction().getClass()))
      throw new IllegalArgumentException("Parameter distanceFunction must be an instance of "
                                         + index.getDistanceFunction().getClass() +
                                         ", but is " + distanceFunction.getClass());

    List<QueryResult<D>> rknnQuery = index.reverseKNNQuery(get(id), k);

    List<QueryResult<T>> result = new ArrayList<QueryResult<T>>();
    for (QueryResult<D> qr : rknnQuery) {
      // noinspection unchecked
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
   * Returns the index of this database.
   *
   * @return the index of this database
   */
  public MetricalIndex<O, D, N, E> getIndex() {
    return index;
  }

  /**
   * Sets the values for the parameter bulk.
   * If the parameters is not specified the default value is set.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    String indexClass = (String) optionHandler.getOptionValue(INDEX_P);
    try {
      //noinspection unchecked
      index = Util.instantiate(MetricalIndex.class, indexClass);
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
   * Returns a short description of the database.
   * (Such as: efficiency in space and time, index structure...)
   *
   * @return a description of the database
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(this.getClass().getName());
    description.append(" holds all the data in a ");
    description.append(index.getClass().getName()).append(" index structure.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }
}