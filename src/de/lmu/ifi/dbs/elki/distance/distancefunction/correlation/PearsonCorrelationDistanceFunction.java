package de.lmu.ifi.dbs.elki.distance.distancefunction.correlation;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDistanceFunction;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Pearson correlation distance function for feature vectors.
 * 
 * The Pearson correlation distance is computed from the Pearson correlation coefficient <code>r</code> as: <code>1-r</code>.
 * Hence, possible values of this distance are between 0 and 2.
 * 
 * The distance between two vectors will be low (near 0), if their attribute values are dimension-wise strictly positively correlated,
 * it will be high (near 2), if their attribute values are dimension-wise strictly negatively correlated.
 * For Features with uncorrelated attributes, the distance value will be intermediate (around 1).
 * 
 * @author Arthur Zimek
 * @param <V> the type of FeatureVector to compute the distances in between
 * @param <N> the type of Number of the attributes of vectors of type V
 */
public class PearsonCorrelationDistanceFunction<V extends NumberVector<V,N>, N extends Number> extends AbstractDistanceFunction<V, DoubleDistance> implements Parameterizable {
  /**
   * Provides a PearsonCorrelationDistanceFunction.
   */
  public PearsonCorrelationDistanceFunction() {
    super(new DoubleDistance());
  }

  /**
   * Computes the Pearson correlation distance for two given feature vectors.
   * 
   * The Pearson correlation distance is computed from the Pearson correlation coefficient <code>r</code> as: <code>1-r</code>.
   * Hence, possible values of this distance are between 0 and 2.
   *
   * @param v1 first feature vector
   * @param v2 second feature vector
   * @return the Pearson correlation distance for two given feature vectors v1 and v2
   */
  public DoubleDistance distance(V v1, V v2) {
    return new DoubleDistance(1 - MathUtil.pearsonCorrelationCoefficient(v1, v2));
  }
}
