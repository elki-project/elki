package de.lmu.ifi.dbs.elki.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

/**
 * Wrapper class for LOF algorithm.
 *
 * @author Elke Achtert
 */
public class LOFWrapper<O extends DatabaseObject> extends FileBasedDatabaseConnectionWrapper<O> {

    /**
     * Parameter to specify the number of nearest neighbors of an object to be considered for computing its LOF,
     * must be an integer greater than 0.
     * <p>Key: {@code -lof.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(
        LOF.MINPTS_ID,
        new GreaterConstraint(0));

    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        new LOFWrapper().runCLIWrapper(args);
    }

    /**
     * Adds parameter
     * {@link #MINPTS_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public LOFWrapper() {
        super();
        addOption(MINPTS_PARAM);
    }

    @Override
    public List<String> getKDDTaskParameters() throws UnusedParameterException {
        List<String> parameters = super.getKDDTaskParameters();

        // algorithm LOF
        OptionUtil.addParameter(parameters, OptionID.ALGORITHM, LOF.class.getName());

        // minpts
        OptionUtil.addParameter(parameters, LOF.MINPTS_ID, Integer.toString(MINPTS_PARAM.getValue()));

        // distance function
        OptionUtil.addParameter(parameters, LOF.DISTANCE_FUNCTION_ID, EuclideanDistanceFunction.class.getName());

        // normalization
//    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
//    parameters.add(AttributeWiseRealVectorNormalization.class.getName());
//    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

        // page size
//    parameters.add(OptionHandler.OPTION_PREFIX + OnlineBasicLOF.PAGE_SIZE_P);
//    parameters.add("8000");

        // cache size
//    parameters.add(OptionHandler.OPTION_PREFIX + OnlineBasicLOF.CACHE_SIZE_P);
//    parameters.add("" + 8000 * 10);

        return parameters;
    }
}


