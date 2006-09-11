package de.lmu.ifi.dbs.utilities.optionhandling;

public class LessGlobalConstraint implements GlobalParameterConstraint {

	private NumberParameter first;
	
	private NumberParameter second;
	
	public LessGlobalConstraint(NumberParameter first, NumberParameter second){
		this.first = first;
		this.second = second;
	}
	
	
	public void test() throws ParameterException {
	
		if(first.getNumberValue().doubleValue() >= second.getNumberValue().doubleValue()){
			// TODO
			throw new WrongParameterValueException("");
		}
	
	}

}
