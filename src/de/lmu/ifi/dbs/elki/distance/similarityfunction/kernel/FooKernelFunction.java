package de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Provides an experimental KernelDistanceFunction for NumberVectors. Currently
 * only supports 2D data and x1^2 ~ x2 correlations.
 * 
 * @author Simon Paradies
 * @param <O> vector type
 */
public class FooKernelFunction<O extends NumberVector<?, ?>> extends AbstractKernelFunction<O, DoubleDistance> {
  /**
   * The default max_degree.
   */
  public static final int DEFAULT_MAX_DEGREE = 2;

  /**
   * OptionID for {@link #MAX_DEGREE_PARAM}
   */
  public static final OptionID MAX_DEGREE_ID = OptionID.getOrCreateOptionID("fookernel.max_degree", "The max degree of the" + FooKernelFunction.class.getSimpleName() + ". Default: " + DEFAULT_MAX_DEGREE);

  /**
   * Parameter for the maximum degree
   */
  private final IntParameter MAX_DEGREE_PARAM = new IntParameter(MAX_DEGREE_ID, DEFAULT_MAX_DEGREE);

  /**
   * Degree of the polynomial kernel function
   */
  private int max_degree;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public FooKernelFunction(Parameterization config) {
    super(DoubleDistance.FACTORY);
    // parameter max_degree
    if(config.grab(MAX_DEGREE_PARAM)) {
      max_degree = MAX_DEGREE_PARAM.getValue();
    }
  }

  /*
   * @Override public String shortDescription() { return
   * "Polynomial Kernel for FeatureVectors. Default max_degree is " +
   * DEFAULT_MAX_DEGREE + ".\n"; }
   */

  /**
   * Provides an experimental kernel similarity between the given two vectors.
   * 
   * @param o1 first vector
   * @param o2 second vector
   * @return the experimental kernel similarity between the given two vectors as
   *         an instance of {@link DoubleDistance DoubleDistance}.
   */
  @Override
  public DoubleDistance similarity(final O o1, final O o2) {
    if(o1.getDimensionality() != o2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors\n  first argument: " + o1.toString() + "\n  second argument: " + o2.toString());
    }
    double sim = 0.0;
    // iterate over differently powered dimensions
    for(int degree = 1; degree <= max_degree; degree++) {
      sim += Math.pow(o1.doubleValue(degree) * o2.doubleValue(degree), degree);
    }
    return new DoubleDistance(sim);
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