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
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.*;

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
     * Flag to demand evaluation of the cluster-models as classifier.
     * <p>Key: {@code -codec.classify} </p>
     */
    private final Flag EVALUATE_AS_CLASSIFIER_FLAG = new Flag(OptionID.CODEC_EVALUATE_AS_CLASSIFIER);

    /**
     * Parameter to specify the designated classLabel class,
     * must extend {@link ClassLabel}.
     * <p>Key: {@code -codec.classlabel} </p>
     * <p>Default value: {@link HierarchicalClassLabel} </p>
     */
    private final ClassParameter<ClassLabel> CLASSLABEL_PARAM =
        new ClassParameter<ClassLabel>(OptionID.CODEC_CLASSLABEL,
            ClassLabel.class, HierarchicalClassLabel.class.getName());

    /**
     * Parameter to specify the clustering algorithm to use to derive cluster,
     * must extend {@link Clustering}.
     * <p>Key: {@code -codec.clusteringAlgorithm} </p>
     * <p>Default value: {@link COPAC} </p>
     */
    private final ClassParameter<Clustering> CLUSTERING_ALGORITHM_PARAM =
        new ClassParameter<Clustering>(OptionID.CODEC_CLUSTERING_ALGORITHM,
            Clustering.class, COPAC.class.getName());

    /**
     * Holds the value of flag classify.
     */
    private boolean evaluateAsClassifier = false;

    /**
     * Holds the value of parameter classlabel.
     */
    private L classLabel;

    /**
     * Holds the value of parameter clusteringAlgorithm.
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
