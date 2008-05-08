package de.lmu.ifi.dbs.distance.similarityfunction.kernel;

import de.lmu.ifi.dbs.data.FeatureVector;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

/**
 * Provides an experimental KernelDistanceFunction for RealVectors.
 * Currently only supports 2D data and x1^2 ~ x2 correlations.
 *
 * @author Simon Paradies
 */
public class FooKernelFunction<O extends FeatureVector> extends AbstractDoubleKernelFunction<O>{

	/**
	 * The default max_degree.
	 */
	public static final int DEFAULT_MAX_DEGREE = 2;
	/**
	 * Description for parameter max_degree.
	 */
	public static final String MAX_DEGREE_D = "The max degree of the"+ FooKernelFunction.class.getSimpleName()+". Default: "
		+ DEFAULT_MAX_DEGREE;
	/**
	 * Parameter for max_degree.
	 */
	public static final String MAX_DEGREE_P = "max_degree";
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
		IntParameter maxDeg = new IntParameter(MAX_DEGREE_P, MAX_DEGREE_D);
		maxDeg.setDefaultValue(DEFAULT_MAX_DEGREE);
		optionHandler.put(maxDeg);
	}

	/**
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
	 */
	@Override
	public String description() {
		return "Polynomial Kernel for FeatureVectors. Default max_degree is "+DEFAULT_MAX_DEGREE+".";
	}

	/**
	 * @throws ParameterException
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
	 */
	@Override
	public String[] setParameters(final String[] args) throws ParameterException{
		final String[] remainingParameters = super.setParameters(args);
		// max_degree
		max_degree = (Integer)optionHandler.getOptionValue(MAX_DEGREE_P);
		
		return remainingParameters;
	}


	/**
	 * Provides an experimental kernel similarity between the given two vectors.
	 * @param o1 first vector
	 * @param o2 second vector
	 * @return the experimental kernel similarity between the given two vectors as an
	 *         instance of {@link DoubleDistance DoubleDistance}.
	 * @see DistanceFunction#distance(de.lmu.ifi.dbs.data.DatabaseObject, de.lmu.ifi.dbs.data.DatabaseObject)
	 */
	public DoubleDistance similarity(final O o1, final O o2) {
		if (o1.getDimensionality() != o2.getDimensionality()) {
			throw new IllegalArgumentException("Different dimensionality of NumberVectors\n  first argument: " + o1.toString() + "\n  second argument: " + o2.toString());
		}
		double sim = 0.0;
		//iterate over differently powered dimensions
		for(int degree = 1; degree <= max_degree; degree++){
				sim += Math.pow(o1.getValue(degree).doubleValue() * o2.getValue(degree).doubleValue(), degree);
			}
		return new DoubleDistance(sim);
	}
}
