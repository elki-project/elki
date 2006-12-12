package de.lmu.ifi.dbs.gui;

import java.awt.Color;

import javax.swing.*;
import javax.swing.border.Border;

import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

public class DoubleEditor extends ParameterEditor {

	private JTextField textField;

	public DoubleEditor(Option option, JFrame owner) {
		super(option, owner);
		createInputField();
	}

	public boolean isValid() {

		try {

			option.isValid(getValue());
		} catch (ParameterException e) {

			Border border = inputField.getBorder();

			textField.setBorder(BorderFactory.createLineBorder(Color.red));
			KDDDialog.showParameterMessage(owner, e.getMessage(), e);
			textField.setBorder(border);
			return false;

		}
		return true;
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
					((DoubleParameter) option).isValid(text);
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
					((DoubleParameter) option).isValid(text);
				} catch (ParameterException e) {

					Border border = textField.getBorder();
					textField.setBorder(BorderFactory.createLineBorder(Color.red));
					KDDDialog.showParameterMessage(owner, e.getMessage(), e);
					textField.setBorder(border);
					textField.setText(null);
					return;
				}
				setValue(text);
			}
		});

		textField.setColumns(10);

		inputField.add(textField);
		inputField.add(helpLabel);

	}

}
