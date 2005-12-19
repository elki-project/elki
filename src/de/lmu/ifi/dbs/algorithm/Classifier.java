package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.Util;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class Classifier<M extends MetricalObject> extends AbstractAlgorithm<M>
{
    protected String[] labels;

    /**
     * Sets parameter settings as AbstractAlgorithm.
     */
    protected Classifier()
    {
        super();
    }

    /**
     * Performs the training.
     * Sets available labels.
     *  
     * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#runInTime(de.lmu.ifi.dbs.database.Database)
     */
    public abstract void runInTime(Database<M> database) throws IllegalStateException;

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
     * Returns the distribution of class probabilities
     * for the given instance.
     * The distribution is related to the class-labels
     * in {@link #labels lables}.
     * 
     * 
     * @param instance an instance to define a class-probability-distribution for
     * @return a class-probability distribution for the given instance
     * @throws IllegalStateException if the Classifier has not been initialized
     * or properly trained
     */
    public abstract double[] classDistribution(M instance) throws IllegalStateException;
}
