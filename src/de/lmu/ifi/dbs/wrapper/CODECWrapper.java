package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.CoDeC;
import de.lmu.ifi.dbs.algorithm.clustering.COPAC;
import de.lmu.ifi.dbs.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.distance.distancefunction.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.DefaultValueGlobalConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

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
     * <p>Key: {@code -epsilon} </p>
     */
    public static final PatternParameter EPSILON_PARAM = new PatternParameter("epsilon",
        "the maximum radius of the neighborhood " +
            "to be considerd, must be suitable to " +
            LocallyWeightedDistanceFunction.class.getName());

    /**
     * Description for parameter k.
     */
    public static final String K_D = "a positive integer specifying the number of " +
        "nearest neighbors considered in the PCA. " +
        "If this value is not defined, k ist set to minpts";

    /**
     * Parameter to specify the threshold for minimum number of points in
     * the epsilon-neighborhood of a point,
     * must be an integer greater than 0.
     * <p>Key: {@code -optics.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(OptionID.OPTICS_MINPTS,
        new GreaterConstraint(0));

    /**
     * The k parameter.
     */
    private IntParameter k;

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
        addOption(EPSILON_PARAM);

        // parameter min points
        addOption(MINPTS_PARAM);

        // paramter k
        k = new IntParameter(KnnQueryBasedHiCOPreprocessor.K_P, K_D, new GreaterConstraint(0));
        k.setOptional(true);
        optionHandler.put(k);

        // global constraint minpts <-> k
        // todo noetig???
        // noinspection unchecked
        GlobalParameterConstraint gpc = new DefaultValueGlobalConstraint(k, MINPTS_PARAM);
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
        parameters.add(OptionHandler.OPTION_PREFIX + CoDeC.CLUSTERING_ALGORITHM_P);
        parameters.add(COPAC.class.getName());

        // partition algorithm
        Util.addParameter(parameters, OptionID.COPAA_PARTITION_ALGORITHM, DBSCAN.class.getName());

        // epsilon
        Util.addParameter(parameters, OptionID.OPTICS_EPSILON, getParameterValue(EPSILON_PARAM));

        // minpts
        Util.addParameter(parameters, OptionID.OPTICS_MINPTS,
            Integer.toString(getParameterValue(MINPTS_PARAM)));

        // distance function
        parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.DISTANCE_FUNCTION_P);
        parameters.add(LocallyWeightedDistanceFunction.class.getName());

        // omit preprocessing
        parameters.add(OptionHandler.OPTION_PREFIX + PreprocessorHandler.OMIT_PREPROCESSING_F);

        // preprocessor for correlation dimension
        Util.addParameter(parameters, OptionID.COPAA_PREPROCESSOR, KnnQueryBasedHiCOPreprocessor.class.getName());

        // k
        parameters.add(OptionHandler.OPTION_PREFIX + KnnQueryBasedHiCOPreprocessor.K_P);
        parameters.add(Integer.toString(getParameterValue(k)));

        return parameters;
    }
}
