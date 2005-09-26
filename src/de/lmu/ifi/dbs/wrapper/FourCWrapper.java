package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.DBSCAN;
import de.lmu.ifi.dbs.algorithm.FourC;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.database.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.distance.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.normalization.AttributeWiseDoubleVectorNormalization;
import de.lmu.ifi.dbs.pca.AbstractCorrelationPCA;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedCorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.preprocessing.RangeQueryBasedCorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.ArrayList;

/**
 * A wrapper for the 4C algorithm.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class FourCWrapper extends AbstractWrapper
{
    /**
     * Default value for the big value (kappa).
     */
    public static final String BIG_DEFAULT = "50";
    
    /**
     * Default value for the small value.
     */
    public static final String SMALL_DEFAULT = "1";
    
    /**
     * Parameter for epsilon.
     */
    public static final String EPSILON_P = "epsilon";

    /**
     * Description for parameter epsilon.
     */
    public static final String EPSILON_D = "<epsilon>an epsilon value suitable to the distance function: " + LocallyWeightedDistanceFunction.class.getName();

    /**
     * Parameter minimum points.
     */
    public static final String MINPTS_P = "minpts";

    /**
     * Description for parameter minimum points.
     */
    public static final String MINPTS_D = "<int>minpts";

    /**
     * Epsilon.
     */
    protected String epsilon;

    /**
     * Minimum points.
     */
    protected String minpts;
    
    /**
     * Parameter lambda.
     */
    protected static final String LAMBDA_P = "lambda";
    
    /**
     * Description for parameter lambda.
     */
    protected static final String LAMBDA_D = "<lambda>(integer) correlation dimensionality";

    /**
     * Holds lambda.
     */
    protected String lambda;
    

    /**
     * Provides a wrapper for the 4C algorithm.
     */
    public FourCWrapper()
    {
        super();
        parameterToDescription.put(EPSILON_P+OptionHandler.EXPECTS_VALUE, EPSILON_D);
        parameterToDescription.put(MINPTS_P+OptionHandler.EXPECTS_VALUE, MINPTS_D);
        parameterToDescription.put(LAMBDA_P+OptionHandler.EXPECTS_VALUE, LAMBDA_D);
        optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
    }

    /**
     * Runs 4C setting default parameters.
     * 
     *
     */
    public void run4C(String[] args)
    {
        if(output == null)
        {
            throw new IllegalArgumentException("Parameter " + AbstractWrapper.OUTPUT_P + " is not set!");
        }
        ArrayList<String> params = new ArrayList<String>();
        for(String s : args)
        {
            params.add(s);
        }

        params.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
        params.add(FourC.class.getName());

        params.add(OptionHandler.OPTION_PREFIX + DBSCAN.DISTANCE_FUNCTION_P);
        params.add(LocallyWeightedDistanceFunction.class.getName());

        params.add(OptionHandler.OPTION_PREFIX + LocallyWeightedDistanceFunction.PREPROCESSOR_CLASS_P);
        params.add(RangeQueryBasedCorrelationDimensionPreprocessor.class.getName());

        params.add(OptionHandler.OPTION_PREFIX + DBSCAN.EPSILON_P);
        params.add(epsilon);
        
        params.add(OptionHandler.OPTION_PREFIX + RangeQueryBasedCorrelationDimensionPreprocessor.EPSILON_P);
        params.add(epsilon);

        params.add(OptionHandler.OPTION_PREFIX + DBSCAN.MINPTS_P);
        params.add(minpts);

        params.add(OptionHandler.OPTION_PREFIX + KnnQueryBasedCorrelationDimensionPreprocessor.K_P);
        params.add(minpts);
        
        params.add(OptionHandler.OPTION_PREFIX + LAMBDA_P);
        params.add(lambda);
        
        params.add(OptionHandler.OPTION_PREFIX + AbstractCorrelationPCA.BIG_VALUE_P);
        params.add(BIG_DEFAULT);
        
        params.add(OptionHandler.OPTION_PREFIX + AbstractCorrelationPCA.SMALL_VALUE_P);
        params.add(SMALL_DEFAULT);

        params.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
        params.add(AttributeWiseDoubleVectorNormalization.class.getName());
        params.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

        params.add(OptionHandler.OPTION_PREFIX + FileBasedDatabaseConnection.INPUT_P);
        params.add(input);

        params.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
        params.add(output);

        if(time)
        {
            params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.TIME_F);
        }
        if(verbose)
        {
            params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
        }

        KDDTask task = new KDDTask();
        task.setParameters(params.toArray(new String[params.size()]));
        task.run();

    }
    
    /**
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingParameters = super.setParameters(args);
        try
        {
            epsilon = optionHandler.getOptionValue(EPSILON_P);
            minpts = optionHandler.getOptionValue(MINPTS_P);
            lambda = optionHandler.getOptionValue(LAMBDA_P);
        }
        catch(UnusedParameterException e)
        {
            throw new IllegalArgumentException(e);
        }
        return remainingParameters;
    }
    
    public static void main(String[] args)
    {
        FourCWrapper wrapper = new FourCWrapper();
        try
        {
            String[] remainingParameters = wrapper.setParameters(args);
            wrapper.run4C(remainingParameters);
        }
        catch(AbortException e)
        {
            System.out.println(e.getMessage());
            System.exit(1);
        }
        catch(IllegalArgumentException e)
        {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        catch(IllegalStateException e)
        {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
