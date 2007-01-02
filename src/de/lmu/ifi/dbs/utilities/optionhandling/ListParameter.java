package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Abstract parameter class defining a parameter for a list of objects.
 * 
 * @author Steffi Wanka
 *
 * @param <T>
 */
public abstract class ListParameter<T> extends Parameter<List<T>,ListParameter> {

	/**
	 * A pattern defining a &quot,&quot.
	 */
	public static final Pattern SPLIT = Pattern.compile(",");

	/**
	 * A pattern defining a &quot:&quot.
	 */
	public static final Pattern VECTOR_SPLIT = Pattern.compile(":");

	/**
	 * Constructs a list parameter with the given name and description.
	 * 
	 * @param name the parameter name
	 * @param description the parameter description
	 */
	public ListParameter(String name, String description) {
		super(name, description);
	}

	/**
	 * Returns the size of this list parameter.
	 * 
	 * @return the size of this list parameter.
	 */
	public int getListSize(){
    if (this.value == null) return 0;
    return this.value.size();
	}
	
	/**
	 * Returns a string representation of this list parameter.
	 * The elements of this list parameters are given in &quot;[  ]&quot;, comma separated.
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		buffer.append("[");
		
		for(int i = 0; i<this.value.size(); i++){
			buffer.append(this.value.get(i).toString());
			if(i!=this.value.size()-1){
				buffer.append(",");
			}
		}		
		buffer.append("]");
		return buffer.toString();
	}
}
