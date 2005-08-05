package de.lmu.ifi.dbs.database;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

/**
 * Database specifies the requirements for any database implementation. Note that
 * any implementing class is supposed to provide a constructor
 * without parameters for dynamic instantiation.
 * 
 * @author Elke Achtert(<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface Database extends Parameterizable
{
    /**
     * The standard association id to associate a label to an object. 
     */
    public static final String ASSOCIATION_ID_LABEL = "associationIDLabel";
    
    /**
     * Initializes the database by inserting the specified objects into the
     * database.
     * 
     * @param objects
     *            the list of objects to be inserted
     * @throws UnableToComplyException if initialization is not possible
     */
    void insert(List<MetricalObject> objects) throws UnableToComplyException;

    /**
     * Initializes the database by inserting the specified objects into the
     * database. While inserting the objects the associations given at the same time
     * are associated using the specified association id.
     * 
     * 
     * @param objects the list of objects to be inserted
     * @param associations the list of associations in the same order as the objects to be inserted
     * @throws UnableToComplyException if initialization is not possible or, e.g., the parameters objects and associations differ in length
     */
    void insert(List<MetricalObject> objects, List<Map<String,Object>> associations) throws UnableToComplyException;
    
    /**
     * Inserts the given object into the database.
     * 
     * @param object
     *            the object to be inserted
     * @return the ID assigned to the inserted object
     * @throws UnableToComplyException if insertion is not possible
     */
    Integer insert(MetricalObject object) throws UnableToComplyException;

    /**
     * Inserts the given object into the database. While inserting the object the association given at the same time
     * is associated using the specified association id.
     * 
     * 
     * @param object the object to be inserted
     * @param associations the associations to be associated with the object
     * @return the ID assigned to the inserted object
     * @throws UnableToComplyException if insertion is not possible
     */
    Integer insert(MetricalObject object, Map<String,Object> associations) throws UnableToComplyException;
    
    /**
     * Removes all objects from the database that are equal to the given object.
     * 
     * @param object the object to be removed from database
     */
    void delete(MetricalObject object);

    /**
     * Removes the object with the given id from the database.
     * 
     * @param id the id of an object to be removed from the database
     */
    void delete(Integer id);

    /**
     * Returns the number of objects contained in this Database.
     * 
     * @return the number of objects in this Database
     */
    int size();

    /**
     * Performs a range query for the given object ID with the given epsilon
     * range and the according distance function. The query result is in
     * ascending order to the distance to the query object.
     * 
     * @param id
     *            the ID of the query object
     * @param epsilon
     *            the string representation of the query range
     * @param distanceFunction
     *            the distance function that computes the distances beween the
     *            objects
     * @return a List of the query results
     */
    List<QueryResult> rangeQuery(Integer id, String epsilon, DistanceFunction distanceFunction);

    /**
     * Performs a k-nearest neighbor query for the given object ID. The query
     * result is in ascending order to the distance to the query object.
     * 
     * @param id
     *            the ID of the query object
     * @param k
     *            the number of nearest neighbors to be returned
     * @param distanceFunction
     *            the distance function that computes the distances beween the
     *            objects
     * @return a List of the query results
     */
    List<QueryResult> kNNQuery(Integer id, int k, DistanceFunction distanceFunction);

    /**
     * Performs a reverse k-nearest neighbor query for the given object ID. The
     * query result is in ascending order to the distance to the query object.
     * 
     * @param id
     *            the ID of the query object
     * @param k
     *            the number of nearest neighbors to be returned
     * @param distanceFunction
     *            the distance function that computes the distances beween the
     *            objects
     * @return a List of the query results
     */
    List<QueryResult> reverseKNNQuery(Integer id, int k, DistanceFunction distanceFunction);

    /**
     * Returns the MetricalObject represented by the specified id.
     * 
     * @param id
     *            the id of the Object to be obtained from the Database
     * @return Object the Object represented by to the specified id in the
     *         Database
     */
    MetricalObject get(Integer id);

    /**
     * Associates a association in a certain relation to a certain Object.
     * 
     * @param associationID
     *            the id of the association, respectively the name of the
     *            relation
     * @param objectID
     *            the id of the Object to which the association is related
     * @param association
     *            the association to be associated with the specified Object
     */
    void associate(String associationID, Integer objectID, Object association);

    /**
     * Returns the association specified by the given associationID and related
     * to the specified Object.
     * 
     * @param associationID
     *            the id of the association, respectively the name of the
     *            relation
     * @param objectID
     *            the id of the Object to which the association is related
     * @return Object the association which is associated with the specified
     *         Object or null, if there is no association with the specified
     *         associationID nor with the specified objectID
     */
    Object getAssociation(String associationID, Integer objectID);

    /**
     * Returns an iterator iterating over all keys of the database.
     * 
     * 
     * @return an iterator iterating over all keys of the database
     */
    Iterator<Integer> iterator();
    
    /**
     * Returns a short description of the database.
     * (Such as: efficiency in space and time, index structure...)
     * 
     * @return a description of the database
     */
    String description();
    
    /**
     * Returns a List of Databases according to the specified List
     * of Lists of IDs.
     * 
     * 
     * @param partitions a List of Lists of IDs defining a partition of the database
     * @return a List of Databases according to the specified List
     * of Lists of IDs
     * @throws UnableToComplyException in case of problems during insertion
     */
    List<Database> partition(List<List<Integer>> partitions) throws UnableToComplyException;
    
    // TODO remaining methods

    // TODO Normalization ???
    
    // int getNumKNNQueries();

    // void resetNumKNNQueries();

    // int getNumRNNQueries();

    // void resetNumRNNQueries();

    
    // int getIOAccess();

    // void resetIOAccess();

}
