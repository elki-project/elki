package de.lmu.ifi.dbs.database;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.utilities.UnableToComplyException;

/**
 * TODO comment
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractSequentialDatabase implements Database
{
    /**
     * Counter to provide a new Integer id.
     */
    private int counter;
    
    private List<Integer> reusableIDs;
    
    private boolean reachedLimit;
    
    protected AbstractSequentialDatabase()
    {
        counter = Integer.MIN_VALUE;
        reachedLimit = false;
        reusableIDs = new ArrayList<Integer>();
    }
    
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
    
    protected void restoreID(Integer id)
    {
        {
            reusableIDs.add(id);
        }
    }
}
