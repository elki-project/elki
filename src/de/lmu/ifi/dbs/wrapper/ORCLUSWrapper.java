package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.ORCLUS;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.List;

/**
 * Wrapper class for COPAC algorithm. Performs an attribute wise normalization
 * on the database objects.
 *
 * @author Elke Achtert
 */
public class ORCLUSWrapper extends FileBasedDatabaseConnectionWrapper {

    /**
     * The value of the k parameter.
     */
    private int k;

    /**
     * The value of the k_i parameter.
     */
    private int k_i;

    /**
     * The value of the dim parameter.
     */
    private int dim;

    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        ORCLUSWrapper wrapper = new ORCLUSWrapper();
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
     * Sets the parameters k, k_i and dim in the parameter map additionally to
     * the parameters provided by super-classes.
     */
    public ORCLUSWrapper() {
        super();
        optionHandler.put(new IntParameter(ORCLUS.K_P, ORCLUS.K_D, new GreaterConstraint(0)));

        IntParameter ki = new IntParameter(ORCLUS.K_I_P, ORCLUS.K_I_D, new GreaterConstraint(0));
        ki.setDefaultValue(ORCLUS.K_I_DEFAULT);
        optionHandler.put(ki);

        optionHandler.put(new IntParameter(ORCLUS.L_P, ORCLUS.L_D, new GreaterConstraint(0)));
    }

    /**
     * @see KDDTaskWrapper#getKDDTaskParameters()
     */
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // ORCLUS algorithm
        parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
        parameters.add(ORCLUS.class.getName());

        // dim
        parameters.add(OptionHandler.OPTION_PREFIX + ORCLUS.L_P);
        parameters.add(Integer.toString(dim));

        // k
        parameters.add(OptionHandler.OPTION_PREFIX + ORCLUS.K_P);
        parameters.add(Integer.toString(k));

        // k_i
        parameters.add(OptionHandler.OPTION_PREFIX + ORCLUS.K_I_P);
        parameters.add(Integer.toString(k_i));

        return parameters;
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        k = (Integer) optionHandler.getOptionValue(ORCLUS.K_P);
        dim = (Integer) optionHandler.getOptionValue(ORCLUS.L_P);
        k_i = (Integer) optionHandler.getOptionValue(ORCLUS.K_I_P);

        return remainingParameters;
	}
}
