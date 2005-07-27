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
public class SequentialDatabase implements Database
{
    /**
     * Map to hold the objects of the database.
     */
    private Map<Integer,MetricalObject> content;
    
    /**
     * Map to hold association maps.
     */
    private Map<String,Map<Integer,Object>> associations;
    
    /**
     * Counter to provide a new Integer id.
     */
    private int counter;
    
    private List<Integer> reusableIDs;
    
    private boolean reachedLimit;
    
    public SequentialDatabase()
    {
        content = new Hashtable<Integer,MetricalObject>();
        associations = new Hashtable<String,Map<Integer,Object>>();
        counter = Integer.MIN_VALUE;
        reachedLimit = false;
        reusableIDs = new ArrayList<Integer>();
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.database.Database#init(java.util.List)
     */
    public void init(List<MetricalObject> objects) throws UnableToComplyException
    {
        for(Iterator<MetricalObject> iter = objects.iterator(); iter.hasNext();)
        {
            insert(iter.next());
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
        if(reachedLimit && reusableIDs.size() == 0)
        {
            throw new UnableToComplyException("Database reached limit of storage.");
        }
        else
        {
            Integer id = new Integer(counter);
            content.put(id,object);
            if(counter < Integer.MAX_VALUE && !reachedLimit)
            {
                counter++;            
            }
            else
            {
                if(reusableIDs.size() > 0)
                {
                    counter = reusableIDs.remove(0).intValue();
                }
                else
                {
                    reachedLimit = true;
                }
            }
            return id;
        }
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
            if(content.get(id)==object)
            {
                content.remove(id);
                if(reachedLimit)
                {
                    counter = id.intValue();
                }
                else
                {
                    reusableIDs.add(id);
                }
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
        reusableIDs.add(id);
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
        // TODO Auto-generated method stub
        return null;
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
     * 
     * @see de.lmu.ifi.dbs.database.Database#reverseKNNQuery(java.lang.Integer, int, de.lmu.ifi.dbs.distance.DistanceFunction)
     */
    public List<QueryResult> reverseKNNQuery(Integer id, int k, DistanceFunction distanceFunction)
    {
        // TODO Auto-generated method stub
        return null;
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
     * @see de.lmu.ifi.dbs.database.Database#associate(java.lang.String, java.lang.Integer, java.lang.Object)
     */
    public void associate(String associationID, Integer objectID, Object association)
    {
        if(!associations.containsKey(associationID))
        {
            associations.put(associationID,new Hashtable<Integer,Object>());
        }
        associations.get(associationID).put(objectID,association);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.database.Database#getAssociation(java.lang.String, java.lang.Integer)
     */
    public Object getAssociation(String associationID, Integer objectID)
    {
        if(associations.containsKey(associationID))
        {
            return associations.get(associationID).get(objectID);
        }
        else
        {
            return null;
        }
    }

}
