package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.List;

public class OneMustBeSetGlobalConstraint implements GlobalParameterConstraint {

	
	private List<Parameter> parameters;
	
	public OneMustBeSetGlobalConstraint(List<Parameter> params){
		parameters = params;
	}
	
	public void test() throws ParameterException {

		for(int i = 0; i < parameters.size(); i++){
			if(parameters.get(i).isSet()){
				return;
			}
			if(!parameters.get(i).isSet() && i == parameters.size()-1){
				// TODO
				throw new WrongParameterValueException("");
			}
		}

	}

}
