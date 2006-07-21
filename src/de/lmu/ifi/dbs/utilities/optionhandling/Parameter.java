package de.lmu.ifi.dbs.utilities.optionhandling;

public class Parameter extends Option {

	public static final int TEXT = 1;
	
	public static final int FILE = 2;
	
	public static final int NUMBER = 3;
	
	private int parameterType;
	
	private String defaultValue;
	
	private String value;
	
	public Parameter(String name, String description){
		super(name,description);
		defaultValue = null;
		parameterType = TEXT;
		this.value = null;
	}
	
	
	public Parameter(String name, String description, String defaultValue, int type){
		super(name,description);
		this.defaultValue = defaultValue;
		this.parameterType = type;
		this.value = null;
	}
	
	public int getType(){
		return parameterType;
	}
	
	public String getDefaultValue(){
		return defaultValue;
	}
	
	public void setType(int k){
		this.parameterType = k; 
	}
	
	/**
	 * Sets the default value for this parameter object. 
	 * @param defaultValue default value
	 */
	public void setDefaultValue(String defaultValue){
		this.defaultValue = defaultValue;
	}
	
	public boolean isSet(){
		return !(value == null);
	}
	
	public void setValue(String value){
		this.value = value;
	}
	
	public String getValue(){
		return this.value;
	}
	
	public boolean hasDefaultValue(){
		return !(defaultValue == null);
	}
}
