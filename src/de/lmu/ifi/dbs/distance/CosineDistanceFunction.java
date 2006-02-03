package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.FeatureVector;
import de.lmu.ifi.dbs.linearalgebra.Matrix;

/**
 * CosineDistanceFunction for FeatureVectors.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class CosineDistanceFunction<V extends FeatureVector> extends DoubleDistanceFunction<V> {

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
   * @see DistanceFunction#distance(O, O)
   */
  public DoubleDistance distance(V o1, V o2) {
    Matrix v1 = o1.getColumnVector();
    v1.normalizeCols();
    Matrix v2 = o2.getColumnVector();
    v2.normalizeCols();
    return new DoubleDistance(v1.transpose().times(v2).get(0, 0));
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    return "Cosine distance for FeatureVectors. No parameters required. Pattern for defining a range: \"" + requiredInputPattern() + "\".";
  }

}
