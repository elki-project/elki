package de.lmu.ifi.dbs.evaluation;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;

/**
 * A holdout procedure is to provide a range of partitions of
 * a database to pairs of training and test data sets.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface Holdout<M extends MetricalObject>
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
}
