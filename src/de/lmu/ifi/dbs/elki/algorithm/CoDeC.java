package de.lmu.ifi.dbs.elki.algorithm;


import de.lmu.ifi.dbs.elki.algorithm.classifier.Classifier;
import de.lmu.ifi.dbs.elki.algorithm.classifier.CorrelationBasedClassifier;
import de.lmu.ifi.dbs.elki.algorithm.clustering.Clustering;
import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.COPAC;
import de.lmu.ifi.dbs.elki.algorithm.result.Result;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.HierarchicalClassLabel;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Arthur Zimek
 * @param <V> the type of RealVector handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */
// todo arthur comment class and constructor
public class CoDeC<V extends RealVector<V, ?>, D extends Distance<D>, L extends ClassLabel<L>> extends AbstractAlgorithm<V> {
    /**
     * OptionID for {@link de.lmu.ifi.dbs.elki.algorithm.CoDeC#CLASSLABEL_PARAM}
     */
    public static final OptionID CLASSLABEL_ID = OptionID.getOrCreateOptionID("" +
        "codec.classlabel",
        "Classname of the designated classLabel class " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(ClassLabel.class) +
            "."
    );

    /**
     * Parameter to specify the designated classLabel class,
     * must extend {@link ClassLabel}.
     * <p>Key: {@code -codec.classlabel} </p>
     * <p>Default value: {@link HierarchicalClassLabel} </p>
     */
    private final ClassParameter<ClassLabel> CLASSLABEL_PARAM =
        new ClassParameter<ClassLabel>(
            CLASSLABEL_ID,
            ClassLabel.class,
            HierarchicalClassLabel.class.getName());

    /**
     * Holds the value of {@link #CLASSLABEL_PARAM}.
     */
    private L classLabel;

