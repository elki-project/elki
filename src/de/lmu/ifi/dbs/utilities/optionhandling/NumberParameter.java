package de.lmu.ifi.dbs.utilities.optionhandling;


public abstract class NumberParameter<T extends Number> extends Parameter<Number>{

	public NumberParameter(String name,String description){
		super(name,description);
	}
	
	public abstract Number getNumberValue();

}
