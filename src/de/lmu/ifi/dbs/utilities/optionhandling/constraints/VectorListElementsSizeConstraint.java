package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import java.util.List;

import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Parameter constraint for testing if each vector in a list of vectors has the reqired dimension. 
 * 
 * @author Steffi Wanka
 *
 */
public class VectorListElementsSizeConstraint<T> implements ParameterConstraint<List<List<T>>> {

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
	public void test(List<List<T>> t) throws ParameterException {
		
		for(List<T> vec : t){
			if(vec.size() != dim){
				throw new WrongParameterValueException("Parameter Constraint Error\n" +
						"The vectors have not the required dimensionality of "+dim +"!");
			}
		}
	}

}
