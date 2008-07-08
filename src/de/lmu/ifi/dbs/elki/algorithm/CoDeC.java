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
 * TODO Arthur comment class and constructor
 *
 * @author Arthur Zimek
 * @param <V> the type of RealVector handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */
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
     * OptionID for {@link de.lmu.ifi.dbs.elki.algorithm.CoDeC#CLUSTERING_ALGORITHM_PARAM}
     */
    public static final OptionID CLUSTERING_ALGORITHM_ID = OptionID.getOrCreateOptionID(
        "codec.clusteringAlgorithm",
        "Classname of the clustering algorithm to use to derive cluster " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Clustering.class) +
            "."
    );

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
     * Holds the value of #EVALUATE_AS_CLASSIFIER_FLAG.
     */
    private boolean evaluateAsClassifier = false;

    /**
     * Holds the value of #CLASSLABEL_PARAM.
     */
    private L classLabel;

    /**
     * Holds the value of #CLUSTERING_ALGORITHM_PARAM.
     */
    private Clustering<V> clusteringAlgorithm;

    /**
     * Holds the result of this algorithm.
     */
    private Result<V> result;

    /**
     * The Dependency Derivator algorithm.
     */
    private DependencyDerivator<V, D> dependencyDerivator = new DependencyDerivator<V, D>();

    /**
     * The classifier for evaluation.
     */
    private Classifier<V, L> classifier = new CorrelationBasedClassifier<V, D, L>();

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
     * @see AbstractAlgorithm#runInTime(de.lmu.ifi.dbs.elki.database.Database)
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

    /**
     * @see Algorithm#getResult()
     */
    public Result<V> getResult() {
        return result;
    }

    /**
     * @see Algorithm#getDescription()
     */
    public Description getDescription() {
        return new Description("CoDeC", "Correlation Derivator and Classifier", "...", "unpublished");
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#getAttributeSettings()
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
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
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
        String[] clusteringAlgorithmParameters = new String[remainingParameters.length];
        System.arraycopy(remainingParameters, 0, clusteringAlgorithmParameters, 0, remainingParameters.length);
        clusteringAlgorithm.setTime(isTime());
        clusteringAlgorithm.setVerbose(isVerbose());
        remainingParameters = clusteringAlgorithm.setParameters(clusteringAlgorithmParameters);
        setParameters(args, remainingParameters);

        // evaluation
        String[] evaluationParmeters = new String[remainingParameters.length];
        System.arraycopy(remainingParameters, 0, evaluationParmeters, 0, remainingParameters.length);

        if (evaluateAsClassifier) {
            classifier.setTime(isTime());
            classifier.setVerbose(isVerbose());
            remainingParameters = classifier.setParameters(evaluationParmeters);
        }
        else {
            dependencyDerivator.setTime(isTime());
            dependencyDerivator.setVerbose(isVerbose());
            remainingParameters = dependencyDerivator.setParameters(evaluationParmeters);
        }
        setParameters(args, remainingParameters);
        return remainingParameters;
    }

}
