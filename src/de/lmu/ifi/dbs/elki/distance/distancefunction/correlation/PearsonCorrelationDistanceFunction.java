package de.lmu.ifi.dbs.elki.distance.distancefunction.correlation;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.MathUtil;

/**
 * Pearson correlation distance function for feature vectors.
 * 
 * The Pearson correlation distance is computed from the Pearson correlation coefficient <code>r</code> as: <code>1-r</code>.
 * Hence, possible values of this distance are between 0 and 2.
 * 
 * The distance between two vectors will be low (near 0), if their attribute values are dimension-wise strictly positively correlated,
 * it will be high (near 2), if their attribute values are dimension-wise strictly negatively correlated.
 * For Features with uncorrelated attributes, the distance value will be intermediate (around 1).
 * 
 * @author Arthur Zimek
 */
public class PearsonCorrelationDistanceFunction extends AbstractPrimitiveDistanceFunction<NumberVector<?,?>, DoubleDistance> {
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
  @Override
  public DoubleDistance distance(NumberVector<?,?> v1, NumberVector<?,?> v2) {
    return new DoubleDistance(1 - MathUtil.pearsonCorrelationCoefficient(v1, v2));
  }

  @Override
  public Class<? super NumberVector<?,?>> getInputDatatype() {
    return NumberVector.class;
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }
}