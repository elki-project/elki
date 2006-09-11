package de.lmu.ifi.dbs.utilities.optionhandling;

public class GreaterConstraint implements ParameterConstraint<Number> {

	
	private Number testNumber;
	
	public GreaterConstraint(Number testNumber){
		this.testNumber = testNumber;
	}
	
	/*
	 * Wenn die uebergebene Number t kleiner gleich als testNumber ist
	 * wird eine Exception geworfen
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.ParameterConstraint#test(java.lang.Object)
	 */
	public void test(Number t) throws ParameterException {
	
		if(t.doubleValue()<=testNumber.doubleValue()){
			throw new WrongParameterValueException("");
		}
	}

}
