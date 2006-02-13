package de.lmu.ifi.dbs.evaluation.holdout;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Puts all data into the training set and lets the test set empty.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class AllTraining<O extends DatabaseObject> extends AbstractHoldout<O>
{


    /**
     * Provides a single pair of training and test data sets,
     * where the training set contains the complete data set
     * as comprised by the given database,
     * the test data set is an empty database of the same type.
     * 
     * @see de.lmu.ifi.dbs.evaluation.holdout.Holdout#partition(de.lmu.ifi.dbs.database.Database)
     */
    public TrainingAndTestSet<O>[] partition(Database<O> database)
    {
        this.database = database;
        setClassLabels(database);
        TrainingAndTestSet<O>[] split = new TrainingAndTestSet[1];
        Map<Integer,List<Integer>> partition = new HashMap<Integer,List<Integer>>();
        partition.put(0,database.getIDs());
        partition.put(1,new ArrayList<Integer>(0));
        try
        {
            Map<Integer,Database<O>> part = database.partition(partition);
            split[0] = new TrainingAndTestSet<O>(part.get(0),part.get(1),labels);
            return split;
        }
        catch(UnableToComplyException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * 
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    public String description()
    {
        return "AllTraining puts the complete database in the training set and gives an empty test set. No parameters required.";
    }

    /**
     * Returns the given parameter array unchanged, since no parameters are required by this class.
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
