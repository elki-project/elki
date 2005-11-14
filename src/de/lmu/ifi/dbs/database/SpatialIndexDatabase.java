package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.index.spatial.DirectoryEntry;
import de.lmu.ifi.dbs.index.spatial.SpatialDistanceFunction;
import de.lmu.ifi.dbs.index.spatial.SpatialIndex;
import de.lmu.ifi.dbs.index.spatial.SpatialNode;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;

import java.util.List;
import java.util.Map;

/**
 * SpatialIndexDatabase is a database implementation which is supported by a
 * spatial index structure.
 *
 * @author Elke Achtert(<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class SpatialIndexDatabase<O extends RealVector> extends IndexDatabase<O> {

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
  protected boolean bulk;

  /**
   * The spatial index storing the data.
   */
  SpatialIndex<O> index;

  public SpatialIndexDatabase() {
    super();
    parameterToDescription.put(BULK_LOAD_F, BULK_LOAD_D);
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
      index = createSpatialIndex(object.getDimensionality());
    }
    index.insert(object);
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
    if (bulk && this.index == null) {
      for (O object : objects) {
        putToContent(object);
      }
      this.index = createSpatialIndex(objects);
    }

    else {
      for (O object : objects) {
        insert(object);
      }
    }
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#insert(java.util.List, java.util.List)
   */
  public void insert(List<O> objects, List<Map<AssociationID, Object>> associations) throws UnableToComplyException {
    if (objects.size() != associations.size()) {
      throw new UnableToComplyException("List of objects and list of associations differ in length.");
    }

    if (bulk && this.index == null) {
      for (int i = 0; i < objects.size(); i++) {
        Integer id = putToContent(objects.get(i));
        setAssociations(id, associations.get(i));
      }

      this.index = createSpatialIndex(objects);
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
  public <D extends Distance> List<QueryResult<D>> rangeQuery(Integer id, String epsilon, DistanceFunction<O, D> distanceFunction) {
    if (!(distanceFunction instanceof SpatialDistanceFunction))
      throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");

    return index.rangeQuery(get(id), epsilon, (SpatialDistanceFunction<O, D>) distanceFunction);
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
  public <D extends Distance> List<QueryResult<D>> kNNQueryForObject(O queryObject, int k, DistanceFunction<O, D> distanceFunction) {
    if (!(distanceFunction instanceof SpatialDistanceFunction))
      throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");

    return index.kNNQuery(queryObject, k, (SpatialDistanceFunction<O, D>) distanceFunction);
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
  public <D extends Distance>List<QueryResult<D>> kNNQueryForID(Integer id, int k, DistanceFunction<O, D> distanceFunction) {
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
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = super.setParameters(args);
    bulk = optionHandler.isSet(BULK_LOAD_F);
    return remainingParameters;
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
   * Returns the spatial node with the specified ID.
   *
   * @param nodeID the id of the node to be returned
   * @return the spatial node with the specified ID
   */
  public SpatialNode getNode(int nodeID) {
    return index.getNode(nodeID);
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
   * Returns the index of this database.
   *
   * @return the index of this database
   */
  public SpatialIndex<O> getIndex() {
    return index;
  }

  /**
   * Returns the spatial index object with the specified parameters for this
   * database.
   *
   * @param objects the objects to be indexed
   */
  public abstract SpatialIndex<O> createSpatialIndex(List<O> objects);

  /**
   * Returns the spatial index object with the specified parameters for this
   * database.
   *
   * @param dimensionality the dimensionality of the objects to be indexed
   */
  public abstract SpatialIndex<O> createSpatialIndex(int dimensionality);
}