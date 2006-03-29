package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.util.List;

/**
 * Wrapper class for OPTICS algorithm. Performs an attribute wise normalization
 * on the database objects.
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class OPTICSWrapper extends FileBasedDatabaseConnectionWrapper
{
    /**
     * Main method to run this wrapper.
     * 
     * @param args
     *            the arguments to run this wrapper
     */
    public static void main(String[] args)
    {
        OPTICSWrapper wrapper = new OPTICSWrapper();
        try
        {
            wrapper.run(args);
        } catch (ParameterException e)
        {
            System.err.println(wrapper.optionHandler.usage(e.getMessage()));
        }
    }

    /**
     * Sets the parameters epsilon and minpts in the parameter map additionally
     * to the parameters provided by super-classes.
     */
    public OPTICSWrapper()
    {
        super();
        parameterToDescription.put(OPTICS.EPSILON_P
                + OptionHandler.EXPECTS_VALUE, OPTICS.EPSILON_D);
        parameterToDescription.put(OPTICS.MINPTS_P
                + OptionHandler.EXPECTS_VALUE, OPTICS.MINPTS_D);
        optionHandler = new OptionHandler(parameterToDescription, getClass()
                .getName());
    }

    /**
     * @see KDDTaskWrapper#getParameters()
     */
    public List<String> getParameters() throws ParameterException
    {
        List<String> parameters = super.getParameters();

        // algorithm OPTICS
        parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
        parameters.add(OPTICS.class.getName());

        // epsilon
        parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.EPSILON_P);
        parameters.add(optionHandler.getOptionValue(OPTICS.EPSILON_P));

        // minpts
        parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.MINPTS_P);
        parameters.add(optionHandler.getOptionValue(OPTICS.MINPTS_P));

        // distance function
        parameters
                .add(OptionHandler.OPTION_PREFIX + OPTICS.DISTANCE_FUNCTION_P);
        parameters.add(EuklideanDistanceFunction.class.getName());

        // normalization
        parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
        parameters.add(AttributeWiseRealVectorNormalization.class.getName());
        parameters.add(OptionHandler.OPTION_PREFIX
                + KDDTask.NORMALIZATION_UNDO_F);

        // database
        // params.add(OptionHandler.OPTION_PREFIX +
        // AbstractDatabaseConnection.DATABASE_CLASS_P);
        // params.add(RTreeDatabase.class.getName());

        // distance cache
        // params.add(OptionHandler.OPTION_PREFIX + AbstractDatabase.CACHE_F);

        // bulk load
        // params.add(OptionHandler.OPTION_PREFIX +
        // SpatialIndexDatabase.BULK_LOAD_F);

        // page size
        // params.add(OptionHandler.OPTION_PREFIX +
        // SpatialIndexDatabase.PAGE_SIZE_P);
        // params.add("4000");

        // cache size
        // params.add(OptionHandler.OPTION_PREFIX +
        // SpatialIndexDatabase.CACHE_SIZE_P);
        // params.add("120000");

        return parameters;
    }
}
