package de.lmu.ifi.dbs.utilities.optionhandling;

public class EqualConstraint implements ParameterConstraint<Number> {

	private Number testNumber;
	
	public EqualConstraint(Number testNumber){
		this.testNumber = testNumber;
	}
	
	/*
	 * die uebergebene Number t muss gleich testNumber sein
	 * ansonsten wird eine Exception geworfen
	 * (non-Javadoc)
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.ParameterConstraint#test(java.lang.Object)
	 */
	public void test(Number t) throws ParameterException {
		
		if(t.doubleValue() != testNumber.doubleValue()){
			throw new WrongParameterValueException("");
		}
	}

}
