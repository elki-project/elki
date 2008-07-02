package de.lmu.ifi.dbs.elki.gui;

import java.util.EventObject;

public class ParameterChangeEvent extends EventObject{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String parameterName;
	
	private String oldValue;
	
	private String newValue;
	
	public ParameterChangeEvent(Object source, String parameterName, String oldValue, String newValue){
		super(source);
		this.parameterName = parameterName;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}

	public String getNewValue() {
		return newValue;
	}

	public String getOldValue() {
		return oldValue;
	}

	public String getParameterName() {
		return parameterName;
	}
}
