package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.FeatureVector;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.util.List;

/**
 * Provides a LP-Norm for FeatureVectors.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 *         TODO: implement SpatialDistanceFunction
 */
public class LPNormDistanceFunction extends DoubleDistanceFunction<FeatureVector> {
  /**
   * Parameter P.
   */
  public static final String P_P = "P";

  /**
   * Description for parameter P.
   */
  public static final String P_D = "<double>the degree of the L-P-Norm (positive number)";

  /**
   * Keeps the curerntly set p.
   */
  private double p;

  /**
   * Provides a LP-Norm for FeatureVectors.
   */
  public LPNormDistanceFunction() {
    super();

    parameterToDescription.put(P_P + OptionHandler.EXPECTS_VALUE, P_D);
    optionHandler = new OptionHandler(parameterToDescription, LPNormDistanceFunction.class.getName());
  }

  /**
   * Returns the distance between the specified FeatureVectors as a LP-Norm
   * for the currently set p.
   *
   * @param o1 first FeatureVector
   * @param o2 second FeatureVector
   * @return the distance between the specified FeatureVectors as a LP-Norm
   *         for the currently set p
   * @see DistanceFunction#distance(de.lmu.ifi.dbs.data.DatabaseObject, de.lmu.ifi.dbs.data.DatabaseObject)
   */
  public DoubleDistance distance(FeatureVector o1, FeatureVector o2) {
    noDistanceComputations++;
    if (o1.getDimensionality() != o2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors\n  first argument: " + o1.toString() + "\n  second argument: " + o2.toString());
    }
    double sqrDist = 0;
    for (int i = 1; i <= o1.getDimensionality(); i++) {
      double manhattanI = Math.abs(o1.getValue(i).doubleValue() - o2.getValue(i).doubleValue());
      sqrDist += Math.pow(manhattanI, p);
    }
    return new DoubleDistance(Math.pow(sqrDist, 1.0 / p));
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(optionHandler.usage("LP-Norm for FeatureVectors.", false));
    description.append('\n');
    return description.toString();
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingOptions = super.setParameters(args);
    try {
      p = Double.parseDouble(optionHandler.getOptionValue(P_P));
      if (p <= 0) {
        throw new WrongParameterValueException(P_P, optionHandler.getOptionValue(P_P), P_D);
      }
    }
    catch (NumberFormatException e) {
      throw new WrongParameterValueException(P_P, optionHandler.getOptionValue(P_P), P_D, e);
    }
    setParameters(args, remainingOptions);
    return remainingOptions;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();
    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(P_P, Double.toString(p));
    return attributeSettings;
  }

}
