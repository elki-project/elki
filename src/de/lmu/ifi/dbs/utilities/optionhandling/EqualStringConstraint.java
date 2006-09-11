package de.lmu.ifi.dbs.utilities.optionhandling;

public class EqualStringConstraint implements ParameterConstraint<String> {

	
	private String testString;
	
	public EqualStringConstraint(String testString){
		this.testString = testString;
	}
	
	public void test(String t) throws ParameterException {
		if(!t.equals(testString)){
			throw new WrongParameterValueException("");
		}

	}

}
