package de.lmu.ifi.dbs.elki.algorithm.classifier;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.Arrays;
import java.util.List;

/**
 * KNNClassifier classifies instances based on the class distribution among the
 * k nearest neighbors in a database.
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 * @param <L> the type of the ClassLabel the Classifier is assigning
 */
public class KNNClassifier<O extends DatabaseObject, D extends Distance<D>, L extends ClassLabel<L>>
    extends DistanceBasedClassifier<O, D, L> {

    /**
     * Generated serial version UID.
     */
    private static final long serialVersionUID = 5467968122892109545L;

    /**
     * Parameter to specify the number of neighbors to take into account for classification,
     * must be an integer greater than 0.
     * <p>Default value: {@code 1} </p>
     * <p>Key: {@code -knnclassifier.k} </p>
     */
    private final IntParameter K_PARAM = new IntParameter(OptionID.KNN_CLASSIFIER_K,
        new GreaterConstraint(0), 1);

    /**
     * Holds the database where the classification is to base on.
     */
    protected Database<O> database;

    /**
     * Holds the value for k.
     */
    protected int k;

    /**
     * Provides a KNNClassifier.
     */
    public KNNClassifier() {
        super();
        // parameter k
        addOption(K_PARAM);
    }

    /**
     * Checks whether the database has the class labels set. Collects the class
     * labels available n the database. Holds the database to lazily classify
     * new instances later on.
     *
     * @see Classifier#buildClassifier(de.lmu.ifi.dbs.elki.database.Database,de.lmu.ifi.dbs.elki.data.ClassLabel[])
     */
    public void buildClassifier(Database<O> database, L[] labels) throws IllegalStateException {
        this.setLabels(labels);
        this.database = database;
    }

    /**
     * Provides a class distribution for the given instance. The distribution is
     * the relative value for each possible class among the k nearest neighbors
     * of the given instance in the previously specified database.
     *
     * @see Classifier#classDistribution(DatabaseObject)
     */
    public double[] classDistribution(O instance) throws IllegalStateException {
        try {
            double[] distribution = new double[getLabels().length];
            int[] occurences = new int[getLabels().length];

            List<QueryResult<D>> query = database.kNNQueryForObject(instance,
                k, getDistanceFunction());
            for (QueryResult<D> neighbor : query) {
                // noinspection unchecked
                int index = Arrays.binarySearch(getLabels(),
                    (CLASS.getType().cast(database.getAssociation(CLASS, neighbor.getID()))));
                if (index >= 0) {
                    occurences[index]++;
                }
            }
            for (int i = 0; i < distribution.length; i++) {
                distribution[i] = ((double) occurences[i])
                                  / (double) query.size();
            }
            return distribution;
        }
        catch (NullPointerException e) {
            IllegalArgumentException iae = new IllegalArgumentException(e);
            iae.fillInStackTrace();
            throw iae;
        }
    }

    /**
     * @see de.lmu.ifi.dbs.elki.algorithm.Algorithm#getDescription()
     */
    public Description getDescription() {
        return new Description(
            "kNN-classifier",
            "kNN-classifier",
            "lazy classifier classifies a given instance to the majority class of the k-nearest neighbors",
            "");
    }

    /**
     * Sets the parameter k, if speicified. Otherwise, k will remain at the
     * default value 1 or the previously specified value, respectively.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // parameter k
        k = getParameterValue(K_PARAM);

        return remainingParameters;
    }

    /**
     * @see Classifier#model()
     */
    public String model() {
        return "lazy learner - provides no model";
    }

}
