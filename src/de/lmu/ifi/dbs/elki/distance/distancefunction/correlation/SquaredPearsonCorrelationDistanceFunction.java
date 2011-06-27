package de.lmu.ifi.dbs.elki.distance.distancefunction.correlation;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractVectorDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Squared Pearson correlation distance function for feature vectors.
 * 
 * The squared Pearson correlation distance is computed from the Pearson
 * correlation coefficient <code>r</code> as: <code>1-r</code><sup>
 * <code>2</code></sup>. Hence, possible values of this distance are between 0
 * and 1.
 * 
 * The distance between two vectors will be low (near 0), if their attribute
 * values are dimension-wise strictly positively or negatively correlated. For
 * Features with uncorrelated attributes, the distance value will be high (near
 * 1).
 * 
 * @author Arthur Zimek
 */
public class SquaredPearsonCorrelationDistanceFunction extends AbstractVectorDoubleDistanceFunction {
  /**
   * Static instance.
   */
  public static final SquaredPearsonCorrelationDistanceFunction STATIC = new SquaredPearsonCorrelationDistanceFunction();

  /**
   * Provides a SquaredPearsonCorrelationDistanceFunction.
   * 
   * @deprecated use static instance!
   */
  @Deprecated
  public SquaredPearsonCorrelationDistanceFunction() {
    super();
  }

  /**
   * Computes the squared Pearson correlation distance for two given feature
   * vectors.
   * 
   * The squared Pearson correlation distance is computed from the Pearson
   * correlation coefficient <code>r</code> as: <code>1-r</code><sup>
   * <code>2</code></sup>. Hence, possible values of this distance are between 0
   * and 1.
   * 
   * @param v1 first feature vector
   * @param v2 second feature vector
   * @return the squared Pearson correlation distance for two given feature
   *         vectors v1 and v2
   */
  @Override
  public double doubleDistance(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
    final double pcc = MathUtil.pearsonCorrelationCoefficient(v1, v2);
    return 1 - pcc * pcc;
  }

  @Override
  public String toString() {
    return "SquaredPearsonCorrelationDistance";
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    return this.getClass().equals(obj.getClass());
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected SquaredPearsonCorrelationDistanceFunction makeInstance() {
      return SquaredPearsonCorrelationDistanceFunction.STATIC;
    }
  }
}