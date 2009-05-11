package de.lmu.ifi.dbs.elki.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.DiSH;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.preprocessing.DiSHPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.DiSHPreprocessor.Strategy;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.EqualStringConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;

/**
 * Wrapper class for DiSH algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert
 * @param <O> object type
 */
public class DiSHWrapper<O extends DatabaseObject> extends NormalizationWrapper<O> {

    /**
     * Parameter that specifies the maximum radius of the neighborhood to be
     * considered in each dimension for determination of the preference vector,
     * must be a double equal to or greater than 0.
     * <p>Default value: {@code 0.001} </p>
     * <p>Key: {@code -dish.epsilon} </p>
     */
    private final DoubleParameter EPSILON_PARAM = new DoubleParameter(
        DiSH.EPSILON_ID,
        new GreaterEqualConstraint(0),
        0.001);

    /**
     * Parameter that specifies the a minimum number of points as a smoothing
     * factor to avoid the single-link-effect,
     * must be an integer greater than 0.
     * <p>Default value: {@code 10} </p>
     * <p>Key: {@code -dish.mu} </p>
     */
    private final IntParameter MU_PARAM = new IntParameter(
        DiSH.MU_ID,
        new GreaterConstraint(0),
        10);

    /**
     * DiSH strategy parameter
     */
    private final PatternParameter STRATEGY_PARAM = new PatternParameter(DiSHPreprocessor.STRATEGY_ID,
        new EqualStringConstraint(new String[]{
            Strategy.APRIORI.toString(),
            Strategy.MAX_INTERSECTION.toString()}),
            DiSHPreprocessor.DEFAULT_STRATEGY.toString()
        );

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
        new DiSHWrapper<DatabaseObject>().runCLIWrapper(args);
    }

     /**
     * Adds parameters
     * {@link #MU_PARAM}, {@link #EPSILON_PARAM}, and {@link #STRATEGY_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public DiSHWrapper() {
        super();

        // parameter mu
        addOption(MU_PARAM);

        //parameter epsilon
        addOption(EPSILON_PARAM);

        // parameter strategy
        addOption(STRATEGY_PARAM);
    }

    @Override
    public List<String> getKDDTaskParameters() throws UnusedParameterException {
        List<String> parameters = super.getKDDTaskParameters();

        // DiSH algorithm
        OptionUtil.addParameter(parameters, OptionID.ALGORITHM, DiSH.class.getName());

        // epsilon
        OptionUtil.addParameter(parameters, DiSH.EPSILON_ID, Double.toString(EPSILON_PARAM.getValue()));

        // minpts
        OptionUtil.addParameter(parameters, DiSH.MU_ID, Integer.toString(MU_PARAM.getValue()));

        // strategy for preprocessor
        if (strategy != null) {
            OptionUtil.addParameter(parameters, DiSHPreprocessor.STRATEGY_ID, strategy);
        }

        return parameters;
    }

    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        if (STRATEGY_PARAM.isSet()) {
            strategy = STRATEGY_PARAM.getValue();
        }

        return remainingParameters;
    }
}
