package de.lmu.ifi.dbs.elki.database;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Database specifies the requirements for any database implementation. Note that
 * any implementing class is supposed to provide a constructor
 * without parameters for dynamic instantiation.
 *
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject as element of the database
 */
public interface Database<O extends DatabaseObject> extends Parameterizable {
  /**
   * Initializes the database by inserting the specified objects and their associations
   * into the database.
   *
   * @param objectsAndAssociationsList the list of objects and their associations to be inserted
   * @throws UnableToComplyException if initialization is not possible
   */
  void insert(List<ObjectAndAssociations<O>> objectsAndAssociationsList) throws UnableToComplyException;

  /**
   * Inserts the given object into the database.
   *
   * @param objectAndAssociations the object and its associations to be inserted
   * @return the ID assigned to the inserted object
   * @throws UnableToComplyException if insertion is not possible
   */
  Integer insert(ObjectAndAssociations<O> objectAndAssociations) throws UnableToComplyException;

  /**
   * Removes all objects from the database that are equal to the given object.
   *
   * @param object the object to be removed from database
   */
  void delete(O object);

  /**
   * Removes and returns the object with the given id from the database.
   *
   * @param id the id of an object to be removed from the database
   * @return the object that has been removed
   */
  O delete(Integer id);

  /**
   * Returns the number of objects contained in this Database.
   *
   * @return the number of objects in this Database
   */
  int size();

  /**
   * Returns a random sample of k ids.
   *
   * @param k    the number of ids to return
   * @param seed for random generator
   * @return a list of k ids
   */
  Set<Integer> randomSample(int k, long seed);
  
  /**
   * Performs a range query for the given object ID with the given epsilon
   * range and the according distance function. The query result is in
   * ascending order to the distance to the query object.
   *
   * @param id               the ID of the query object
   * @param epsilon          the string representation of the query range
   * @param distanceFunction the distance function that computes the distances between the
   *                         objects
   * @return a List of the query results
   */
  <D extends Distance<D>> List<QueryResult<D>> rangeQuery(Integer id, String epsilon, DistanceFunction<O, D> distanceFunction);

  /**
   * Performs a k-nearest neighbor query for the given object ID. The query
   * result is in ascending order to the distance to the query object.
   *
   * @param id               the ID of the query object
   * @param k                the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances between the
   *                         objects
   * @return a List of the query results
   */
  <D extends Distance<D>> List<QueryResult<D>> kNNQueryForID(Integer id, int k, DistanceFunction<O, D> distanceFunction);

  /**
   * Performs a k-nearest neighbor query for the given object. The query
   * result is in ascending order to the distance to the query object.
   *
   * @param queryObject      the query object
   * @param k                the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances between the
   *                         objects
   * @return a List of the query results
   */
  <D extends Distance<D>> List<QueryResult<D>> kNNQueryForObject(O queryObject, int k, DistanceFunction<O, D> distanceFunction);

  /**
   * Performs k-nearest neighbor queries for the given object IDs. The query
   * result is in ascending order to the distance to the query object.
   *
   * @param ids              the IDs of the query objects
   * @param k                the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances between the
   *                         objects
   * @return a List of List of the query results
   */
  <D extends Distance<D>> List<List<QueryResult<D>>> bulkKNNQueryForID(List<Integer> ids, int k, DistanceFunction<O, D> distanceFunction);

  /**
   * Performs a reverse k-nearest neighbor query for the given object ID. The
   * query result is in ascending order to the distance to the query object.
   *
   * @param id               the ID of the query object
   * @param k                the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances between the
   *                         objects
   * @return a List of the query results
   */
  <D extends Distance<D>> List<QueryResult<D>> reverseKNNQuery(Integer id, int k, DistanceFunction<O, D> distanceFunction);

  /**
   * Returns the DatabaseObject represented by the specified id.
   *
   * @param id the id of the Object to be obtained from the Database
   * @return Object the Object represented by to the specified id in the
   *         Database
   */
  O get(Integer id);

  /**
   * Associates a association in a certain relation to a certain Object.
   *
   * @param associationID the id of the association, respectively the name of the
   *                      relation
   * @param objectID      the id of the Object to which the association is related
   * @param association   the association to be associated with the specified Object
   * @throws ClassCastException if the association cannot be cast as the class that is specified by the associationID
   */
  <T> void associate(AssociationID<T> associationID, Integer objectID, T association) throws ClassCastException;

  /**
   * Associates a global association in a certain relation to the database.
   *
   * @param associationID the id of the association, respectively the name of the
   *                      relation
   * @param association   the association to be associated with the database
   * @throws ClassCastException if the association cannot be cast as the class that is specified by the associationID
   */
  <T> void associateGlobally(AssociationID<T> associationID, T association) throws ClassCastException;

  /**
   * Returns all associations for a given ID.
   *
   * @param id the id for which the associations are to be returned
   * @return all associations for a given ID
   */
  public Associations getAssociations(final Integer id);

