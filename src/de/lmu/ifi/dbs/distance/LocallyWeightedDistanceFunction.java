package de.lmu.ifi.dbs.distance;

import java.util.Hashtable;
import java.util.Map;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.pca.CorrelationPCA;
import de.lmu.ifi.dbs.preprocessing.CorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedCorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

/**
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class LocallyWeightedDistanceFunction extends DoubleDistanceFunction
{
    /**
     * The association id to associate a pca to an object.
     */
    public static final String ASSOCIATION_ID_PCA = CorrelationDimensionPreprocessor.ASSOCIATION_ID_PCA;

    /**
     * The default preprocessor class name.
     */
    public static final Class DEFAULT_PREPROCESSOR_CLASS = KnnQueryBasedCorrelationDimensionPreprocessor.class;

    /**
     * Parameter for preprocessor.
     */
    public static final String PREPROCESSOR_CLASS_P = "preprocessor";

    /**
     * Description for parameter preprocessor.
     */
    public static final String PREPROCESSOR_CLASS_D = "<classname>the preprocessor to determine the correlation dimensions of the objects - must implement " + CorrelationDimensionPreprocessor.class.getName() + ". (Default: " + DEFAULT_PREPROCESSOR_CLASS.getName() + ").";

    /**
     * Property key for preprocessors.
     */
    public static final String PROPERTY_PREPROCESSOR_LOCALLY_WEIGHTED_DISTANCE_FUNCTION = "CORRELATION_DISTANCE_PREPROCESSOR";
    
    
    
    /**
     * OptionHandler for handling options.
     */
    protected OptionHandler optionHandler;

    /**
     * The database that holds the associations for the MetricalObject for which
     * the distances should be computed.
     */
    protected Database db;

    /**
     * The preprocessor to determine the correlation dimensions of the objects.
     */
    private CorrelationDimensionPreprocessor preprocessor;

    public LocallyWeightedDistanceFunction()
    {
        super();
        Map<String, String> parameterToDescription = new Hashtable<String, String>();
        parameterToDescription.put(PREPROCESSOR_CLASS_P + OptionHandler.EXPECTS_VALUE, PREPROCESSOR_CLASS_D);
        optionHandler = new OptionHandler(parameterToDescription, "");
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.distance.RealVectorDistanceFunction#distance(de.lmu.ifi.dbs.data.RealVector,
     *      de.lmu.ifi.dbs.data.RealVector)
     */
    @Override
    public Distance distance(RealVector rv1, RealVector rv2)
    {
        CorrelationPCA pca1 = (CorrelationPCA) db.getAssociation(ASSOCIATION_ID_PCA,rv1.getID());
        CorrelationPCA pca2 = (CorrelationPCA) db.getAssociation(ASSOCIATION_ID_PCA,rv2.getID());
        
        Matrix m1 = pca1.getSimilarityMatrix();
        Matrix m2 = pca2.getSimilarityMatrix();
        
        Matrix rv1Mrv2 = rv1.plus(rv2.negativeVector()).getVector();        
        Matrix rv2Mrv1 = rv2.plus(rv1.negativeVector()).getVector();
        
        double dist1 = rv1Mrv2.times(m1).times(rv1Mrv2.transpose()).get(0,0);
        double dist2 = rv2Mrv1.times(m2).times(rv2Mrv1.transpose()).get(0,0);
        
        return new DoubleDistance(Math.max(Math.sqrt(dist1),Math.sqrt(dist2)));
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.distance.DistanceFunction#setDatabase(de.lmu.ifi.dbs.database.Database)
     */
    public void setDatabase(Database db)
    {
        this.db = db;
        preprocessor.run(this.db);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    public String description()
    {
        StringBuffer description = new StringBuffer();
        description.append(optionHandler.usage("Locally weighted distance function. Pattern for defining a range: \""+requiredInputPattern()+"\".", false));
        description.append('\n');
        description.append("Preprocessors available within this framework for distance function "+this.getClass().getName()+":");
        description.append('\n');
        String preprocessors = PROPERTIES.getProperty(PROPERTY_PREPROCESSOR_LOCALLY_WEIGHTED_DISTANCE_FUNCTION);
        String[] preprocessorNames = (preprocessors == null ? new String[0] : PROPERTY_SEPARATOR.split(preprocessors));
        for(int i = 0; i < preprocessorNames.length; i++)
        {
            try
            {
                String desc = ((CorrelationDimensionPreprocessor) Class.forName(preprocessorNames[i]).newInstance()).description();
                description.append(preprocessorNames[i]);
                description.append('\n');
                description.append(desc);
                description.append('\n');
            }
            catch(InstantiationException e)
            {
                System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
            catch(IllegalAccessException e)
            {
                System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
            catch(ClassNotFoundException e)
            {
                System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
            catch(ClassCastException e)
            {
                System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
        }

        description.append('\n');

        return description.toString();
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingParameters = optionHandler.grabOptions(args);
        if(optionHandler.isSet(PREPROCESSOR_CLASS_P))
        {
            try
            {
                preprocessor = (CorrelationDimensionPreprocessor) Class.forName(optionHandler.getOptionValue(PREPROCESSOR_CLASS_P)).newInstance();
            }
            catch(UnusedParameterException e)
            {
                throw new IllegalArgumentException(e.getMessage());
            }
            catch(NoParameterValueException e)
            {
                throw new IllegalArgumentException(e.getMessage());
            }
            catch(IllegalAccessException e)
            {
                throw new IllegalArgumentException(e.getMessage());
            }
            catch(ClassNotFoundException e)
            {
                throw new IllegalArgumentException(e.getMessage());
            }
            catch(InstantiationException e)
            {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
        else
        {
            try
            {
                preprocessor = (CorrelationDimensionPreprocessor) DEFAULT_PREPROCESSOR_CLASS.newInstance();
            }
            catch(InstantiationException e)
            {
                throw new IllegalArgumentException(e.getMessage());
            }
            catch(IllegalAccessException e)
            {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
        return preprocessor.setParameters(remainingParameters);
    }

}
