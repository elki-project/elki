package de.lmu.ifi.dbs.utilities.optionhandling;

public class ListSizeConstraint implements ParameterConstraint<ListParameter> {

	
	private int sizeConstraint;
	
	public ListSizeConstraint(int size){
		sizeConstraint = size;
		
	}
	
	public ListSizeConstraint(int size, String message){
		sizeConstraint = size;
	}
	
	public void test(ListParameter t) throws ParameterException {
		
		if(t.getListSize() != sizeConstraint){
			throw new WrongParameterValueException("Parameter Constraint Error!\n" +
					"List parameter '"+t.getName()+"' has not the requested size!" +
							"(current size: "+t.getListSize()+", requested size: "+sizeConstraint);
		}
	}
}
