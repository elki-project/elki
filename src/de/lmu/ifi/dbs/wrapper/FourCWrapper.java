package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.algorithm.clustering.FourC;
import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.distance.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.ArrayList;
import java.util.List;

/**
 * A wrapper for the 4C algorithm.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class FourCWrapper extends AbstractAlgorithmWrapper {
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
   * Epsilon.
   */
  protected String epsilon;

  /**
   * Minimum points.
   */
  protected String minpts;

  /**
   * Parameter lambda.
   */
  protected static final String LAMBDA_P = "lambda";

  /**
   * Description for parameter lambda.
   */
  protected static final String LAMBDA_D = "<lambda>(integer) correlation dimensionality";

  /**
   * Holds lambda.
   */
  protected String lambda;


  /**
   * Provides a wrapper for the 4C algorithm.
   */
  public FourCWrapper() {
    super();
    parameterToDescription.put(EPSILON_P + OptionHandler.EXPECTS_VALUE, EPSILON_D);
    parameterToDescription.put(MINPTS_P + OptionHandler.EXPECTS_VALUE, MINPTS_D);
    parameterToDescription.put(LAMBDA_P + OptionHandler.EXPECTS_VALUE, LAMBDA_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see AbstractAlgorithmWrapper#initParameters(java.util.List<java.lang.String>)
   */
  public List<String> initParameters(List<String> remainingParameters) {
    ArrayList<String> parameters = new ArrayList<String>(remainingParameters);

    // 4C algorithm
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(FourC.class.getName());

    // distance function
    parameters.add(OptionHandler.OPTION_PREFIX + DBSCAN.DISTANCE_FUNCTION_P);
    parameters.add(LocallyWeightedDistanceFunction.class.getName());

    // epsilon for 4C
    parameters.add(OptionHandler.OPTION_PREFIX + DBSCAN.EPSILON_P);
    parameters.add(epsilon);

    // minpts for 4C
    parameters.add(OptionHandler.OPTION_PREFIX + DBSCAN.MINPTS_P);
    parameters.add(minpts);

    // lambda for 4C
    parameters.add(OptionHandler.OPTION_PREFIX + FourC.LAMBDA_P);
    parameters.add(lambda);

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

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = super.setParameters(args);

    epsilon = optionHandler.getOptionValue(EPSILON_P);
    minpts = optionHandler.getOptionValue(MINPTS_P);
    lambda = optionHandler.getOptionValue(LAMBDA_P);

    return remainingParameters;
  }

  public static void main(String[] args) {
    FourCWrapper wrapper = new FourCWrapper();
    try {
      wrapper.run(args);
    }
    catch (WrongParameterValueException e) {
      System.err.println(wrapper.optionHandler.usage(e.getMessage()));
    }
    catch (NoParameterValueException e) {
      System.err.println(wrapper.optionHandler.usage(e.getMessage()));
    }
    catch (UnusedParameterException e) {
      System.err.println(wrapper.optionHandler.usage(e.getMessage()));
    }
  }
}
