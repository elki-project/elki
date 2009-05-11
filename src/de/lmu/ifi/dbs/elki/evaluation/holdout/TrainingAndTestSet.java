package de.lmu.ifi.dbs.elki.evaluation.holdout;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;

/**
 * Wrapper to hold a pair of training and test data sets.
 * The labels of both training and test set are provided in labels.
 * 
 * @author Arthur Zimek
 */
public class TrainingAndTestSet<O extends DatabaseObject, L extends ClassLabel>
{
    /**
     * The overall labels.
     */
    private L[] labels;
    
    /**
     * The training data.
     */
    private Database<O> training;
    
    /**
     * The test data.
     */
    private Database<O> test;

    /**
     * Provides a pair of training and test data sets
     * out of the given two databases.
     */
    public TrainingAndTestSet(Database<O> training, Database<O> test, L[] labels)
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
    public Database<O> getTest()
    {
        return test;
    }

    /**
     * Returns the training data set.
     * 
     * 
     * @return the training data set
     */
    public Database<O> getTraining()
    {
        return training;
    }
    
    /**
     * Returns the overall labels.
     * 
     * 
     * @return the overall labels
     */
    public L[] getLabels()
    {
        return labels;
    }

    /**
     * Array constructor helper.
     * 
     * @param <O> Database object class
     * @param <L> Class label class
     * @param len array length
     * @return new array of the given size.
     */
    public static <O extends DatabaseObject, L extends ClassLabel> TrainingAndTestSet<O,L>[] newArray(int len) {
      return ClassGenericsUtil.newArrayOfNull(len);
    }
}
