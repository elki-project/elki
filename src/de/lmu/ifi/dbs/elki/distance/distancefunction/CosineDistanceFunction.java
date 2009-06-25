package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;

/**
 * Cosine distance function for feature vectors.
 *
 * @author Arthur Zimek
 * @param <V> the type of FeatureVector to compute the distances in between
 */
public class CosineDistanceFunction<V extends FeatureVector<V,?>> extends AbstractDoubleDistanceFunction<V> {

  /**
   * Provides a CosineDistanceFunction.
   */
  public CosineDistanceFunction() {
    super();
  }

  /**
   * Computes the cosine distance for two given feature vectors.
   * 
   * The cosine distance is computed from the cosine similarity by <code>1-(cosine similarity)</code>.
   *
   * @param v1 first feature vector
   * @param v2 second feature vector
   * @return the cosine distance for two given feature vectors v1 and v2
   */
  public DoubleDistance distance(V v1, V v2) {
    Matrix m1 = v1.getColumnVector();
    m1.normalizeColumns();
    Matrix m2 = v2.getColumnVector();
    m2.normalizeColumns();

    double d = 1 - m1.transpose().times(m2).get(0, 0);
    if (d < 0) d = 0;
    return new DoubleDistance(d);
  }

  @Override
  public String shortDescription() {
    return "Cosine distance for feature vectors. No parameters required.\n";
  }

}
