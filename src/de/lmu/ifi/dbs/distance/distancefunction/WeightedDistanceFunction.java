package de.lmu.ifi.dbs.distance.distancefunction;

import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;

/**
 * Provides the Weighted distance for feature vectors.
 *
 * @author Elke Achtert 
 *         todo weight matrix as parameter
 */
public class WeightedDistanceFunction<O extends NumberVector<O, ? extends Number>> extends AbstractDoubleDistanceFunction<O> {

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
   *         instance of {@link de.lmu.ifi.dbs.distance.DoubleDistance DoubleDistance}.
   * @see DistanceFunction#distance(de.lmu.ifi.dbs.data.DatabaseObject, de.lmu.ifi.dbs.data.DatabaseObject)
   */
  public DoubleDistance distance(O o1, O o2) {
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

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   *      todo: parameters required
   */
  public String description() {
    return "Weighted distance for feature vectors. " +
           "No parameters required. " +
           "Pattern for defining a range: \"" + requiredInputPattern() + "\".";
  }
}
