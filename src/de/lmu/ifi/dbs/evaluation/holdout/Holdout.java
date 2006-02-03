package de.lmu.ifi.dbs.evaluation.holdout;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

/**
 * A holdout procedure is to provide a range of partitions of
 * a database to pairs of training and test data sets.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface Holdout<M extends MetricalObject> extends Parameterizable
{
    /**
     * Provides a range of partitions of
     * a database to pairs of training and test data sets.
     * 
     * 
     * @param database the database to partition
     * @return a range of partitions of
     * a database to pairs of training and test data sets
     */
    public TrainingAndTestSet<M>[] partition(Database<M> database);
    
    public void setClassLabels(Database<M> database);
    
    public Database<M> completeData();
}
