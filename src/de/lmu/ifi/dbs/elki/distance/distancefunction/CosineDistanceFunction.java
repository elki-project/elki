package de.lmu.ifi.dbs.elki.distance.distancefunction;


import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Cosine distance function for feature vectors.
 * 
 * The cosine distance is computed from the cosine similarity by
 * <code>1-(cosine similarity)</code>.
 * 
 * @author Arthur Zimek
 */
public class CosineDistanceFunction extends AbstractCosineDistanceFunction {
  /**
   * Static instance
   */
  public static final AbstractCosineDistanceFunction STATIC = new CosineDistanceFunction();

  /**
   * Provides a CosineDistanceFunction.
   * 
   * @deprecated Use static instance
   */
  @Deprecated
  public CosineDistanceFunction() {
    super();
  }

  /**
   * Computes the cosine distance for two given feature vectors.
   * 
   * The cosine distance is computed from the cosine similarity by
   * <code>1-(cosine similarity)</code>.
   * 
   * @param v1 first feature vector
   * @param v2 second feature vector
   * @return the cosine distance for two given feature vectors v1 and v2
   */
  @Override
  public double doubleDistance(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
    double d = 1 - angle(v1, v2);
    if(d < 0) {
      d = 0;
    }
    return d;
  }

  @Override
  public String toString() {
    return "CosineDistance";
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
    protected AbstractCosineDistanceFunction makeInstance() {
      return CosineDistanceFunction.STATIC;
    }
  }
}