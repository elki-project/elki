package de.lmu.ifi.dbs.utilities.optionhandling;

import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;

public class ClassConstraint implements ParameterConstraint<String> {

	private Class restrictionClass;
	
	public ClassConstraint(Class restrictionClass){
		this.restrictionClass = restrictionClass;
	}
	
	
	public void test(String classLabel) throws ParameterException {
	
		try {
			Util.instantiate(restrictionClass, classLabel);
		} catch (UnableToComplyException e) {
			throw new WrongParameterValueException("");
		}
	}

}
