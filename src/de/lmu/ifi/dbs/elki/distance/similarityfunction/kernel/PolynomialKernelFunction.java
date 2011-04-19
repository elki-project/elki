package de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.query.DistanceSimilarityQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceSimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.PrimitiveSimilarityFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Provides a polynomial Kernel function that computes a similarity between the
 * two feature vectors V1 and V2 defined by (V1^T*V2)^degree.
 * 
 * @author Simon Paradies
 */
public class PolynomialKernelFunction extends AbstractPrimitiveDistanceFunction<NumberVector<?, ?>, DoubleDistance> implements PrimitiveSimilarityFunction<NumberVector<?, ?>, DoubleDistance> {
  /**
   * The default degree.
   */
  public static final double DEFAULT_DEGREE = 2.0;

  /**
   * Degree parameter
   */
  public static final OptionID DEGREE_ID = OptionID.getOrCreateOptionID("kernel.degree", "The degree of the polynomial kernel function. Default: " + DEFAULT_DEGREE);

  /**
   * Degree of the polynomial kernel function
   */
  private double degree = 0.0;

  /**
   * Constructor.
   * 
   * @param degree Kernel degree
   */
  public PolynomialKernelFunction(double degree) {
    super();
    this.degree = degree;
  }

  /**
   * Provides the linear kernel similarity between the given two vectors.
   * 
   * @param o1 first vector
   * @param o2 second vector
   * @return the linear kernel similarity between the given two vectors as an
   *         instance of {@link DoubleDistance DoubleDistance}.
   */
  @Override
  public DoubleDistance similarity(NumberVector<?, ?> o1, NumberVector<?, ?> o2) {
    if(o1.getDimensionality() != o2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of Feature-Vectors" + "\n  first argument: " + o1.toString() + "\n  second argument: " + o2.toString());
    }

    double sim = 0;
    for(int i = 1; i <= o1.getDimensionality(); i++) {
      sim += o1.doubleValue(i) * o2.doubleValue(i);
    }
    return new DoubleDistance(Math.pow(sim, degree));
  }

  @Override
  public DoubleDistance distance(final NumberVector<?, ?> fv1, final NumberVector<?, ?> fv2) {
    return new DoubleDistance(Math.sqrt(similarity(fv1, fv1).doubleValue() + similarity(fv2, fv2).doubleValue() - 2 * similarity(fv1, fv2).doubleValue()));
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
  public <T extends NumberVector<?, ?>> DistanceSimilarityQuery<T, DoubleDistance> instantiate(Relation<T> database) {
    return new PrimitiveDistanceSimilarityQuery<T, DoubleDistance>(database, this, this);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    protected double degree = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter degreeP = new DoubleParameter(DEGREE_ID, DEFAULT_DEGREE);
      if(config.grab(degreeP)) {
        degree = degreeP.getValue();
      }
    }

    @Override
    protected PolynomialKernelFunction makeInstance() {
      return new PolynomialKernelFunction(degree);
    }
  }
}