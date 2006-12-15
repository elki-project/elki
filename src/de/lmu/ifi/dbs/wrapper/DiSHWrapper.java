package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.DiSH;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.preprocessing.DiSHPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.util.List;

/**
 * Wrapper class for DiSH algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DiSHWrapper extends NormalizationWrapper {

  /**
   * The epsilon value in each dimension;
   */
  private List<Double> epsilon;

  /**
   * The value of the minpts parameter.
   */
  private int minpts;


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
   * Sets the parameter minpts and k in the parameter map additionally to the
   * parameters provided by super-classes.
   */
  public DiSHWrapper() {
    super();
    // parameter min points
    optionHandler.put(DiSHPreprocessor.MINPTS_P,
                      new IntParameter(DiSHPreprocessor.MINPTS_P,
                                       DiSHPreprocessor.MINPTS_D,
                                       new GreaterConstraint(0)));

    //parameter epsilon
    DoubleListParameter eps = new DoubleListParameter(DiSHPreprocessor.EPSILON_P, DiSHPreprocessor.EPSILON_D);
    eps.setOptional(true);
    optionHandler.put(DiSHPreprocessor.EPSILON_P, eps);
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
    parameters.add(Integer.toString(minpts));

    // minpts for preprocessor
    parameters.add(OptionHandler.OPTION_PREFIX + DiSHPreprocessor.MINPTS_P);
    parameters.add(Integer.toString(minpts));

    // epsilon for preprocessor
    if (epsilon != null) {
      parameters.add(OptionHandler.OPTION_PREFIX + DiSHPreprocessor.EPSILON_P);
      String epsString = "";
      for (int i = 0; i < epsilon.size(); i++) {
        epsString += epsilon.get(i);
        if (i != epsilon.size()-1) epsString += ",";
      }
      parameters.add(epsString);
    }

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
    if (optionHandler.isSet(DiSHPreprocessor.EPSILON_P)) {
      epsilon = (List<Double>) optionHandler.getOptionValue(DiSHPreprocessor.EPSILON_P);
    }
    minpts = (Integer) optionHandler.getOptionValue(DiSHPreprocessor.MINPTS_P);

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();
    AttributeSettings mySettings = settings.get(0);
    mySettings.addSetting(DiSHPreprocessor.EPSILON_P, epsilon.toString());
    mySettings.addSetting(DiSHPreprocessor.MINPTS_P, Integer.toString(minpts));
    return settings;
  }

}
