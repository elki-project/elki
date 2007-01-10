package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.List;

/**
 * Parameter constraint for testing if each vector in a list of vectors has the reqired dimension. 
 * 
 * @author Steffi Wanka
 *
 */
public class VectorListElementsSizeConstraint implements ParameterConstraint<VectorListParameter> {

	/**
	 * Dimension constraint
	 */
	private int dim;
	
	/**
	 * Constructs a vector list size constraint for testing if each vector of a vector list parameter
	 * has the given dimension.
	 * 
	 * @param constraintDim the constraint dimension 
	 */
	public VectorListElementsSizeConstraint(int constraintDim){
		this.dim = constraintDim;
	}
	
	/**
	 * Checks if each element of the list of vector has the required dimension. 
	 * If not so a ParameterException is thrown.
	 */
	public void test(VectorListParameter t) throws ParameterException {
		
		for(List<Double> vec : t.getValue()){
			if(vec.size() != dim){
				throw new WrongParameterValueException("Parameter Constraint Error\n" +
						"The vectors have not the required dimensionality of "+dim +"!");
			}
		}
	}

}
