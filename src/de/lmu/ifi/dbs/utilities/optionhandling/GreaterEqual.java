package de.lmu.ifi.dbs.utilities.optionhandling;

public class GreaterEqual implements ParameterConstraint<Number> {

	private Number testNumber;
	
	public GreaterEqual(Number testNumber){
		this.testNumber = testNumber;
	}
	
	/**
	 * die uebergebene Number t muss groesser gleich als testNumber sein
	 * ansonsten wird eine Exception geworfen
	 * 
	 */
	public void test(Number t) throws ParameterException {
		
		if(t.doubleValue() < testNumber.doubleValue()){
			throw new WrongParameterValueException("");
		}
	}

}
