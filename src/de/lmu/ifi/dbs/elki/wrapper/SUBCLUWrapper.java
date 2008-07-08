package de.lmu.ifi.dbs.elki.wrapper;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.SUBCLU;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.List;

/**
 * Wrapper class for SUBCLU algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert
 */
public class SUBCLUWrapper extends NormalizationWrapper {

    /**
     * Parameter to specify the maximum radius of the neighborhood to be considered,
     * must be suitable to {@link de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDimensionsSelectingDoubleDistanceFunction}.
     * <p>Key: {@code -subclu.epsilon} </p>
     */
    private final PatternParameter EPSILON_PARAM = new PatternParameter(SUBCLU.EPSILON_ID);

    /**
     * Parameter to specify the threshold for minimum number of points in
     * the epsilon-neighborhood of a point,
     * must be an integer greater than 0.
     * <p>Key: {@code -subclu.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(
        SUBCLU.MINPTS_ID,
        new GreaterConstraint(0));

    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        SUBCLUWrapper wrapper = new SUBCLUWrapper();
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
    public SUBCLUWrapper() {
        super();
        //  parameter epsilon
        addOption(EPSILON_PARAM);

        // parameter min points
        addOption(MINPTS_PARAM);
    }

    /**
     * @see KDDTaskWrapper#getKDDTaskParameters()
     */
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // algorithm SUBCLU
        Util.addParameter(parameters, OptionID.ALGORITHM, SUBCLU.class.getName());

        // epsilon
        Util.addParameter(parameters, EPSILON_PARAM, getParameterValue(EPSILON_PARAM));

        // minpts
        Util.addParameter(parameters, MINPTS_PARAM, Integer.toString(getParameterValue(MINPTS_PARAM)));

        return parameters;
    }
}
