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
				throw new WrongParameterValueException("Global Parameter Constraint Error!\n" +
						"Only one of the parameters "+constraintsToString()+" is allowed to be set!");
			}
		}
	}
	
	private String constraintsToString(){
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
