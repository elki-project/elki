package de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
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
public class PolynomialKernelFunction<O extends NumberVector<O, ?>> extends AbstractDoubleKernelFunction<O> {
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
   * Provides a polynomial Kernel function that computes a similarity between
   * the two feature vectors V1 and V2 defined by (V1^T*V2)^degree.
   */
  public PolynomialKernelFunction(Parameterization config) {
    super();
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
}
