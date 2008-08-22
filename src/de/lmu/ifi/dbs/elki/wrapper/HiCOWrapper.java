package de.lmu.ifi.dbs.elki.wrapper;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PCABasedCorrelationDistanceFunction;
import de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.DefaultValueGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.varianceanalysis.PercentageEigenPairFilter;

import java.util.List;

/**
 * Wrapper class for HiCO algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert
 *         todo parameter
 */
public class HiCOWrapper extends NormalizationWrapper {

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
        OptionID.KNN_HICO_PREPROCESSOR_K,
        new GreaterConstraint(0),
        true);

    /**
     * The alpha parameter.
     */
    private DoubleParameter ALPHA_PARAM = new DoubleParameter(OptionID.EIGENPAIR_FILTER_ALPHA,
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
        HiCOWrapper wrapper = new HiCOWrapper();
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
     * Adds parameters
     * {@link #MINPTS_PARAM}, {@link #K_PARAM}, {@link #DELTA_PARAM}, and {@link #ALPHA_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public HiCOWrapper() {
        super();

        // parameter minpts
        optionHandler.put(MINPTS_PARAM);

        // parameter k
        K_PARAM.setShortDescription("The number of nearest neighbors considered in the PCA. " +
            "If this parameter is not set, k ist set to the value of " +
            MINPTS_PARAM.getName());
        optionHandler.put(K_PARAM);

        // global constraint k <-> minpts
        // noinspection unchecked
        GlobalParameterConstraint gpc = new DefaultValueGlobalConstraint(K_PARAM, MINPTS_PARAM);
        optionHandler.setGlobalParameterConstraint(gpc);

        // parameter delta
        optionHandler.put(DELTA_PARAM);

        // parameter alpha
        optionHandler.put(ALPHA_PARAM);
    }

    @Override
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // OPTICS algorithm
        Util.addParameter(parameters, OptionID.ALGORITHM, OPTICS.class.getName());

        // distance function
        Util.addParameter(parameters, OPTICS.DISTANCE_FUNCTION_ID, PCABasedCorrelationDistanceFunction.class.getName());

        // omit flag
        Util.addFlag(parameters, PreprocessorHandler.OMIT_PREPROCESSING_ID);

        // epsilon
        Util.addParameter(parameters, OPTICS.EPSILON_ID, PCABasedCorrelationDistanceFunction.INFINITY_PATTERN);

        // minpts
        Util.addParameter(parameters, MINPTS_PARAM, Integer.toString(getParameterValue(MINPTS_PARAM)));

        // preprocessor
        Util.addParameter(parameters, PreprocessorHandler.PREPROCESSOR_ID, KnnQueryBasedHiCOPreprocessor.class.getName());

        // k for preprocessor
        Util.addParameter(parameters, K_PARAM, Integer.toString(getParameterValue(K_PARAM)));

        // alpha
        parameters.add(OptionHandler.OPTION_PREFIX + ALPHA_PARAM.getName());
        parameters.add(Double.toString(getParameterValue(ALPHA_PARAM)));

        // delta
        parameters.add(OptionHandler.OPTION_PREFIX + DELTA_PARAM.getName());
        parameters.add(Double.toString(getParameterValue(DELTA_PARAM)));

        return parameters;
    }
}
