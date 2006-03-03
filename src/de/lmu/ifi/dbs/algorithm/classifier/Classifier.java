package de.lmu.ifi.dbs.algorithm.classifier;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;

import java.io.Serializable;



/**
 * A Classifier is to hold a model that is built based on a database,
 * and to classify a new instance of the same type.
 * 
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface Classifier<O extends DatabaseObject> extends Algorithm<O>,Serializable
{

    
    /**
     * Performs the training.
     * Sets available labels.
     * 
     * @param database the database to build the model on
     * @param classLabels the classes to be learned
     * @throws IllegalStateException if the classifier is not properly initiated (e.g. parameters are not set)
     */
    public void buildClassifier(Database<O> database, ClassLabel[] classLabels) throws IllegalStateException;

    /**
     * Provides a classification for a given instance.
     * The classification is the index of the class-label.
     * 
     * @param instance an instance to classify
     * @return a classification for the given instance
     * @throws IllegalStateException if the Classifier has not been initialized
     * or properly trained
     */
    public int classify(O instance) throws IllegalStateException;

    /**
     * Returns the class label for the given class index.
     * 
     * 
     * @param index the class index
     * @return the class label for the given index
     * @throws IllegalArgumentException if the given index is not valid
     */
    public ClassLabel getClassLabel(int index) throws IllegalArgumentException;
    
    /**
     * Returns the distribution of class probabilities
     * for the given instance.
     * The distribution is related to the class-labels.
     * 
     * 
     * @param instance an instance to define a class-probability-distribution for
     * @return a class-probability distribution for the given instance
     * @throws IllegalStateException if the Classifier has not been initialized
     * or properly trained
     */
    public double[] classDistribution(O instance) throws IllegalStateException;
    
    /**
     * Provides a String representation of the classification model.
     * 
     * 
     * @return a String representation of the classification model
     */
    public String model(); 
}
