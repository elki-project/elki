package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.*;

/**
 * SequentialDatabase is a simple implementation of a Database. <p/> It does not
 * support any index structure and holds all objects in main memory (as a Map).
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class SequentialDatabase<O extends MetricalObject> extends AbstractDatabase<O>
{
    /**
     * Map to hold the objects of the database.
     */
    private Map<Integer, O> content;

    /**
     * Provides a database for main memory holding all objects in a hashtable.
     */
    public SequentialDatabase()
    {
        super();
        content = new Hashtable<Integer, O>();
        optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
    }

    /**
     * @see de.lmu.ifi.dbs.database.Database#insert(java.util.List)
     */
    public void insert(List<O> objects) throws UnableToComplyException
    {
        for(O object : objects)
        {
            insert(object);
        }
    }

    /**
     * 
     */
    public void insert(List<O> objects, List<Map<AssociationID, Object>> associations) throws UnableToComplyException
    {
        if(objects.size() != associations.size())
        {
            throw new UnableToComplyException("List of objects and list of associations differ in length.");
        }
        for(int i = 0; i < objects.size(); i++)
        {
            insert(objects.get(i), associations.get(i));
        }
    }

    /**
     * @throws UnableToComplyException
     *             if database reached limit of storage capacity
     * @see de.lmu.ifi.dbs.database.Database#insert(de.lmu.ifi.dbs.data.MetricalObject)
     */
    public Integer insert(O object) throws UnableToComplyException
    {
        Integer id = setNewID(object);
        content.put(id, object);
        return id;
    }

    /**
     * 
     */
    public Integer insert(O object, Map<AssociationID, Object> associations) throws UnableToComplyException
    {
        Integer id = insert(object);
        setAssociations(id, associations);
        return id;
    }

    /**
     * @see de.lmu.ifi.dbs.database.Database#delete(de.lmu.ifi.dbs.data.MetricalObject)
     */
    public void delete(O object)
    {
        for(Integer id : content.keySet())
        {
            if(content.get(id).equals(object))
            {
                delete(id);
            }
        }
    }

    /**
     * @see de.lmu.ifi.dbs.database.Database#delete(java.lang.Integer)
     */
    public void delete(Integer id)
    {
        content.remove(id);
        restoreID(id);
        deleteAssociations(id);
    }

    /**
     * @see de.lmu.ifi.dbs.database.Database#size()
     */
    public int size()
    {
        return content.size();
    }

    /**
     * @see de.lmu.ifi.dbs.database.Database#kNNQuery(O, int,
     *      de.lmu.ifi.dbs.distance.DistanceFunction<O,D>)
     */
    public <D extends Distance> List<QueryResult<D>> kNNQuery(O queryObject, int k, DistanceFunction<O, D> distanceFunction)
    {
        distanceFunction.setDatabase(this, false); // TODO: unnötig in dieser Function?

        KNNList<D> knnList = new KNNList<D>(k, distanceFunction.infiniteDistance());
        for(Integer candidateID : content.keySet())
        {            
            O candidate = get(candidateID);
            knnList.add(new QueryResult<D>(candidateID, distanceFunction.distance(queryObject, candidate)));
        }
        return knnList.toList();
    }

    /**
     * @see de.lmu.ifi.dbs.database.Database#kNNQuery(java.lang.Integer, int,
     *      de.lmu.ifi.dbs.distance.DistanceFunction)
     */
    public <D extends Distance> List<QueryResult<D>> kNNQuery(Integer id, int k, DistanceFunction<O, D> distanceFunction)
    {
        distanceFunction.setDatabase(this, false);

        KNNList<D> knnList = new KNNList<D>(k, distanceFunction.infiniteDistance());

        for(Integer candidateID : content.keySet())
        {
            knnList.add(new QueryResult<D>(candidateID, distanceFunction.distance(id, candidateID)));
        }
        return knnList.toList();
    }

    /**
     * @see de.lmu.ifi.dbs.database.Database#rangeQuery(java.lang.Integer,
     *      java.lang.String, de.lmu.ifi.dbs.distance.DistanceFunction)
     */
    public <D extends Distance> List<QueryResult<D>> rangeQuery(Integer id, String epsilon, DistanceFunction<O, D> distanceFunction)
    {
        List<QueryResult<D>> result = new ArrayList<QueryResult<D>>();
        Distance distance = distanceFunction.valueOf(epsilon);
        for(Integer currentID : content.keySet())
        {
            D currentDistance = distanceFunction.distance(id, currentID);
            // System.out.println(currentDistance);
            if(currentDistance.compareTo(distance) <= 0)
            {
                result.add(new QueryResult<D>(currentID, currentDistance));
            }
        }
        Collections.sort(result);
        return result;
    }

    /**
     * Presently not supported. TODO : comment correct???
     * 
     * @throws UnsupportedOperationException
     * @see de.lmu.ifi.dbs.database.Database#reverseKNNQuery(java.lang.Integer,
     *      int, de.lmu.ifi.dbs.distance.DistanceFunction)
     */
    public <D extends Distance> List<QueryResult<D>> reverseKNNQuery(Integer id, int k, DistanceFunction<O, D> distanceFunction)
    {
        distanceFunction.setDatabase(this, false);

        List<QueryResult<D>> result = new ArrayList<QueryResult<D>>();
        for(Iterator<Integer> iter = iterator(); iter.hasNext();)
        {
            Integer candidateID = iter.next();
            List<QueryResult<D>> knns = kNNQuery(candidateID, k, distanceFunction);
            for(QueryResult<D> knn : knns)
            {
                if(knn.getID() == id)
                {
                    result.add(new QueryResult<D>(candidateID, knn.getDistance()));
                }
            }
        }
        Collections.sort(result);
        return result;
    }

    /**
     * @see de.lmu.ifi.dbs.database.Database#get(java.lang.Integer)
     */
    public O get(Integer id)
    {
        return content.get(id);
    }

    /**
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

}
