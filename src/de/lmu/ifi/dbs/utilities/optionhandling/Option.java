package de.lmu.ifi.dbs.utilities.optionhandling;

public abstract class Option {

	
	protected String name;
	
	protected String description;
	
	public Option(String name, String description){
		this.name = name;
		this.description = description;
	}
	
	
	public String getName(){
		return name;
	}
	
	public String getDescription(){
		return description;
	}
	
	public abstract boolean isSet();
	
	public abstract void setValue( String value);
	
	public abstract String getValue();
}
