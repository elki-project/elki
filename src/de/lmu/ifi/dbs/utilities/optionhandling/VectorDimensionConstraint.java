package de.lmu.ifi.dbs.utilities.optionhandling;

public class VectorDimensionConstraint implements GlobalParameterConstraint {

	private VectorParameter vectors;
	
	private IntParameter dim;
	
	public VectorDimensionConstraint(VectorParameter v, IntParameter i){
		vectors = v;
		dim = i;
	}
	
	
	public void test() throws ParameterException {
		
		int dim = this.dim.getValue();
		
		for(int size : vectors.vectorSizes()){
			if(size != dim){
				// TODO
				throw new WrongParameterValueException("");
			}
		}
	}

}
