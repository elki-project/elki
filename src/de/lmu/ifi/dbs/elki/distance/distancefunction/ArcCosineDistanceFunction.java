package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Cosine distance function for feature vectors.
 * 
 * The cosine distance is computed as the arcus from the cosine similarity
 * value, i.e., <code>arccos(&lt;v1,v2&gt;)</code>.
 * 
 * @author Arthur Zimek
 */
public class ArcCosineDistanceFunction extends AbstractCosineDistanceFunction {
  /**
   * Static instance
   */
  public static final ArcCosineDistanceFunction STATIC = new ArcCosineDistanceFunction();
  
  /**
   * Provides a CosineDistanceFunction.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public ArcCosineDistanceFunction() {
    super();
  }

  /**
   * Computes the cosine distance for two given feature vectors.
   * 
   * The cosine distance is computed as the arcus from the cosine similarity
   * value, i.e., <code>arccos(&lt;v1,v2&gt;)</code>.
   * 
   * @param v1 first feature vector
   * @param v2 second feature vector
   * @return the cosine distance for two given feature vectors v1 and v2
   */
  @Override
  public double doubleDistance(NumberVector<?,?> v1, NumberVector<?,?> v2) {
    double d = Math.acos(angle(v1, v2));
    if(d < 0) {
      d = 0;
    }
    return d;
  }

  @Override
  public String toString() {
    return "ArcCosineDistance";
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(obj == this) {
      return true;
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
    protected ArcCosineDistanceFunction makeInstance() {
      return ArcCosineDistanceFunction.STATIC;
    }
  }
}