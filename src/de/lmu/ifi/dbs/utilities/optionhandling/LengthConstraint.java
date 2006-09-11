package de.lmu.ifi.dbs.utilities.optionhandling;

public class LengthConstraint implements GlobalParameterConstraint {

	private ListParameter list;
	
	private IntParameter length; 
	
	public LengthConstraint(ListParameter v, IntParameter i){
		list = v;
		length = i;
	}
	
	public void test() throws ParameterException {
		
		if(list.getListSize() != Integer.parseInt(length.getValue())){
			// TODO
			throw new WrongParameterValueException("");
		}
	}

}
