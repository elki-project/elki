package experimentalcode.arthur;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDoubleDistanceFunction;

/**
 * Pearson correlation distance function for feature vectors.
 * 
 * The Pearson correlation distance is computed from the Pearson correlation coefficient <code>r</code> as: <code>1-r</code>.
 * 
 * @author Arthur Zimek
 * @param <V> the type of FeatureVector to compute the distances in between
 * @param <N> the type of Number of the attributes of vectors of type V
 */
public class PearsonCorrelationDistanceFunction<V extends FeatureVector<V,N>, N extends Number> extends AbstractDoubleDistanceFunction<V> {

  /**
   * Provides a PearsonCorrelationDistanceFunction.
   */
  public PearsonCorrelationDistanceFunction() {
    super();
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

  @Override
  public String shortDescription() {
    return "Pearson correlation distance for feature vectors. No parameters required.\n";
  }

}
