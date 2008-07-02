package de.lmu.ifi.dbs.elki.evaluation.holdout;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * A holdout procedure is to provide a range of partitions of
 * a database to pairs of training and test data sets.
 * 
 * @author Arthur Zimek
 */
public interface Holdout<O extends DatabaseObject,L extends ClassLabel<L>> extends Parameterizable
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
    public  TrainingAndTestSet<O,L>[] partition(Database<O> database);
    
    /**
     * Sets the class labels occuring in the given database
     * to this holdout.
     * 
     * 
     * @param database the database to take all class labels from
     */
    public void setClassLabels(Database<O> database);
    
    /**
     * Returns the complete database as it has been set in
     * the partition method.
     * 
     * 
     * @return the complete database as it has been set in
     * the partition method
     */
    public Database<O> completeData();
}
