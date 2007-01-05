package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.List;

public class AllVectorElementsSizeConstraint implements ParameterConstraint<VectorParameter> {

	
	private int dim;
	
	public AllVectorElementsSizeConstraint(int constraintDim){
		this.dim = constraintDim;
	}
	
	public void test(VectorParameter t) throws ParameterException {
		
		for(List<Double> vec : t.getValue()){
			if(vec.size() != dim){
				throw new WrongParameterValueException("Parameter Constraint Error\n" +
						"The vectors have not the required dimensionality of "+dim +"!");
			}
		}
	}

}
