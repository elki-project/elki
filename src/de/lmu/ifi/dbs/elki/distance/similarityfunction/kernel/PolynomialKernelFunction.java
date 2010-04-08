package de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Provides a polynomial Kernel function that computes a similarity between the
 * two feature vectors V1 and V2 defined by (V1^T*V2)^degree.
 * 
 * @author Simon Paradies
 * @param <O> vector type
 */
public class PolynomialKernelFunction<O extends NumberVector<O, ?>> extends AbstractKernelFunction<O, DoubleDistance> {
  /**
   * The default degree.
   */
  public static final double DEFAULT_DEGREE = 2.0;

  /**
   * OptionID for {@link #DEGREE_PARAM}
   */
  public static final OptionID DEGREE_ID = OptionID.getOrCreateOptionID("kernel.degree", "The degree of the polynomial kernel function. Default: " + DEFAULT_DEGREE);

  /**
   * 
   */
  private final DoubleParameter DEGREE_PARAM = new DoubleParameter(DEGREE_ID, DEFAULT_DEGREE);

  /**
   * Degree of the polynomial kernel function
   */
  private double degree = 0.0;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public PolynomialKernelFunction(Parameterization config) {
    super(DoubleDistance.FACTORY);
    // parameter degree
    if (config.grab(DEGREE_PARAM)) {
      degree = DEGREE_PARAM.getValue();
    }
  }

  /**
   * Provides the linear kernel similarity between the given two vectors.
   * 
   * @param o1 first vector
   * @param o2 second vector
   * @return the linear kernel similarity between the given two vectors as an
   *         instance of {@link DoubleDistance DoubleDistance}.
   * @see DistanceFunction#distance(de.lmu.ifi.dbs.elki.data.DatabaseObject,
   *      de.lmu.ifi.dbs.elki.data.DatabaseObject)
   */
  @Override
  public DoubleDistance similarity(O o1, O o2) {
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
  public DoubleDistance distance(final O fv1, final O fv2) {
    return new DoubleDistance(Math.sqrt(similarity(fv1, fv1).doubleValue() + similarity(fv2, fv2).doubleValue() - 2 * similarity(fv1, fv2).doubleValue()));
  }

  @Override
  public Class<? super O> getInputDatatype() {
    return NumberVector.class;
  }
}