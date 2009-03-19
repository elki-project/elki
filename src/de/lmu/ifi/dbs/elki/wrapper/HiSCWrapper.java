package de.lmu.ifi.dbs.elki.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.distancefunction.HiSCDistanceFunction;
import de.lmu.ifi.dbs.elki.preprocessing.HiSCPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;

/**
 * Wrapper class for HiSC algorithm.
 *
 * @author Elke Achtert
 *         todo parameter
 */
public class HiSCWrapper<O extends DatabaseObject> extends FileBasedDatabaseConnectionWrapper<O> {
  /**
   * Alpha parameter
   */
  private final DoubleParameter ALPHA_PARAM = new DoubleParameter(HiSCPreprocessor.ALPHA_ID,
      new IntervalConstraint(0.0, IntervalConstraint.IntervalBoundary.OPEN,
          1.0, IntervalConstraint.IntervalBoundary.OPEN), HiSCPreprocessor.DEFAULT_ALPHA);

  /**
   * k Parameter
   */
  private final IntParameter K_PARAM = new IntParameter(HiSCPreprocessor.K_ID,
      new GreaterConstraint(0), true);


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
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        new HiSCWrapper().runCLIWrapper(args);
    }

    /**
     * Adds parameters
     * {@link #K_PARAM} and {@link #ALPHA_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public HiSCWrapper() {
        super();
        // parameter k
        addOption(K_PARAM);

        // parameter alpha
        addOption(ALPHA_PARAM);
    }

    @Override
    public List<String> getKDDTaskParameters() throws UnusedParameterException {
        List<String> parameters = super.getKDDTaskParameters();

        // OPTICS algorithm
        OptionUtil.addParameter(parameters, OptionID.ALGORITHM, OPTICS.class.getName());

        // distance function
        OptionUtil.addParameter(parameters, OPTICS.DISTANCE_FUNCTION_ID, HiSCDistanceFunction.class.getName());

        // omit flag
        OptionUtil.addFlag(parameters, PreprocessorHandler.OMIT_PREPROCESSING_ID);

        // epsilon for OPTICS
        OptionUtil.addParameter(parameters, OPTICS.EPSILON_ID, HiSCDistanceFunction.INFINITY_PATTERN);

        // minpts for OPTICS
        OptionUtil.addParameter(parameters, OPTICS.MINPTS_ID, "2");

        // preprocessor
        OptionUtil.addParameter(parameters, PreprocessorHandler.PREPROCESSOR_ID, HiSCPreprocessor.class.getName());

        // k for preprocessor
        if (k != null) {
            parameters.add(OptionHandler.OPTION_PREFIX + HiSCPreprocessor.K_ID.getName());
            parameters.add(Integer.toString(k));
        }

        // alpha for preprocessor
        parameters.add(OptionHandler.OPTION_PREFIX + HiSCPreprocessor.ALPHA_ID.getName());
        parameters.add(Double.toString(alpha));

        // epsilon for distance function
        OptionUtil.addParameter(parameters, HiSCDistanceFunction.EPSILON_ID, Double.toString(alpha));

        return parameters;
    }

    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        if (K_PARAM.isSet()) {
            k = K_PARAM.getValue();
        }
        else {
            k = null;
        }

        alpha = ALPHA_PARAM.getValue();

        return remainingParameters;
    }
}
