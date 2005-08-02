package de.lmu.ifi.dbs.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;

/**
 * SequentialDatabase is a simple implementation of a Database.
 * 
 * It does not support any index structure and holds all objects
 * in main memory (as a Map).
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class SequentialDatabase extends AbstractDatabase
{
    /**
     * Map to hold the objects of the database.
     */
    private Map<Integer,MetricalObject> content;
    
    /**
     * Provides a database for main memory holding all objects in a hashtable.
     *
     */
    public SequentialDatabase()
    {
        super();
        content = new Hashtable<Integer,MetricalObject>();

    }

    /**
     * 
     * @see de.lmu.ifi.dbs.database.Database#insert(java.util.List)
     */
    public void insert(List<MetricalObject> objects) throws UnableToComplyException
    {
        for(Iterator<MetricalObject> iter = objects.iterator(); iter.hasNext();)
        {
            insert(iter.next());
        }
    }
    
    /**
     * 
     * 
     * @see de.lmu.ifi.dbs.database.Database#insert(java.util.List, java.util.List, java.lang.String)
     */
    public void insert(List<MetricalObject> objects, List<Object> associations, String associationID) throws UnableToComplyException
    {
        if(objects.size()!=associations.size())
        {
            throw new UnableToComplyException("List of objects and list of associations differ in length.");
        }
        for(int i = 0; i < objects.size(); i++)
        {
            insert(objects.get(i), associations.get(i), associationID);
        }
    }

    /**
     *
     * @throws UnableToComplyException if database reached limit of storage capacity
     * 
     * @see de.lmu.ifi.dbs.database.Database#insert(de.lmu.ifi.dbs.data.MetricalObject)
     */
    public Integer insert(MetricalObject object) throws UnableToComplyException
    {
        Integer id = newID();
        content.put(id,object);
        return id;
    }
    
    /**
     * 
     * 
     * @see de.lmu.ifi.dbs.database.Database#insert(de.lmu.ifi.dbs.data.MetricalObject, java.lang.Object, java.lang.String)
     */
    public Integer insert(MetricalObject object, Object association, String associationID) throws UnableToComplyException
    {
        Integer id = insert(object);
        associate(associationID, id, association);
        return id;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.database.Database#delete(de.lmu.ifi.dbs.data.MetricalObject)
     */
    public void delete(MetricalObject object)
    {
        for(Iterator<Integer> iter = content.keySet().iterator(); iter.hasNext();)
        {
            Integer id = iter.next();
            if(content.get(id).equals(object))
            {
                delete(id);
            }
        }
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.database.Database#delete(java.lang.Integer)
     */
    public void delete(Integer id)
    {
        content.remove(id);
        restoreID(id);
        deleteAssociations(id);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.database.Database#size()
     */
    public int size()
    {
        return content.size();
    }
    
    /**
     * 
     * @see de.lmu.ifi.dbs.database.Database#kNNQuery(java.lang.Integer, int, de.lmu.ifi.dbs.distance.DistanceFunction)
     */
    public List<QueryResult> kNNQuery(Integer id, int k, DistanceFunction distanceFunction)
    {
        KNNList knnList = new KNNList(k, distanceFunction.infiniteDistance());
        MetricalObject seed = get(id);
        for(Iterator<Integer> iter = iterator(); iter.hasNext();)
        {
            Integer candidateID = iter.next();
            MetricalObject candidate = get(candidateID);
            knnList.add(new QueryResult(candidateID, distanceFunction.distance(seed,candidate)));
        }
        return knnList.toList();
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.database.Database#rangeQuery(java.lang.Integer, java.lang.String, de.lmu.ifi.dbs.distance.DistanceFunction)
     */
    public List<QueryResult> rangeQuery(Integer id, String epsilon, DistanceFunction distanceFunction)
    {
        List<QueryResult> result = new ArrayList<QueryResult>();
        MetricalObject queryObject = content.get(id);
        Distance distance = distanceFunction.valueOf(epsilon);
        for(Iterator<Integer> iter = content.keySet().iterator(); iter.hasNext();)
        {
            Integer currentID = iter.next();
            MetricalObject currentObject = content.get(currentID);
            Distance currentDistance = distanceFunction.distance(queryObject, currentObject); 
            if(currentDistance.compareTo(distance) <= 0)
            {
                result.add(new QueryResult(currentID.intValue(), currentDistance));
            }
        }
        Collections.sort(result);
        return result;
    }

    /**
     * Presently not supported.
     * 
     * @throws UnsupportedOperationException
     * @see de.lmu.ifi.dbs.database.Database#reverseKNNQuery(java.lang.Integer, int, de.lmu.ifi.dbs.distance.DistanceFunction)
     */
    public List<QueryResult> reverseKNNQuery(Integer id, int k, DistanceFunction distanceFunction)
    {
        throw new UnsupportedOperationException();
        // TODO implement eventually
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.database.Database#get(java.lang.Integer)
     */
    public MetricalObject get(Integer id)
    {
        return content.get(id);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.database.Database#iterator()
     */
    public Iterator<Integer> iterator()
    {
        return content.keySet().iterator();
    }


    /**
     * Provides a description for SequentialDatabase.
     * 
     * @see de.lmu.ifi.dbs.database.Database#description()
     */
    public String description()
    {
        StringBuffer description = new StringBuffer();
        description.append(SequentialDatabase.class.getName());
        description.append(" holds all the data in main memory backed by a Hashtable.");
        return description.toString();
    }
    
    /**
     * SequentialDatabase does not require any parameters.
     * Thus, this method returns the given parameters unchanged.
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    public String[] setParameters(String[] args)
    {
        return args;
    }
}
