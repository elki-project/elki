package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.distance.CorrelationDistanceFunction;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;

import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Wrapper class for HiCo algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class HiCoWrapper extends FileBasedDatabaseConnectionWrapper {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"UNUSED_SYMBOL"})
  private static final boolean DEBUG = LoggingConfiguration.DEBUG;

  /**
   * The logger of this class.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * Description for parameter k.
   */
  public static final String K_D = "<int>a positive integer specifying the number of " +
                                   "nearest neighbors considered in the PCA. " +
                                   "If this value is not defined, k ist set to minpts";

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    HiCoWrapper wrapper = new HiCoWrapper();
    try {
      wrapper.run(args);
    }
    catch (ParameterException e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), cause);
    }
  }

  /**
   * Sets the parameter minpts and k in the parameter map additionally to the
   * parameters provided by super-classes.
   */
  public HiCoWrapper() {
    super();
    parameterToDescription.put(OPTICS.MINPTS_P + OptionHandler.EXPECTS_VALUE, OPTICS.MINPTS_D);
    parameterToDescription.put(KnnQueryBasedHiCOPreprocessor.K_P + OptionHandler.EXPECTS_VALUE, K_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see KDDTaskWrapper#getParameters()
   */
  public List<String> getParameters() throws ParameterException {
    List<String> parameters = super.getParameters();

    // OPTICS algorithm
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(OPTICS.class.getName());

    // distance function
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.DISTANCE_FUNCTION_P);
    parameters.add(CorrelationDistanceFunction.class.getName());

    // omit flag
    parameters.add(OptionHandler.OPTION_PREFIX + CorrelationDistanceFunction.OMIT_PREPROCESSING_F);

    // epsilon for OPTICS
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.EPSILON_P);
    parameters.add(CorrelationDistanceFunction.INFINITY_PATTERN);

    // minpts for OPTICS
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.MINPTS_P);
    parameters.add(optionHandler.getOptionValue(OPTICS.MINPTS_P));

    // preprocessor
    parameters.add(OptionHandler.OPTION_PREFIX + CorrelationDistanceFunction.PREPROCESSOR_CLASS_P);
    parameters.add(KnnQueryBasedHiCOPreprocessor.class.getName());

    // k for preprocessor
    parameters.add(OptionHandler.OPTION_PREFIX + KnnQueryBasedHiCOPreprocessor.K_P);
    if (optionHandler.isSet(KnnQueryBasedHiCOPreprocessor.K_P)) {
      parameters.add(optionHandler.getOptionValue(KnnQueryBasedHiCOPreprocessor.K_P));
    }
    else {
      parameters.add(optionHandler.getOptionValue(OPTICS.MINPTS_P));
    }

    // normalization
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
    parameters.add(AttributeWiseRealVectorNormalization.class.getName());
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    return parameters;
  }
}
