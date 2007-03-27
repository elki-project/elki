package de.lmu.ifi.dbs.gui;

import java.awt.Color;
import java.awt.Window;

import javax.swing.*;
import javax.swing.border.Border;

import de.lmu.ifi.dbs.utilities.optionhandling.*;

public class TextFieldParameterEditor extends ParameterEditor {

	protected JTextField textField;

	public TextFieldParameterEditor(Option<?> option, Window owner) {
		super(option, owner);
		createInputField();
	}

	public boolean isValid() {

		if (getValue() == null) {
			if (((Parameter<?, ?>) option).isOptional()) {
				return true;
			}
			return false;
		}

		return true;

		// if (((Parameter<?, ?>) option).isOptional() && getValue() == null) {
		// return true;
		// }
		//
		// try {
		// option.isValid(getValue());
		// } catch (ParameterException e) {
		//
		// System.out.println("value: "+getValue());
		// e.printStackTrace();
		//			
		// showErrorMessage(e);
		// return false;
		// }
		// return true;
	}

	public String getValue() {
		if (((Parameter<?, ?>) this.option).isOptional() && this.value == null) {
			return "";
		}
		return value;
	}

	protected void addInputVerifier() {
		this.textField.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent input) {
				return checkInput();
			}

			public boolean shouldYieldFocus(JComponent input) {
				return verify(input);
			}

			private boolean checkInput() {

				String text = textField.getText();
				if (text.equals("")) {
					setValue(null);
					return true;
				}
				try {
					((Parameter<?, ?>) option).isValid(text);
				} catch (ParameterException e) {

					showErrorMessage(e);
					return false;
				}
				setValue(text);
				return true;
			}
		});
	}

	private void showErrorMessage(ParameterException e) {
		Border border = this.textField.getBorder();
		this.textField.setBorder(BorderFactory.createLineBorder(Color.red));
		KDDDialog.showParameterMessage(owner, e.getMessage(), e);
		this.textField.setBorder(border);
		this.textField.setText(null);
	}

	@Override
	protected void createInputField() {
		inputField = new JPanel();

		textField = new JTextField();

		if (((Parameter<?, ?>) option).hasDefaultValue()) {
			textField.setText(((Parameter<?, ?>) option).getDefaultValue().toString());
			setValue(textField.getText());
		}

		addInputVerifier();

		textField.setColumns(getColumnNumber());

		inputField.add(textField);

		inputField.add(helpLabel);
	}

	protected int getColumnNumber() {

		if (this.option instanceof IntParameter) {
			return TextFieldType.INT_TYP.getColumnNumber();
		}
		if (this.option instanceof DoubleParameter) {
			return TextFieldType.DOUBLE_TYP.getColumnNumber();
		}

		return TextFieldType.STRING_TYP.getColumnNumber();
	}

	enum TextFieldType {

		INT_TYP(7), DOUBLE_TYP(10), STRING_TYP(30);

		private final int columnNumber;

		TextFieldType(int columnNumber) {
			this.columnNumber = columnNumber;
		}

		public int getColumnNumber() {
			return columnNumber;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.lmu.ifi.dbs.gui.ParameterEditor#parameterToValue()
	 */
	public String[] parameterToValue() {

		if (((Parameter<?, ?>) option).isOptional() && this.value == null) {
			return new String[] {};
		}

		String[] paramToValue = new String[2];
		paramToValue[0] = "-" + option.getName();
		paramToValue[1] = getValue();

		return paramToValue;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.lmu.ifi.dbs.gui.ParameterEditor#setValue(java.lang.String)
	 */
	public void setValue(String value) {
		this.value = value;
		if(value==null){
			((Parameter<?,?>)option).reset();
			return;
		}
		try {
			option.setValue(value);
		} catch (ParameterException e) {
			KDDDialog.showParameterMessage(owner, e.getMessage(), e);
		}
	}
}
