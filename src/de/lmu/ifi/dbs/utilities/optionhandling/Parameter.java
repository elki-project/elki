package de.lmu.ifi.dbs.utilities.optionhandling;
/**
 * Holds a parameter object, i.e. an option object requiring a value. 
 * 
 * @author Steffi Wanka
 *
 */
public class Parameter extends Option {

	
	/**
	 * Enumeration providing different parameter types.
	 * 
	 * @author Steffi Wanka
	 *
	 */
	public enum Types{
		
		CLASS,
		FILE,
		INT,
		DOUBLE,
		STRING,
		DISTANCE_PATTERN;
		
		
		public String toString(){
			return this.name().toLowerCase();
		}
	}
	
	/**
	 * The type of the parameter.
	 */
	private Types parameterType;
	
	/**
	 * The default value of the parameter (may be null). 
	 */
	private String defaultValue;
	
	/**
	 * The value of the parameter.
	 */
	private String value;
	
	/**
	 * Constructs a parameter object with the given name and description.
	 * <p/> The value and default value are set to null, the type of the
	 * parameter is set to {@link Types#STRING}.
	 * 
	 * @param name the name of the parameter.
	 * @param description the description of the parameter.
	 */
	public Parameter(String name, String description){
		super(name,description);
		defaultValue = null;
		parameterType = Types.STRING;
		this.value = null;		
	}
	
	/**
	 * Constructs a parameter object with the given name, description, and type.
	 * <p/> The value and default value are set to null.
	 * 
	 * @param name the name of the parameter.
	 * @param description the description of the parameter.
	 * @param pType the type of the parameter.
	 */
	public Parameter(String name, String description, Types pType){
		super(name,description);
		defaultValue = null;
		parameterType = pType;
		value = null;
	}
	
	/**
	 * Constructs a parameter object with the given name, description, default value, and type.
	 * <p/> The value of the parameter is set to null.
	 * 
	 * @param name the name of the parameter. 
	 * @param description the description of the parameter.
	 * @param defaultValue the default value of the parameter.
	 * @param type the type of the parameter.
	 */
	public Parameter(String name, String description, String defaultValue, Types type){
		super(name,description);
		this.defaultValue = defaultValue;
		this.parameterType = type;
		this.value = null;
	}
	
	/**
	 * Returns the type of the parameter.
	 *  
	 * @return the type of the parameter.
	 */
	public Types getType(){
		return parameterType;
	}
	
	/**
	 * Returns the default value of the parameter.
	 * 
	 * @return the default value of the parameter.
	 */
	public String getDefaultValue(){
		return defaultValue;
	}
	
	/**
	 * Sets the type of the parameter.
	 * 
	 * @param k type of the parameter.
	 */
	public void setType(Types k){
		this.parameterType = k; 
	}
	
	/**
	 * Sets the default value for this parameter object. 
	 * @param defaultValue default value
	 */
	public void setDefaultValue(String defaultValue){
		this.defaultValue = defaultValue;
	}
	
	
	/* (non-Javadoc)
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#isSet()
	 */
	public boolean isSet(){
		return !(value == null);
	}
	
	/* (non-Javadoc)
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#setValue(java.lang.String)
	 */
	public void setValue(String value){
		this.value = value;
	}
	
	/*
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#getValue()
	 */
	public String getValue(){
		return this.value;
	}
	
	/**
	 * Returns true if this parameter has a default value, false otherwise.
	 * 
	 * @return true if this parameter has a default value, false otherwise.
	 */
	public boolean hasDefaultValue(){
		return !(defaultValue == null);
	}
	
	
	/**
	 * Returns the description of this parameter.
	 * <p/> The description starts with the type of the parameter:  &lt{@link Types#STRING}&gt.
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#getDescription()
	 */
	@Override
	public String getDescription(){
		return "<"+parameterType.toString()+">"+description;
	}
}
