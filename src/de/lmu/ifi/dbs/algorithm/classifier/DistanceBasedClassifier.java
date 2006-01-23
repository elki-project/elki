package de.lmu.ifi.dbs.algorithm.classifier;

import de.lmu.ifi.dbs.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.utilities.Util;

/**
 * An abstract classifier already based on DistanceBasedAlgorithm
 * making use of settings for time and verbose and DistanceFunction.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class DistanceBasedClassifier<M extends MetricalObject, D extends Distance<D>> extends DistanceBasedAlgorithm<M,D> implements Classifier<M>
{
    /**
     * Holds the available labels.
     * Should be set by the training method
     * {@link Classifier#buildClassifier(Database) buildClassifier(Database)}.
     */
    protected ClassLabel[] labels = new ClassLabel[0];

    /**
     * Sets parameter settings as AbstractAlgorithm.
     */
    protected DistanceBasedClassifier()
    {
        super();
    }

    /**
     * Calls {@link Classifier#buildClassifier(Database) buildClassifier(database}
     * encapsulated in start and end time of the training.
     * 
     * @param database the database to build the model on
     * @throws IllegalStateException if the classifier is not properly initiated (e.g. parameters are not set)
     */
    @Override
    public void runInTime(Database<M> database) throws IllegalStateException
    {
        buildClassifier(database);        
    }


    /**
     * Provides a classification for a given instance.
     * The classification is the index of the class-label
     * in {@link #labels labels}.
     * 
     * This method returns the index of the maximum probability
     * as provided by {@link #classDistribution(M) classDistribution(M)}.
     * If an extending classifier requires a different classification,
     * it should overwrite this method.
     * 
     * @param instance an instance to classify
     * @return a classification for the given instance
     * @throws IllegalStateException if the Classifier has not been initialized
     * or properly trained
     */
    public int classify(M instance) throws IllegalStateException
    {
        return Util.getIndexOfMaximum(classDistribution(instance));
    }


    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.classifier.Classifier#getClassLabel(int)
     */
    public ClassLabel getClassLabel(int index) throws IllegalArgumentException
    {
        try
        {
            return labels[index];
        }
        catch(ArrayIndexOutOfBoundsException e)
        {
            IllegalArgumentException iae = new IllegalArgumentException("Invalid class index.",e);
            iae.fillInStackTrace();
            throw iae;
        }
    }
    
    

}
