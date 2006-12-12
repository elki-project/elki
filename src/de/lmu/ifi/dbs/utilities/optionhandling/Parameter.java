package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.List;
import java.util.Vector;

/**
 * Holds a parameter object, i.e. an option object requiring a value.
 * 
 * @author Steffi Wanka
 * @param <O>
 * @param <O>
 * 
 */
public abstract class Parameter<T> extends Option<T> {

	
	/**
	 * The default value of the parameter (may be null).
	 */
	protected T defaultValue;
	
	private boolean defaultValueTaken;

	protected boolean optionalParameter;

	protected List<ParameterConstraint> constraints;
	
	public Parameter(String name, String description){
		super(name, description);
		constraints = new Vector<ParameterConstraint>();
		optionalParameter = false;
		defaultValueTaken = false;
	}

	
	protected void addConstraint(ParameterConstraint constraint){
		constraints.add(constraint);
	}

	protected void addConstraintList(List<ParameterConstraint> constraints){
		this.constraints.addAll(constraints);
	}
	
	
	public void setDefaultValue(T defaultValue){
		this.defaultValue = defaultValue;
		defaultValueTaken = true;
	}
	
	public boolean hasDefaultValue(){
		return !(defaultValue == null);
	}
	
	// ich gehe davon aus, dass die default-werte korrekt sind!!
	//TODO sollen default-werte noch zusaetzlich ueberprueft werden??
	public void setDefaultValueToValue()throws ParameterException{
		this.value = defaultValue;
	}
	
	public void setOptional(boolean opt){
		this.optionalParameter = opt;
	}
	
	
	public boolean isOptional(){
		return this.optionalParameter;
	}
	
	public boolean tookDefaultValue(){
		return defaultValueTaken;
	}
	
	//TODO bin nicht sicher, ob dass funktioniert....
	public void checkConstraint(ParameterConstraint<T> cons) throws ParameterException{
		cons.test(getValue());
	}
}
