package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

//todo weight matrix as parameter

/**
 * Provides the Weighted distance for feature vectors.
 * 
 * @author Elke Achtert
 * 
 * @param <V> the type of FeatureVector to compute the distances in between
 */
public class WeightedDistanceFunction<V extends NumberVector<V, ?>> extends AbstractDoubleDistanceFunction<V> {
  /**
   * The weight matrix.
   */
  protected Matrix weightMatrix;

  /**
   * Provides the Weighted distance for feature vectors.
   * 
   * @param weightMatrix weight matrix
   */
  public WeightedDistanceFunction(Matrix weightMatrix) {
    super();
    this.weightMatrix = weightMatrix;
  }

  /**
   * Provides the Weighted distance for feature vectors.
   * 
   * @return the Weighted distance between the given two vectors as an instance
   *         of {@link de.lmu.ifi.dbs.elki.distance.DoubleDistance
   *         DoubleDistance}.
   */
  public DoubleDistance distance(V o1, V o2) {
    if(o1.getDimensionality() != o2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + o1.toString() + "\n  second argument: " + o2.toString());
    }

    Vector o1_minus_o2 = o1.minus(o2).getColumnVector();
    double dist = MathUtil.mahalanobisDistance(weightMatrix, o1_minus_o2);

    return new DoubleDistance(dist);
  }
}