    /**
     * OptionID for {@link #CLUSTERING_ALGORITHM_PARAM}
     */
    public static final OptionID CLUSTERING_ALGORITHM_ID = OptionID.getOrCreateOptionID(
        "codec.clusteringAlgorithm",
        "Classname of the clustering algorithm to use to derive cluster " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Clustering.class) +
            "."
    );

    /**
     * Parameter to specify the clustering algorithm to use to derive cluster,
     * must extend {@link Clustering}.
     * <p>Key: {@code -codec.clusteringAlgorithm} </p>
     * <p>Default value: {@link COPAC} </p>
     */
    private final ClassParameter<Clustering> CLUSTERING_ALGORITHM_PARAM =
        new ClassParameter<Clustering>(
            CLUSTERING_ALGORITHM_ID,
            Clustering.class,
            COPAC.class.getName());

    /**
     * Holds the instance of the clustering algorithm by {@link #CLUSTERING_ALGORITHM_PARAM}.
     */
    private Clustering<V> clusteringAlgorithm;

    /**
     * OptionID for {@link de.lmu.ifi.dbs.elki.algorithm.CoDeC#EVALUATE_AS_CLASSIFIER_FLAG}
     */
    public static final OptionID EVALUATE_AS_CLASSIFIER_ID = OptionID.getOrCreateOptionID(
        "codec.classify",
        "Flag to demand evaluation of the cluster-models as classifier.");

    /**
     * Flag to demand evaluation of the cluster-models as classifier.
     * <p>Key: {@code -codec.classify} </p>
     */
    private final Flag EVALUATE_AS_CLASSIFIER_FLAG = new Flag(EVALUATE_AS_CLASSIFIER_ID);

    /**
     * Holds the value of {@link #EVALUATE_AS_CLASSIFIER_FLAG}.
     */
    private boolean evaluateAsClassifier = false;

    /**
     * Holds the result of this algorithm.
     */
    private Result<V> result;

    /**
     * The Dependency Derivator algorithm.
     */
    private DependencyDerivator<V, D> dependencyDerivator;

    /**
     * The classifier for evaluation.
     */
    private Classifier<V, L> classifier;

    /**
     * Adds flag {@link #EVALUATE_AS_CLASSIFIER_FLAG} and
     * parameters {@link #CLASSLABEL_PARAM} and
     * {@link #CLUSTERING_ALGORITHM_PARAM} to the option handler
     * to the option handler additionally to parameters of super class.
     */
    @SuppressWarnings("unchecked")
    public CoDeC() {
        super();

        // flag classify
        addOption(EVALUATE_AS_CLASSIFIER_FLAG);

        // parameter class label
        addOption(CLASSLABEL_PARAM);

        // parameter clustering algorithm
        addOption(CLUSTERING_ALGORITHM_PARAM);
    }

    /**
     * Performs the CoDeC algorithm on the given database.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void runInTime(Database<V> database) throws IllegalStateException {
        // run clustering algorithm
        if (isVerbose()) {
            verbose("\napply clustering algorithm: " + clusteringAlgorithm.getClass().getName());
        }
        clusteringAlgorithm.run(database);
        // if evaluate as classifier:
        // demand database with class labels, evaluate
        // CorrelationBasedClassifier
        if (evaluateAsClassifier) {
            Database<V> annotatedClasses = clusteringAlgorithm.getResult().associate(classLabel.getClass());
            classifier.run(annotatedClasses);
            result = classifier.getResult();
        }
        // if not evaluate as classifier:
        // derive dependency model for every cluster
        else {
            ClusteringResult<V> clusterResult = clusteringAlgorithm.getResult();
            Map<L, Database<V>> cluster = clusterResult.clustering((Class<L>) classLabel.getClass());
            List<L> keys = new ArrayList<L>(cluster.keySet());
            Collections.sort(keys);
            for (L label : keys) {
                if (isVerbose()) {
                    verbose("Deriving dependencies for cluster " + label.toString());
                }
                dependencyDerivator.run(cluster.get(label));
                clusterResult.appendModel(label, dependencyDerivator.getResult());
            }
            result = clusterResult;
        }

    }

    public Result<V> getResult() {
        return result;
    }

    public Description getDescription() {
        return new Description("CoDeC", "Correlation Derivator and Classifier", "...", "unpublished");
    }

    /**
     * Calls the super method
     * and adds to the returned attribute settings the attribute settings of
     * {@link #classifier} or {@link #dependencyDerivator}
     * (if {@link #EVALUATE_AS_CLASSIFIER_FLAG} is set or not).
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();

        attributeSettings.addAll(clusteringAlgorithm.getAttributeSettings());
        if (evaluateAsClassifier) {
            attributeSettings.addAll(classifier.getAttributeSettings());
        }
        else {
            attributeSettings.addAll(dependencyDerivator.getAttributeSettings());
        }
        return attributeSettings;
    }

    /**
     * Calls the super method
     * and sets additionally the value of the flag
     * {@link #EVALUATE_AS_CLASSIFIER_FLAG}
     * and instantiates {@link #classLabel} according to the value of parameter {@link #CLASSLABEL_PARAM}
     * and {@link #clusteringAlgorithm} according to the value of parameter {@link #CLUSTERING_ALGORITHM_PARAM}.
     * The remaining parameters are passed to the {@link #classifier} or {@link #dependencyDerivator}
     * dependent on the value of {@link #EVALUATE_AS_CLASSIFIER_FLAG}.
     */
    @SuppressWarnings("unchecked")
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // evaluateAsClassifier
        evaluateAsClassifier = isSet(EVALUATE_AS_CLASSIFIER_FLAG);

        // classlabel
        classLabel = (L) CLASSLABEL_PARAM.instantiateClass();

        // clusteringAlgorithm
        clusteringAlgorithm = CLUSTERING_ALGORITHM_PARAM.instantiateClass();
        clusteringAlgorithm.setTime(isTime());
        clusteringAlgorithm.setVerbose(isVerbose());
        remainingParameters = clusteringAlgorithm.setParameters(remainingParameters);

        // evaluation
        if (evaluateAsClassifier) {
            classifier = new CorrelationBasedClassifier<V, D, L>();
            classifier.setTime(isTime());
            classifier.setVerbose(isVerbose());
            remainingParameters = classifier.setParameters(remainingParameters);
        }
        else {
            dependencyDerivator = new DependencyDerivator<V, D>();
            dependencyDerivator.setTime(isTime());
            dependencyDerivator.setVerbose(isVerbose());
            remainingParameters = dependencyDerivator.setParameters(remainingParameters);
        }
        setParameters(args, remainingParameters);
        return remainingParameters;
    }

    /**
     * Calls the super method
     * and appends the parameter description of {@link #clusteringAlgorithm},
     * {@link #classifier}, and {@link #dependencyDerivator} (if they are already initialized).
     *
     */
    @Override
    public String parameterDescription() {
        StringBuilder description = new StringBuilder();
        description.append(super.parameterDescription());

        // clustering algorithm
        if (clusteringAlgorithm != null) {
            description.append(Description.NEWLINE);
            description.append(clusteringAlgorithm.parameterDescription());
        }
        // classifier
        if (classifier != null) {
            description.append(Description.NEWLINE);
            description.append(classifier.parameterDescription());
        }
        // dependency derivator
        if (dependencyDerivator != null) {
            description.append(Description.NEWLINE);
            description.append(dependencyDerivator.parameterDescription());
        }
        return description.toString();
    }

}
