package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.distance.CorrelationDistanceFunction;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedCorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class for HiCo algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class HiCoWrapper extends AbstractAlgorithmWrapper {
  /**
   * Parameter minimum points.
   */
  public static final String MINPTS_P = "minpts";

  /**
   * Description for parameter minimum points.
   */
  public static final String MINPTS_D = "<int>minpts";

  /**
   * Option string for parameter k.
   */
  public static final String K_P = "k";

  /**
   * Description for parameter k.
   */
  public static final String K_D = "<k>a integer specifying the number of " +
                                   "nearest neighbors considered in the PCA. " +
                                   "If this value is not defined, k ist set minpts";

  /**
   * Minimum points.
   */
  protected int minpts;

  /**
   * k.
   */
  protected int k;

  /**
   * Provides a wrapper for the HiCo algorithm.
   */
  public HiCoWrapper() {
    super();
    parameterToDescription.put(MINPTS_P + OptionHandler.EXPECTS_VALUE, MINPTS_D);
    parameterToDescription.put(K_P + OptionHandler.EXPECTS_VALUE, K_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see AbstractAlgorithmWrapper#initParameters(java.util.List<java.lang.String>)
   */
  public List<String> initParameters(List<String> remainingParameters) {
    ArrayList<String> parameters = new ArrayList<String>(remainingParameters);

    // OPTICS algorithm
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(OPTICS.class.getName());

    // distance function
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.DISTANCE_FUNCTION_P);
    parameters.add(CorrelationDistanceFunction.class.getName());

    // epsilon for OPTICS
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.EPSILON_P);
    parameters.add(CorrelationDistanceFunction.INFINITY_PATTERN);

    // minpts for OPTICS
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.MINPTS_P);
    parameters.add(Integer.toString(minpts));

    // preprocessor
    parameters.add(OptionHandler.OPTION_PREFIX + CorrelationDistanceFunction.PREPROCESSOR_CLASS_P);
    parameters.add(KnnQueryBasedCorrelationDimensionPreprocessor.class.getName());

    // k for preprocessor
    parameters.add(OptionHandler.OPTION_PREFIX + KnnQueryBasedCorrelationDimensionPreprocessor.K_P);
    parameters.add(Integer.toString(k));

    // normalization
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
    parameters.add(AttributeWiseRealVectorNormalization.class.getName());
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    // input
    parameters.add(OptionHandler.OPTION_PREFIX + FileBasedDatabaseConnection.INPUT_P);
    parameters.add(input);

    // output
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
    parameters.add(output);

    if (time) {
      parameters.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.TIME_F);
    }
    if (verbose) {
      parameters.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
    }

    return parameters;
  }

  public static void main(String[] args) {
    HiCoWrapper wrapper = new HiCoWrapper();
    try {
      wrapper.run(args);
    }
    catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = super.setParameters(args);

    try {
      minpts = Integer.parseInt(optionHandler.getOptionValue(MINPTS_P));
    }
    catch (NumberFormatException e) {
      WrongParameterValueException pe = new WrongParameterValueException(MINPTS_P, optionHandler.getOptionValue(MINPTS_P), MINPTS_D);
      pe.fillInStackTrace();
      throw pe;
    }

    if (optionHandler.isSet(K_P)) {
      try {
        k = Integer.parseInt(optionHandler.getOptionValue(K_P));
      }
      catch (NumberFormatException e) {
        WrongParameterValueException pe = new WrongParameterValueException(K_P, optionHandler.getOptionValue(K_P), K_D);
        pe.fillInStackTrace();
        throw pe;
      }
    }
    else {
      k = minpts;
    }

    return remainingParameters;
  }
}
