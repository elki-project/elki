package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.FourC;
import de.lmu.ifi.dbs.preprocessing.FourCPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.LessEqualConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint;
import de.lmu.ifi.dbs.varianceanalysis.LimitEigenPairFilter;

import java.util.List;
import java.util.Vector;

/**
 * A wrapper for the 4C algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Arthur Zimek
 */
public class FourCWrapper extends NormalizationWrapper {

    /**
     * Parameter minpts.
     */
    private IntParameter minpts;

    /**
     * Parameter lambda.
     */
    private IntParameter lambda;

    /**
     * Parameter delta.
     */
    private DoubleParameter delta;

    /**
     * Absolute flag.
     */
    private Flag absolute;

    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        FourCWrapper wrapper = new FourCWrapper();
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
    public FourCWrapper() {
        super();
        // epsilon
        optionHandler.put(FourC.EPSILON_PARAM);

        // minpts
        minpts = new IntParameter(FourC.MINPTS_P, FourC.MINPTS_D, new GreaterConstraint(0));
        optionHandler.put(minpts);

        // lambda
        lambda = new IntParameter(FourC.LAMBDA_P, FourC.LAMBDA_D, new GreaterConstraint(0));
        optionHandler.put(lambda);

        // absolute flag
        absolute = new Flag(FourCPreprocessor.ABSOLUTE_F, FourCPreprocessor.ABSOLUTE_D);
        optionHandler.put(absolute);

        // delta
        List<ParameterConstraint<Number>> cons = new Vector<ParameterConstraint<Number>>();
        cons.add(new GreaterEqualConstraint(0));
        cons.add(new LessEqualConstraint(1));
        delta = new DoubleParameter(FourCPreprocessor.DELTA_P, FourCPreprocessor.DELTA_D, cons);
        delta.setDefaultValue(LimitEigenPairFilter.DEFAULT_DELTA);
        optionHandler.put(delta);
    }

    /**
     * @see KDDTaskWrapper#getKDDTaskParameters()
     */
    public List<String> getKDDTaskParameters() throws UnusedParameterException {
        List<String> parameters = super.getKDDTaskParameters();

        // 4C algorithm
        parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
        parameters.add(FourC.class.getName());

        put(parameters, FourC.EPSILON_PARAM);
        put(parameters, minpts);
        put(parameters, lambda);
        put(parameters, delta);
        if (optionHandler.isSet(absolute))
            put(parameters, absolute);

        return parameters;
    }
}
