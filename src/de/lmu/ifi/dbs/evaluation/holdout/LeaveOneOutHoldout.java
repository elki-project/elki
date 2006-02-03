package de.lmu.ifi.dbs.evaluation.holdout;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A leave-one-out-holdout is to provide a set of partitions of a database
 * where each instanceis once hold out as a test instance while the respectively remaining
 * instances are training instances.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class LeaveOneOutHoldout<M extends MetricalObject> extends AbstractHoldout<M>
{
    

    /**
     * Provides a leave-one-out partitioner.
     */
    public LeaveOneOutHoldout()
    {
        optionHandler = new OptionHandler(parameterToDescription,LeaveOneOutHoldout.class.getName());        
    }

    /**
     * Provides a set of partitions of the database, where each element is once
     * hold out as test instance, the remainng instances are given in the
     * training set.
     * @see de.lmu.ifi.dbs.evaluation.holdout.Holdout#partition(de.lmu.ifi.dbs.database.Database)
     */
    public TrainingAndTestSet<M>[] partition(Database<M> database)
    {
        setClassLabels(database);
        int size = database.size();
        TrainingAndTestSet<M>[] partitions = new TrainingAndTestSet[size];
        List<Integer> ids = database.getIDs();
        for(int i = 0; i < size; i++)
        {
            Map<Integer,List<Integer>> partition = new HashMap<Integer,List<Integer>>();
            List<Integer> training = new ArrayList<Integer>(ids);
            List<Integer> test = new ArrayList<Integer>();
            Integer holdoutID = training.remove(i);
            test.add(holdoutID);
            partition.put(0,training);
            partition.put(1,test);
            try
            {
                Map<Integer,Database<M>> part = database.partition(partition);
                partitions[i] = new TrainingAndTestSet<M>(part.get(0),part.get(1),this.labels);
            }
            catch(UnableToComplyException e)
            {
                throw new RuntimeException(e);
            }
        }
        return partitions;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    public String description()
    {
        return "Provides a leave-one-out (jackknife) holdout.";
    }

    /**
     * Does not need any parameters. The given parameter array
     * is returned unchanged.
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        return super.setParameters(args);
    }
    
    public List<AttributeSettings> getAttributeSettings()
    {
        List<AttributeSettings> settings = super.getAttributeSettings();
        return settings;
    }

}
