package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.FeatureVector;
import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.index.spatial.SpatialDistanceFunction;
import de.lmu.ifi.dbs.index.spatial.SpatialIndex;
import de.lmu.ifi.dbs.index.spatial.SpatialNode;
import de.lmu.ifi.dbs.index.spatial.Entry;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.*;

/**
 * SpatialIndexDatabase is a database implementation which is supported by a
 * spatial index structure.
 *
 * @author Elke Achtert(<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class SpatialIndexDatabase extends AbstractDatabase<DoubleVector> {

  /**
   * Option string for parameter bulk.
   */
  public static final String BULK_LOAD_F = "bulk";

  /**
   * Description for parameter flat.
   */
  public static final String BULK_LOAD_D = "flag to specify bulk load (default is no bulk load)";

  /**
   * If true, a bulk load will be performed.
   */
  private boolean bulk;

  /**
   * The spatial index storing the data.
   */
  private SpatialIndex index;

  /**
   * Map to hold the objects of the database.
   */
  private final Map<Integer, DoubleVector> content;

  /**
   * Map providing a mapping of parameters to their descriptions.
   */
  Map<String, String> parameterToDescription = new Hashtable<String, String>();

  /**
   * OptionHandler for handling options.
   */
  OptionHandler optionHandler;

  public SpatialIndexDatabase() {
    super();
    parameterToDescription.put(BULK_LOAD_F, BULK_LOAD_D);
    this.content = new Hashtable<Integer, DoubleVector>();
  }

  /**
   * Inserts the given object into the database.
   *
   * @param object the object to be inserted
   * @return the ID assigned to the inserted object
   * @throws de.lmu.ifi.dbs.utilities.UnableToComplyException
   *          if insertion is not possible
   */
  public Integer insert(DoubleVector object) throws UnableToComplyException {

    if (this.index == null) {
      index = createSpatialIndex(object.getDimensionality());
    }

    Integer id = setNewID(object);
    index.insert(object);
    content.put(id, object);
    return id;
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#insert(de.lmu.ifi.dbs.data.MetricalObject, java.util.Map)
   */
  public Integer insert(DoubleVector object, Map<String, Object> associations) throws UnableToComplyException {
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
  public void insert(List<DoubleVector> objects) throws UnableToComplyException {
    if (bulk && this.index == null) {
      for (DoubleVector object : objects) {
        Integer id = setNewID(object);
        content.put(id, object);
        setNewID(object);
      }
      this.index = createSpatialIndex(objects.toArray(new FeatureVector[objects.size()]));
    }

    else {
      for (DoubleVector object : objects) {
        insert(object);
      }
    }
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#insert(java.util.List, java.util.List)
   */
  public void insert(List<DoubleVector> objects, List<Map<String, Object>> associations) throws UnableToComplyException {
    if (objects.size() != associations.size()) {
      throw new UnableToComplyException("List of objects and list of associations differ in length.");
    }

    if (bulk && this.index == null) {
      for (int i = 0; i < objects.size(); i++) {
        Integer id = setNewID(objects.get(i));
        content.put(id, objects.get(i));
        setAssociations(id, associations.get(i));
      }

      this.index = createSpatialIndex(objects.toArray(new FeatureVector[objects.size()]));
    }
    else {
      for (int i = 0; i < objects.size(); i++) {
        if (i % 1000 == 0) System.out.println("i " + i);
        insert(objects.get(i), associations.get(i));
      }
    }

  }

  /**
   * Removes all objects from the database that are equal to the given object.
   *
   * @param object the object to be removed from database
   */
  public void delete(DoubleVector object) {
    for (Integer id : content.keySet()) {
      if (content.get(id).equals(object)) {
        delete(id);
      }
    }
  }

  /**
   * Removes the object with the given id from the database.
   *
   * @param id the id of an object to be removed from the database
   */
  public void delete(Integer id) {
    DoubleVector object = content.remove(id);
    index.delete(object);
    restoreID(id);
    deleteAssociations(id);
  }

  /**
   * Returns the number of objects contained in this Database.
   *
   * @return the number of objects in this Database
   */
  public int size() {
    return content.size();
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
  public List<QueryResult> rangeQuery(Integer id, String epsilon, DistanceFunction<DoubleVector> distanceFunction) {
    if (!(distanceFunction instanceof SpatialDistanceFunction))
      throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");

    return index.rangeQuery(content.get(id), epsilon, (SpatialDistanceFunction) distanceFunction);
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
  public List<QueryResult> kNNQuery(DoubleVector queryObject, int k, DistanceFunction<DoubleVector> distanceFunction) {
    if (!(distanceFunction instanceof SpatialDistanceFunction))
      throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");

    return index.kNNQuery(queryObject, k, (SpatialDistanceFunction) distanceFunction);
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
  public List<QueryResult> reverseKNNQuery(Integer id, int k, DistanceFunction<DoubleVector> distanceFunction) {
    throw new UnsupportedOperationException("Not yet supported!");
  }

  /**
   * Returns the MetricalObject represented by the specified id.
   *
   * @param id the id of the Object to be obtained from the Database
   * @return Object the Object represented by to the specified id in the
   *         Database
   */
  public DoubleVector get(Integer id) {
    return content.get(id);
  }

  /**
   * Returns an iterator iterating over all keys of the database.
   *
   * @return an iterator iterating over all keys of the database
   */
  public Iterator<Integer> iterator() {
    return content.keySet().iterator();
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
    String[] remainingParameters = optionHandler.grabOptions(super.setParameters(args));

    bulk = optionHandler.isSet(BULK_LOAD_F);
    return remainingParameters;
  }

  /**
   * Returns a list of the ids of the leaf nodes of the underlying spatial index of this database.
   * @return a list of the ids of the leaf nodes of the underlying spatial index of this database
   */
  public List<Entry> getLeafNodes() {
    return index.getLeafNodes();
  }

  /**
   * Returns the spatial node with the specified ID.
   * @param nodeID the id of the node to be returned
   * @return the spatial node with the specified ID
   */
  public SpatialNode getNode(int nodeID) {
    return index.getNode(nodeID);  
  }

  /**
   * Returns the spatial index object with the specified parameters for this
   * database.
   *
   * @param objects the objects to be indexed
   */
  public abstract SpatialIndex createSpatialIndex(final FeatureVector[] objects);

  /**
   * Returns the spatial index object with the specified parameters for this
   * database.
   *
   * @param dimensionality the dimensionality of the objects to be indexed
   */
  public abstract SpatialIndex createSpatialIndex(int dimensionality);
}