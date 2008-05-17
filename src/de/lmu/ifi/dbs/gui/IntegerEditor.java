package de.lmu.ifi.dbs.gui;

import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;

import javax.swing.*;

// todo steffi comment all
public class IntegerEditor extends TextFieldParameterEditor {


    public static final int COLUMN_NUMBER = 7;

    public IntegerEditor(IntParameter option, JFrame owner, ParameterChangeListener l) {
        super(option, owner, l);
//		this.textField.getDocument().addDocumentListener(new MyDocumentListener());

    }

//	@Override
//	protected void createInputField() {
//		
//		inputField = new JPanel();
//		
//		textField = new JTextField();
//
//		if(((IntParameter)option).hasDefaultValue()){
//			textField.setText(((IntParameter)option).getDefaultValue().toString());
//			setValue(textField.getText());
//		}
//
//		addInputVerifier();
////		textField.setInputVerifier(new InputVerifier() {
////			public boolean verify(JComponent input) {
////				return checkInput();
////			}
////
////			public boolean shouldYieldFocus(JComponent input) {
////				return verify(input);
////			}
////
////			private boolean checkInput() {
////
////				String text = textField.getText();
////				if (text.equals("")) {
////					return true;
////				}
////				try {
////					((IntParameter) option).isValid(text);
////				} catch (ParameterException e) {
////
////					Border border = textField.getBorder();
////					textField.setBorder(BorderFactory.createLineBorder(Color.red));
////					KDDDialog.showParameterMessage(owner, e.getMessage(), e);
////					textField.setBorder(border);
////					textField.setText(null);
////					return false;
////				}
////				setValue(text);
////				return true;
////			}
////		});
//
//		textField.setColumns(COLUMN_NUMBER);
//		
//
//		inputField.add(textField);
//		
//		inputField.add(helpLabel);
//	}

    @Override
    protected int getColumnNumber() {
        return COLUMN_NUMBER;
    }
}
