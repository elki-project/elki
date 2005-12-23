package de.lmu.ifi.dbs.evaluation;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Puts all data into the training set and lets the test set empty.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class AllTraining<M extends MetricalObject> implements Holdout<M>
{


    /**
     * Provides a single pair of training and test data sets,
     * where the training set contains the complete data set
     * as comprised by the given database,
     * the test data set is an empty database of the same type.
     * 
     * @see de.lmu.ifi.dbs.evaluation.Holdout#partition(de.lmu.ifi.dbs.database.Database)
     */
    public TrainingAndTestSet<M>[] partition(Database<M> database)
    {
        TrainingAndTestSet<M>[] split = new TrainingAndTestSet[1];
        Map<Integer,List<Integer>> partition = new HashMap<Integer,List<Integer>>();
        partition.put(0,database.getIDs());
        partition.put(1,new ArrayList<Integer>(0));
        try
        {
            Map<Integer,Database<M>> part = database.partition(partition);
            split[0] = new TrainingAndTestSet<M>(part.get(0),part.get(1));
            return split;
        }
        catch(UnableToComplyException e)
        {
            throw new RuntimeException(e);
        }
    }

}
