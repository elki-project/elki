package de.lmu.ifi.dbs.elki.wrapper;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.ERiC;
import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.COPAC;
import de.lmu.ifi.dbs.elki.distance.distancefunction.ERiCDistanceFunction;
import de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.*;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.DefaultValueGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.List;

/**
 * Wrapper class for hierarchical COPAC algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert
 *         todo parameter
 */
public class ERiCWrapper extends NormalizationWrapper {

    /**
     * Parameter to specify the threshold for minimum number of points in
     * the epsilon-neighborhood of a point,
     * must be an integer greater than 0.
     * <p>Key: {@code -dbscan.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(DBSCAN.MINPTS_ID,
        new GreaterConstraint(0));

    /**
     * Optional parameter to specify the number of nearest neighbors considered in the PCA,
     * must be an integer greater than 0. If this parameter is not set, k ist set to the
     * value of {@link ERiCWrapper#MINPTS_PARAM}.
     * <p>Key: {@code -hicopreprocessor.k} </p>
     * <p>Default value: {@link ERiCWrapper#MINPTS_PARAM} </p>
     */
    private final IntParameter K_PARAM = new IntParameter(OptionID.KNN_HICO_PREPROCESSOR_K,
        new GreaterConstraint(0), true);

    /**
     * The value of the delta parameter.
     */
    private double delta;

    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        ERiCWrapper wrapper = new ERiCWrapper();
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
     * Sets the parameter epsilon, minpts and k in the parameter map additionally to the
     * parameters provided by super-classes.
     */
    public ERiCWrapper() {
        super();

        // parameter minpts
        optionHandler.put(MINPTS_PARAM);

        // parameter k
        K_PARAM.setShortDescription("The number of nearest neighbors considered in the PCA. " +
            "If this parameter is not set, k ist set to the value of " +
            MINPTS_PARAM.getName() + ".");
        optionHandler.put(K_PARAM);

        // global constraint k <-> minpts
        // noinspection unchecked
        GlobalParameterConstraint gpc = new DefaultValueGlobalConstraint(K_PARAM, MINPTS_PARAM);
        optionHandler.setGlobalParameterConstraint(gpc);

        // parameter delta
        DoubleParameter deltaPam = new DoubleParameter(ERiCDistanceFunction.DELTA_P,
            ERiCDistanceFunction.DELTA_D,
            new GreaterConstraint(0));
        deltaPam.setDefaultValue(ERiCDistanceFunction.DEFAULT_DELTA);
        optionHandler.put(deltaPam);
    }

    /**
     * @see de.lmu.ifi.dbs.elki.wrapper.KDDTaskWrapper#getKDDTaskParameters()
     */
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // algorithm ERiC
        Util.addParameter(parameters, OptionID.ALGORITHM, ERiC.class.getName());

        // partition algorithm DBSCAN
        Util.addParameter(parameters, COPAC.PARTITION_ALGORITHM_ID, DBSCAN.class.getName());

        // epsilon
        Util.addParameter(parameters, DBSCAN.EPSILON_ID, "0");

        // minpts
        Util.addParameter(parameters, MINPTS_PARAM, Integer.toString(getParameterValue(MINPTS_PARAM)));

        // distance function
        Util.addParameter(parameters, DBSCAN.DISTANCE_FUNCTION_ID, ERiCDistanceFunction.class.getName());

        // omit preprocessing
        parameters.add(OptionHandler.OPTION_PREFIX + PreprocessorHandler.OMIT_PREPROCESSING_F);

        // preprocessor for correlation dimension
        Util.addParameter(parameters, COPAC.PREPROCESSOR_ID, KnnQueryBasedHiCOPreprocessor.class.getName());

        // k
        Util.addParameter(parameters, K_PARAM, Integer.toString(getParameterValue(K_PARAM)));

        // delta
        parameters.add(OptionHandler.OPTION_PREFIX + ERiCDistanceFunction.DELTA_P);
        parameters.add(Double.toString(delta));

        // tau
        parameters.add(OptionHandler.OPTION_PREFIX + ERiCDistanceFunction.TAU_P);
        parameters.add(Double.toString(delta));

        return parameters;
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // delta
        delta = (Double) optionHandler.getOptionValue(ERiCDistanceFunction.DELTA_P);

        return remainingParameters;
    }
}
