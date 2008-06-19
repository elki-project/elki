package de.lmu.ifi.dbs.algorithm.classifier;

import de.lmu.ifi.dbs.algorithm.DependencyDerivator;
import de.lmu.ifi.dbs.algorithm.result.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;

/**
 * TODO Arthur comment class
 *
 * @author Arthur Zimek
 * @param <V> the type of RealVector handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 * @param <L> the type of the ClassLabel the Classifier is assigning
 */
public class CorrelationBasedClassifier<V extends RealVector<V, ?>, D extends Distance<D>, L extends ClassLabel<L>>
    extends AbstractClassifier<V, L> {

    /**
     * Generated serial version UID.
     */
    private static final long serialVersionUID = -6786297567169490313L;

    /**
     * todo arthur comment
     */
    private DependencyDerivator<V, D> dependencyDerivator = new DependencyDerivator<V, D>();

    /**
     * todo arthur comment
     */
    private CorrelationAnalysisSolution<V>[] model;

    /**
     * @see Classifier#buildClassifier(de.lmu.ifi.dbs.database.Database, de.lmu.ifi.dbs.data.ClassLabel[])
     */
    @SuppressWarnings("unchecked")
    public void buildClassifier(Database<V> database, L[] classLabels) throws IllegalStateException {
        setLabels(classLabels);
        model = (CorrelationAnalysisSolution<V>[]) new CorrelationAnalysisSolution[classLabels.length];

        // init partitions
        Map<Integer, List<Integer>> partitions = new Hashtable<Integer, List<Integer>>();
        for (int i = 0; i < getLabels().length; i++) {
            partitions.put(i, new ArrayList<Integer>());
        }
        // add each db object to its class
        for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
            Integer id = it.next();
            Integer classID = Arrays.binarySearch(getLabels(), database.getAssociation(CLASS, id));
            partitions.get(classID).add(id);
        }

        try {
            Map<Integer, Database<V>> clusters = database.partition(partitions);
            List<Integer> keys = new ArrayList<Integer>(clusters.keySet());
            Collections.sort(keys);
            for (Integer classID : keys) {
                if (isVerbose()) {
                    verbose("Deriving model for class "
                            + this.getClassLabel(classID).toString());
                }
                Database<V> cluster = clusters.get(classID);
                dependencyDerivator.run(cluster);
                model[classID] = dependencyDerivator.getResult();
            }
        }
        catch (UnableToComplyException e) {
            IllegalStateException ise = new IllegalStateException(e);
            ise.fillInStackTrace();
            throw ise;
        }

    }

    /**
     * Provides the Normally distributed probability density value for a given
     * value distance and a given &sigma;. &mu; is assumed as 0.
     *
     * @param distance the distance to assess the probability of
     * @param sigma    the standard deviation of the underlying distribution
     * @return the density for the given distance and sigma
     */
    @SuppressWarnings("unchecked")
    protected double density(double distance, double sigma) {
        double distanceDivSigma = distance / sigma;
        double density = StrictMath.pow(Math.E, (distanceDivSigma * distanceDivSigma * -0.5))
            / (sigma * Math.sqrt(2 * Math.PI));
        return density;
    }

    /**
     * @see Classifier#classDistribution(de.lmu.ifi.dbs.data.DatabaseObject)
     */
    public double[] classDistribution(V instance) throws IllegalStateException {
        double[] distribution = new double[this.model.length];
        double sumOfDensities = 0.0;
        for (int i = 0; i < distribution.length; i++) {
            double distance = model[i].distance(instance);
            distribution[i] = density(distance, model[i].getStandardDeviation());
            sumOfDensities += distribution[i];
        }
        for (int i = 0; i < distribution.length; i++) {
            distribution[i] /= sumOfDensities;
        }
        return distribution;
    }

    /**
     * @see Classifier#model()
     */
    public String model() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(stream);
        for (int classID = 0; classID < model.length; classID++) {
            CorrelationAnalysisSolution<V> model_i = model[classID];
            try {
                printStream.print("Model for class ");
                printStream.println(getClassLabel(classID).toString());
                model_i.output(printStream, null, dependencyDerivator.getAttributeSettings());
            }
            catch (UnableToComplyException e) {
                warning(e.getMessage() + "\n");
            }
        }
        return stream.toString();
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
     * todo arthur description
     */
    public Description getDescription() {
        return new Description("CorrelationBasedClassifier",
            "CorrelationBasedClassifier", "...", "unpublished");
    }

    /**
     * Returns the AttributeSettings of CorrelationBasedClassifier and of the
     * inherent DependencyDerivator.
     *
     * @see AbstractClassifier#getAttributeSettings()
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> settings = super.getAttributeSettings();
        settings.addAll(dependencyDerivator.getAttributeSettings());
        return settings;
    }

    /**
     * Sets the parameters required by
     * {@link CorrelationBasedClassifier CorrelationBasedClassifier} and passes
     * the remaining parameters to the inherent
     * {@link DependencyDerivator DependencyDerivator}.
     *
     * @see AbstractClassifier#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // dependency derivator
        String[] dependencyDerivatorParameters = new String[remainingParameters.length];
        System.arraycopy(remainingParameters, 0, dependencyDerivatorParameters, 0, remainingParameters.length);
        dependencyDerivator.setVerbose(isVerbose());
        dependencyDerivator.setTime(isTime());
        remainingParameters = dependencyDerivator.setParameters(dependencyDerivatorParameters);

        setParameters(args, remainingParameters);
        return remainingParameters;
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#description()
     */
    @Override
    public String description() {
        StringBuffer description = new StringBuffer();
        description.append(super.description());
        description.append(dependencyDerivator.description());
        return description.toString();
    }

}
