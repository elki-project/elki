package de.lmu.ifi.dbs.gui;

import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.lmu.ifi.dbs.utilities.optionhandling.DoubleListParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.Option;

public class DoubleListEditor extends TextFieldParameterEditor {
	

	public DoubleListEditor(Option<Double> option, JFrame owner) {
		super(option, owner);
//		createInputField();
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
}
