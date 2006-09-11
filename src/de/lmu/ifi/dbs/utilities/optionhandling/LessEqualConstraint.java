package de.lmu.ifi.dbs.utilities.optionhandling;



public class LessEqualConstraint implements ParameterConstraint<Number> {

	private Number testNumber;
	
	public LessEqualConstraint(Number testNumber){
		this.testNumber = testNumber;
	}
	
	// die uebergebenen Number muss less equal der testNumber sein, 
	// ansonsten wird eine Exception geworfen
	public void test(Number t) throws ParameterException{
		if(t.doubleValue() > testNumber.doubleValue()){
			throw new WrongParameterValueException("");
		}
		
	}

}
