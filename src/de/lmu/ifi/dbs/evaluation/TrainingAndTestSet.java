package de.lmu.ifi.dbs.evaluation;

import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;

/**
 * Wrapper to hold a pair of training and test data sets.
 * The labels of both, training and test set, are provided in labels.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class TrainingAndTestSet<M extends MetricalObject>
{
    /**
     * The overall labels.
     */
    private ClassLabel[] labels;
    
    /**
     * The training data.
     */
    private Database<M> training;
    
    /**
     * The test data.
     */
    private Database<M> test;

    /**
     * Provides a pair of training and test data sets
     * out of the given two databases.
     */
    public TrainingAndTestSet(Database<M> training, Database<M> test, ClassLabel[] labels)
    {
        this.training = training;
        this.test = test;
        this.labels = labels;
    }

    /**
     * Returns the test data set.
     * 
     * 
     * @return the test data set
     */
    public Database<M> getTest()
    {
        return test;
    }

    /**
     * Returns the training data set.
     * 
     * 
     * @return the training data set
     */
    public Database<M> getTraining()
    {
        return training;
    }
    
    /**
     * Returns the overall labels.
     * 
     * 
     * @return the overall labels
     */
    public ClassLabel[] getLabels()
    {
        return labels;
    }

}
