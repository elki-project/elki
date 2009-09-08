package de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Provides an experimental KernelDistanceFunction for RealVectors.
 * Currently only supports 2D data and x1^2 ~ x2 correlations.
 *
 * @author Simon Paradies
 * @param <O> vector type
 */
public class FooKernelFunction<O extends FeatureVector<?,?>> extends AbstractDoubleKernelFunction<O>{

	/**
	 * The default max_degree.
	 */
	public static final int DEFAULT_MAX_DEGREE = 2;
	
  /**
   * OptionID for {@link #MAX_DEGREE_PARAM}
   */
  public static final OptionID MAX_DEGREE_ID = OptionID.getOrCreateOptionID("fookernel.max_degree",
      "The max degree of the"+ FooKernelFunction.class.getSimpleName()+". Default: "
      + DEFAULT_MAX_DEGREE);

  /**
   * Parameter for the maximum degree
   */
  private final IntParameter MAX_DEGREE_PARAM = new IntParameter(MAX_DEGREE_ID, null,
      DEFAULT_MAX_DEGREE);

  /**
	 * Degree of the polynomial kernel function
	 */
	private int max_degree;

	/**
	 * Provides a polynomial Kernel function that computes
	 * a similarity between the two vectors V1 and V2 definded by (V1^T*V2)^max_degree
	 */
	public FooKernelFunction() {
		super();
		//parameter max_degree
		addOption(MAX_DEGREE_PARAM);
	}

	@Override
	public String shortDescription() {
		return "Polynomial Kernel for FeatureVectors. Default max_degree is "+DEFAULT_MAX_DEGREE+".\n";
	}

	@Override
	public List<String> setParameters(List<String> args) throws ParameterException{
		List<String> remainingParameters = super.setParameters(args);
		// max_degree
		max_degree = MAX_DEGREE_PARAM.getValue();
		
		return remainingParameters;
	}


	/**
	 * Provides an experimental kernel similarity between the given two vectors.
	 * @param o1 first vector
	 * @param o2 second vector
	 * @return the experimental kernel similarity between the given two vectors as an
	 *         instance of {@link DoubleDistance DoubleDistance}.
	 */
	public DoubleDistance similarity(final O o1, final O o2) {
		if (o1.getDimensionality() != o2.getDimensionality()) {
			throw new IllegalArgumentException("Different dimensionality of FeatureVectors\n  first argument: " + o1.toString() + "\n  second argument: " + o2.toString());
		}
		double sim = 0.0;
		//iterate over differently powered dimensions
		for(int degree = 1; degree <= max_degree; degree++){
				sim += Math.pow(o1.getValue(degree).doubleValue() * o2.getValue(degree).doubleValue(), degree);
			}
		return new DoubleDistance(sim);
	}
}
