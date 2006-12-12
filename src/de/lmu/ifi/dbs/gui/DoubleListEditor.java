package de.lmu.ifi.dbs.gui;

import java.awt.Color;

import javax.swing.*;
import javax.swing.border.Border;

import de.lmu.ifi.dbs.utilities.optionhandling.DoubleListParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

public class DoubleListEditor extends ParameterEditor {

	private JTextField textField;

	public DoubleListEditor(Option option, JFrame owner) {
		super(option, owner);
		createInputField();
	}

	@Override
	protected void createInputField() {
		
		inputField = new JPanel();
		textField = new JTextField() {
			public void setText(String t) {
				String text = getText();
				if (text == null || text.equals("")) {
					setText(t);
				} else {
					setText(text.concat("," + t));
				}
			}
		};

		textField.setColumns(30);
		textField.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent comp) {

				String text = textField.getText();
				if (text.equals("")) {
					return true;
				}
				try {
					((DoubleListParameter) option).isValid(text);
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
					((DoubleListParameter) option).isValid(text);
				} catch (ParameterException e) {

					Border border = inputField.getBorder();
					inputField.setBorder(BorderFactory.createLineBorder(Color.red));
					KDDDialog.showParameterMessage(owner, e.getMessage(), e);
					inputField.setBorder(border);
					((JTextField) inputField).setText(null);
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

			if (inputField instanceof ObjectEditor) {

			} else {
				Border border = inputField.getBorder();

				inputField.setBorder(BorderFactory.createLineBorder(Color.red));
				KDDDialog.showParameterMessage(owner, e.getMessage(), e);
				inputField.setBorder(border);
			}
			return false;

		}

		return true;
	}

}
