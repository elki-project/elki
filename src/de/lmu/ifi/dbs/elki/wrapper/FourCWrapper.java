package de.lmu.ifi.dbs.elki.wrapper;

import java.util.List;
import java.util.Vector;

import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.FourC;
import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.PreDeCon;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.LimitEigenPairFilter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * A wrapper for the 4C algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Arthur Zimek
 * @param <O> object type
 */
public class FourCWrapper<O extends DatabaseObject> extends NormalizationWrapper<O> {

    /**
     * Parameter to specify the maximum radius of the neighborhood to be considered,
     * must be suitable to {@link de.lmu.ifi.dbs.elki.distance.distancefunction.LocallyWeightedDistanceFunction}.
     * <p>Key: {@code -projdbscan.epsilon} </p>
     */
    private final PatternParameter EPSILON_PARAM = new PatternParameter(FourC.EPSILON_ID);


    /**
     * Parameter to specify the threshold for minimum number of points in
     * the epsilon-neighborhood of a point,
     * must be an integer greater than 0.
     * <p>Key: {@code -projdbscan.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(
        FourC.MINPTS_ID,
        new GreaterConstraint(0));

    /**
     * Parameter to specify the intrinsic dimensionality of the clusters to find,
     * must be an integer greater than 0.
     * <p>Key: {@code -projdbscan.lambda} </p>
     */
    private final IntParameter LAMBDA_PARAM = new IntParameter(
        PreDeCon.LAMBDA_ID,
        new GreaterConstraint(0));

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
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        new FourCWrapper().runCLIWrapper(args);
    }

    /**
     * Adds parameters
     * {@link #EPSILON_PARAM}, {@link #MINPTS_PARAM}, {@link #LAMBDA_PARAM}, {@link #LAMBDA_PARAM} and flag {@link LimitEigenPairFilter#EIGENPAIR_FILTER_ABSOLUTE} todo
     * to the option handler additionally to parameters of super class.
     */
    public FourCWrapper() {
        super();
        // epsilon
        addOption(EPSILON_PARAM);

        // minpts
        addOption(MINPTS_PARAM);

        // lambda
        addOption(LAMBDA_PARAM);

        // delta
        List<ParameterConstraint<Number>> cons = new Vector<ParameterConstraint<Number>>();
        cons.add(new GreaterEqualConstraint(0));
        cons.add(new LessEqualConstraint(1));
        delta = new DoubleParameter(LimitEigenPairFilter.EIGENPAIR_FILTER_DELTA, cons);
        delta.setDefaultValue(LimitEigenPairFilter.DEFAULT_DELTA);
        addOption(delta);

        // absolute flag
        absolute = new Flag(LimitEigenPairFilter.EIGENPAIR_FILTER_ABSOLUTE);
        addOption(absolute);
    }

    @Override
    public List<String> getKDDTaskParameters() throws UnusedParameterException {
        List<String> parameters = super.getKDDTaskParameters();

        // 4C algorithm
        OptionUtil.addParameter(parameters, OptionID.ALGORITHM, FourC.class.getName());

        // epsilon
        OptionUtil.addParameter(parameters, EPSILON_PARAM, EPSILON_PARAM.getValue());

        // minpts
        OptionUtil.addParameter(parameters, MINPTS_PARAM, Integer.toString(MINPTS_PARAM.getValue()));

        // lambda
        OptionUtil.addParameter(parameters, LAMBDA_PARAM, Integer.toString(LAMBDA_PARAM.getValue()));

        // delta
        OptionUtil.addParameter(parameters, delta, delta.getValue().toString());

        // absolute flag
        if (absolute.isSet())
            OptionUtil.addFlag(parameters, absolute);

        return parameters;
    }
}
