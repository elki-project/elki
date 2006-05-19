package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.index.spatial.DirectoryEntry;
import de.lmu.ifi.dbs.index.spatial.SpatialDistanceFunction;
import de.lmu.ifi.dbs.index.spatial.SpatialIndex;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.List;

/**
 * SpatialIndexDatabase is a database implementation which is supported by a
 * spatial index structure.
 *
 * @author Elke Achtert(<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class SpatialIndexDatabase<O extends NumberVector> extends IndexDatabase<O> {

  /**
   * Option string for parameter index.
   */
  public static final String INDEX_P = "index";

  /**
   * Description for parameter index.
   */
  public static final String INDEX_D = "<class>the spatial index to use " +
                                       Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(SpatialIndex.class) +
                                       ".";

  /**
   * The index structure storing the data.
   */
  protected SpatialIndex<O> index;

  public SpatialIndexDatabase() {
    super();
    parameterToDescription.put(INDEX_P + OptionHandler.EXPECTS_VALUE, INDEX_D);
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
    super.insert(objectsAndAssociationsList);
    index.insert(getObjects(objectsAndAssociationsList));
  }

  /**
   * @see Database#rangeQuery(Integer, String, de.lmu.ifi.dbs.distance.DistanceFunction)
   */
  public <D extends Distance<D>> List<QueryResult<D>> rangeQuery(Integer id, String epsilon, DistanceFunction<O, D> distanceFunction) {
    if (!(distanceFunction instanceof SpatialDistanceFunction))
      throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");

    return index.rangeQuery(get(id), epsilon, (SpatialDistanceFunction<O, D>) distanceFunction);
  }

  /**
   * @see Database#kNNQueryForObject(de.lmu.ifi.dbs.data.DatabaseObject, int, de.lmu.ifi.dbs.distance.DistanceFunction)
   */
  public <D extends Distance<D>> List<QueryResult<D>> kNNQueryForObject(O queryObject, int k, DistanceFunction<O, D> distanceFunction) {
    if (!(distanceFunction instanceof SpatialDistanceFunction))
      throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");

    return index.kNNQuery(queryObject, k, (SpatialDistanceFunction<O, D>) distanceFunction);
  }

  /**
   * @see Database#kNNQueryForID(Integer, int, de.lmu.ifi.dbs.distance.DistanceFunction)
   */
  public <D extends Distance<D>>List<QueryResult<D>> kNNQueryForID(Integer id, int k, DistanceFunction<O, D> distanceFunction) {
    if (!(distanceFunction instanceof SpatialDistanceFunction))
      throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");

    return index.kNNQuery(get(id), k, (SpatialDistanceFunction<O, D>) distanceFunction);
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
  public <D extends Distance> List<QueryResult<D>> reverseKNNQuery(Integer id, int k, DistanceFunction<O, D> distanceFunction) {
    throw new UnsupportedOperationException("Not yet supported!");
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
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    String indexClass = optionHandler.getOptionValue(INDEX_P);
    try {
      //noinspection unchecked
      index = Util.instantiate(SpatialIndex.class, indexClass);
    }
    catch (UnableToComplyException e) {
      throw new WrongParameterValueException(INDEX_P, indexClass, INDEX_D, e);
    }

    remainingParameters = index.setParameters(remainingParameters);
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();

    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(INDEX_P, index.getClass().toString());

    return attributeSettings;
  }

  /**
   * Returns a list of the leaf nodes of the underlying spatial index of this database.
   *
   * @return a list of the leaf nodes of the underlying spatial index of this database
   */
  public List<DirectoryEntry> getLeaves() {
    return index.getLeaves();
  }

  /**
   * Returns the id of the root of the underlying index.
   *
   * @return the id of the root of the underlying index
   */
  public DirectoryEntry getRootEntry() {
    return index.getRootEntry();
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
  public SpatialIndex<O> getIndex() {
    return index;
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