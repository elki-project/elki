package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.FourC;
import de.lmu.ifi.dbs.preprocessing.FourCPreprocessor;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.LessEqualConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint;
import de.lmu.ifi.dbs.varianceanalysis.LimitEigenPairFilter;

import java.util.List;
import java.util.Vector;

/**
 * A wrapper for the 4C algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class FourCWrapper extends NormalizationWrapper {

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
   * The value of the delta parameter.
   */
  private double delta;

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
    // parameter epsilon
    optionHandler.put(FourC.EPSILON_P, new PatternParameter(FourC.EPSILON_P, FourC.EPSILON_D));

    // parameter min points
    optionHandler.put(FourC.MINPTS_P, new IntParameter(FourC.MINPTS_P, FourC.MINPTS_D, new GreaterConstraint(0)));

    // parameter lambda
    optionHandler.put(FourC.LAMBDA_P, new IntParameter(FourC.LAMBDA_P, FourC.LAMBDA_D, new GreaterConstraint(0)));

    // parameter absolut f
    optionHandler.put(FourCPreprocessor.ABSOLUTE_F, new Flag(FourCPreprocessor.ABSOLUTE_F, FourCPreprocessor.ABSOLUTE_D));

    // parameter delta
    List<ParameterConstraint<Number>> cons = new Vector<ParameterConstraint<Number>>();
    ParameterConstraint<Number> aboveNull = new GreaterEqualConstraint(0);
    cons.add(aboveNull);
    ParameterConstraint<Number> underOne = new LessEqualConstraint(1);
    cons.add(underOne);
    DoubleParameter delta = new DoubleParameter(FourCPreprocessor.DELTA_P, FourCPreprocessor.DELTA_D, cons);
    delta.setDefaultValue(LimitEigenPairFilter.DEFAULT_DELTA);
    optionHandler.put(FourCPreprocessor.DELTA_P, delta);
  }

  /**
   * @see KDDTaskWrapper#getKDDTaskParameters()
   */
  public List<String> getKDDTaskParameters() throws UnusedParameterException {
    List<String> parameters = super.getKDDTaskParameters();

    // 4C algorithm
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(FourC.class.getName());

    // epsilon for 4C
    parameters.add(OptionHandler.OPTION_PREFIX + FourC.EPSILON_P);
    parameters.add(epsilon);

    // minpts for 4C
    parameters.add(OptionHandler.OPTION_PREFIX + FourC.MINPTS_P);
    parameters.add(Integer.toString(minpts));

    // lambda for 4C
    parameters.add(OptionHandler.OPTION_PREFIX + FourC.LAMBDA_P);
    parameters.add(Integer.toString(lambda));

    // delta for 4C
    parameters.add(OptionHandler.OPTION_PREFIX + LimitEigenPairFilter.DELTA_P);
    parameters.add(Double.toString(delta));

    return parameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    epsilon = (String) optionHandler.getOptionValue(FourC.EPSILON_P);
    minpts = (Integer) optionHandler.getOptionValue(FourC.MINPTS_P);
    lambda = (Integer) optionHandler.getOptionValue(FourC.LAMBDA_P);
    delta = (Double) optionHandler.getOptionValue(LimitEigenPairFilter.DELTA_P);

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();
    AttributeSettings mySettings = settings.get(0);
    mySettings.addSetting(FourC.EPSILON_P, epsilon);
    mySettings.addSetting(FourC.MINPTS_P, Integer.toString(minpts));
    mySettings.addSetting(FourC.LAMBDA_P, Integer.toString(lambda));
    mySettings.addSetting(LimitEigenPairFilter.DELTA_P, Double.toString(delta));
    return settings;
  }
}
