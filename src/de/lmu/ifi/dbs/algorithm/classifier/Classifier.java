package de.lmu.ifi.dbs.algorithm.classifier;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;

import java.io.Serializable;



/**
 * A Classifier is to hold a model that is built based on a database,
 * and to classify a new instance of the same type.
 * 
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface Classifier<M extends MetricalObject> extends Algorithm<M>,Serializable
{

    /**
     * The association id for the class label.
     */
    public static final AssociationID CLASS = AssociationID.CLASS;
    
    /**
     * Performs the training.
     * Sets available labels.
     * 
     * @param database the database to build the model on
     * @throws IllegalStateException if the classifier is not properly initiated (e.g. parameters are not set)
     */
    public abstract void buildClassifier(Database<M> database) throws IllegalStateException;

    /**
     * Provides a classification for a given instance.
     * The classification is the index of the class-label
     * in {@link #labels labels}.
     * 
     * @param instance an instance to classify
     * @return a classification for the given instance
     * @throws IllegalStateException if the Classifier has not been initialized
     * or properly trained
     */
    public abstract int classify(M instance) throws IllegalStateException;

    /**
     * Returns the class label for the given class index.
     * 
     * 
     * @param index the class index
     * @return the class label for the given index
     * @throws IllegalArgumentException if the given index is not valid
     */
    public abstract String getClassLabel(int index) throws IllegalArgumentException;
    
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
