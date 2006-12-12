package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotEqualValueGlobalConstraint implements GlobalParameterConstraint {

	private List<NumberParameter> parameters;
	
	public NotEqualValueGlobalConstraint(List<NumberParameter> parameters){
		this.parameters = parameters;
	}
	
	public void test() throws ParameterException {

		Set<Number> numbers = new HashSet<Number>();
		
		for(NumberParameter param : parameters){
			if(param.isSet()){
				if(!numbers.add(param.getNumberValue())){
					throw new WrongParameterValueException("Global Parameter Constraint Error: \n" +
							"Parameters "+names()+" must have different values!\n");
				}
			}
		}
	}
	
	private String names(){
		StringBuffer buffer = new StringBuffer();
		buffer.append("[");
		for(int i = 0; i < parameters.size(); i++){
			buffer.append(parameters.get(i).getName());
		
			if(i != parameters.size()-1){
				buffer.append(",");
			}
		}
		buffer.append("]");
		return buffer.toString();
	}

}
