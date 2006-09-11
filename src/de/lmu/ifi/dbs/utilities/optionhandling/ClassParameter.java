package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;

import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.properties.PropertyName;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;

public class ClassParameter extends Parameter<String> {

	private Class restrictionClass;


	public ClassParameter(String name, String description,
			Class restrictionClass) {
		super(name, description);
		this.restrictionClass = restrictionClass;
	}

	public ClassParameter(String name, String description,
			Class restrictionClass, List<ParameterConstraint> constraints) {
		this(name, description,restrictionClass);
		addConstraintList(constraints);
	}

	public ClassParameter(String name, String description,
			Class restrictionClass, ParameterConstraint constraint) {
		this(name, description,restrictionClass);
		addConstraint(constraint);
	}

	public ClassParameter(String name, String description, String defaultValue,
			Class restrictionClass) {
		this(name, description,restrictionClass);
		this.defaultValue = defaultValue;
	}

	private JComponent createInputField() {

		JComboBox field = new JComboBox();
		// for(String prop :
		// Properties.KDD_FRAMEWORK_PROPERTIES.getProperty(PropertyName
		// .getOrCreatePropertyName(classType))){
		//			
		// System.out.println("property: "+prop);
		// }
		if (restrictionClass != null) {
			field
					.setModel(new DefaultComboBoxModel(
							Properties.KDD_FRAMEWORK_PROPERTIES
									.getProperty(PropertyName
											.getOrCreatePropertyName(restrictionClass))));
		} else {
			field.setModel(new DefaultComboBoxModel());
		}
		return field;
	}


	public JComponent getInputField() {
		if (inputField == null) {
			inputField = createInputField();
		}
		return inputField;
	}

	

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public boolean isSet() {
		return (value != null);
	}

	@Override
	public void setValue(String value) throws ParameterException {
		
		try {
			Util.instantiate(restrictionClass, value);
			
		}
		// TODO
		catch (UnableToComplyException e) {
			throw new WrongParameterValueException("");
		}
		
		// set value
		this.value = value;
	}

	

	@Override
	public void setValue() throws ParameterException {
		
		try {
			String selectedClass = (String) ((JComboBox) inputField).getSelectedItem();
			setValue(selectedClass);
		}
		// TODO
		catch (ClassCastException e) {
			throw new WrongParameterValueException("");
		}		
	}

}
