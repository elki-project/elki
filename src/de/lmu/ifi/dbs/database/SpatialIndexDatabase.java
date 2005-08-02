package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.index.spatial.SpatialDistanceFunction;
import de.lmu.ifi.dbs.index.spatial.SpatialIndex;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

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
    if (this.index == null) {
      index = createSpatialIndex(o.getDimensionality());
    }

    Integer id = newID();
    index.insert(id.intValue(), o);
    content.put(id, o);
    return id;
  }

  /**
   * Inserts the given object into the database. While inserting the object
   * the association given at the same time is associated using the specified
   * association id.
   *
   * @param object        the object to be inserted
   * @param association   the association to be associated with the object
   * @param associationID the association id for the asociation
   * @return the ID assigned to the inserted object
   * @throws de.lmu.ifi.dbs.utilities.UnableToComplyException
   *          if insertion is not possible
   */
  public Integer insert(MetricalObject object, Object association, String associationID) throws UnableToComplyException {
    Integer id = insert(object);
    associate(associationID, id, association);
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
  public void insert(List<MetricalObject> objects) throws UnableToComplyException {
    if (this.index == null) {
      int[] ids = new int[objects.size()];
      for (int i = 0; i < objects.size(); i++) {
        Integer id = newID();
        content.put(id, (RealVector) objects.get(i));
        ids[i] = newID().intValue();
      }

      this.index = createSpatialIndex(objects.toArray(new RealVector[objects.size()]), ids);
    }
    else {
      for (int i = 0; i < objects.size(); i++) {
        MetricalObject o = objects.get(i);
        insert(o);
      }
    }
  }

  /**
   * Initializes the database by inserting the specified objects into the
   * database. While inserting the objects the associations given at the same
   * time are associated using the specified association id.
   *
   * @param objects       the list of objects to be inserted
   * @param associations  the list of associations in the same order as the objects to
   *                      be inserted
   * @param associationID the association id for the association
   * @throws de.lmu.ifi.dbs.utilities.UnableToComplyException
   *          if initialization is not possible or, e.g., the parameters
   *          objects and associations differ in length
   */
  public void insert(List<MetricalObject> objects, List<Object> associations, String associationID) throws UnableToComplyException {
    if (objects.size() != associations.size()) {
      throw new UnableToComplyException("List of objects and list of associations differ in length.");
    }

    if (false) {
//    if (this.index == null) {
      int[] ids = new int[objects.size()];
      for (int i = 0; i < objects.size(); i++) {
        Integer id = newID();
        content.put(id, (RealVector) objects.get(i));
        associate(associationID, id, associations.get(i));
        ids[i] = newID().intValue();
      }

      this.index = createSpatialIndex(objects.toArray(new RealVector[objects.size()]), ids);
    }
    else {
      for (int i = 0; i < objects.size(); i++) {
        MetricalObject o = objects.get(i);
        insert(o);
      }
    }
  }

  /**
   * Removes the given object from the database.
   *
   * @param object the object to be removed from database
   */
  public void delete(MetricalObject object) {
    if (!(object instanceof RealVector)) {
      throw new IllegalArgumentException("Object must be instance of RealVector!");
    }
    RealVector o = (RealVector) object;

    for (Iterator<Integer> iter = content.keySet().iterator(); iter.hasNext();) {
      Integer id = iter.next();
      if (content.get(id).equals(o)) {// TODO equals or == ???
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
    RealVector object = content.remove(id);
    index.delete(id.intValue(), object);
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
   * Returns a string representation of this database.
   *
   * @return a string representation of this database.
   */
  public String toString() {
    return index.toString();
  }

  /**
   * Returns the spatial index object with the specified parameters for this
   * database.
   *
   * @param objects the objects to be indexed
   * @param ids     the ids of the objects
   */
  public abstract SpatialIndex createSpatialIndex(final RealVector[] objects, final int[] ids);

  /**
   * Returns the spatial index object with the specified parameters for this
   * database.
   *
   * @param dimensionality the dimensionality of the objects to be indexed
   */
  public abstract SpatialIndex createSpatialIndex(int dimensionality);
}