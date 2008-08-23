package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;

/**
 * Provides the Weighted distance for feature vectors.
 *
 * @author Elke Achtert 
 *         todo weight matrix as parameter
 * @param <V> the type of NumberVector to compute the distances in between
 */
public class WeightedDistanceFunction<V extends NumberVector<V, ? >>
    extends AbstractDoubleDistanceFunction<V> {

  /**
   * The weight matrix.
   */
  private Matrix weightMatrix;

  /**
   * Provides the Weighted distance for feature vectors.
   */
  public WeightedDistanceFunction(Matrix weightMatrix) {
    super();
    this.weightMatrix = weightMatrix;
  }

  /**
   * Provides the Weighted distance for feature vectors.
   *
   * @return the Weighted distance between the given two vectors as an
   *         instance of {@link de.lmu.ifi.dbs.elki.distance.DoubleDistance DoubleDistance}.
   */
  public DoubleDistance distance(V o1, V o2) {
    if (o1.getDimensionality() != o2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of NumberVectors" +
                                         "\n  first argument: " + o1.toString() +
                                         "\n  second argument: " + o2.toString());
    }

    //noinspection unchecked
    Matrix o1_minus_o2 = o1.plus(o2.negativeVector()).getColumnVector();
    double sqrDist = o1_minus_o2.transpose().times(weightMatrix).times(o1_minus_o2).get(0, 0);

    if (sqrDist < 0 && Math.abs(sqrDist) < 0.000000001) {
      sqrDist = Math.abs(sqrDist);
    }

    return new DoubleDistance(Math.sqrt(sqrDist));
  }

  // todo: parameters required
  public String parameterDescription() {
    return "Weighted distance for feature vectors. " +
           "No parameters required. " +
           "Pattern for defining a range: \"" + requiredInputPattern() + "\".";
  }
}
