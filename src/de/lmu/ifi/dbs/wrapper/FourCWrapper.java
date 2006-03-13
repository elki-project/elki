package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.FourC;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.List;

/**
 * A wrapper for the 4C algorithm.
 *
 * @author Arthur Zimek (<a  href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class FourCWrapper extends AbstractAlgorithmWrapper {
  /**
   * Parameter for epsilon.
   */
  public static final String EPSILON_P = FourC.EPSILON_P;

  /**
   * Description for parameter epsilon.
   */
  public static final String EPSILON_D = FourC.EPSILON_D;

  /**
   * Parameter minimum points.
   */
  public static final String MINPTS_P = FourC.MINPTS_P;

  /**
   * Description for parameter minimum points.
   */
  public static final String MINPTS_D = FourC.MINPTS_D;

  /**
   * Parameter lambda.
   */
  protected static final String LAMBDA_P = FourC.LAMBDA_P;

  /**
   * Description for parameter lambda.
   */
  protected static final String LAMBDA_D = FourC.LAMBDA_D;

  /**
   * Epsilon.
   */
  protected String epsilon;

  /**
   * Minimum points.
   */
  protected String minpts;

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
   * @see AbstractAlgorithmWrapper#addParameters(java.util.List<java.lang.String>)
   */
  public void addParameters(List<String> parameters) {
    // 4C algorithm
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(FourC.class.getName());

    // epsilon for 4C
    parameters.add(OptionHandler.OPTION_PREFIX + FourC.EPSILON_P);
    parameters.add(epsilon);

    // minpts for 4C
    parameters.add(OptionHandler.OPTION_PREFIX + FourC.MINPTS_P);
    parameters.add(minpts);

    // lambda for 4C
    parameters.add(OptionHandler.OPTION_PREFIX + FourC.LAMBDA_P);
    parameters.add(lambda);

    // normalization
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
    parameters.add(AttributeWiseRealVectorNormalization.class.getName());
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);
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
