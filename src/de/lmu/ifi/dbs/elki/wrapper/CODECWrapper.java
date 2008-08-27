package de.lmu.ifi.dbs.elki.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.CoDeC;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.COPAC;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.DefaultValueGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

/**
 * Wrapper class for the CoDeC algorithm. Performs an attribute wise
 * normalization on the database objects.
 *
 * @author Elke Achtert
 */
public class CODECWrapper extends NormalizationWrapper {

    /**
     * Parameter to specify the maximum radius of the neighborhood to be considered,
     * must be suitable to {@link LocallyWeightedDistanceFunction LocallyWeightedDistanceFunction}.
     * <p>Key: {@code -dbscan.epsilon} </p>
     */
    private final PatternParameter EPSILON_PARAM = new PatternParameter(DBSCAN.EPSILON_ID);

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
     * value of {@link CODECWrapper#MINPTS_PARAM}.
     * <p>Key: {@code -hicopreprocessor.k} </p>
     * <p>Default value: {@link CODECWrapper#MINPTS_PARAM} </p>
     */
    private final IntParameter K_PARAM = new IntParameter(OptionID.KNN_HICO_PREPROCESSOR_K,
        new GreaterConstraint(0), true);

    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        new CODECWrapper().runCLIWrapper(args);
    }

    /**
     * Adds parameters
     * {@link #EPSILON_PARAM}, {@link #MINPTS_PARAM}, and {@link #K_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public CODECWrapper() {
        super();
        // parameter epsilon
        EPSILON_PARAM.setShortDescription("The maximum radius of the neighborhood " +
            "to be considerd, must be suitable to " +
            LocallyWeightedDistanceFunction.class.getName());
        addOption(EPSILON_PARAM);

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
    }

    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // algorithm CoDeC
        Util.addParameter(parameters, OptionID.ALGORITHM, CoDeC.class.getName());

        // clustering algorithm COPAC
        Util.addParameter(parameters, CoDeC.CLUSTERING_ALGORITHM_ID, COPAC.class.getName());

        // partition algorithm
        Util.addParameter(parameters, COPAC.PARTITION_ALGORITHM_ID, DBSCAN.class.getName());

        // epsilon
        Util.addParameter(parameters, OPTICS.EPSILON_ID, getParameterValue(EPSILON_PARAM));

        // minpts
        Util.addParameter(parameters, OPTICS.MINPTS_ID,
            Integer.toString(getParameterValue(MINPTS_PARAM)));

        // distance function
        Util.addParameter(parameters, DBSCAN.DISTANCE_FUNCTION_ID, LocallyWeightedDistanceFunction.class.getName());

        // omit preprocessing
        Util.addFlag(parameters, PreprocessorHandler.OMIT_PREPROCESSING_ID);

        // preprocessor for correlation dimension
        Util.addParameter(parameters, COPAC.PREPROCESSOR_ID, KnnQueryBasedHiCOPreprocessor.class.getName());

        // k
        Util.addParameter(parameters, K_PARAM, Integer.toString(getParameterValue(K_PARAM)));

        return parameters;
    }
}
