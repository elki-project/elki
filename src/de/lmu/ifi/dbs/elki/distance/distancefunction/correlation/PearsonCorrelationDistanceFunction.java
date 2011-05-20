package de.lmu.ifi.dbs.elki.distance.distancefunction.correlation;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

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
   * Static instance.
   */
  public static final PearsonCorrelationDistanceFunction STATIC = new PearsonCorrelationDistanceFunction();
  
  /**
   * Provides a PearsonCorrelationDistanceFunction.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
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
  public VectorFieldTypeInformation<? super NumberVector<?, ?>> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }

  @Override
  public String toString() {
    return "PearsonCorrelationDistance";
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
    protected PearsonCorrelationDistanceFunction makeInstance() {
      return PearsonCorrelationDistanceFunction.STATIC;
    }
  }
}