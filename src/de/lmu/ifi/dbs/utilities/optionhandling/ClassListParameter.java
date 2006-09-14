package de.lmu.ifi.dbs.utilities.optionhandling;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.properties.PropertyName;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;

public class ClassListParameter extends ListParameter<String> implements
		ActionListener {

	

	private Class restrictionClass;

	private JTextField classField;

	public ClassListParameter(String name, String description,
			Class restrictionClass) {
		super(name, description);
		this.restrictionClass = restrictionClass;

		inputField = createInputField();
	}

	private JComponent createInputField() {

		JPanel base = new JPanel();

		classField = new JTextField();
		classField.setColumns(30);
		base.add(classField);

		JComboBox classSelector = new JComboBox();
		if (restrictionClass != null) {
			classSelector
					.setModel(new DefaultComboBoxModel(
							Properties.KDD_FRAMEWORK_PROPERTIES
									.getProperty(PropertyName
											.getOrCreatePropertyName(restrictionClass))));
		} else {
			classSelector.setModel(new DefaultComboBoxModel());
		}

		classSelector.addActionListener(this);

		base.add(classSelector);
		return base;

	}

	@Override
	public Component getInputField() {
		return inputField;
	}

	@Override
	public String getValue() {
		StringBuffer buffer = new StringBuffer();

		for (int i = 0; i < value.size(); i++) {
			buffer.append(value.get(i));
			if (i != value.size() - 1) {
				buffer.append(",");
			}
		}

		return buffer.toString();
	}

	@Override
	public boolean isSet() {
		return (value != null);
	}

	@Override
	public void setValue(String value) throws ParameterException {

		String[] classes = SPLIT.split(value);
		if (classes.length == 0) {
			throw new WrongParameterValueException("");
		}
		for (String cl : classes) {
			try {
				Util.instantiate(restrictionClass, cl);
			} catch (UnableToComplyException e) {
				throw new WrongParameterValueException("");
			}
		}
		this.value = Arrays.asList(classes);

	}

	@Override
	public void setValue() throws ParameterException {

		setValue(classField.getText());
	}

	public void actionPerformed(ActionEvent e) {

		JComboBox box = (JComboBox) e.getSource();
		String selClass = (String) box.getSelectedItem();
		updateClassField(selClass);
	}

	private void updateClassField(String cl) {

		String text = classField.getText();

		if (text == null || text.equals("")) {
			classField.setText(cl);
		} else {
			classField.setText(text.concat("," + cl));
		}
	}

	@Override
	public int getListSize() {
		return this.value.size();
	}

}
