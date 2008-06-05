package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.clustering.SUBCLU;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.List;

/**
 * Wrapper class for SUBCLU algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert
 *         todo parameter
 */
public class SUBCLUWrapper extends NormalizationWrapper {

    /**
     * The value of the epsilon parameter.
     */
    private String epsilon;

    /**
     * Parameter to specify the threshold for minimum number of points in
     * the epsilon-neighborhood of a point,
     * must be an integer greater than 0.
     * <p>Key: {@code -subclu.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(OptionID.SUBCLU_MINPTS,
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
        addOption(SUBCLU.EPSILON_PARAM);

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
        parameters.add(OptionHandler.OPTION_PREFIX + SUBCLU.EPSILON_PARAM.getName());
        parameters.add(epsilon);

        // minpts
        Util.addParameter(parameters, MINPTS_PARAM, Integer.toString(getParameterValue(MINPTS_PARAM)));

        return parameters;
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // epsilon, minpts
        epsilon = getParameterValue(SUBCLU.EPSILON_PARAM);

        return remainingParameters;
    }
}
