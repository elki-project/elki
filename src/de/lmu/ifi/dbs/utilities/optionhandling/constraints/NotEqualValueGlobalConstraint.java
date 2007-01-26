package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.dbs.utilities.optionhandling.NumberParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Global parameter constraint specifying that parameters of a list of number
 * parameters are not allowed to have the same value.
 * 
 * @author Steffi Wanka
 * 
 */
public class NotEqualValueGlobalConstraint implements GlobalParameterConstraint {

	/**
	 * List of parameters to be checked.
	 */
	private List<NumberParameter> parameters;

	/**
	 * Constructs a global parameter constraint specifying that the elements of
	 * a list of number parameters are not allowed to have equal values.
	 * 
	 * @param parameters
	 */
	public NotEqualValueGlobalConstraint(List<NumberParameter> parameters) {
		this.parameters = parameters;
	}

	/**
	 * Checks if the elements of a list of parameters do have different values.
	 * If not, a parameter exception is thrown.
	 */
	public void test() throws ParameterException {

		Set<Number> numbers = new HashSet<Number>();

		for (NumberParameter param : parameters) {
			if (param.isSet()) {

				if (!numbers.add(param.getNumberValue())) {
					throw new WrongParameterValueException("Global Parameter Constraint Error: \n" + "Parameters " + names()
							+ " must have different values!\n");
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
