package de.lmu.ifi.dbs.varianceanalysis;

import de.lmu.ifi.dbs.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.*;
import java.util.logging.Logger;

/**
 * The PercentageEigenPairFilter sorts the eigenpairs in decending order
 * of their eigenvalues and returns the first eigenpairs, whose sum of
 * eigenvalues is higher than the given percentage of the sum of all eigenvalues.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

public class PercentageEigenPairFilter implements EigenPairFilter {
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
   * The default value for alpha.
   */
  public static final double DEFAULT_ALPHA = 0.85;

  /**
   * Option string for parameter alpha.
   */
  public static final String ALPHA_P = "alpha";

  /**
   * Description for parameter alpha.
   */
  public static final String ALPHA_D = "<alpha>a double between 0 and 1 specifying " +
                                       "the threshold for strong eigenvectors: " +
                                       "the strong eigenvectors explain a " +
                                       "portion of at least alpha of the total variance " +
                                       "(default is alpha = " + DEFAULT_ALPHA + ")";

  /**
   * OptionHandler for handling options.
   */
  private OptionHandler optionHandler;

  /**
   * Holds the currently set parameter array.
   */
  private String[] currentParameterArray = new String[0];

  /**
   * The threshold for strong eigenvectors: the strong eigenvectors explain a
   * portion of at least alpha of the total variance.
   */
  private double alpha;

  /**
   * Contains the strong eigenpairs.
   */
  private List<EigenPair> strongEigenPairs;

  /**
   * Contains the weak eigenpairs.
   */
  private List<EigenPair> weakEigenPairs;

  /**
   * Provides a new EigenPairFilter that sorts the eigenpairs in decending order
   * of their eigenvalues and returns the first eigenpairs, whose sum of
   * eigenvalues is higher than the given percentage of the sum of all eigenvalues.
   */
  public PercentageEigenPairFilter() {
    Map<String, String> parameterToDescription = new Hashtable<String, String>();
    parameterToDescription.put(ALPHA_P + OptionHandler.EXPECTS_VALUE, ALPHA_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see EigenPairFilter#passEigenPairs(de.lmu.ifi.dbs.math.linearalgebra.EigenPair[])
   */
  public void passEigenPairs(EigenPair[] eigenPairs) {
    StringBuffer msg = new StringBuffer();
    if (DEBUG) {
      msg.append("\nalpha = ").append(alpha);
    }

    // init strong and weak eigenpairs
    strongEigenPairs = new ArrayList<EigenPair>();
    weakEigenPairs = new ArrayList<EigenPair>();

    // sort eigenpairs in decending order
    EigenPair[] sortedEigenPairs = SortedEigenPairs.sortDescending(eigenPairs);
    if (DEBUG) {
      msg.append("\nsortedEigenPairs " + Arrays.asList(sortedEigenPairs));
    }

    // determine sum of eigenvalues
    double totalSum = 0;
    for (EigenPair eigenPair : sortedEigenPairs) {
      totalSum += eigenPair.getEigenvalue();
    }
    if (DEBUG) {
      msg.append("\n totalSum = ").append(totalSum);
    }

    double currSum = 0;
    boolean found = false;
    for (EigenPair eigenPair : sortedEigenPairs) {
      currSum += eigenPair.getEigenvalue();
      if (currSum / totalSum >= alpha) {
        if (!found) {
          found = true;
          strongEigenPairs.add(eigenPair);
        }
        else {
          weakEigenPairs.add(eigenPair);
        }
      }
      else {
        strongEigenPairs.add(eigenPair);
      }
    }
  }

  /**
   * @return the strong eigenPairs
   * @see EigenPairFilter#getStrongEigenPairs()
   */
  public List<EigenPair> getStrongEigenPairs() {
    return strongEigenPairs;
  }

  /**
   * @see EigenPairFilter#getWeakEigenPairs() ()
   */
  public List<EigenPair> getWeakEigenPairs() {
    return weakEigenPairs;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(PercentageEigenPairFilter.class.getName());
    description.append(" sorts the eigenpairs in decending order " +
                       "of their eigenvalues and returns the first eigenpairs, whose sum of" +
                       "eigenvalues is higher than the given percentage of the sum of all eigenvalues.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = optionHandler.grabOptions(args);

    //alpha
    if (optionHandler.isSet(ALPHA_P)) {
      String alphaString = optionHandler.getOptionValue(ALPHA_P);
      try {
        alpha = Double.parseDouble(alphaString);
        if (alpha <= 0 || alpha >= 1)
          throw new WrongParameterValueException(ALPHA_P, alphaString, ALPHA_D);
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(ALPHA_P, alphaString, ALPHA_D, e);
      }
    }
    else {
      alpha = DEFAULT_ALPHA;
    }

    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Sets the difference of the first array minus the second array as the
   * currently set parameter array.
   *
   * @param complete the complete array
   * @param part     an array that contains only elements of the first array
   */
  protected void setParameters(String[] complete, String[] part) {
    currentParameterArray = Util.difference(complete, part);
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getParameters()
   */
  public String[] getParameters() {
    String[] param = new String[currentParameterArray.length];
    System.arraycopy(currentParameterArray, 0, param, 0, currentParameterArray.length);
    return param;
  }

  /**
   * Returns the setting of the attributes of the parameterizable.
   *
   * @return the setting of the attributes of the parameterizable
   */
  public List<AttributeSettings> getAttributeSettings() {
List<AttributeSettings> attributeSettings = new ArrayList<AttributeSettings>();

    AttributeSettings mySettings = new AttributeSettings(this);
    mySettings.addSetting(ALPHA_P, Double.toString(alpha));
    attributeSettings.add(mySettings);

    return attributeSettings;  }


}
