package de.lmu.ifi.dbs.elki.distance.distancefunction.correlation;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractVectorDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.math.MathUtil;

/**
 * Pearson correlation distance function for feature vectors.
 * 
 * The Pearson correlation distance is computed from the Pearson correlation
 * coefficient <code>r</code> as: <code>1-r</code>. Hence, possible values of
 * this distance are between 0 and 2.
 * 
 * The distance between two vectors will be low (near 0), if their attribute
 * values are dimension-wise strictly positively correlated, it will be high
 * (near 2), if their attribute values are dimension-wise strictly negatively
 * correlated. For Features with uncorrelated attributes, the distance value
 * will be intermediate (around 1).
 * 
 * This variation is for weighted dimensions.
 * 
 * @author Arthur Zimek
 * @author Erich Schubert
 */
public class WeightedPearsonCorrelationDistanceFunction extends AbstractVectorDoubleDistanceFunction {
  /**
   * Weights
   */
  private double[] weights;

  /**
   * Provides a PearsonCorrelationDistanceFunction.
   * 
   * @param weights Weights
   */
  public WeightedPearsonCorrelationDistanceFunction(double[] weights) {
    super();
    this.weights = weights;
  }

  /**
   * Computes the Pearson correlation distance for two given feature vectors.
   * 
   * The Pearson correlation distance is computed from the Pearson correlation
   * coefficient <code>r</code> as: <code>1-r</code>. Hence, possible values of
   * this distance are between 0 and 2.
   * 
   * @param v1 first feature vector
   * @param v2 second feature vector
   * @return the Pearson correlation distance for two given feature vectors v1
   *         and v2
   */
  @Override
  public double doubleDistance(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
    return 1 - MathUtil.weightedPearsonCorrelationCoefficient(v1, v2, weights);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null) {
      return false;
    }
    if (!this.getClass().equals(obj.getClass())) {
      return false;
    }
    return Arrays.equals(this.weights, ((WeightedPearsonCorrelationDistanceFunction)obj).weights);
  }
}