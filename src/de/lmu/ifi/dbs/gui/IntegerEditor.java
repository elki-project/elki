package de.lmu.ifi.dbs.gui;

import java.awt.Color;

import javax.swing.*;
import javax.swing.border.Border;

import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

public class IntegerEditor extends ParameterEditor {

	
	private JTextField textField;
	
	public IntegerEditor(Option option, JFrame owner) {
		super(option, owner);
		createInputField();
	}

	@Override
	protected void createInputField() {
		
		inputField = new JPanel();
		
		textField = new JTextField();

		textField.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent input) {
				JTextField tf = (JTextField) input;
				String text = tf.getText();
				if (text.equals("")) {
					return true;
				}

				try {
					((IntParameter) option).isValid(text);
				} catch (ParameterException e) {
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
					((IntParameter) option).isValid(text);
				} catch (ParameterException e) {

					Border border = textField.getBorder();
					textField.setBorder(BorderFactory.createLineBorder(Color.red));
					KDDDialog.showParameterMessage(owner, e.getMessage(), e);
					textField.setBorder(border);
					textField.setText(null);
					return;
				}
				value = text;
			}
		});

		textField.setColumns(5);

		inputField.add(textField);
		
		inputField.add(helpLabel);
	}

	@Override
	public boolean isValid() {
		try {

			option.isValid(value);
		} catch (ParameterException e) {

			Border border = textField.getBorder();

			textField.setBorder(BorderFactory.createLineBorder(Color.red));
			KDDDialog.showParameterMessage(owner, e.getMessage(), e);
			textField.setBorder(border);
			return false;

		}
		return true;
	}

}
