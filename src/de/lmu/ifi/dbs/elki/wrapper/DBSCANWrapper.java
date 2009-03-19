package de.lmu.ifi.dbs.elki.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

/**
 * Wrapper class for the {@link DBSCAN} algorithm with default parametrization.
 *
 * @author Elke Achtert
 */
public class DBSCANWrapper<O extends DatabaseObject> extends NormalizationWrapper<O> {

    /**
     * Parameter to specify the maximum radius of the neighborhood to be considered,
     * must be suitable to {@link de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction}.
     * <p>Default value: {@code 1.0} </p>
     * <p>Key: {@code -dbscan.epsilon} </p>
     */
    private final PatternParameter EPSILON_PARAM = new PatternParameter(
        DBSCAN.EPSILON_ID,
        "1.0");

    /**
     * Parameter to specify the threshold for minimum number of points in
     * the epsilon-neighborhood of a point,
     * must be an integer greater than 0.
     * <p>Default value: {@code 10} </p>
     * <p>Key: {@code -dbscan.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(
        DBSCAN.MINPTS_ID,
        new GreaterConstraint(0),
        10);


    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        new DBSCANWrapper().runCLIWrapper(args);
    }

     /**
     * Adds parameters
     * {@link #EPSILON_PARAM} and {@link #MINPTS_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public DBSCANWrapper() {
        super();

        // parameter epsilon
        EPSILON_PARAM.setShortDescription("The maximum radius of the neighborhood " +
                                     "to be considerd, must be suitable to " +
                                     EuclideanDistanceFunction.class.getName());
        addOption(EPSILON_PARAM);

        // parameter min points
        addOption(MINPTS_PARAM);
    }

    @Override
    public List<String> getKDDTaskParameters() throws UnusedParameterException {
        List<String> parameters = super.getKDDTaskParameters();

        // algorithm DBSCAN
        OptionUtil.addParameter(parameters, OptionID.ALGORITHM, DBSCAN.class.getName());

        // epsilon
        OptionUtil.addParameter(parameters, EPSILON_PARAM, EPSILON_PARAM.getValue());

        // minpts
        OptionUtil.addParameter(parameters, MINPTS_PARAM, Integer.toString(MINPTS_PARAM.getValue()));

        // database
//    params.add(OptionHandler.OPTION_PREFIX + AbstractDatabaseConnection.DATABASE_CLASS_P);
//    params.add(RTreeDatabase.class.getName());

        // distance cache
//    params.add(OptionHandler.OPTION_PREFIX + AbstractDatabase.CACHE_F);

        // bulk load
//    params.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.BULK_LOAD_F);

        // page size
//    params.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.PAGE_SIZE_P);
//    params.add("4000");

        // cache size
//    params.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.CACHE_SIZE_P);
//    params.add("120000");

        return parameters;
    }
}
