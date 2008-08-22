package de.lmu.ifi.dbs.elki.wrapper;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.elki.distance.distancefunction.HiSCDistanceFunction;
import de.lmu.ifi.dbs.elki.preprocessing.HiSCPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.List;

/**
 * Wrapper class for HiSC algorithm.
 *
 * @author Elke Achtert
 *         todo parameter
 */
public class HiSCWrapper extends FileBasedDatabaseConnectionWrapper {

    /**
     * The value of the k parameter.
     */
    private Integer k;

    /**
     * The value of the alpha parameter.
     */
    private double alpha;

    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        HiSCWrapper wrapper = new HiSCWrapper();
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
     * {@link } and {@link } todo
     * to the option handler additionally to parameters of super class.
     */
    public HiSCWrapper() {
        super();
        // parameter k
        IntParameter k = new IntParameter(HiSCPreprocessor.K_P, HiSCPreprocessor.K_D, new GreaterConstraint(0));
        k.setOptional(true);
        optionHandler.put(k);

        // parameter alpha
        DoubleParameter alpha = new DoubleParameter(HiSCPreprocessor.ALPHA_P, HiSCPreprocessor.ALPHA_D, new GreaterConstraint(0));
        optionHandler.put(alpha);
    }

    @Override
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // OPTICS algorithm
        Util.addParameter(parameters, OptionID.ALGORITHM, OPTICS.class.getName());

        // distance function
        Util.addParameter(parameters, OPTICS.DISTANCE_FUNCTION_ID, HiSCDistanceFunction.class.getName());

        // omit flag
        Util.addFlag(parameters, PreprocessorHandler.OMIT_PREPROCESSING_ID);

        // epsilon for OPTICS
        Util.addParameter(parameters, OPTICS.EPSILON_ID, HiSCDistanceFunction.INFINITY_PATTERN);

        // minpts for OPTICS
        Util.addParameter(parameters, OPTICS.MINPTS_ID, "2");

        // preprocessor
        Util.addParameter(parameters, PreprocessorHandler.PREPROCESSOR_ID, HiSCPreprocessor.class.getName());

        // k for preprocessor
        if (k != null) {
            parameters.add(OptionHandler.OPTION_PREFIX + HiSCPreprocessor.K_P);
            parameters.add(Integer.toString(k));
        }

        // alpha for preprocessor
        parameters.add(OptionHandler.OPTION_PREFIX + HiSCPreprocessor.ALPHA_P);
        parameters.add(Double.toString(alpha));

        // epsilon for distance function
        Util.addParameter(parameters, HiSCDistanceFunction.EPSILON_ID, Double.toString(alpha));

        return parameters;
    }

    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        if (optionHandler.isSet(HiSCPreprocessor.K_P)) {
            k = (Integer) optionHandler.getOptionValue(HiSCPreprocessor.K_P);
        }
        else {
            k = null;
        }

        alpha = (Double) optionHandler.getOptionValue(HiSCPreprocessor.ALPHA_P);


        return remainingParameters;
    }
}
