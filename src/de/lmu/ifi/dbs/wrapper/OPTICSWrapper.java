package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.distance.distancefunction.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.List;

/**
 * Wrapper class for OPTICS algorithm. Performs an attribute wise normalization
 * on the database objects.
 *
 * @author Elke Achtert
 */
public class OPTICSWrapper extends NormalizationWrapper {

    /**
     * Parameter to specify the maximum radius of the neighborhood to be considered,
     * must be suitable to {@link EuklideanDistanceFunction EuklideanDistanceFunction}.
     * <p>Key: {@code -optics.epsilon} </p>
     */
    private final PatternParameter EPSILON_PARAM;

    /**
     * Parameter to specify the threshold for minimum number of points in
     * the epsilon-neighborhood of a point,
     * must be an integer greater than 0.
     * <p>Key: {@code -optics.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(OptionID.OPTICS_MINPTS,
        new GreaterConstraint(0));

    /**
     * Holds the value of the epsilon parameter.
     */
    private String epsilon;

    /**
     * THolds the value of the minpts parameter.
     */
    private int minpts;

    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        OPTICSWrapper wrapper = new OPTICSWrapper();
        try {
            wrapper.setParameters(args);
            wrapper.run();
        }
        catch (ParameterException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), cause);
        }
        catch (AbortException e) {
            wrapper.verbose(e.getMessage());
        }
        catch (Exception e) {
            wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), e);
        }
    }

    /**
     * Sets the parameters epsilon and minpts in the parameter map additionally
     * to the parameters provided by super-classes.
     */
    public OPTICSWrapper() {
        super();
        // parameter epsilon
        EPSILON_PARAM = new PatternParameter(OptionID.OPTICS_EPSILON);
        EPSILON_PARAM.setDescription("<pattern>The maximum radius of the neighborhood " +
            "to be considered, must be suitable to " +
            EuklideanDistanceFunction.class.getName());
        optionHandler.put(EPSILON_PARAM);

        //parameter min points
        optionHandler.put(MINPTS_PARAM);
    }

    /**
     * @see KDDTaskWrapper#getKDDTaskParameters()
     */
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // algorithm OPTICS
        Util.addParameter(parameters, OptionID.ALGORITHM, OPTICS.class.getName());

        // epsilon
        Util.addParameter(parameters, EPSILON_PARAM, epsilon);

        // minpts
        Util.addParameter(parameters, MINPTS_PARAM, Integer.toString(minpts));

        // distance function
        parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.DISTANCE_FUNCTION_P);
        parameters.add(EuklideanDistanceFunction.class.getName());

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

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // epsilon, minpts
        epsilon = getParameterValue(EPSILON_PARAM);
        minpts = getParameterValue(MINPTS_PARAM);

        return remainingParameters;
    }
}
