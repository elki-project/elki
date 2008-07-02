package de.lmu.ifi.dbs.elki.wrapper;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PCABasedCorrelationDistanceFunction;
import de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.*;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.*;
import de.lmu.ifi.dbs.elki.varianceanalysis.PercentageEigenPairFilter;

import java.util.ArrayList;
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
    private final IntParameter MINPTS_PARAM = new IntParameter(OptionID.OPTICS_MINPTS,
        new GreaterConstraint(0));

    /**
     * Optional parameter to specify the number of nearest neighbors considered in the PCA,
     * must be an integer greater than 0. If this parameter is not set, k ist set to the
     * value of {@link HiCOWrapper#MINPTS_PARAM}.
     * <p>Key: {@code -hicopreprocessor.k} </p>
     * <p>Default value: {@link HiCOWrapper#MINPTS_PARAM} </p>
     */
    private final IntParameter K_PARAM = new IntParameter(OptionID.KNN_HICO_PREPROCESSOR_K,
        new GreaterConstraint(0), true);

    /**
     * The alpha parameter.
     */
    private DoubleParameter alpha;

    /**
     * The delta parameter.
     */
    private DoubleParameter delta;

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
     * Sets the parameter minpts and k in the parameter map additionally to the
     * parameters provided by super-classes.
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
        delta = new DoubleParameter(PCABasedCorrelationDistanceFunction.DELTA_P,
            PCABasedCorrelationDistanceFunction.DELTA_D,
            new GreaterEqualConstraint(0));
        delta.setDefaultValue(PCABasedCorrelationDistanceFunction.DEFAULT_DELTA);
        optionHandler.put(delta);

        // parameter alpha
        ArrayList<ParameterConstraint<Number>> alphaConstraints = new ArrayList<ParameterConstraint<Number>>();
        alphaConstraints.add(new GreaterConstraint(0));
        alphaConstraints.add(new LessConstraint(1));
        alpha = new DoubleParameter(PercentageEigenPairFilter.ALPHA_P,
            PercentageEigenPairFilter.ALPHA_D,
            alphaConstraints);
        alpha.setDefaultValue(PercentageEigenPairFilter.DEFAULT_ALPHA);
        optionHandler.put(alpha);
    }

    /**
     * @see KDDTaskWrapper#getKDDTaskParameters()
     */
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // OPTICS algorithm
        Util.addParameter(parameters, OptionID.ALGORITHM, OPTICS.class.getName());

        // distance function
        Util.addParameter(parameters, OptionID.ALGORITHM_DISTANCEFUNCTION, PCABasedCorrelationDistanceFunction.class.getName());

        // omit flag
        parameters.add(OptionHandler.OPTION_PREFIX + PreprocessorHandler.OMIT_PREPROCESSING_F);

        // epsilon
        Util.addParameter(parameters, OptionID.OPTICS_EPSILON, PCABasedCorrelationDistanceFunction.INFINITY_PATTERN);

        // minpts
        Util.addParameter(parameters, MINPTS_PARAM, Integer.toString(getParameterValue(MINPTS_PARAM)));

        // preprocessor
        parameters.add(OptionHandler.OPTION_PREFIX + PreprocessorHandler.PREPROCESSOR_CLASS_P);
        parameters.add(KnnQueryBasedHiCOPreprocessor.class.getName());

        // k for preprocessor
        Util.addParameter(parameters, K_PARAM, Integer.toString(getParameterValue(K_PARAM)));

        // alpha
        parameters.add(OptionHandler.OPTION_PREFIX + alpha.getName());
        parameters.add(Double.toString(getParameterValue(alpha)));

        // delta
        parameters.add(OptionHandler.OPTION_PREFIX + delta.getName());
        parameters.add(Double.toString(getParameterValue(delta)));

        return parameters;
    }
}
