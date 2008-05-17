package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.DiSH;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.preprocessing.DiSHPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.List;

/**
 * Wrapper class for DiSH algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert
 */
public class DiSHWrapper extends NormalizationWrapper {

    /**
     * The epsilon value in each dimension;
     */
    private Double epsilon;

    /**
     * The value of the minpts parameter.
     */
    private int minpts;

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
        // parameter min points
        optionHandler.put(new IntParameter(DiSHPreprocessor.MINPTS_P,
            DiSHPreprocessor.MINPTS_D,
            new GreaterConstraint(0)));

        //parameter epsilon
        DoubleParameter eps = new DoubleParameter(DiSH.EPSILON_P, DiSH.EPSILON_D);
        eps.setOptional(true);
        optionHandler.put(eps);

        //strategy
        // parameter strategy
        StringParameter strat = new StringParameter(DiSHPreprocessor.STRATEGY_P, DiSHPreprocessor.STRATEGY_D);
        strat.setOptional(true);
        optionHandler.put(strat);
    }

    /**
     * @see de.lmu.ifi.dbs.wrapper.KDDTaskWrapper#getKDDTaskParameters()
     */
    public List<String> getKDDTaskParameters() throws UnusedParameterException {
        List<String> parameters = super.getKDDTaskParameters();

        // DiSH algorithm
        parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
        parameters.add(DiSH.class.getName());

        // epsilon
        if (epsilon != null) {
            parameters.add(OptionHandler.OPTION_PREFIX + DiSH.EPSILON_P);
            parameters.add(Double.toString(epsilon));
        }

        // minpts for OPTICS
        parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.MINPTS_P);
        parameters.add(Integer.toString(minpts));

        // minpts for preprocessor
        parameters.add(OptionHandler.OPTION_PREFIX + DiSHPreprocessor.MINPTS_P);
        parameters.add(Integer.toString(minpts));

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

        if (optionHandler.isSet(DiSHPreprocessor.EPSILON_P)) {
            epsilon = (Double) optionHandler.getOptionValue(DiSH.EPSILON_P);
        }

        minpts = (Integer) optionHandler.getOptionValue(DiSHPreprocessor.MINPTS_P);

        if (optionHandler.isSet(DiSHPreprocessor.STRATEGY_P)) {
            strategy = (String) optionHandler.getOptionValue(DiSHPreprocessor.STRATEGY_P);
        }

        return remainingParameters;
    }
}
