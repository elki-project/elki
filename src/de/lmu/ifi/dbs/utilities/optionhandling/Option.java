package de.lmu.ifi.dbs.utilities.optionhandling;

import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * Abstract class for holding program arguments.
 * 
 * @author Steffi Wanka
 * 
 */
public abstract class Option<T> {

	/**
	 * The name of the option.,
	 */
	protected String name;

	/**
	 * The description of the option.
	 */
	protected String description;

	/**
	 * Specific GUI-component for specifying the parameter's value
	 */
	protected JComponent inputField;

	/**
	 * Specific GUI-component displaying the parameter's name
	 */
	protected JComponent titleField;

	protected T value;

	/**
	 * Sets the name and description of the option.
	 * 
	 * @param name
	 *            The name of the option.
	 * @param description
	 *            The description of the option.
	 */
	public Option(String name, String description) {
		this.name = name;
		this.description = description;
		titleField = new JLabel(name);
		titleField.setToolTipText(name);
	}

	/**
	 * Returns the name of the option.
	 * 
	 * @return the option's name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the description of the option.
	 * 
	 * @return the option's description.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns true if the value of the option is set, false otherwise.
	 * 
	 * @return true if the value of the option is set, false otherwise.
	 */
	public abstract boolean isSet();

	/**
	 * Sets the value of the option.
	 * 
	 * @param value
	 *            the option's value to be set
	 */
	public abstract void setValue(String value) throws ParameterException;

	public abstract void setValue() throws ParameterException;

	/**
	 * Returns the value of the option.
	 * 
	 * @return the option's value.
	 */
	public abstract String getValue();

	public abstract Component getInputField();

	public JComponent getTitleField(){
		return titleField;
	}

}
