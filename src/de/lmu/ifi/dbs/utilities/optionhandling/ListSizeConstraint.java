package de.lmu.ifi.dbs.utilities.optionhandling;

/**
 *	Represents a list size constraint. The size of the list parameter to be tested
 *  has to be equal to the specified list size constraint.
 * 
 * @author Steffi Wanka
 *
 */
public class ListSizeConstraint implements ParameterConstraint<ListParameter> {

	/**
	 * The list size constraint
	 */
	private int sizeConstraint;
	
	/**
	 * Constructs a list size constraint with the given constraint size.
	 * 
	 * @param size the size constraint for the list parameter
	 */
	public ListSizeConstraint(int size){
		sizeConstraint = size;
		
	}
	
	
	/**
	 * Checks if the list parameter fulfills the size constraint. If not, a parameter 
	 * exception is thrown.
	 * 
	 * @throws A parameter exception, if the size of the list parameter given is not
	 * equal to the list size constraint specified.
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.ParameterConstraint#test(java.lang.Object)
	 */
	public void test(ListParameter t) throws ParameterException {
		
		if(t.getListSize() != sizeConstraint){
			throw new WrongParameterValueException("Parameter Constraint Error!\n" +
					"List parameter '"+t.getName()+"' has not the requested size!" +
							"(current size: "+t.getListSize()+", requested size: "+sizeConstraint);
		}
	}
}
