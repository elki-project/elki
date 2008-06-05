package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.clustering.COPAA;
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
 * Wrapper class for COPAA algorithm. Performs an attribute wise normalization on
 * the database objects, partitions the database according to the correlation dimension of
 * its objects and then performs the algorithm OPTICS over the partitions.
 *
 * @author Elke Achtert
 */
public class COPAAWrapper extends NormalizationWrapper {

    /**
     * Parameter to specify the maximum radius of the neighborhood to be considered,
     * must be suitable to {@link LocallyWeightedDistanceFunction LocallyWeightedDistanceFunction}.
     * <p>Key: {@code -epsilon} </p>
     */
    public final PatternParameter EPSILON_PARAM;

    /**
     * Parameter to specify the threshold for minimum number of points in
     * the epsilon-neighborhood of a point,
     * must be an integer greater than 0.
     * <p>Key: {@code -optics.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(OptionID.OPTICS_MINPTS,
        new GreaterConstraint(0));

    /**
     * Description for parameter k.
     */
    public static final String K_D = "a positive integer specifying the number of " +
        "nearest neighbors considered in the PCA. " +
        "If this value is not defined, k ist set to minpts";

    /**
     * Hold the value of the epsilon parameter.
     */
    private String epsilon;

    /**
     * Holds the value of the minpts parameter.
     */
    private int minpts;

    /**
     * Holds the value of the k parameter.
     */
    private int k;

    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        COPAAWrapper wrapper = new COPAAWrapper();
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
    public COPAAWrapper() {
        super();
        // parameter epsilon
        EPSILON_PARAM = new PatternParameter(OptionID.OPTICS_EPSILON);
        EPSILON_PARAM.setDescription("<pattern>The maximum radius of the neighborhood " +
            "to be considerd, must be suitable to " +
            LocallyWeightedDistanceFunction.class.getName());
        optionHandler.put(EPSILON_PARAM);

        // parameter minpts
        optionHandler.put(MINPTS_PARAM);

        // parameter k
        IntParameter kPam = new IntParameter(KnnQueryBasedHiCOPreprocessor.K_P, K_D, new GreaterConstraint(0));
        kPam.setOptional(true);
        optionHandler.put(kPam);

        // global constraint k <-> minpts
        // noinspection unchecked
        GlobalParameterConstraint gpc = new DefaultValueGlobalConstraint(kPam, MINPTS_PARAM);
        optionHandler.setGlobalParameterConstraint(gpc);
    }

    /**
     * @see KDDTaskWrapper#getKDDTaskParameters()
     */
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // algorithm COPAA
        Util.addParameter(parameters, OptionID.ALGORITHM, COPAA.class.getName());

        // partition algorithm
        Util.addParameter(parameters, OptionID.COPAA_PARTITION_ALGORITHM, OPTICS.class.getName());

        // epsilon
        parameters.add(OptionHandler.OPTION_PREFIX + EPSILON_PARAM.getName());
        parameters.add(epsilon);

        // minpts
        Util.addParameter(parameters, OptionID.OPTICS_MINPTS, Integer.toString(minpts));

        // distance function
        parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.DISTANCE_FUNCTION_P);
        parameters.add(LocallyWeightedDistanceFunction.class.getName());

        // omit preprocessing
        parameters.add(OptionHandler.OPTION_PREFIX + PreprocessorHandler.OMIT_PREPROCESSING_F);

        // preprocessor for correlation dimension
        Util.addParameter(parameters, OptionID.COPAA_PREPROCESSOR, KnnQueryBasedHiCOPreprocessor.class.getName());

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
        epsilon = getParameterValue(EPSILON_PARAM);
        minpts = getParameterValue(MINPTS_PARAM);
        k = (Integer) optionHandler.getOptionValue(KnnQueryBasedHiCOPreprocessor.K_P);

        return remainingParameters;
    }
}
