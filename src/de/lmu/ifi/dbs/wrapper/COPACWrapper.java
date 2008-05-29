package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.COPAC;
import de.lmu.ifi.dbs.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.distance.distancefunction.ERiCDistanceFunction;
import de.lmu.ifi.dbs.distance.distancefunction.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.List;

/**
 * Wrapper class for COPAC algorithm. Performs an attribute wise normalization on
 * the database objects, partitions the database according to the correlation dimension of
 * its objects and then performs the algorithm DBSCAN over the partitions.
 *
 * @author Elke Achtert
 */
public class COPACWrapper extends NormalizationWrapper {

    /**
     * Parameter to specify the maximum radius of the neighborhood to be considered,
     * must be suitable to {@link LocallyWeightedDistanceFunction LocallyWeightedDistanceFunction}.
     * <p>Key: {@code -epsilon} </p>
     */
    public static final PatternParameter EPSILON_PARAM = new PatternParameter("epsilon",
        "the maximum radius of the neighborhood " +
            "to be considered, must be suitable to " +
            LocallyWeightedDistanceFunction.class.getName());

    /**
     * Description for parameter k.
     */
    public static final String K_D = "a positive integer specifying the number of " +
        "nearest neighbors considered in the PCA. " +
        "If this value is not defined, k ist set to minpts";

    /**
     * The value of the epsilon parameter.
     */
    private String epsilon;

    /**
     * The value of the minpts parameter.
     */
    private int minpts;

    /**
     * The value of the k parameter.
     */
    private int k;

    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        COPACWrapper wrapper = new COPACWrapper();
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
    public COPACWrapper() {
        super();

        // parameter epsilon
        optionHandler.put(EPSILON_PARAM);

        // parameter min points
        IntParameter minPam = new IntParameter(DBSCAN.MINPTS_P, OPTICS.MINPTS_D, new GreaterConstraint(0));
        optionHandler.put(minPam);

        // parameter k
        IntParameter kPam = new IntParameter(KnnQueryBasedHiCOPreprocessor.K_P, K_D, new GreaterConstraint(0));
        kPam.setOptional(true);
        optionHandler.put(kPam);
    }

    /**
     * @see KDDTaskWrapper#getKDDTaskParameters()
     */
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // algorithm COPAC
        parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
        parameters.add(COPAC.class.getName());

        // partition algorithm DBSCAN
        Util.addParameter(parameters, OptionID.COPAA_PARTITION_ALGORITHM, DBSCAN.class.getName());

        // epsilon
        parameters.add(OptionHandler.OPTION_PREFIX + EPSILON_PARAM.getName());
        parameters.add(epsilon);

        // minpts
        parameters.add(OptionHandler.OPTION_PREFIX + DBSCAN.MINPTS_P);
        parameters.add(Integer.toString(minpts));

        // distance function
        parameters.add(OptionHandler.OPTION_PREFIX + DBSCAN.DISTANCE_FUNCTION_P);
//    parameters.add(LocallyWeightedDistanceFunction.class.getName());
        parameters.add(ERiCDistanceFunction.class.getName());

        // omit preprocessing
        parameters.add(OptionHandler.OPTION_PREFIX + PreprocessorHandler.OMIT_PREPROCESSING_F);

        // preprocessor for correlation dimension
        Util.addParameter(parameters, OptionID.COPAA_PREPROCESSOR, KnnQueryBasedHiCOPreprocessor.class.getName());

        // k
        parameters.add(OptionHandler.OPTION_PREFIX + KnnQueryBasedHiCOPreprocessor.K_P);
        parameters.add(Integer.toString(k));
        // k
        parameters.add(OptionHandler.OPTION_PREFIX + KnnQueryBasedHiCOPreprocessor.K_P);
        parameters.add(Integer.toString(k));

        return parameters;
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);
        // epsilon, minpts
        epsilon = getParameterValue(EPSILON_PARAM);
        minpts = (Integer) optionHandler.getOptionValue(DBSCAN.MINPTS_P);

        // k
        if (optionHandler.isSet(KnnQueryBasedHiCOPreprocessor.K_P)) {
            k = (Integer) optionHandler.getOptionValue(KnnQueryBasedHiCOPreprocessor.K_P);
        }
        else {
            k = minpts;
        }

        return remainingParameters;
    }
}
