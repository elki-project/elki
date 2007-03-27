package de.lmu.ifi.dbs.gui;

import java.awt.Color;

import javax.swing.*;
import javax.swing.border.Border;

import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

public class DoubleEditor extends TextFieldParameterEditor {


	public static final int COLUMN_NUMBER = 10;
	
	public DoubleEditor(Option<Double> option, JFrame owner) {
		super(option, owner);
		createInputField();
	}

	@Override
	protected void createInputField() {

		inputField = new JPanel();
		textField = new JTextField();
		
		// check for default value
		if(((DoubleParameter)option).hasDefaultValue()){
			textField.setText(((DoubleParameter)option).getDefaultValue().toString());
			setValue(textField.getText());
		}
		textField.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent input) {
				return checkInput();
			}

			public boolean shouldYieldFocus(JComponent input) {
				return verify(input);
			}

			private boolean checkInput() {

				String text = textField.getText();
				if (text.equals("")) {
					return true;
				}

				try {
					((DoubleParameter) option).isValid(text);
				} catch (ParameterException e) {

					Border border = textField.getBorder();
					textField.setBorder(BorderFactory.createLineBorder(Color.red));
					KDDDialog.showParameterMessage(owner, e.getMessage(), e);
					textField.setBorder(border);
					textField.setText(null);
					return false;
				}
				setValue(text);
				try {
					DoubleEditor.this.option.setValue(text);
				} catch (ParameterException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return true;
			}
		});

		textField.setColumns(COLUMN_NUMBER);

		inputField.add(textField);
		inputField.add(helpLabel);

	}

}
