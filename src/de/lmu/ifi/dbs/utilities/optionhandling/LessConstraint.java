package de.lmu.ifi.dbs.utilities.optionhandling;

public class LessConstraint implements ParameterConstraint<Number> {

	private Number testNumber;
	
	public LessConstraint(Number testNumber){
		this.testNumber = testNumber;
	}
	
	// die uebergebene Number muss kleiner als testNumber sein
	// ansonsten wird eine Exception geworfen
	public void test(Number t) throws ParameterException {
		
		if(t.doubleValue() >= testNumber.doubleValue()){
			throw new WrongParameterValueException("");
		}
	}

}
