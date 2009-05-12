package de.lmu.ifi.dbs.elki.algorithm.classifier;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.Util;

/**
 * An abstract classifier already based on AbstractAlgorithm making use of
 * settings for time and verbose.
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <L> the type of the ClassLabel the Classifier is assigning
 * @param <R> result type
 */
public abstract class AbstractClassifier<O extends DatabaseObject, L extends ClassLabel, R extends Result> extends AbstractAlgorithm<O,R> implements Classifier<O, L, R> {
    /**
     * Holds the available labels. Should be set by the training method
     * {@link Classifier#buildClassifier(de.lmu.ifi.dbs.elki.database.Database,de.lmu.ifi.dbs.elki.data.ClassLabel[])}
     */
    private L[] labels = ClassGenericsUtil.newArrayOfNull(0, ClassLabel.class);

    /**
     * Provides a classification for a given instance. The classification is the
     * index of the class-label in {@link #labels labels}. <p/> This method
     * returns the index of the maximum probability as provided by
     * {@link #classDistribution(DatabaseObject)}. If an extending classifier
     * requires a different classification, it should overwrite this method.
     *
     * @param instance an instance to classify
     * @return a classification for the given instance
     * @throws IllegalStateException if the Classifier has not been initialized or properly
     *                               trained
     */
    public int classify(O instance) throws IllegalStateException {
        return Util.getIndexOfMaximum(classDistribution(instance));
    }

    /**
     * Return ith class label.
     * 
     * @param index index
     * @return class label with index index
     */
    public final L getClassLabel(int index) throws IllegalArgumentException {
        try {
            return labels[index];
        }
        catch (ArrayIndexOutOfBoundsException e) {
            IllegalArgumentException iae = new IllegalArgumentException("Invalid class index.", e);
            iae.fillInStackTrace();
            throw iae;
        }
    }

    /**
     * Returns the class labels as currently set.
     *
     * @return the class labels
     */
    public final L[] getLabels() {
        return this.labels;
    }

    /**
     * Sets the given class labels as class labels to use. The given array gets
     * sorted by the setting method.
     *
     * @param labels the labels to use for building the classifier
     */
    public final void setLabels(L[] labels) {
        this.labels = labels;
        Arrays.sort(this.labels);
    }

}
