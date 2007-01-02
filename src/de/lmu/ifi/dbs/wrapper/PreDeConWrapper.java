package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.PreDeCon;
import de.lmu.ifi.dbs.distance.distancefunction.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.util.List;

/**
 * A wrapper for the PreDeCon algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class PreDeConWrapper extends NormalizationWrapper {

  /**
   * The value of the epsilon parameter.
   */
  private String epsilon;

  /**
   * The value of the minpts parameter.
   */
  private int minpts;

  /**
   * The value of the lambda parameter.
   */
  private int lambda;

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    PreDeConWrapper wrapper = new PreDeConWrapper();
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
   * Provides a wrapper for the 4C algorithm.
   */
  public PreDeConWrapper() {
    super();
    // parameter epsilon
    //TODO pattern distance constraint!
    optionHandler.put(PreDeCon.EPSILON_P, new PatternParameter(PreDeCon.EPSILON_P, PreDeCon.EPSILON_D, LocallyWeightedDistanceFunction.class));

    // parameter min points
    optionHandler.put(PreDeCon.MINPTS_P, new IntParameter(PreDeCon.MINPTS_P, PreDeCon.MINPTS_D, new GreaterConstraint(0)));

    // parameter lambda
    optionHandler.put(PreDeCon.LAMBDA_P, new IntParameter(PreDeCon.LAMBDA_P, PreDeCon.LAMBDA_D, new GreaterConstraint(0)));
  }

  /**
   * @see de.lmu.ifi.dbs.wrapper.KDDTaskWrapper#getKDDTaskParameters()
   */
  public List<String> getKDDTaskParameters() {
    List<String> parameters = super.getKDDTaskParameters();

    // PreDeCon algorithm
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(PreDeCon.class.getName());

    // epsilon for PreDeCon
    parameters.add(OptionHandler.OPTION_PREFIX + PreDeCon.EPSILON_P);
    parameters.add(epsilon);

    // minpts for PreDeCon
    parameters.add(OptionHandler.OPTION_PREFIX + PreDeCon.MINPTS_P);
    parameters.add(Integer.toString(minpts));

    // lambda for PreDeCon
    parameters.add(OptionHandler.OPTION_PREFIX + PreDeCon.LAMBDA_P);
    parameters.add(Integer.toString(lambda));

    return parameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // epsilon, minpts, lambda
    epsilon = (String) optionHandler.getOptionValue(PreDeCon.EPSILON_P);
    minpts = (Integer) optionHandler.getOptionValue(PreDeCon.MINPTS_P);
    lambda = (Integer) optionHandler.getOptionValue(PreDeCon.LAMBDA_D);

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();
    AttributeSettings mySettings = settings.get(0);
    mySettings.addSetting(PreDeCon.EPSILON_P, epsilon);
    mySettings.addSetting(PreDeCon.MINPTS_P, Integer.toString(minpts));
    mySettings.addSetting(PreDeCon.LAMBDA_P, Integer.toString(lambda));
    return settings;
  }
}
