package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.index.spatial.SpatialDistanceFunction;
import de.lmu.ifi.dbs.index.spatial.SpatialIndex;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;

import java.util.*;

/**
 * SpatialIndexDatabase is a database implementation which is
 * supported by a spatial index structure.
 *
 * @author Elke Achtert(<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class SpatialIndexDatabase extends AbstractDatabase {

  /**
   * The spatial index storing the data.
   */
  private SpatialIndex index;

  /**
   * Map to hold the objects of the database.
   */
  private final Map<Integer, RealVector> content;

  public SpatialIndexDatabase() {
    super();
    this.content = new Hashtable<Integer, RealVector>();
  }

  /**
   * Initializes the databases by inserting the specified objects into the
   * database.
   *
   * @param objects the list of objects to be inserted
   * @throws de.lmu.ifi.dbs.utilities.UnableToComplyException
   *          if initialization is not possible
   */
  public void init(List<MetricalObject> objects) throws UnableToComplyException {
    int[] ids = new int[objects.size()];
    for (int i = 0; i < objects.size(); i++) {
      Integer id = newID();
      content.put(id, (RealVector) objects.get(i));
      ids[i] = newID().intValue();
    }

    this.index = createSpatialIndex(objects.toArray(new RealVector[objects.size()]), ids);
  }

  /**
   * Inserts the given object into the database.
   *
   * @param object the object to be inserted
   * @return the ID assigned to the inserted object
   * @throws de.lmu.ifi.dbs.utilities.UnableToComplyException
   *          if insertion is not possible
   */
  public Integer insert(MetricalObject object) throws UnableToComplyException {
    if (!(object instanceof RealVector))
      throw new UnableToComplyException("Object must be instance of RealVector!");

    RealVector o = (RealVector) object;

    Integer id = newID();
    index.insert(id.intValue(), o);
    content.put(id, o);
    return id;
  }

  /**
   * Removes the given object from the database.
   *
   * @param object the object to be removed from database
   */
  public void delete(MetricalObject object) {
    if (!(object instanceof RealVector))
      throw new IllegalArgumentException("Object must be instance of RealVector!");

    RealVector o = (RealVector) object;

    for (Iterator<Integer> iter = content.keySet().iterator(); iter.hasNext();) {
      Integer id = iter.next();
      if (content.get(id).equals(o)) {
        index.delete(id.intValue(), o);
        content.remove(id);
        restoreID(id);
      }
    }
  }

  /**
   * Removes the object with the given id from the database.
   *
   * @param id the id of an object to be removed from the database
   */
  public void delete(Integer id) {
    RealVector object = content.remove(id);
    index.delete(id.intValue(), object);
    restoreID(id);
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
  public List<QueryResult> rangeQuery(Integer id, String epsilon, DistanceFunction distanceFunction) {
    if (!(distanceFunction instanceof SpatialDistanceFunction))
      throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");

    RealVector object = content.get(id);
    return index.rangeQuery(object, epsilon, (SpatialDistanceFunction) distanceFunction);
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
  public List<QueryResult> kNNQuery(Integer id, int k, DistanceFunction distanceFunction) {
    if (!(distanceFunction instanceof SpatialDistanceFunction))
      throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");

    RealVector object = content.get(id);
    return index.kNNQuery(object, k, (SpatialDistanceFunction) distanceFunction);
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
  public List<QueryResult> reverseKNNQuery(Integer id, int k, DistanceFunction distanceFunction) {
    throw new UnsupportedOperationException("Not yet supported!");
  }

  /**
   * Returns the MetricalObject represented by the specified id.
   *
   * @param id the id of the Object to be obtained from the Database
   * @return Object the Object represented by to the specified id in the
   *         Database
   */
  public MetricalObject get(Integer id) {
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
   * Returns the spatial index object with the specified parameters
   * for this database.
   *
   * @param objects the objects to be indexed
   * @param ids     the ids of the objects
   */
  public abstract SpatialIndex createSpatialIndex(final RealVector[] objects,
                                                  final int[] ids);
}