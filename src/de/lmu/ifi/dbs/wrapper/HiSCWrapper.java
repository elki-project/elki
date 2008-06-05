package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.distance.distancefunction.HiSCDistanceFunction;
import de.lmu.ifi.dbs.preprocessing.HiSCPreprocessor;
import de.lmu.ifi.dbs.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.List;

/**
 * Wrapper class for HiSC algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert
 */
//public class HiSCWrapper extends NormalizationWrapper {
// todo: richtig?
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
     * Sets the parameter minpts and k in the parameter map additionally to the
     * parameters provided by super-classes.
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

    /**
     * @see de.lmu.ifi.dbs.wrapper.KDDTaskWrapper#getKDDTaskParameters()
     */
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // OPTICS algorithm
        Util.addParameter(parameters, OptionID.ALGORITHM, OPTICS.class.getName());

        // distance function
        parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.DISTANCE_FUNCTION_P);
        parameters.add(HiSCDistanceFunction.class.getName());

        // omit flag
        parameters.add(OptionHandler.OPTION_PREFIX + PreprocessorHandler.OMIT_PREPROCESSING_F);

        // epsilon for OPTICS
        Util.addParameter(parameters, OptionID.OPTICS_EPSILON, HiSCDistanceFunction.INFINITY_PATTERN);

        // minpts for OPTICS
        Util.addParameter(parameters, OptionID.OPTICS_MINPTS, "2");

        // preprocessor
        parameters.add(OptionHandler.OPTION_PREFIX + PreprocessorHandler.PREPROCESSOR_CLASS_P);
        parameters.add(HiSCPreprocessor.class.getName());

        // k for preprocessor
        if (k != null) {
            parameters.add(OptionHandler.OPTION_PREFIX + HiSCPreprocessor.K_P);
            parameters.add(Integer.toString(k));
        }

        // alpha for preprocessor
        parameters.add(OptionHandler.OPTION_PREFIX + HiSCPreprocessor.ALPHA_P);
        parameters.add(Double.toString(alpha));

        // epsilon for distance function
        parameters.add(OptionHandler.OPTION_PREFIX + HiSCDistanceFunction.EPSILON_P);
        parameters.add(Double.toString(alpha));

        return parameters;
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
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
