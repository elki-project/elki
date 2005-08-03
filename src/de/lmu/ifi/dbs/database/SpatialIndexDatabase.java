package de.lmu.ifi.dbs.database;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.index.spatial.SpatialDistanceFunction;
import de.lmu.ifi.dbs.index.spatial.SpatialIndex;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;

/**
 * SpatialIndexDatabase is a database implementation which is supported by a
 * spatial index structure.
 * 
 * @author Elke Achtert(<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class SpatialIndexDatabase extends AbstractDatabase
{

    /**
     * The spatial index storing the data.
     */
    private SpatialIndex index;

    /**
     * Map to hold the objects of the database.
     */
    private final Map<Integer, RealVector> content;

    public SpatialIndexDatabase()
    {
        super();
        this.content = new Hashtable<Integer, RealVector>();
    }

    /**
     * Inserts the given object into the database.
     * 
     * @param object
     *            the object to be inserted
     * @return the ID assigned to the inserted object
     * @throws de.lmu.ifi.dbs.utilities.UnableToComplyException
     *             if insertion is not possible
     */
    public Integer insert(MetricalObject object) throws UnableToComplyException
    {
        if(!(object instanceof RealVector))
            throw new UnableToComplyException("Object must be instance of RealVector!");

        RealVector o = (RealVector) object;
        if(this.index == null)
        {
            index = createSpatialIndex(o.getDimensionality());
        }

        Integer id = newID();
        index.insert(id.intValue(), o);
        content.put(id, o);
        return id;
    }

    /**
     * 
     * 
     * @see de.lmu.ifi.dbs.database.Database#insert(de.lmu.ifi.dbs.data.MetricalObject, java.util.Map)
     */
    public Integer insert(MetricalObject object, Map<String, Object> associations) throws UnableToComplyException
    {
        Integer id = insert(object);
        setAssociations(id, associations);
        return id;
    }

    /**
     * Initializes the databases by inserting the specified objects into the
     * database.
     * 
     * @param objects
     *            the list of objects to be inserted
     * @throws de.lmu.ifi.dbs.utilities.UnableToComplyException
     *             if initialization is not possible
     */
    public void insert(List<MetricalObject> objects) throws UnableToComplyException
    {
        if(this.index == null)
        {
            int[] ids = new int[objects.size()];
            for(int i = 0; i < objects.size(); i++)
            {
                Integer id = newID();
                content.put(id, (RealVector) objects.get(i));
                ids[i] = newID().intValue();
            }

            this.index = createSpatialIndex(objects.toArray(new RealVector[objects.size()]), ids);
        }
        else
        {
            for(int i = 0; i < objects.size(); i++)
            {
                MetricalObject o = objects.get(i);
                insert(o);
            }
        }
    }

    /**
     * 
     * 
     * @see de.lmu.ifi.dbs.database.Database#insert(java.util.List, java.util.List)
     */
    public void insert(List<MetricalObject> objects, List<Map<String, Object>> associations) throws UnableToComplyException
    {
        if(objects.size() != associations.size())
        {
            throw new UnableToComplyException("List of objects and list of associations differ in length.");
        }

        // TODO insertion ???
        
    }

    public void insert(List<MetricalObject> objects, List<Object> associations, String associationID) throws UnableToComplyException
    {
        if(objects.size() != associations.size())
        {
            throw new UnableToComplyException("List of objects and list of associations differ in length.");
        }

        if(false)
        {
            // XXX why this branch?

            // if (this.index == null) {
            int[] ids = new int[objects.size()];
            for(int i = 0; i < objects.size(); i++)
            {
                Integer id = newID();
                content.put(id, (RealVector) objects.get(i));
                associate(associationID, id, associations.get(i));
                ids[i] = newID().intValue();
            }

            this.index = createSpatialIndex(objects.toArray(new RealVector[objects.size()]), ids);
        }
        else
        {
            // XXX association???
            for(int i = 0; i < objects.size(); i++)
            {
                MetricalObject o = objects.get(i);
                insert(o);
            }
        }
    }

    /**
     * Removes all objects from the database that are equal to the given object.
     * 
     * @param object
     *            the object to be removed from database
     */
    public void delete(MetricalObject object)
    {
        if(!(object instanceof RealVector))
        {
            throw new IllegalArgumentException("Object must be instance of RealVector!");
        }
        RealVector o = (RealVector) object;

        for(Iterator<Integer> iter = content.keySet().iterator(); iter.hasNext();)
        {
            Integer id = iter.next();
            if(content.get(id).equals(o))
            {
                delete(id);
            }
        }
    }

    /**
     * Removes the object with the given id from the database.
     * 
     * @param id
     *            the id of an object to be removed from the database
     */
    public void delete(Integer id)
    {
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
    public int size()
    {
        return content.size();
    }

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
    public List<QueryResult> rangeQuery(Integer id, String epsilon, DistanceFunction distanceFunction)
    {
        if(!(distanceFunction instanceof SpatialDistanceFunction))
            throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");

        RealVector object = content.get(id);
        return index.rangeQuery(object, epsilon, (SpatialDistanceFunction) distanceFunction);
    }

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
    public List<QueryResult> kNNQuery(Integer id, int k, DistanceFunction distanceFunction)
    {
        if(!(distanceFunction instanceof SpatialDistanceFunction))
            throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");

        RealVector object = content.get(id);
        return index.kNNQuery(object, k, (SpatialDistanceFunction) distanceFunction);
    }

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
    public List<QueryResult> reverseKNNQuery(Integer id, int k, DistanceFunction distanceFunction)
    {
        throw new UnsupportedOperationException("Not yet supported!");
    }

    /**
     * Returns the MetricalObject represented by the specified id.
     * 
     * @param id
     *            the id of the Object to be obtained from the Database
     * @return Object the Object represented by to the specified id in the
     *         Database
     */
    public MetricalObject get(Integer id)
    {
        return content.get(id);
    }

    /**
     * Returns an iterator iterating over all keys of the database.
     * 
     * @return an iterator iterating over all keys of the database
     */
    public Iterator<Integer> iterator()
    {
        return content.keySet().iterator();
    }

    /**
     * Returns a string representation of this database.
     * 
     * @return a string representation of this database.
     */
    public String toString()
    {
        return index.toString();
    }

    /**
     * Returns the spatial index object with the specified parameters for this
     * database.
     * 
     * @param objects
     *            the objects to be indexed
     * @param ids
     *            the ids of the objects
     */
    public abstract SpatialIndex createSpatialIndex(final RealVector[] objects, final int[] ids);

    /**
     * Returns the spatial index object with the specified parameters for this
     * database.
     * 
     * @param dimensionality
     *            the dimensionality of the objects to be indexed
     */
    public abstract SpatialIndex createSpatialIndex(int dimensionality);
}