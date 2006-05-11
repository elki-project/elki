package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.distance.PreferenceVectorBasedCorrelationDistanceFunction;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.preprocessing.HiSCPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper class for HiSC algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class HiSCWrapper extends FileBasedDatabaseConnectionWrapper {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"unused", "UNUSED_SYMBOL"})
  private static final boolean DEBUG = LoggingConfiguration.DEBUG;

  //private static final boolean DEBUG = true;

  /**
   * The logger of this class.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * Description for parameter k.
   */
  public static final String K_D = "<int>a positive integer specifying the number of " + "nearest neighbors considered to determine the preference vector. " + "If this value is not defined, k ist set to minpts.";

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    HiSCWrapper wrapper = new HiSCWrapper();
    try {
      wrapper.run(args);
    }
    catch (ParameterException e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), cause);
    }
    catch (AbortException e) {
      wrapper.logger.info(e.getMessage());
    }
    catch (Exception e) {
      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), e);
    }
  }

  /**
   * Sets the parameter minpts and k in the parameter map additionally to the
   * parameters provided by super-classes.
   */
  public HiSCWrapper() {
    super();
    parameterToDescription.put(OPTICS.MINPTS_P + OptionHandler.EXPECTS_VALUE, OPTICS.MINPTS_D);
    parameterToDescription.put(HiSCPreprocessor.ALPHA_P + OptionHandler.EXPECTS_VALUE, HiSCPreprocessor.ALPHA_D);
    parameterToDescription.put(HiSCPreprocessor.K_P + OptionHandler.EXPECTS_VALUE, HiSCWrapper.K_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see de.lmu.ifi.dbs.wrapper.KDDTaskWrapper#getParameters()
   */
  public List<String> getParameters() throws ParameterException {
    List<String> parameters = super.getParameters();

    // OPTICS algorithm
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(OPTICS.class.getName());

    // distance function
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.DISTANCE_FUNCTION_P);
    parameters.add(PreferenceVectorBasedCorrelationDistanceFunction.class.getName());

    // omit flag
    parameters.add(OptionHandler.OPTION_PREFIX + PreferenceVectorBasedCorrelationDistanceFunction.OMIT_PREPROCESSING_F);

    // epsilon for OPTICS
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.EPSILON_P);
    parameters.add(PreferenceVectorBasedCorrelationDistanceFunction.INFINITY_PATTERN);

    // minpts for OPTICS
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.MINPTS_P);
    parameters.add(optionHandler.getOptionValue(OPTICS.MINPTS_P));

    // preprocessor
    parameters.add(OptionHandler.OPTION_PREFIX + PreferenceVectorBasedCorrelationDistanceFunction.PREPROCESSOR_CLASS_P);
    parameters.add(HiSCPreprocessor.class.getName());

    // k for preprocessor
    parameters.add(OptionHandler.OPTION_PREFIX + HiSCPreprocessor.K_P);
    if (optionHandler.isSet(HiSCPreprocessor.K_P)) {
      parameters.add(optionHandler.getOptionValue(HiSCPreprocessor.K_P));
    }
    else {
      parameters.add(optionHandler.getOptionValue(OPTICS.MINPTS_P));
    }

    // alpha for preprocessor
    if (optionHandler.isSet(HiSCPreprocessor.ALPHA_P)) {
      parameters.add(OptionHandler.OPTION_PREFIX + HiSCPreprocessor.ALPHA_P);
      parameters.add(optionHandler.getOptionValue(HiSCPreprocessor.ALPHA_P));
    }

    // normalization
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
    parameters.add(AttributeWiseRealVectorNormalization.class.getName());
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    return parameters;
  }
}
