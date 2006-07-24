package de.lmu.ifi.dbs.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.DiSH;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.preprocessing.DiSHPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

/**
 * Wrapper class for DiSH algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DiSHWrapper extends NormalizationWrapper {

  /**
   * The value of the epsilon parameter.
   */
  private String epsilon;

  /**
   * The value of the minpts parameter.
   */
  private String minpts;


  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    DiSHWrapper wrapper = new DiSHWrapper();
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
      e.printStackTrace();
      wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), e);
    }
  }

  /**
   * Sets the parameter minpts and k in the parameter map additionally to the
   * parameters provided by super-classes.
   */
  public DiSHWrapper() {
    super();
    optionHandler.put(DiSHPreprocessor.MINPTS_P, new Parameter(DiSHPreprocessor.MINPTS_P,DiSHPreprocessor.MINPTS_D,Parameter.Types.INT));
    optionHandler.put(DiSHPreprocessor.EPSILON_P, new Parameter(DiSHPreprocessor.EPSILON_P,DiSHPreprocessor.EPSILON_D,Parameter.Types.DOUBLE));
  }

  /**
   * @see de.lmu.ifi.dbs.wrapper.KDDTaskWrapper#getKDDTaskParameters()
   */
  public List<String> getKDDTaskParameters() {
    List<String> parameters = super.getKDDTaskParameters();

    // DiSH algorithm
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(DiSH.class.getName());

    // minpts for OPTICS
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.MINPTS_P);
    parameters.add(minpts);
//    parameters.add("2");

    // minpts for preprocessor
    parameters.add(OptionHandler.OPTION_PREFIX + DiSHPreprocessor.MINPTS_P);
    parameters.add(minpts);

    // epsilon for preprocessor
    parameters.add(OptionHandler.OPTION_PREFIX + DiSHPreprocessor.EPSILON_P);
    parameters.add(epsilon);

    // strategy for preprocessor
    parameters.add(OptionHandler.OPTION_PREFIX + DiSHPreprocessor.STRATEGY_P);
    parameters.add(DiSHPreprocessor.Strategy.MAX_INTERSECTION.toString());

    return parameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // epsilon, minpts
    epsilon = optionHandler.getOptionValue(DiSHPreprocessor.EPSILON_P);
    minpts = optionHandler.getOptionValue(DiSHPreprocessor.MINPTS_P);

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();
    AttributeSettings mySettings = settings.get(0);
    mySettings.addSetting(DiSHPreprocessor.EPSILON_P, epsilon);
    mySettings.addSetting(DiSHPreprocessor.MINPTS_P, minpts);
    return settings;
  }

}
