package de.lmu.ifi.dbs.distance.distancefunction;

import de.lmu.ifi.dbs.data.FeatureVector;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.distance.DoubleDistance;

/**
 * CosineDistanceFunction for FeatureVectors.
 *
 * @author Arthur Zimek
 */
public class CosineDistanceFunction<V extends FeatureVector<V,? >> extends AbstractDoubleDistanceFunction<V> {

  /**
   * Provides a CosineDistanceFunction.
   */
  public CosineDistanceFunction() {
    super();
  }

  /**
   * Computes the cosine distance for two given FeatureVectors.
   *
   * @param o1 first FeatureVector
   * @param o2 second FeatureVector
   * @return the cosine distance for two given FeatureVectors o1 and o2
   * @see de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction#distance(de.lmu.ifi.dbs.data.DatabaseObject, de.lmu.ifi.dbs.data.DatabaseObject)
   */
  public DoubleDistance distance(V o1, V o2) {
    Matrix v1 = o1.getColumnVector();
    v1.normalizeColumns();
    Matrix v2 = o2.getColumnVector();
    v2.normalizeColumns();

    double d = 1 - v1.transpose().times(v2).get(0, 0);
    if (d < 0) d = 0;
    return new DoubleDistance(d);
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    return "Cosine distance for FeatureVectors. No parameters required. Pattern for defining a range: \"" + requiredInputPattern() + "\".";
  }

}
