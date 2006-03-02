package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.classifier.Classifier;
import de.lmu.ifi.dbs.algorithm.classifier.CorrelationBasedClassifier;
import de.lmu.ifi.dbs.algorithm.clustering.COPAC;
import de.lmu.ifi.dbs.algorithm.clustering.Clustering;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.data.HierarchicalClassLabel;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.List;
import java.util.Map;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class CoDeC extends AbstractAlgorithm<DoubleVector>
{
    public static final String EVALUATE_AS_CLASSIFIER_F = "classify";
    
    public static final String EVALUATE_AS_CLASSIFIER_D = "demand evaluation of the cluster-models as classifier";
    
    public static final String CLASS_LABEL_P = "classlabel";
    
    public static final String DEFAULT_CLASS_LABEL_CLASS = HierarchicalClassLabel.class.toString();
    
    public static final String CLASS_LABEL_D = "<class>use the designated classLabel class (must implement "+ClassLabel.class.getName()+"). Default: "+DEFAULT_CLASS_LABEL_CLASS;
    
    public static final String CLUSTERING_ALGORITHM_P = "clusteringAlgorithm";
    
    public static final String CLUSTERING_ALGORITHM_D = "<class>the clustering algorithm to use to derive cluster - default: "+COPAC.class.getName()+".";
    
    private boolean evaluateAsClassifier = false;
    
    private ClassLabel classLabel = new HierarchicalClassLabel();
    
    private Result<DoubleVector> result;
    
    private Clustering<DoubleVector> clusteringAlgorithm = new COPAC();
    
    /**
     * The Dependency Derivator algorithm.
     */
    private DependencyDerivator dependencyDerivator = new DependencyDerivator();
    
    private Classifier<DoubleVector> classifier = new CorrelationBasedClassifier();

    public CoDeC()
    {
        parameterToDescription.put(EVALUATE_AS_CLASSIFIER_F, EVALUATE_AS_CLASSIFIER_D);
        parameterToDescription.put(CLASS_LABEL_P+OptionHandler.EXPECTS_VALUE,CLASS_LABEL_D);
        parameterToDescription.put(CLUSTERING_ALGORITHM_P+OptionHandler.EXPECTS_VALUE, CLUSTERING_ALGORITHM_D);
        optionHandler = new OptionHandler(parameterToDescription,CoDeC.class.getName());
    }
    
    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#runInTime(de.lmu.ifi.dbs.database.Database)
     */
    @SuppressWarnings("unchecked")
    protected void runInTime(Database<DoubleVector> database) throws IllegalStateException
    {
        // run clustering algorithm
        if(isVerbose())
        {
            System.out.println("\napply clustering algorithm: "+clusteringAlgorithm.getClass().getName());
        }
        clusteringAlgorithm.run(database);
        // if evaluate as classifier:
        // demand database with class labels, evaluate CorrelationBasedClassifier
        if(evaluateAsClassifier)
        {
            Database<DoubleVector> annotatedClasses = clusteringAlgorithm.getResult().associate((Class<ClassLabel>) classLabel.getClass()); 
            classifier.run(annotatedClasses);
            result = classifier.getResult();
        }        
        // if not evaluate as classifier:
        // derive dependency model for every cluster
        else
        {
            ClusteringResult<DoubleVector> clusterResult = clusteringAlgorithm.getResult();
            Map<ClassLabel,Database<DoubleVector>> cluster = clusterResult.clustering((Class<ClassLabel>) classLabel.getClass());
            for(ClassLabel label : cluster.keySet())
            {
                dependencyDerivator.run(cluster.get(label));
                clusterResult.appendModel(label, dependencyDerivator.getResult());
            }
            result = clusterResult;
        }

    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
     */
    public Result<DoubleVector> getResult()
    {
        return result;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
     */
    public Description getDescription()
    {
        return new Description("CoDeC","Correlation Derivator and Classifier","...","unpublished");
    }

    @Override
    public String description()
    {
        StringBuilder description = new StringBuilder();
        description.append(super.description());
        description.append('\n');
        description.append(clusteringAlgorithm.description());
        description.append("\nand either:\n");
        description.append(classifier.description());
        description.append("\nor:\n");
        description.append(dependencyDerivator.description());
        description.append('\n');
        return description.toString();
    }

    @Override
    public List<AttributeSettings> getAttributeSettings()
    {
        List<AttributeSettings> settings = super.getAttributeSettings();
        settings.addAll(clusteringAlgorithm.getAttributeSettings());
        if(evaluateAsClassifier)
        {
            settings.addAll(classifier.getAttributeSettings());
        }
        else
        {
            settings.addAll(dependencyDerivator.getAttributeSettings());
        }
        return settings;
    }

    @Override
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingParameters = super.setParameters(args);
        evaluateAsClassifier = optionHandler.isSet(EVALUATE_AS_CLASSIFIER_F);
        if(optionHandler.isSet(CLASS_LABEL_P))
        {
            classLabel = Util.instantiate(ClassLabel.class, optionHandler.getOptionValue(CLASS_LABEL_P));
        }
        if(optionHandler.isSet(CLUSTERING_ALGORITHM_P))
        {
            clusteringAlgorithm = Util.instantiate(Clustering.class, optionHandler.getOptionValue(CLUSTERING_ALGORITHM_P));
        }
        remainingParameters = clusteringAlgorithm.setParameters(remainingParameters);
        if(evaluateAsClassifier)
        {
            remainingParameters = classifier.setParameters(remainingParameters);
        }
        else
        {
            remainingParameters = dependencyDerivator.setParameters(remainingParameters);
        }
        return remainingParameters;
    }

}
