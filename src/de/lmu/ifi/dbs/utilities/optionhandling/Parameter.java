package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.List;
import java.util.Vector;

/**
 * Holds a parameter object, i.e. an option object requiring a value.
 * 
 * @author Steffi Wanka
 * 
 */
public abstract class Parameter<T> extends Option<T> {

	
	/**
	 * The default value of the parameter (may be null).
	 */
	protected T defaultValue;

	protected boolean optionalParameter;

	protected List<ParameterConstraint> constraints;
	
	public Parameter(String name, String description){
		super(name, description);
		constraints = new Vector<ParameterConstraint>();
		optionalParameter = false;
	}

	
	public Parameter(String name, String description, List<ParameterConstraint> constraints){
		super(name,description);
		addConstraintList(constraints);
	}
	
	public void addConstraint(ParameterConstraint constraint){
		constraints.add(constraint);
	}

	public void addConstraintList(List<ParameterConstraint> constraints){
		this.constraints.addAll(constraints);
	}
	
	
	public void setDefaultValue(T defaultValue){
		this.defaultValue = defaultValue;
	}
	
	public void setOptionalState(boolean opt){
		this.optionalParameter = opt;
	}
	
	
	public boolean isOptional(){
		return this.optionalParameter;
	}
	/**
	 * Constructs a parameter object with the given name and description. <p/>
	 * The value and default value are set to null, the type of the parameter is
	 * set to {@link Types#STRING}.
	 * 
	 * @param name
	 *            the name of the parameter.
	 * @param description
	 *            the description of the parameter.
	 */
//	public Parameter(String name, String description) {
//		super(name, description);
//		defaultValue = null;
//		parameterType = Types.STRING;
//		this.value = null;
//	}
//
//	/**
//	 * Constructs a parameter object with the given name, description, and type.
//	 * <p/> The value and default value are set to null.
//	 * 
//	 * @param name
//	 *            the name of the parameter.
//	 * @param description
//	 *            the description of the parameter.
//	 * @param pType
//	 *            the type of the parameter.
//	 */
//	public Parameter(String name, String description, Types pType) {
//		super(name, description);
//		defaultValue = null;
//		parameterType = pType;
//		value = null;
//	}
//
//	/**
//	 * Constructs a parameter object with the given name, description, default
//	 * value, and type. <p/> The value of the parameter is set to null.
//	 * 
//	 * @param name
//	 *            the name of the parameter.
//	 * @param description
//	 *            the description of the parameter.
//	 * @param defaultValue
//	 *            the default value of the parameter.
//	 * @param type
//	 *            the type of the parameter.
//	 */
//	public Parameter(String name, String description, String defaultValue,
//			Types type) {
//		super(name, description);
//		this.defaultValue = defaultValue;
//		this.parameterType = type;
//		this.value = null;
//	}
//
//	
//	public Parameter(String name, String description, ParamType type){
//		
//		super(name,description);
//		this.type = type;
//	}
//	
//	/**
//	 * Returns the type of the parameter.
//	 * 
//	 * @return the type of the parameter.
//	 */
//	public Types getType() {
//		return parameterType;
//	}
//
//	/**
//	 * Returns the default value of the parameter.
//	 * 
//	 * @return the default value of the parameter.
//	 */
//	public String getDefaultValue() {
//		return defaultValue;
//	}
//
//	/**
//	 * Sets the type of the parameter.
//	 * 
//	 * @param k
//	 *            type of the parameter.
//	 */
//	public void setType(Types k) {
//		this.parameterType = k;
//	}
//
//	/**
//	 * Sets the default value for this parameter object.
//	 * 
//	 * @param defaultValue
//	 *            default value
//	 */
//	public void setDefaultValue(String defaultValue) {
//		this.defaultValue = defaultValue;
//	}
//
//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#isSet()
//	 */
//	public boolean isSet() {
//		return !(value == null);
//	}
//
//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#setValue(java.lang.String)
//	 */
////	public void setValue(String value) {
////		this.value = value;
////	}
//
//	/*
//	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#getValue()
//	 */
//	public String getValue() {
//		return value;
//	}
//
//	/**
//	 * Returns true if this parameter has a default value, false otherwise.
//	 * 
//	 * @return true if this parameter has a default value, false otherwise.
//	 */
//	public boolean hasDefaultValue() {
//		return !(defaultValue == null);
//	}
//
//	/**
//	 * Returns the description of this parameter. <p/> The description starts
//	 * with the type of the parameter: &lt{@link Types#STRING}&gt.
//	 * 
//	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#getDescription()
//	 */
//	@Override
//	public String getDescription() {
//		return "<" + parameterType.toString() + ">" + description;
//	}
//
//	public Component getInputField() {
//
//		JPanel base = new JPanel();
//		
//		
//		
//		if(type != null){
//			base.add(type.getInputField());
//		}
//		return base;
//	}
//	
//	public Component getTitleField(){
//		
//		return new JLabel(this.name);
//	}
//
//	
//
//	@Override
//	public void setValue(String value) throws ParameterException {
//		// TODO Auto-generated method stub
//		
//	}
}
