package de.lmu.ifi.dbs.elki.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

/**
 * Wrapper class for OPTICS algorithm. Performs an attribute wise normalization
 * on the database objects.
 *
 * @author Elke Achtert
 */
public class OPTICSWrapper<O extends DatabaseObject> extends NormalizationWrapper<O> {

    /**
     * Parameter to specify the maximum radius of the neighborhood to be considered,
     * must be suitable to {@link EuclideanDistanceFunction}.
     * <p>Key: {@code -optics.epsilon} </p>
     */
    private final PatternParameter EPSILON_PARAM;

    /**
     * Parameter to specify the threshold for minimum number of points in
     * the epsilon-neighborhood of a point,
     * must be an integer greater than 0.
     * <p>Key: {@code -optics.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(OPTICS.MINPTS_ID,
        new GreaterConstraint(0));

    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        new OPTICSWrapper().runCLIWrapper(args);
    }

    /**
     * Adds parameters
     * {@link #EPSILON_PARAM} and {@link #MINPTS_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public OPTICSWrapper() {
        super();
        // parameter epsilon
        EPSILON_PARAM = new PatternParameter(OPTICS.EPSILON_ID);
        EPSILON_PARAM.setShortDescription("The maximum radius of the neighborhood " +
            "to be considered, must be suitable to " +
            EuclideanDistanceFunction.class.getName());
        optionHandler.put(EPSILON_PARAM);

        //parameter min points
        optionHandler.put(MINPTS_PARAM);
    }

    @Override
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // algorithm OPTICS
        Util.addParameter(parameters, OptionID.ALGORITHM, OPTICS.class.getName());

        // epsilon
        Util.addParameter(parameters, EPSILON_PARAM, getParameterValue(EPSILON_PARAM));

        // minpts
        Util.addParameter(parameters, MINPTS_PARAM, Integer.toString(getParameterValue(MINPTS_PARAM)));

        // distance function
        Util.addParameter(parameters, OPTICS.DISTANCE_FUNCTION_ID, EuclideanDistanceFunction.class.getName());

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
