package de.lmu.ifi.dbs.elki.wrapper;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.PreDeCon;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.List;

/**
 * A wrapper for the PreDeCon algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert
 *         todo parameter
 */
public class PreDeConWrapper extends NormalizationWrapper {

    /**
     * The value of the epsilon parameter.
     */
    private String epsilon;

    /**
     * Parameter to specify the threshold for minimum number of points in
     * the epsilon-neighborhood of a point,
     * must be an integer greater than 0.
     * <p>Key: {@code -projdbscan.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(OptionID.PROJECTED_DBSCAN_MINPTS,
        new GreaterConstraint(0));

    /**
     * The value of the lambda parameter.
     */
    private int lambda;

    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        PreDeConWrapper wrapper = new PreDeConWrapper();
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
     * Provides a wrapper for the 4C algorithm.
     */
    public PreDeConWrapper() {
        super();
        // parameter epsilon
        addOption(PreDeCon.EPSILON_PARAM);

        // parameter min points
        addOption(MINPTS_PARAM);

        // parameter lambda
        addOption(new IntParameter(PreDeCon.LAMBDA_P, PreDeCon.LAMBDA_D, new GreaterConstraint(0)));
    }

    /**
     * @see de.lmu.ifi.dbs.elki.wrapper.KDDTaskWrapper#getKDDTaskParameters()
     */
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // PreDeCon algorithm
        Util.addParameter(parameters, OptionID.ALGORITHM, PreDeCon.class.getName());

        // epsilon for PreDeCon
        parameters.add(OptionHandler.OPTION_PREFIX + PreDeCon.EPSILON_PARAM.getName());
        parameters.add(epsilon);

        // minpts for PreDeCon
        Util.addParameter(parameters, MINPTS_PARAM, Integer.toString(getParameterValue(MINPTS_PARAM)));

        // lambda for PreDeCon
        parameters.add(OptionHandler.OPTION_PREFIX + PreDeCon.LAMBDA_P);
        parameters.add(Integer.toString(lambda));

        return parameters;
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // epsilon, minpts, lambda
        epsilon = getParameterValue(PreDeCon.EPSILON_PARAM);
        lambda = (Integer) optionHandler.getOptionValue(PreDeCon.LAMBDA_P);

        return remainingParameters;
    }
}
