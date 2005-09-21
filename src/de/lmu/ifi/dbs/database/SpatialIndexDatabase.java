package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.data.FeatureVector;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.index.spatial.Entry;
import de.lmu.ifi.dbs.index.spatial.SpatialDistanceFunction;
import de.lmu.ifi.dbs.index.spatial.SpatialIndex;
import de.lmu.ifi.dbs.index.spatial.SpatialNode;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
   * Option string for parameter fileName.
   */
  public static final String FILE_NAME_P = "filename";

  /**
   * Description for parameter filename.
   */
  public static final String FILE_NAME_D = "<name>a file name specifying the name of the file storing the index. " +
    "If this parameter is not set the index is hold in the main memory.";

  /**
   * The default pagesize.
   */
  public static final int DEFAULT_PAGE_SIZE = 4000;

  /**
   * Option string for parameter pagesize.
   */
  public static final String PAGE_SIZE_P = "pagesize";

  /**
   * Description for parameter filename.
   */
  public static final String PAGE_SIZE_D = "<int>an integer value specifying the size of a page in bytes " +
    "(default is " + DEFAULT_PAGE_SIZE + " Byte)";

  /**
   * The default cachesize.
   */
  public static final int DEFAULT_CACHE_SIZE = 1000000;

  /**
   * Option string for parameter cachesize.
   */
  public static final String CACHE_SIZE_P = "cachesize";

  /**
   * Description for parameter cachesize.
   */
  public static final String CACHE_SIZE_D = "<int>an integer value specifying the size of the cache in bytes " +
    "(default is " + DEFAULT_CACHE_SIZE + " Byte)";

  /**
   * The name of the file for storing the DeliRTree.
   */
  protected String fileName;

  /**
   * The size of a page in bytes.
   */
  protected int pageSize;

  /**
   * Tthe size of the cache.
   */
  protected int cacheSize;

  /**
   * If true, a bulk load will be performed.
   */
  protected boolean bulk;

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
    parameterToDescription.put(FILE_NAME_P + OptionHandler.EXPECTS_VALUE, FILE_NAME_D);
    parameterToDescription.put(PAGE_SIZE_P + OptionHandler.EXPECTS_VALUE, PAGE_SIZE_D);
    parameterToDescription.put(CACHE_SIZE_P + OptionHandler.EXPECTS_VALUE, CACHE_SIZE_D);

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

    return index.rangeQuery(content.get(id), epsilon, (SpatialDistanceFunction<DoubleVector>) distanceFunction);
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
  public List<QueryResult> kNNQuery(DoubleVector queryObject, int k, DistanceFunction<DoubleVector> distanceFunction) {
    if (!(distanceFunction instanceof SpatialDistanceFunction))
      throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");

    return index.kNNQuery(queryObject, k, (SpatialDistanceFunction<DoubleVector>) distanceFunction);
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

    if (optionHandler.isSet(FILE_NAME_P)) {
      try {
        fileName = optionHandler.getOptionValue(FILE_NAME_P);
      }
      catch (UnusedParameterException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (NoParameterValueException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }
    else {
      fileName = null;
    }

    if (optionHandler.isSet(PAGE_SIZE_P)) {
      try {
        pageSize = Integer.parseInt(optionHandler.getOptionValue(PAGE_SIZE_P));
        if (pageSize < 0)
          throw new IllegalArgumentException("RTreeDatabase: pagesize has to be greater than zero!");
      }
      catch (UnusedParameterException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (NoParameterValueException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (NumberFormatException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }
    else {
      pageSize = DEFAULT_PAGE_SIZE;
    }

    if (optionHandler.isSet(CACHE_SIZE_P)) {
      try {
        cacheSize = Integer.parseInt(optionHandler.getOptionValue(CACHE_SIZE_P));
        if (cacheSize < 0)
          throw new IllegalArgumentException("RTreeDatabase: cachesize has to be greater than zero!");
      }
      catch (UnusedParameterException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (NoParameterValueException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (NumberFormatException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }
    else {
      cacheSize = DEFAULT_CACHE_SIZE;
    }

    return remainingParameters;
  }

  /**
   * Returns a list of the ids of the leaf nodes of the underlying spatial index of this database.
   *
   * @return a list of the ids of the leaf nodes of the underlying spatial index of this database
   */
  public List<Entry> getLeafNodes() {
    return index.getLeafNodes();
  }

  /**
   * Returns the spatial node with the specified ID.
   *
   * @param nodeID the id of the node to be returned
   * @return the spatial node with the specified ID
   */
  public SpatialNode getNode(int nodeID) {
    return index.getNode(nodeID);
  }

  /**
   * Returns the rot of the underlying index.
   *
   * @return the rot of the index
   */
  public SpatialNode getRootNode() {
    return index.getRoot();
  }


  /**
   * Returns the I/O-Access of this database.
   *
   * @return the I/O-Access of this database
   */
  public int getIOAccess() {
    return index.getIOAccess();
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