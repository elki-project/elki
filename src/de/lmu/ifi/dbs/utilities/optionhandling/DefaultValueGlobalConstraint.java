package de.lmu.ifi.dbs.utilities.optionhandling;

public class DefaultValueGlobalConstraint implements GlobalParameterConstraint {

	private NumberParameter needsValue;
	
	private NumberParameter hasValue;
	
	public DefaultValueGlobalConstraint(NumberParameter needsValue, NumberParameter hasValue ){
		this.needsValue = needsValue;
		this.hasValue = hasValue;
	}
	
	public void test() throws ParameterException {
		

		if(!this.hasValue.isSet()){
			throw new WrongParameterValueException("Parameter "+hasValue.getName()+" is not set but has to be!");
		}
		
		if(!needsValue.isSet()){
			needsValue.setDefaultValue(hasValue.getValue());
			needsValue.setDefaultValueToValue();
		}
	}

}
