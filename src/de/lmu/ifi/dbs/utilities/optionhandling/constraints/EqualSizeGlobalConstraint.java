package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import java.util.List;

import de.lmu.ifi.dbs.utilities.optionhandling.*;

/**
 * Global parameter constraint defining that a number of ListParameters must have 
 * equal list sizes. 
 * 
 * @author Steffi Wanka
 *
 */
public class EqualSizeGlobalConstraint implements GlobalParameterConstraint {

	/**
	 * ListParameters to be tested
	 */
	private List<ListParameter> parameters;
	
	/**
	 * Creates a global parameter constraint for testing if a number of list parameters
	 * have equal list sizes. 
	 * 
	 * @param params list parameters to be tested for equal list sizes
	 */
	public EqualSizeGlobalConstraint(List<ListParameter> params){
		this.parameters = params;
	}
	
	/**
	 * Checks if the list parameters have equal list sizes. If not, a parameter exception
	 * is thrown.
	 */
	public void test() throws ParameterException{
		
		int first = 0;
		for(int i = 0; i < parameters.size(); i++){
			if(i == 0){
				first = parameters.get(i).getListSize();
				continue;
			}
			if(first != parameters.get(i).getListSize()){
				throw new WrongParameterValueException("Global constraint errror!\nThe list parameters "+paramsToString()+ " must" +
						" have equal list sizes!");
			}
		}
	}
	
	private String paramsToString(){
		StringBuffer buffer = new StringBuffer();
		buffer.append("[");
		int counter = 1;
		for(ListParameter p : parameters){
			buffer.append(p.getName());
			if(counter != parameters.size()){
				buffer.append(",");
			}
			counter++;
		}
		buffer.append("]");
		return buffer.toString();
	}

}
