package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.clustering.DiSH;
import de.lmu.ifi.dbs.preprocessing.DiSHPreprocessor;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterEqualConstraint;

import java.util.List;
import java.util.Arrays;

/**
 * todo parameter
 * Wrapper class for DiSH algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert
 */
public class DiSHWrapper extends NormalizationWrapper {

    /**
     * Parameter that specifies the maximum radius of the neighborhood to be
     * considered in each dimension for determination of the preference vector,
     * must be a double equal to or greater than 0.
     * <p>Default value: {@code 0.001} </p>
     * <p>Key: {@code -dish.epsilon} </p>
     */
    private final DoubleParameter EPSILON_PARAM =
        new DoubleParameter(OptionID.DISH_EPSILON, new GreaterEqualConstraint(0), 0.001);

    /**
     * Parameter that specifies the a minimum number of points as a smoothing
     * factor to avoid the single-link-effect,
     * must be an integer greater than 0.
     * <p>Default value: {@code 1} </p>
     * <p>Key: {@code -dish.mu} </p>
     */
    private final IntParameter MU_PARAM = new IntParameter(OptionID.DISH_MU,
        new GreaterConstraint(0), 1);

    /**
     * The strategy for determination of the preference vector.
     */
    private String strategy;


    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        DiSHWrapper wrapper = new DiSHWrapper();
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
     * Sets the parameter minpts and k in the parameter map additionally to the
     * parameters provided by super-classes.
     */
    public DiSHWrapper() {
        super();

        // parameter mu
        addOption(MU_PARAM);

        //parameter epsilon
        addOption(EPSILON_PARAM);

        // parameter strategy
        StringParameter strat = new StringParameter(DiSHPreprocessor.STRATEGY_P, DiSHPreprocessor.STRATEGY_D);
        strat.setOptional(true);
        optionHandler.put(strat);
    }

    /**
     * @see de.lmu.ifi.dbs.wrapper.KDDTaskWrapper#getKDDTaskParameters()
     */
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // DiSH algorithm
        Util.addParameter(parameters, OptionID.ALGORITHM, DiSH.class.getName());

        // epsilon
        Util.addParameter(parameters, OptionID.DISH_EPSILON, Double.toString(getParameterValue(EPSILON_PARAM)));

        // minpts
        Util.addParameter(parameters, OptionID.DISH_MU, Integer.toString(getParameterValue(MU_PARAM)));

        // strategy for preprocessor
        if (strategy != null) {
            parameters.add(OptionHandler.OPTION_PREFIX + DiSHPreprocessor.STRATEGY_P);
            parameters.add(strategy);
        }

        return parameters;
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        if (optionHandler.isSet(DiSHPreprocessor.STRATEGY_P)) {
            strategy = (String) optionHandler.getOptionValue(DiSHPreprocessor.STRATEGY_P);
        }

        return remainingParameters;
    }
}
