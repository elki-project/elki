package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Provides a LP-Norm for FeatureVectors.
 * 
 * @author Arthur Zimek
 * @param <V> the type of FeatureVector to compute the distances in between
 * 
 *        TODO: implement SpatialDistanceFunction
 */
public class LPNormDistanceFunction<V extends NumberVector<V, ?>> extends AbstractDistanceFunction<V, DoubleDistance> implements RawDoubleDistance<V> {
  /**
   * OptionID for {@link #P_PARAM}
   */
  public static final OptionID P_ID = OptionID.getOrCreateOptionID("lpnorm.p", "the degree of the L-P-Norm (positive number)");

  /**
   * Keeps the currently set p.
   */
  private double p;

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param <V> Vector type
   * @param config Parameterization
   * @return Distance function
   */
  public static <V extends NumberVector<V, ?>> LPNormDistanceFunction<V> parameterize(Parameterization config) {
    final DoubleParameter P_PARAM = new DoubleParameter(P_ID, new GreaterConstraint(0));
    if(config.grab(P_PARAM)) {
      final double p = P_PARAM.getValue();
      if(p == 1.0) {
        return new ManhattanDistanceFunction<V>();
      }
      if(p == 2.0) {
        return new EuclideanDistanceFunction<V>();
      }
      if(p == Double.POSITIVE_INFINITY) {
        return new MaximumDistanceFunction<V>();
      }
      return new LPNormDistanceFunction<V>(p);
    }
    return null;
  }

  /**
   * Constructor, internal version.
   * 
   * @param p Parameter p
   */
  public LPNormDistanceFunction(double p) {
    super(DoubleDistance.FACTORY);
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
  public DoubleDistance distance(V v1, V v2) {
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
  public double doubleDistance(V v1, V v2) {
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
  public Class<? super V> getInputDatatype() {
    return NumberVector.class;
  }
}