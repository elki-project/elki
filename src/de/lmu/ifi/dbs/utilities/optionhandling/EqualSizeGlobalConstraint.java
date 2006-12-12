package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.List;

public class EqualSizeGlobalConstraint implements GlobalParameterConstraint {

	
	private List<ListParameter> parameters;
	
	public EqualSizeGlobalConstraint(List<ListParameter> params){
		this.parameters = params;
	}
	
	public void test() throws ParameterException{
		
		int first = 0;
		for(int i = 0; i < parameters.size(); i++){
			if(i == 0){
				first = parameters.get(i).getListSize();
				continue;
			}
			if(first != parameters.get(i).getListSize()){
				//TODO
				throw new WrongParameterValueException("");
			}
		}
	}

}
