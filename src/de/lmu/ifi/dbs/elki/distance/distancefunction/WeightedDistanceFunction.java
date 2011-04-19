package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/**
 * Provides the Weighted distance for feature vectors.
 * 
 * @author Elke Achtert
 * 
 */
// TODO: Factory with parameterizable weight matrix?
public class WeightedDistanceFunction extends AbstractPrimitiveDistanceFunction<NumberVector<?, ?>, DoubleDistance> {
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
    assert (weightMatrix.getColumnDimensionality() == weightMatrix.getRowDimensionality());
  }

  /**
   * Provides the Weighted distance for feature vectors.
   * 
   * @return the Weighted distance between the given two vectors as an instance
   *         of
   *         {@link de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance
   *         DoubleDistance}.
   */
  @Override
  public DoubleDistance distance(NumberVector<?, ?> o1, NumberVector<?, ?> o2) {
    if(o1.getDimensionality() != o2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + o1.toString() + "\n  second argument: " + o2.toString());
    }

    Vector o1_minus_o2 = o1.getColumnVector().minus(o2.getColumnVector());
    double dist = MathUtil.mahalanobisDistance(weightMatrix, o1_minus_o2);

    return new DoubleDistance(dist);
  }

  @Override
  public VectorFieldTypeInformation<? super NumberVector<?, ?>> getInputTypeRestriction() {
    return VectorFieldTypeInformation.get(NumberVector.class, weightMatrix.getColumnDimensionality());
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }
}