package de.lmu.ifi.dbs.gui;

import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.lmu.ifi.dbs.utilities.optionhandling.DoubleListParameter;

public class DoubleListEditor extends TextFieldParameterEditor {
	

	public DoubleListEditor(DoubleListParameter option, JFrame owner, ParameterChangeListener l) {
		super(option, owner,l);
	}

	@SuppressWarnings("serial")
	@Override
	protected void createInputField() {
		
		inputField = new JPanel();
		textField = new JTextField() {
			public void setText(String t) {
				String text = getText();
				if (text == null || text.equals("")) {
					super.setText(t);
				} else {
					super.setText(text.concat("," + t));
				}
			}
		};

		textField.setColumns(getColumnNumber());
		
		addInputVerifier();
		
		if(((DoubleListParameter)option).hasDefaultValue()){
			List<Double> defaultValues = ((DoubleListParameter)option).getDefaultValue();
			for(Double d : defaultValues){
				this.textField.setText(d.toString());
			}
			setValue(this.textField.getText());
		}
		inputField.add(textField);
		inputField.add(helpLabel);
	}

	@Override
	protected int getColumnNumber() {
		return StringEditor.COLUMN_NUMBER;
	}
}
