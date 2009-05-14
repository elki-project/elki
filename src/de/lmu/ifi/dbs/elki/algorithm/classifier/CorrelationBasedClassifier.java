package de.lmu.ifi.dbs.elki.algorithm.classifier;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.algorithm.DependencyDerivator;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.data.model.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * @author Arthur Zimek
 * @param <V> the type of RealVector handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 * @param <L> the type of the ClassLabel the Classifier is assigning
 */
// todo arthur comment class
public class CorrelationBasedClassifier<V extends RealVector<V, ?>, D extends Distance<D>, L extends ClassLabel>
    extends AbstractClassifier<V, L, Result> {

    // todo arthur comment
    private DependencyDerivator<V, D> dependencyDerivator = new DependencyDerivator<V, D>();

    // todo arthur comment
    private CorrelationAnalysisSolution<V>[] model;

    public void buildClassifier(Database<V> database, ArrayList<L> classLabels) throws IllegalStateException {
        setLabels(classLabels);
        Class<CorrelationAnalysisSolution<V>> vcls = ClassGenericsUtil.uglyCastIntoSubclass(CorrelationAnalysisSolution.class);
        model = ClassGenericsUtil.newArrayOfNull(classLabels.size(), vcls);

        // init partitions
        Map<Integer, List<Integer>> partitions = new Hashtable<Integer, List<Integer>>();
        for (int i = 0; i < getLabels().size(); i++) {
            partitions.put(i, new ArrayList<Integer>());
        }
        // add each db object to its class
        for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
            Integer id = it.next();
            Integer classID = Collections.binarySearch(getLabels(), database.getAssociation(AssociationID.CLASS, id));
            partitions.get(classID).add(id);
        }

        try {
            Map<Integer, Database<V>> clusters = database.partition(partitions);
            List<Integer> keys = new ArrayList<Integer>(clusters.keySet());
            Collections.sort(keys);
            for (Integer classID : keys) {
                if (logger.isVerbose()) {
                  logger.verbose("Deriving model for class "
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
    protected double density(double distance, double sigma) {
        double distanceDivSigma = distance / sigma;
        double density = StrictMath.pow(Math.E, (distanceDivSigma * distanceDivSigma * -0.5))
            / (sigma * Math.sqrt(2 * Math.PI));
        return density;
    }

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
              LoggingUtil.warning(e.getMessage());
            }
            catch(IOException e) {
              LoggingUtil.warning(e.getMessage());
            }
        }
        return stream.toString();
    }

    public Description getDescription() {
        return new Description("CorrelationBasedClassifier",
            "CorrelationBasedClassifier", "...", "unpublished");
    }

    /**
     * Calls the super method
     * and passes the remaining parameters to {@link #dependencyDerivator}.
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
        addParameterizable(dependencyDerivator);

        rememberParametersExcept(args, remainingParameters);
        return remainingParameters;
    }

    /**
     * Calls the super method
     * and appends the parameter description of {@link #dependencyDerivator} if it is already initialized.
     */
    @Override
    public String parameterDescription() {
        StringBuffer description = new StringBuffer();
        description.append(super.parameterDescription());
        description.append(dependencyDerivator.parameterDescription());
        return description.toString();
    }

    @Override
    protected CorrelationAnalysisSolution<V> runInTime(@SuppressWarnings("unused") Database<V> database) throws IllegalStateException {
      // TODO Add sensible result.
      return null;
    }

    @Override
    public CorrelationAnalysisSolution<V> getResult() {
      // TODO: add when runInTime was implemented. 
      return null;
    }

}
