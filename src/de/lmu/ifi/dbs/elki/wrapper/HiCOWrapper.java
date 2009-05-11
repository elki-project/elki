package de.lmu.ifi.dbs.elki.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PCABasedCorrelationDistanceFunction;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PercentageEigenPairFilter;
import de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.DefaultValueGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;

/**
 * Wrapper class for HiCO algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert
 * @param <O> object type
 */
public class HiCOWrapper<O extends DatabaseObject> extends NormalizationWrapper<O> {

    /**
     * Parameter to specify the threshold for minimum number of points in
     * the epsilon-neighborhood of a point,
     * must be an integer greater than 0.
     * <p>Key: {@code -optics.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(
        OPTICS.MINPTS_ID,
        new GreaterConstraint(0));

    /**
     * Optional parameter to specify the number of nearest neighbors considered in the PCA,
     * must be an integer greater than 0. If this parameter is not set, k ist set to the
     * value of {@link HiCOWrapper#MINPTS_PARAM}.
     * <p>Key: {@code -hicopreprocessor.k} </p>
     * <p>Default value: {@link HiCOWrapper#MINPTS_PARAM} </p>
     */
    private final IntParameter K_PARAM = new IntParameter(
        KnnQueryBasedHiCOPreprocessor.KNN_HICO_PREPROCESSOR_K,
        new GreaterConstraint(0),
        true);

    /**
     * The alpha parameter.
     */
    private DoubleParameter ALPHA_PARAM = new DoubleParameter(PercentageEigenPairFilter.EIGENPAIR_FILTER_ALPHA,
        new IntervalConstraint(0.0,IntervalConstraint.IntervalBoundary.OPEN,1.0,
            IntervalConstraint.IntervalBoundary.OPEN), PercentageEigenPairFilter.DEFAULT_ALPHA);

    /**
     * Parameter to specify the threshold of a distance between a vector q and a given space
     * that indicates that q adds a new dimension to the space,
     * must be a double equal to or greater than 0.
     * <p>Default value: {@code 0.25} </p>
     * <p>Key: {@code -pcabasedcorrelationdf.delta} </p>
     */
    private final DoubleParameter DELTA_PARAM = new DoubleParameter(
        PCABasedCorrelationDistanceFunction.DELTA_ID,
        new GreaterEqualConstraint(0),
        0.25
    );

    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        new HiCOWrapper<DatabaseObject>().runCLIWrapper(args);
    }

    /**
     * Adds parameters
     * {@link #MINPTS_PARAM}, {@link #K_PARAM}, {@link #DELTA_PARAM}, and {@link #ALPHA_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public HiCOWrapper() {
        super();

        // parameter minpts
        addOption(MINPTS_PARAM);

        // parameter k
        K_PARAM.setShortDescription("The number of nearest neighbors considered in the PCA. " +
            "If this parameter is not set, k ist set to the value of " +
            MINPTS_PARAM.getName());
        addOption(K_PARAM);

        // global constraint k <-> minpts
        GlobalParameterConstraint gpc = new DefaultValueGlobalConstraint<Integer>(K_PARAM, MINPTS_PARAM);
        optionHandler.setGlobalParameterConstraint(gpc);

        // parameter delta
        addOption(DELTA_PARAM);

        // parameter alpha
        addOption(ALPHA_PARAM);
    }

    @Override
    public List<String> getKDDTaskParameters() throws UnusedParameterException {
        List<String> parameters = super.getKDDTaskParameters();

        // OPTICS algorithm
        OptionUtil.addParameter(parameters, OptionID.ALGORITHM, OPTICS.class.getName());

        // distance function
        OptionUtil.addParameter(parameters, OPTICS.DISTANCE_FUNCTION_ID, PCABasedCorrelationDistanceFunction.class.getName());

        // omit flag
        OptionUtil.addFlag(parameters, PreprocessorHandler.OMIT_PREPROCESSING_ID);

        // epsilon
        OptionUtil.addParameter(parameters, OPTICS.EPSILON_ID, PCABasedCorrelationDistanceFunction.INFINITY_PATTERN);

        // minpts
        OptionUtil.addParameter(parameters, MINPTS_PARAM, Integer.toString(MINPTS_PARAM.getValue()));

        // preprocessor
        OptionUtil.addParameter(parameters, PreprocessorHandler.PREPROCESSOR_ID, KnnQueryBasedHiCOPreprocessor.class.getName());

        // k for preprocessor
        OptionUtil.addParameter(parameters, K_PARAM, Integer.toString(K_PARAM.getValue()));

        // alpha
        parameters.add(OptionHandler.OPTION_PREFIX + ALPHA_PARAM.getName());
        parameters.add(Double.toString(ALPHA_PARAM.getValue()));

        // delta
        parameters.add(OptionHandler.OPTION_PREFIX + DELTA_PARAM.getName());
        parameters.add(Double.toString(DELTA_PARAM.getValue()));

        return parameters;
    }
}
