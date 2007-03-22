package de.lmu.ifi.dbs.gui;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

import de.lmu.ifi.dbs.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.StringParameter;

public class StringEditor extends TextFieldParameterEditor {

//	private JTextField textField;

	public StringEditor(Option<String> option, JFrame owner) {
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
//					 TODO at the moment StringEditor both for StringParameters
					// and PatternParameters!
					if (option instanceof StringParameter) {
						((StringParameter) option).isValid(text);
					} else if (option instanceof PatternParameter) {
						((PatternParameter) option).isValid(text);
					}
				} catch (ParameterException e) {

					Border border = inputField.getBorder();
					inputField.setBorder(BorderFactory
							.createLineBorder(Color.red));
					KDDDialog.showParameterMessage(owner, e.getMessage(), e);
					inputField.setBorder(border);
					textField.setText(null);
					return false;
				}
				setValue(text);
				return true;
			}
		});

		inputField.add(textField);
		inputField.add(helpLabel);
	}

//	@Override
//	public boolean isValid() {
//		try {
//
//			option.isValid(getValue());
//		} catch (ParameterException e) {
//
//			Border border = inputField.getBorder();
//
//			inputField.setBorder(BorderFactory.createLineBorder(Color.red));
//			KDDDialog.showParameterMessage(owner, e.getMessage(), e);
//			inputField.setBorder(border);
//			return false;
//
//		}
//		return true;
//	}
}
