package de.lmu.ifi.dbs.gui;

import java.awt.Color;

import javax.swing.*;
import javax.swing.border.Border;

import de.lmu.ifi.dbs.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.StringParameter;

public class StringEditor extends ParameterEditor {

	
	private JTextField textField;
	
	public StringEditor(Option option, JFrame owner) {
		super(option, owner);
		createInputField();
	}

	@Override
	protected void createInputField() {
		
		inputField = new JPanel();
		
		textField = new JTextField();
		textField.setColumns(30);

		textField.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent input) {
				JTextField tf = (JTextField) input;
				String text = tf.getText();
				if (text.equals("")) {
					return true;
				}

				try {
					((StringParameter) option).isValid(text);
				} catch (ParameterException ex) {
					return false;
				}
				return true;
			}

			public boolean shouldYieldFocus(JComponent input) {
				boolean inputOK = verify(input);
				checkInput();
				return inputOK;
			}

			public void checkInput() {

				String text = textField.getText();
				if (text.equals("")) {
					return;
				}

				try {
					((StringParameter) option).isValid(text);
				} catch (ParameterException e) {

					Border border = inputField.getBorder();
					inputField.setBorder(BorderFactory.createLineBorder(Color.red));
					KDDDialog.showParameterMessage(owner, e.getMessage(), e);
					inputField.setBorder(border);
					textField.setText(null);
					return;
				}
				setValue(text);
			}
		});

		inputField.add(textField);
		inputField.add(helpLabel);
	}

	@Override
	public boolean isValid() {
		try {

			option.isValid(getValue());
		} catch (ParameterException e) {

			Border border = inputField.getBorder();

			inputField.setBorder(BorderFactory.createLineBorder(Color.red));
			KDDDialog.showParameterMessage(owner, e.getMessage(), e);
			inputField.setBorder(border);
			return false;

		}
		return true;
	}
}
