package de.lmu.ifi.dbs.distance.distancefunction;

import de.lmu.ifi.dbs.data.FeatureVector;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

/**
 * Provides a LP-Norm for FeatureVectors.
 *
 * @author Arthur Zimek 
 *         TODO: implement SpatialDistanceFunction
 */
public class LPNormDistanceFunction<V extends FeatureVector<V,N>,N extends Number> extends AbstractDoubleDistanceFunction<V> {
  /**
   * Parameter P.
   */
  public static final String P_P = "P";

  /**
   * Description for parameter P.
   */
  public static final String P_D = "the degree of the L-P-Norm (positive number)";

  /**
   * Keeps the curerntly set p.
   */
  private double p;

  /**
   * Provides a LP-Norm for FeatureVectors.
   */
  public LPNormDistanceFunction() {
    super();

    optionHandler.put(new DoubleParameter(P_P,P_D,new GreaterConstraint(0)));
  }

  /**
   * Returns the distance between the specified FeatureVectors as a LP-Norm
   * for the currently set p.
   *
   * @param o1 first FeatureVector
   * @param o2 second FeatureVector
   * @return the distance between the specified FeatureVectors as a LP-Norm
   *         for the currently set p
   * @see de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction#distance(de.lmu.ifi.dbs.data.DatabaseObject, de.lmu.ifi.dbs.data.DatabaseObject)
   */
  public DoubleDistance distance(V o1, V o2) {
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
    
    p = (Integer)optionHandler.getOptionValue(P_P);
   
    return remainingOptions;
  }
}
