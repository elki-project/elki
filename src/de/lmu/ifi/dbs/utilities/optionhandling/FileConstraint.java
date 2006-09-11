package de.lmu.ifi.dbs.utilities.optionhandling;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class FileConstraint implements ParameterConstraint<String> {

	
	
	public void test(String t) throws ParameterException {
		
		try {
			new FileInputStream(t);
		} catch (FileNotFoundException e) {
			throw new WrongParameterValueException("");
		}

	}

}
