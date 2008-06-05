package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.clustering.DBSCAN;
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
 */
public class SUBCLUWrapper extends NormalizationWrapper {

    /**
     * The value of the epsilon parameter.
     */
    private String epsilon;

    /**
     * The value of the minpts parameter.
     */
    private int minpts;

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
        optionHandler.put(SUBCLU.EPSILON_PARAM);

        // parameter min points
        optionHandler.put(new IntParameter(DBSCAN.MINPTS_P, DBSCAN.MINPTS_D, new GreaterConstraint(0)));
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
        parameters.add(OptionHandler.OPTION_PREFIX + DBSCAN.MINPTS_P);
        parameters.add(Integer.toString(minpts));

        return parameters;
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // epsilon, minpts
        epsilon = getParameterValue(SUBCLU.EPSILON_PARAM);
        minpts = (Integer) optionHandler.getOptionValue(DBSCAN.MINPTS_P);

        return remainingParameters;
    }
}
