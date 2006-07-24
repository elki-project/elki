package de.lmu.ifi.dbs.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.PreDeCon;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

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
  private String minpts;

  /**
   * The value of the lambda parameter.
   */
  private String lambda;

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
      wrapper.exception(wrapper.optionHandler.usage(e.getMessage()),cause);
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
    optionHandler.put(PreDeCon.EPSILON_P, new Parameter(PreDeCon.EPSILON_P,PreDeCon.EPSILON_D,Parameter.Types.DISTANCE_PATTERN));
    optionHandler.put(PreDeCon.MINPTS_P,new Parameter(PreDeCon.MINPTS_P,PreDeCon.MINPTS_D,Parameter.Types.INT));
    optionHandler.put(PreDeCon.LAMBDA_P, new Parameter(PreDeCon.LAMBDA_P,PreDeCon.LAMBDA_D,Parameter.Types.INT));
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
    parameters.add(minpts);

    // lambda for PreDeCon
    parameters.add(OptionHandler.OPTION_PREFIX + PreDeCon.LAMBDA_P);
    parameters.add(lambda);

    return parameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // epsilon, minpts, lambda
    epsilon = optionHandler.getOptionValue(PreDeCon.EPSILON_P);
    minpts = optionHandler.getOptionValue(PreDeCon.MINPTS_P);
    lambda = optionHandler.getOptionValue(PreDeCon.LAMBDA_D);

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();
    AttributeSettings mySettings = settings.get(0);
    mySettings.addSetting(PreDeCon.EPSILON_P, epsilon);
    mySettings.addSetting(PreDeCon.MINPTS_P, minpts);
    mySettings.addSetting(PreDeCon.LAMBDA_P, lambda);
    return settings;
  }
}
