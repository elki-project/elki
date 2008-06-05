package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.distance.distancefunction.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.List;

/**
 * Wrapper class for DBSCAN algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DBSCANWrapper extends NormalizationWrapper {

    /**
     * Parameter to specify the maximum radius of the neighborhood to be considered,
     * must be suitable to {@link de.lmu.ifi.dbs.distance.distancefunction.EuklideanDistanceFunction}.
     * <p>Key: {@code -dbscan.epsilon} </p>
     */
    private final PatternParameter EPSILON_PARAM = new PatternParameter(OptionID.DBSCAN_EPSILON);

    /**
     * Parameter to specify the threshold for minimum number of points in
     * the epsilon-neighborhood of a point,
     * must be an integer greater than 0.
     * <p>Key: {@code -dbscan.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(OptionID.OPTICS_MINPTS,
        new GreaterConstraint(0));


    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        DBSCANWrapper wrapper = new DBSCANWrapper();
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
     * Sets the parameters epsilon and minpts in the parameter map additionally to the
     * parameters provided by super-classes.
     */
    public DBSCANWrapper() {
        super();

        // parameter epsilon
        EPSILON_PARAM.setDescription("<pattern>The maximum radius of the neighborhood " +
                                     "to be considerd, must be suitable to " +
                                     EuklideanDistanceFunction.class.getName());

        // parameter min points
        addOption(MINPTS_PARAM);
    }

    /**
     * @see KDDTaskWrapper#getKDDTaskParameters()
     */
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // algorithm DBSCAN
        Util.addParameter(parameters, OptionID.ALGORITHM, DBSCAN.class.getName());

        // epsilon
        Util.addParameter(parameters, EPSILON_PARAM, getParameterValue(EPSILON_PARAM));

        // minpts
        Util.addParameter(parameters, MINPTS_PARAM, Integer.toString(getParameterValue(MINPTS_PARAM)));

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
