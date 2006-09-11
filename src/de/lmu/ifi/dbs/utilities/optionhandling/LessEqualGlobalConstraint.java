package de.lmu.ifi.dbs.utilities.optionhandling;

public class LessEqualGlobalConstraint implements GlobalParameterConstraint {

	private NumberParameter first;
	
	private NumberParameter second;
	
	public LessEqualGlobalConstraint(NumberParameter first, NumberParameter sec){
		this.first = first;
		this.second = sec;
	}
	
	// the first paramter has to be less equal than the second
	// otherwise throw a ParamterException
	public void test() throws ParameterException {
		
		if(first.getNumberValue().doubleValue() > second.getNumberValue().doubleValue()){
			//TODO 
			throw new WrongParameterValueException("");
		}
	}

}
