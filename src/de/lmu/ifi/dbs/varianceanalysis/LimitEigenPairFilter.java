package de.lmu.ifi.dbs.varianceanalysis;

import de.lmu.ifi.dbs.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * The LimitEigenPairFilter marks all eigenpairs having an (absolute) eigenvalue below
 * the specified threshold (relative or absolute) as weak eigenpairs,
 * the others are marked as strong eigenpairs.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

public class LimitEigenPairFilter extends AbstractEigenPairFilter {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"UNUSED_SYMBOL"})
//  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
  private static final boolean DEBUG = true;

  /**
   * The logger of this class.
   */
  @SuppressWarnings({"UNUSED_SYMBOL"})
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * Flag for marking parameter delta as an absolute value.
   */
  public static final String ABSOLUTE_F = "abs";

  /**
   * Description for flag abs.
   */
  public static final String ABSOLUTE_D = "flag to mark delta as an absolute value.";

  /**
   * The default value for delta.
   */
  public static final double DEFAULT_DELTA = 0.01;

  /**
   * Option string for parameter delta.
   */
  public static final String DELTA_P = "delta";

  /**
   * Description for parameter delta.
   */
  public static final String DELTA_D = "<double>a double specifying the threshold for " +
                                       "strong Eigenvalues. If not otherwise specified, delta " +
                                       "is a relative value w.r.t. the (absolute) highest " +
                                       "Eigenvalues and has to be a double between 0 and 1 " +
                                       "(default is delta = " + DEFAULT_DELTA + "). " +
                                       "To mark delta as an absolute value, use " +
                                       "the option -" + ABSOLUTE_F + ".";

  /**
   * Threshold for strong eigenpairs, can be absolute or relative.
   */
  private double delta;

  /**
   * Indicates wether delta is an absolute or a relative value.
   */
  private boolean absolute;

  /**
   * Provides a new EigenPairFilter that marks all eigenpairs having an
   * (absolute) eigenvalue below the specified threshold
   * (relative or absolute) as weak eigenpairs,
   * the others are marked as strong eigenpairs.
   */
  public LimitEigenPairFilter() {
    parameterToDescription.put(DELTA_P + OptionHandler.EXPECTS_VALUE, DELTA_D);
    parameterToDescription.put(ABSOLUTE_F, ABSOLUTE_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see EigenPairFilter#filter(de.lmu.ifi.dbs.math.linearalgebra.EigenPair[])
   */
  public void filter(EigenPair[] eigenPairs) {
    StringBuffer msg = new StringBuffer();
    if (DEBUG) {
      msg.append("\ndelta = ").append(delta);
    }

    // determine limit
    double limit;
    if (absolute) {
      limit = delta;
    }
    else {
      double max = Double.NEGATIVE_INFINITY;
      for (EigenPair eigenPair : eigenPairs) {
        double eigenValue = Math.abs(eigenPair.getEigenvalue());
        if (max < eigenValue) {
          max = eigenValue;
        }
      }
      limit = max * delta;
    }
    if (DEBUG) {
      msg.append("\nlimit = ").append(limit);
    }

    // init strong and weak eigenpairs
    strongEigenPairs = new ArrayList<EigenPair>();
    weakEigenPairs = new ArrayList<EigenPair>();

    // determine strong and weak eigenpairs
    for (EigenPair eigenPair : eigenPairs) {
      double eigenValue = Math.abs(eigenPair.getEigenvalue());
      if (eigenValue >= limit) {
        strongEigenPairs.add(eigenPair);
      }
      else {
        weakEigenPairs.add(eigenPair);
      }
    }
    if (DEBUG) {
      msg.append("\nstrong EigenPairs = ").append(strongEigenPairs);
      msg.append("\nweak EigenPairs = ").append(weakEigenPairs);
      logger.fine(msg.toString());
    }
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // absolute
    absolute = optionHandler.isSet(ABSOLUTE_F);

    //alpha
    if (optionHandler.isSet(DELTA_P)) {
      String deltaString = optionHandler.getOptionValue(DELTA_P);
      try {
        delta = Double.parseDouble(deltaString);
        if (! absolute && delta < 0 || delta > 1)
          throw new WrongParameterValueException(DELTA_P, deltaString, DELTA_D);
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(DELTA_P, deltaString, DELTA_D, e);
      }
    }
    else if (! absolute) {
      delta = DEFAULT_DELTA;
    }
    else {
      throw new WrongParameterValueException("Illegal parameter setting: " +
                                             "Flag " + ABSOLUTE_F + " is set, " +
                                             "but no value for " + DELTA_P + " is specified.");
    }

    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();

    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(DELTA_P, Double.toString(delta));
    mySettings.addSetting(ABSOLUTE_F, Boolean.toString(absolute));

    return attributeSettings;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(PercentageEigenPairFilter.class.getName());
    description.append(" filters all eigenpairs, " +
                       " which are lower than a given value.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }


}
