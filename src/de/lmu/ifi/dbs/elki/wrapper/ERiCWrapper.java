package de.lmu.ifi.dbs.elki.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.COPAC;
import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.ERiC;
import de.lmu.ifi.dbs.elki.distance.distancefunction.ERiCDistanceFunction;
import de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.DefaultValueGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;

/**
 * Wrapper class for hierarchical COPAC algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert
 */
public class ERiCWrapper extends NormalizationWrapper {

    /**
     * Parameter to specify the threshold for minimum number of points in
     * the epsilon-neighborhood of a point,
     * must be an integer greater than 0.
     * <p>Key: {@code -dbscan.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(
        DBSCAN.MINPTS_ID,
        new GreaterConstraint(0));

    /**
     * Optional parameter to specify the number of nearest neighbors considered in the PCA,
     * must be an integer greater than 0. If this parameter is not set, k ist set to the
     * value of {@link ERiCWrapper#MINPTS_PARAM}.
     * <p>Key: {@code -hicopreprocessor.k} </p>
     * <p>Default value: {@link ERiCWrapper#MINPTS_PARAM} </p>
     */
    private final IntParameter K_PARAM = new IntParameter(
        OptionID.KNN_HICO_PREPROCESSOR_K,
        new GreaterConstraint(0), true);

    /**
     * Parameter to specify the threshold for approximate linear dependency:
     * the strong eigenvectors of q are approximately linear dependent
     * from the strong eigenvectors p if the following condition
     * holds for all stroneg eigenvectors q_i of q (lambda_q < lambda_p):
     * q_i' * M^check_p * q_i <= delta^2,
     * must be a double equal to or greater than 0.
     * <p>Default value: {@code 0.1} </p>
     * <p>Key: {@code -ericdf.delta} </p>
     */
    private final DoubleParameter DELTA_PARAM = new DoubleParameter(
        ERiCDistanceFunction.DELTA_ID,
        new GreaterEqualConstraint(0),
        0.1
    );

    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        new ERiCWrapper().runCLIWrapper(args);
    }

    /**
     * Adds parameters
     * {@link #MINPTS_PARAM}, {@link #K_PARAM}, and {@link #DELTA_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public ERiCWrapper() {
        super();

        // parameter minpts
        addOption(MINPTS_PARAM);

        // parameter k
        K_PARAM.setShortDescription("The number of nearest neighbors considered in the PCA. " +
            "If this parameter is not set, k ist set to the value of " +
            MINPTS_PARAM.getName() + ".");
        addOption(K_PARAM);

        // global constraint k <-> minpts
        GlobalParameterConstraint gpc = new DefaultValueGlobalConstraint<Integer>(K_PARAM, MINPTS_PARAM);
        optionHandler.setGlobalParameterConstraint(gpc);

        // parameter delta
        addOption(DELTA_PARAM);
    }

    @Override
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
        Util.addFlag(parameters, PreprocessorHandler.OMIT_PREPROCESSING_ID);

        // preprocessor for correlation dimension
        Util.addParameter(parameters, COPAC.PREPROCESSOR_ID, KnnQueryBasedHiCOPreprocessor.class.getName());

        // k
        Util.addParameter(parameters, K_PARAM, Integer.toString(getParameterValue(K_PARAM)));

        // delta
        Util.addParameter(parameters, ERiCDistanceFunction.DELTA_ID, Double.toString(getParameterValue(DELTA_PARAM)));

        // tau
        Util.addParameter(parameters, ERiCDistanceFunction.TAU_ID, Double.toString(getParameterValue(DELTA_PARAM)));

        return parameters;
    }
}
