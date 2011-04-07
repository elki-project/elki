package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Provides a LP-Norm for FeatureVectors.
 * 
 * @author Arthur Zimek
 */
public class LPNormDistanceFunction extends AbstractPrimitiveDistanceFunction<NumberVector<?, ?>, DoubleDistance> implements RawDoubleDistance<NumberVector<?, ?>> {
  // TODO: implement SpatialDistanceFunction

  /**
   * OptionID for the "p" parameter
   */
  public static final OptionID P_ID = OptionID.getOrCreateOptionID("lpnorm.p", "the degree of the L-P-Norm (positive number)");

  /**
   * Keeps the currently set p.
   */
  private double p;

  /**
   * Constructor, internal version.
   * 
   * @param p Parameter p
   */
  public LPNormDistanceFunction(double p) {
    super();
    this.p = p;
  }

  /**
   * Returns the distance between the specified FeatureVectors as a LP-Norm for
   * the currently set p.
   * 
   * @param v1 first FeatureVector
   * @param v2 second FeatureVector
   * @return the distance between the specified FeatureVectors as a LP-Norm for
   *         the currently set p
   */
  @Override
  public DoubleDistance distance(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
    return new DoubleDistance(doubleDistance(v1, v2));
  }

  /**
   * Returns the distance between the specified FeatureVectors as a LP-Norm for
   * the currently set p.
   * 
   * @param v1 first FeatureVector
   * @param v2 second FeatureVector
   * @return the distance between the specified FeatureVectors as a LP-Norm for
   *         the currently set p
   */
  @Override
  public double doubleDistance(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
    if(v1.getDimensionality() != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString());
    }

    double sqrDist = 0;
    for(int i = 1; i <= v1.getDimensionality(); i++) {
      double manhattanI = Math.abs(v1.doubleValue(i) - v2.doubleValue(i));
      sqrDist += Math.pow(manhattanI, p);
    }
    return Math.pow(sqrDist, 1.0 / p);
  }

  @Override
  public Class<? super NumberVector<?, ?>> getInputDatatype() {
    return NumberVector.class;
  }

  /**
   * Get the functions p parameter.
   * 
   * @return p
   */
  public double getP() {
    return p;
  }

  @Override
  public boolean isMetric() {
    return (p >= 1);
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * The value of p.
     */
    protected double p = 0.0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter paramP = new DoubleParameter(P_ID, new GreaterConstraint(0));
      if(config.grab(paramP)) {
        p = paramP.getValue();
      }
    }

    @Override
    protected LPNormDistanceFunction makeInstance() {
      if(p == 1.0) {
        return ManhattanDistanceFunction.STATIC;
      }
      if(p == 2.0) {
        return EuclideanDistanceFunction.STATIC;
      }
      if(p == Double.POSITIVE_INFINITY) {
        return MaximumDistanceFunction.STATIC;
      }
      return new LPNormDistanceFunction(p);
    }
  }
}