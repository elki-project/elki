package de.lmu.ifi.dbs.elki.wrapper;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.FourC;
import de.lmu.ifi.dbs.elki.preprocessing.FourCPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;
import de.lmu.ifi.dbs.elki.varianceanalysis.LimitEigenPairFilter;

import java.util.List;
import java.util.Vector;

/**
 * A wrapper for the 4C algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Arthur Zimek
 *         todo parameter
 */
public class FourCWrapper extends NormalizationWrapper {

    /**
     * Parameter to specify the threshold for minimum number of points in
     * the epsilon-neighborhood of a point,
     * must be an integer greater than 0.
     * <p>Key: {@code -projdbscan.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(OptionID.PROJECTED_DBSCAN_MINPTS,
        new GreaterConstraint(0));

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
        addOption(FourC.EPSILON_PARAM);

        // minpts
        addOption(MINPTS_PARAM);

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
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // 4C algorithm
        Util.addParameter(parameters, OptionID.ALGORITHM, FourC.class.getName());

        // epsilon
        Util.addParameter(parameters, FourC.EPSILON_PARAM, getParameterValue(FourC.EPSILON_PARAM));

        // minpts
        Util.addParameter(parameters, MINPTS_PARAM, Integer.toString(getParameterValue(MINPTS_PARAM)));

        // lambda
        Util.addParameter(parameters, lambda, getParameterValue(lambda).toString());

        // delta
        Util.addParameter(parameters, delta, getParameterValue(delta).toString());

        // absolute flag
        if (optionHandler.isSet(absolute))
            Util.addFlag(parameters, absolute);

        return parameters;
    }
}
