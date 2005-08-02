package de.lmu.ifi.dbs.database;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.utilities.UnableToComplyException;

/**
 * Provides a mapping for associations based on a Hashtable and functions to get
 * the next usable ID for insertion, making IDs reusable after deletion of the
 * entry. Make sure to delete any associations when deleting an entry (e.g. by
 * calling {@link #deleteAssociations(Integer) deleteAssociations(id)}).
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractDatabase implements Database
{

    /**
     * Map to hold association maps.
     */
    private final Map<String, Map<Integer, Object>> associations;

    /**
     * Counter to provide a new Integer id.
     */
    private int counter;

    /**
     * Provides a list of reusable ids.
     */
    private List<Integer> reusableIDs;

    /**
     * Whether the limit is reached.
     */
    private boolean reachedLimit;

    /**
     * Provides an abstract database including a mapping for associations based on a Hashtable and functions to get
     * the next usable ID for insertion, making IDs reusable after deletion of the
     * entry. Make sure to delete any associations when deleting an entry (e.g. by
     * calling {@link #deleteAssociations(Integer) deleteAssociations(id)}).
     *
     */
    protected AbstractDatabase()
    {
        associations = new Hashtable<String, Map<Integer, Object>>();
        counter = 1;
        reachedLimit = false;
        reusableIDs = new ArrayList<Integer>();
    }

    /**
     * @see de.lmu.ifi.dbs.database.Database#associate(java.lang.String,
     *      java.lang.Integer, java.lang.Object)
     */
    public void associate(String associationID, Integer objectID, Object association)
    {
        if(!associations.containsKey(associationID))
        {
            associations.put(associationID, new Hashtable<Integer, Object>());
        }
        associations.get(associationID).put(objectID, association);
    }

    /**
     * @see de.lmu.ifi.dbs.database.Database#getAssociation(java.lang.String,
     *      java.lang.Integer)
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

    /**
     * Provides a new id suitable as key for a new insertion.
     * 
     * 
     * @return a new id suitable as key for a new insertion
     * @throws UnableToComplyException if the database has reached the limit and, therefore, new insertions are not possible
     */
    protected Integer newID() throws UnableToComplyException
    {
        if(reachedLimit && reusableIDs.size() == 0)
        {
            throw new UnableToComplyException("Database reached limit of storage.");
        }
        else
        {
            Integer id = new Integer(counter);
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
     * Makes the given id reusable for new insertion operations.
     * 
     * 
     * @param id the id to become reusable
     */
    protected void restoreID(Integer id)
    {
        {
            reusableIDs.add(id);
        }
    }
    
    /**
     * Deletes associations for the given id if there are any.
     * 
     * 
     * @param id id of which all associations are to be deleted
     */
    protected void deleteAssociations(Integer id)
    {
        for(Iterator iter = associations.keySet().iterator(); iter.hasNext();)
        {
            associations.get(iter.next()).remove(id);
        }
    }
}
