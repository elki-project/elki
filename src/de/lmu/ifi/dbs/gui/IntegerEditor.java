package de.lmu.ifi.dbs.gui;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.Option;

public class IntegerEditor extends TextFieldParameterEditor {

	
	public static final int COLUMN_NUMBER = 7;
	
	public IntegerEditor(Option<Integer> option, JFrame owner) {
		super(option, owner);
		createInputField();
	}

	@Override
	protected void createInputField() {
		
		inputField = new JPanel();
		
		textField = new JTextField();

		if(((IntParameter)option).hasDefaultValue()){
			textField.setText(((IntParameter)option).getDefaultValue().toString());
			setValue(textField.getText());
		}

		addInputVerifier();
//		textField.setInputVerifier(new InputVerifier() {
//			public boolean verify(JComponent input) {
//				return checkInput();
//			}
//
//			public boolean shouldYieldFocus(JComponent input) {
//				return verify(input);
//			}
//
//			private boolean checkInput() {
//
//				String text = textField.getText();
//				if (text.equals("")) {
//					return true;
//				}
//				try {
//					((IntParameter) option).isValid(text);
//				} catch (ParameterException e) {
//
//					Border border = textField.getBorder();
//					textField.setBorder(BorderFactory.createLineBorder(Color.red));
//					KDDDialog.showParameterMessage(owner, e.getMessage(), e);
//					textField.setBorder(border);
//					textField.setText(null);
//					return false;
//				}
//				setValue(text);
//				return true;
//			}
//		});

		textField.setColumns(COLUMN_NUMBER);
		

		inputField.add(textField);
		
		inputField.add(helpLabel);
	}

//	@Override
//	public boolean isValid() {
//		
//		if(((Parameter)option).isOptional() && getValue() == null){
//			return true;
//		}
//		
//		try {
//
//			option.isValid(getValue());
//		} catch (ParameterException e) {
//
//			Border border = textField.getBorder();
//
//			textField.setBorder(BorderFactory.createLineBorder(Color.red));
//			KDDDialog.showParameterMessage(owner, e.getMessage(), e);
//			textField.setBorder(border);
//			return false;
//
//		}
//		return true;
//	}

}
