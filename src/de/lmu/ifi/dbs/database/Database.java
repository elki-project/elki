package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.DistanceFunction;

import java.util.List;

/**
 * Defines the requirements of a database.
 * 
 * @author Elke Achtert(<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface Database
{
    /**
     * The standard association id to associate a label to an object. 
     */
    public static final String ASSOCIATION_ID_LABEL = "associationIDLabel";

    /**
     * Initializes the databases by inserting the specified objects into the
     * database.
     * 
     * @param objects
     *            the list of objects to be inserted
     */
    void init(List<MetricalObject> objects);

    /**
     * Inserts the given object into the database.
     * 
     * @param object
     *            the object to be inserted
     * @return the ID assigned to the inserted object
     */
    Integer insert(MetricalObject object);

    /**
     * 
     * @param object
     */
    void delete(MetricalObject object);

    /**
     * 
     * @param id
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
     * @return a List of the query result ids
     */
    List<Integer> rangeQuery(Integer id, String epsilon, DistanceFunction distanceFunction);

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
     * @return a List of the query result ids
     */
    List<Integer> kNNQuery(Integer id, int k, DistanceFunction distanceFunction);

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
     * @return a List of the query result ids
     */
    List<Integer> reverseKNNQuery(Integer id, int k, DistanceFunction distanceFunction);

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
     *         Object
     */
    Object getAssociation(String associationID, Integer objectID);

    // TODO

    // int getNumKNNQueries();

    // void resetNumKNNQueries();

    // int getNumRNNQueries();

    // void resetNumRNNQueries();

    // public abstract DBIterator iterator();

    // int getIOAccess();

    // void resetIOAccess();

}
