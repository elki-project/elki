package de.lmu.ifi.dbs.algorithm.classifier;

import static de.lmu.ifi.dbs.algorithm.classifier.KNNClassifier.K_D;
import static de.lmu.ifi.dbs.algorithm.classifier.KNNClassifier.K_DEFAULT;
import static de.lmu.ifi.dbs.algorithm.classifier.KNNClassifier.K_P;

import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.Arrays;
import java.util.List;

/**
 * KNNClassifier classifies instances based on the class distribution among the k nearest neighbors
 * in a database.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class KNNClassifier<O extends DatabaseObject,D extends Distance<D>> extends DistanceBasedClassifier<O,D>
{
    /**
     * Generated serial version UID.
     */
    private static final long serialVersionUID = 5467968122892109545L;
    
    /**
     * The parameter k.
     */
    public static final String K_P = "k";
    
    /**
     * Default value for the parameter k.
     */
    public static final int K_DEFAULT = 1;
    
    /**
     * Description for parameter k.
     */
    public static final String K_D = "<k>number of neighbors to take into account for classification (default="+K_DEFAULT+")";

    /**
     * Holds the database where the classification is to base on.
     */
    protected Database<O> database;

    /**
     * Holds the value for k.
     */
    protected int k = K_DEFAULT;
    
    /**
     * Provides a KNNClassifier.
     */
    public KNNClassifier()
    {
        super();
        parameterToDescription.put(K_P+OptionHandler.EXPECTS_VALUE,K_D);
        optionHandler = new OptionHandler(parameterToDescription,KNNClassifier.class.getName());
    }

    /**
     * Checks whether the database has the class labels set.
     * Collects the class labels available n the database.
     * Holds the database to lazily classify new instances later on.
     * 
     * @see de.lmu.ifi.dbs.algorithm.classifier.Classifier#buildClassifier(de.lmu.ifi.dbs.database.Database)
     */
    public void buildClassifier(Database<O> database, ClassLabel[] labels) throws IllegalStateException
    {
        this.setLabels(labels);
        this.database = database;        
    }

    /**
     * Provides a class distribution for the given instance.
     * The distribution is the relative value for each possible class
     * among the k nearest neighbors of the given instance in the previously
     * specified database.
     * @see de.lmu.ifi.dbs.algorithm.classifier.Classifier#classDistribution(O)
     */
    public double[] classDistribution(O instance) throws IllegalStateException
    {
        try
        {
            double[] distribution = new double[getLabels().length];
            int[] occurences = new int[getLabels().length];
            
            List<QueryResult<D>> query = database.kNNQueryForObject(instance,k,getDistanceFunction());
            for(QueryResult<D> neighbor : query)
            {
                int index = Arrays.binarySearch(getLabels(),(CLASS.getType().cast(database.getAssociation(CLASS,neighbor.getID()))));
                if(index >= 0)
                {
                    occurences[index]++;
                }
            }
            for(int i = 0; i < distribution.length; i++)
            {
                distribution[i] = ((double) occurences[i]) / (double) query.size();
            }
            return distribution;
        }
        catch(NullPointerException e)
        {
            IllegalArgumentException iae = new IllegalArgumentException(e);
            iae.fillInStackTrace();
            throw iae;
        }
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
     */
    public Description getDescription()
    {
        return new Description("kNN-classifier","kNN-classifier","lazy classifier classifies a given instance to the majority class of the k-nearest neighbors","");
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getAttributeSettings()
     */
    @Override
    public List<AttributeSettings> getAttributeSettings()
    {
        List<AttributeSettings> result = super.getAttributeSettings();

        AttributeSettings attributeSettings = new AttributeSettings(this);
        attributeSettings.addSetting(K_P,Integer.toString(k));
        
        result.add(attributeSettings);
        
        return result;
    }

    /**
     * Sets the parameter k, if speicified. Otherwise, k will remain at the default value 1
     * or the previously specified value, respectively.
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    @Override
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingParameters = super.setParameters(args);
        if(optionHandler.isSet(K_P))
        {
            try
            {
                int k = Integer.parseInt(optionHandler.getOptionValue(K_P));
                if(k<1)
                {
                    throw new NumberFormatException("Parameter "+K_P+" is supposed to be a positive integer. Found: "+optionHandler.getOptionValue(K_P));
                }
                this.k = k;
            }
            catch(NumberFormatException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }
        }
        return remainingParameters;
    }

    /**
     * 
     * 
     * @see de.lmu.ifi.dbs.algorithm.classifier.Classifier#model()
     */
    public String model()
    {
        return "lazy learner - provides no model";
    }

    
}
