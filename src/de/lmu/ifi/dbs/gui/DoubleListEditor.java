package de.lmu.ifi.dbs.gui;

import java.awt.Color;

import javax.swing.*;
import javax.swing.border.Border;

import de.lmu.ifi.dbs.utilities.optionhandling.DoubleListParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

public class DoubleListEditor extends TextFieldParameterEditor {

//	private JTextField textField;
	
	public static final int COLUMN_NUMBER = 30;

	public DoubleListEditor(Option<Double> option, JFrame owner) {
		super(option, owner);
		createInputField();
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

		textField.setColumns(COLUMN_NUMBER);
		
		//TODO default value!
		
		textField.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent comp) {

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
					((DoubleListParameter) option).isValid(text);
				} catch (ParameterException e) {

					Border border = inputField.getBorder();
					inputField.setBorder(BorderFactory.createLineBorder(Color.red));
					KDDDialog.showParameterMessage(owner, e.getMessage(), e);
					inputField.setBorder(border);
					((JTextField) inputField).setText(null);
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
//			
//				Border border = inputField.getBorder();
//
//				inputField.setBorder(BorderFactory.createLineBorder(Color.red));
//				KDDDialog.showParameterMessage(owner, e.getMessage(), e);
//				inputField.setBorder(border);
//			
//			return false;
//
//		}
//
//		return true;
//	}

}
