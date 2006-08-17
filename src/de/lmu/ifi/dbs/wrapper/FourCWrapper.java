package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.FourC;
import de.lmu.ifi.dbs.preprocessing.FourCPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.varianceanalysis.LimitEigenPairFilter;

import java.util.List;

/**
 * A wrapper for the 4C algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Arthur Zimek (<a  href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class FourCWrapper extends NormalizationWrapper {

  /**
   * The value of the epsilon parameter.
   */
  private String epsilon;

  /**
   * The value of the minpts parameter.
   */
  private String minpts;

  /**
   * The value of the lambda parameter.
   */
  private String lambda;

  /**
   * The value of the delta parameter.
   */
  private String delta;

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    FourCWrapper wrapper = new FourCWrapper();
    try {
      wrapper.setParameters(args);
      wrapper.run();
    }
    catch (ParameterException e) {
      e.printStackTrace();
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), cause);
    }
    catch (AbortException e) {
      wrapper.verbose(e.getMessage());
    }
    catch (Exception e) {
      e.printStackTrace();
      wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), e);
    }
  }

  /**
   * Provides a wrapper for the 4C algorithm.
   */
  public FourCWrapper() {
    super();
    optionHandler.put(FourC.EPSILON_P, new Parameter(FourC.EPSILON_P, FourC.EPSILON_D, Parameter.Types.DISTANCE_PATTERN));
    optionHandler.put(FourC.MINPTS_P, new Parameter(FourC.MINPTS_P, FourC.MINPTS_D, Parameter.Types.INT));
    optionHandler.put(FourC.LAMBDA_P, new Parameter(FourC.LAMBDA_P, FourC.LAMBDA_D, Parameter.Types.INT));
    optionHandler.put(FourCPreprocessor.ABSOLUTE_F, new Flag(FourCPreprocessor.ABSOLUTE_F, FourCPreprocessor.ABSOLUTE_D));
    optionHandler.put(FourCPreprocessor.DELTA_P, new Parameter(FourCPreprocessor.DELTA_P, FourCPreprocessor.DELTA_D, Parameter.Types.DOUBLE));
  }

  /**
   * @see KDDTaskWrapper#getKDDTaskParameters()
   */
  public List<String> getKDDTaskParameters() {
    List<String> parameters = super.getKDDTaskParameters();

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

    // delta for 4C
    parameters.add(OptionHandler.OPTION_PREFIX + LimitEigenPairFilter.DELTA_P);
    parameters.add(delta);

    return parameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // epsilon, minpts, lambda
    epsilon = optionHandler.getOptionValue(FourC.EPSILON_P);
    minpts = optionHandler.getOptionValue(FourC.MINPTS_P);
    lambda = optionHandler.getOptionValue(FourC.LAMBDA_P);

    // delta
    if (optionHandler.isSet(LimitEigenPairFilter.DELTA_P)) {
      delta = optionHandler.getOptionValue(LimitEigenPairFilter.DELTA_P);
    }
    else {
      delta = Double.toString(LimitEigenPairFilter.DEFAULT_DELTA);
    }

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();
    AttributeSettings mySettings = settings.get(0);
    mySettings.addSetting(FourC.EPSILON_P, epsilon);
    mySettings.addSetting(FourC.MINPTS_P, minpts);
    mySettings.addSetting(FourC.LAMBDA_P, lambda);
    mySettings.addSetting(LimitEigenPairFilter.DELTA_P, delta);
    return settings;
  }
}
