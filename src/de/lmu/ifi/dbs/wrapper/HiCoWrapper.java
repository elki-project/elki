package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.distance.CorrelationDistanceFunction;
import de.lmu.ifi.dbs.distance.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.normalization.AttributeWiseDoubleVectorNormalization;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedCorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.preprocessing.RangeQueryBasedCorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.ArrayList;

/**
 * Wrapper class for HiCo algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class HiCoWrapper extends AbstractWrapper {
  /**
   * Parameter for epsilon.
   */
  public static final String EPSILON_P = "epsilon";

  /**
   * Description for parameter epsilon.
   */
  public static final String EPSILON_D = "<epsilon>an epsilon value suitable to the distance function: " + LocallyWeightedDistanceFunction.class.getName();

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
   * Epsilon.
   */
  protected String epsilon;

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
    parameterToDescription.put(EPSILON_P + OptionHandler.EXPECTS_VALUE, EPSILON_D);
    parameterToDescription.put(MINPTS_P + OptionHandler.EXPECTS_VALUE, MINPTS_D);
    parameterToDescription.put(K_P + OptionHandler.EXPECTS_VALUE, K_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * Runs the COPAC algorithm accordingly to the specified parameters.
   *
   * @param args parameter list according to description
   */
  public void run(String[] args) {
    this.setParameters(args);
    ArrayList<String> params = getRemainingParameters();

    // OPTICS algorithm
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    params.add(OPTICS.class.getName());

    // distance function
    params.add(OptionHandler.OPTION_PREFIX + OPTICS.DISTANCE_FUNCTION_P);
    params.add(CorrelationDistanceFunction.class.getName());

    // epsilon for OPTICS
    params.add(OptionHandler.OPTION_PREFIX + OPTICS.EPSILON_P);
    params.add(CorrelationDistanceFunction.INFINITY_PATTERN);

    // minpts for OPTICS
    params.add(OptionHandler.OPTION_PREFIX + OPTICS.MINPTS_P);
    params.add(Integer.toString(minpts));

    // preprocessor
    params.add(OptionHandler.OPTION_PREFIX + CorrelationDistanceFunction.PREPROCESSOR_CLASS_P);
    params.add(KnnQueryBasedCorrelationDimensionPreprocessor.class.getName());

    // k for preprocessor
    params.add(OptionHandler.OPTION_PREFIX + RangeQueryBasedCorrelationDimensionPreprocessor.EPSILON_P);
    params.add(Integer.toString(k));

    // normalization
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
    params.add(AttributeWiseDoubleVectorNormalization.class.getName());
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    // input
    params.add(OptionHandler.OPTION_PREFIX + FileBasedDatabaseConnection.INPUT_P);
    params.add(input);

    // output
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
    params.add(output);

    if (time) {
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.TIME_F);
    }
    if (verbose) {
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
    }

    KDDTask task = new KDDTask();
    task.setParameters(params.toArray(new String[params.size()]));
    task.run();


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
    super.setParameters(args);
    try {
      epsilon = optionHandler.getOptionValue(EPSILON_P);
      minpts = Integer.parseInt(optionHandler.getOptionValue(MINPTS_P));
      if (optionHandler.isSet(K_P)) {
        k = Integer.parseInt(optionHandler.getOptionValue(K_P));
      }
      else
        k = minpts;
    }
    catch (UnusedParameterException e) {
      throw new IllegalArgumentException(e);
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException(e);
    }
    return new String[0];
  }
}
