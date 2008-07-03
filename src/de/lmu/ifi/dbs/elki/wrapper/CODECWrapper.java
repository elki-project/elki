package de.lmu.ifi.dbs.elki.wrapper;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.algorithm.CoDeC;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.COPAC;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.*;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.DefaultValueGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.List;

/**
 * Wrapper class for the CoDeC algorithm. Performs an attribute wise
 * normalization on the database objects, partitions the database according to
 * the correlation dimension of its objects, performs the algorithm DBSCAN over
 * the partitions and then determines the correlation dependencies in each
 * cluster of each partition.
 *
 * @author Elke Achtert
 */
public class CODECWrapper extends NormalizationWrapper {

    /**
     * Parameter to specify the maximum radius of the neighborhood to be considered,
     * must be suitable to {@link LocallyWeightedDistanceFunction LocallyWeightedDistanceFunction}.
     * <p>Key: {@code -dbscan.epsilon} </p>
     */
    private final PatternParameter EPSILON_PARAM = new PatternParameter(OptionID.DBSCAN_EPSILON);

    /**
     * Parameter to specify the threshold for minimum number of points in
     * the epsilon-neighborhood of a point,
     * must be an integer greater than 0.
     * <p>Key: {@code -dbscan.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(OptionID.DBSCAN_MINPTS,
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
        CODECWrapper wrapper = new CODECWrapper();
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
     * Sets the parameter epsilon, minpts and k in the parameter map
     * additionally to the parameters provided by super-classes.
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
        // noinspection unchecked
        GlobalParameterConstraint gpc = new DefaultValueGlobalConstraint(K_PARAM, MINPTS_PARAM);
        optionHandler.setGlobalParameterConstraint(gpc);
    }

    /**
     * @see KDDTaskWrapper#getKDDTaskParameters()
     */
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // algorithm CoDeC
        Util.addParameter(parameters, OptionID.ALGORITHM, CoDeC.class.getName());

        // clustering algorithm COPAC
        Util.addParameter(parameters, CoDeC.CODEC_CLUSTERING_ALGORITHM, COPAC.class.getName());

        // partition algorithm
        Util.addParameter(parameters, OptionID.COPAA_PARTITION_ALGORITHM, DBSCAN.class.getName());

        // epsilon
        Util.addParameter(parameters, OptionID.OPTICS_EPSILON, getParameterValue(EPSILON_PARAM));

        // minpts
        Util.addParameter(parameters, OptionID.OPTICS_MINPTS,
            Integer.toString(getParameterValue(MINPTS_PARAM)));

        // distance function
        Util.addParameter(parameters, OptionID.ALGORITHM_DISTANCEFUNCTION, LocallyWeightedDistanceFunction.class.getName());

        // omit preprocessing
        parameters.add(OptionHandler.OPTION_PREFIX + PreprocessorHandler.OMIT_PREPROCESSING_F);

        // preprocessor for correlation dimension
        Util.addParameter(parameters, OptionID.COPAA_PREPROCESSOR, KnnQueryBasedHiCOPreprocessor.class.getName());

        // k
        Util.addParameter(parameters, K_PARAM, Integer.toString(getParameterValue(K_PARAM)));

        return parameters;
    }
}
