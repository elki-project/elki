package de.lmu.ifi.dbs.gui;

import javax.swing.JFrame;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;

public class IntegerEditor extends TextFieldParameterEditor {

	
	public static final int COLUMN_NUMBER = 7;
	
	public IntegerEditor(IntParameter option, JFrame owner, ParameterChangeListener l) {
		super(option, owner,l);
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
	private class MyDocumentListener implements DocumentListener {
	    String newline = "\n";
	 
	    public void insertUpdate(DocumentEvent e) {
	    	System.out.println(e.getDocument().toString());
	    	System.out.println(e.getType().toString());
	    	System.out.println(e.toString());
	    }
	    public void removeUpdate(DocumentEvent e) {
	    	System.out.println(e.getType().toString());
	        System.out.println(e.toString());
	    }
	    public void changedUpdate(DocumentEvent e) {
	    	System.out.println(e.getType().toString());
	       System.out.println(e.toString());
	    }

	   
	}
}
