package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.List;

public class NotAllAllowedToBeSetGlobalConstraint implements
		GlobalParameterConstraint {

	
	private List<Parameter> parameters;
	
	public NotAllAllowedToBeSetGlobalConstraint(List<Parameter> params){
		parameters = params;
	}
	
	public void test() throws ParameterException {
		
		boolean oneIsSet = false;

		for(Parameter p : parameters){
			
			if(!oneIsSet){
				oneIsSet = p.isSet();
			}
			else if(oneIsSet && p.isSet()){
				// TODO 
				throw new WrongParameterValueException("");
			}
		}
	}

}