  /**
   * Returns the association specified by the given associationID and related
   * to the specified Object.
   *
   * @param associationID the id of the association, respectively the name of the
   *                      relation
   * @param objectID      the id of the Object to which the association is related
   * @return Object the association which is associated with the specified
   *         Object or null, if there is no association with the specified
   *         associationID nor with the specified objectID
   */
  <T> T getAssociation(AssociationID<T> associationID, Integer objectID);

  /**
   * Returns the global association specified by the given associationID.
   *
   * @param associationID the id of the association, respectively the name of the
   *                      relation
   * @return Object the association or null, if there is no association with the specified
   *         associationID
   */
  <T> T getGlobalAssociation(AssociationID<T> associationID);

  /**
   * Returns an iterator iterating over all keys of the database.
   *
   * @return an iterator iterating over all keys of the database
   */
  Iterator<Integer> iterator();

  /**
   * Returns a list comprising all IDs currently in use.
   *
   * @return a list comprising all IDs currently in use
   */
  List<Integer> getIDs();

  /**
   * Returns a short description of the database.
   * (Such as: efficiency in space and time, index structure...)
   *
   * @return a description of the database
   */
  String parameterDescription();

  /**
   * Returns a Map of partition IDs to Databases of the specified class
   * according to the specified Map of partition IDs
   * to Lists of IDs.
   *
   * @param partitions   a Map of partition IDs to Lists of IDs defining a partition of the database
   * @param dbClass      the class of the databases to be returned, if this argument is <code>null</code>
   *                     the returned databases have the same class as this database
   * @param dbParameters the parameter array of the returned database class, only necessary if parameter
   *                     <code>dbClass</code> is not null
   * @return a Map of partition IDs to Databases of the specified class according to the specified Map
   *         of Lists of IDs - the databases in this map may contain the same objects,
   *         but the managing IDs are generally independent from the IDs in
   *         the original database
   * @throws UnableToComplyException in case of problems during insertion or class instantiation
   */
  Map<Integer, Database<O>> partition(Map<Integer, List<Integer>> partitions,
                                      Class<? extends Database<O>> dbClass, String[] dbParameters) throws UnableToComplyException;

  /**
   * Returns a Map of partition IDs to Databases according to the specified Map of partition IDs
   * to Lists of IDs. Returns the same result as <code>partition(partitions, null, null)</code>.
   *
   * @param partitions a Map of partition IDs to Lists of IDs defining a partition of the database
   * @return a Map of partition IDs to Databases of the specified class according to the specified Map
   *         of Lists of IDs - the databases in this map may contain the same objects,
   *         but the managing IDs are generally independent from the IDs in
   *         the original database
   * @throws UnableToComplyException in case of problems during insertion
   */
  Map<Integer, Database<O>> partition(Map<Integer, List<Integer>> partitions) throws UnableToComplyException;

  /**
   * Checks whether an association is set for every id
   * in the database.
   *
   * @param associationID an association id to be checked
   * @return true, if the association is set for every id in the database, false otherwise
   */
  public boolean isSetForAllObjects(AssociationID<?> associationID);

  /**
   * Checks whether an association is set for at least one id
   * in the database.
   *
   * @param associationID an association id to be checked
   * @return true, if the association is set for every id in the database, false otherwise
   */
  public boolean isSet(AssociationID<?> associationID);

  /**
   * Checks whether a global association is set
   * in the database.
   * 
   * 
   * @param associationID an association id to be checked
   * @return true, if the global association is set in the database, false otherwise 
   */
  public boolean isSetGlobally(AssociationID<?> associationID);
  
  /**
   * Returns the dimensionality of the data contained by this database
   * in case of {@link Database O} extends {@link de.lmu.ifi.dbs.elki.data.FeatureVector FeatureVector}.
   *
   * @return the dimensionality of the data contained by this database
   *         in case of O extends FeatureVector
   * @throws UnsupportedOperationException if {@link Database O} does not extend {@link de.lmu.ifi.dbs.elki.data.FeatureVector FeatureVector}
   *                                       or the database is empty
   */
  public int dimensionality() throws UnsupportedOperationException;

  /**
   * Adds a listener for the <code>DatabaseEvent</code>
   * posted after the database changes.
   *
   * @param l the listener to add
   * @see #removeDatabaseListener
   */
  void addDatabaseListener(DatabaseListener l);

  /**
   * Removes a listener previously added with
   * <code>addTreeModelListener</code>.
   *
   * @param l the listener to remove
   * @see #addDatabaseListener
   */
  void removeDatabaseListener(DatabaseListener l);
  // TODO remaining methods

  // int getNumKNNQueries();

  // void resetNumKNNQueries();

  // int getNumRNNQueries();

  // void resetNumRNNQueries();

  // int getIOAccess();

  // void resetIOAccess();

}
