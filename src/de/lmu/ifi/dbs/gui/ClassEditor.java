package de.lmu.ifi.dbs.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import de.lmu.ifi.dbs.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

public class ClassEditor extends ParameterEditor {

	private JComboBox comboField;

	private JTextField textField;
	
	public ClassEditor(ClassParameter<?> option, JFrame owner, ParameterChangeListener l) {
		super(option, owner,l);
//		createInputField();
	}

	
	public boolean isOptional(){
		return ((ClassParameter<?>)this.option).isOptional();
	}
	
	@Override
	protected void createInputField() {

		inputField = new JPanel();
		// System.out.println(Arrays.toString(((ClassParameter)option).getRestrictionClasses()));
		// System.out.println(((ClassParameter)option).getRestrictionClass().toString());
		String[] restrClasses = ((ClassParameter<?>) option).getRestrictionClasses();
		if (restrClasses.length == 0) {
			textField = new JTextField(20);
			inputField.add(textField);
		} else { // use combo field
			comboField = new JComboBox();
			comboField.setModel(new DefaultComboBoxModel(restrClasses));

			if (((ClassParameter<?>) option).hasDefaultValue()) {
				comboField.setSelectedItem(((ClassParameter<?>) option).getDefaultValue());
			}
			setValue((String) comboField.getSelectedItem());
			comboField.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					setValue((String) comboField.getSelectedItem());
				}

			});
			inputField.add(comboField);
		}
		

		// }
		inputField.add(helpLabel);
		inputField.setInputVerifier(new InputVerifier() {

			public boolean verify(JComponent input) {

				return checkInput();
			}

			public boolean shouldYieldFocus(JComponent input) {
				return verify(input);
			}

			private boolean checkInput() {

				try {
					((ClassParameter<?>) option).isValid(getValue());
				} catch (ParameterException e) {

					KDDDialog.showParameterMessage(owner, e.getMessage(), e);
					return false;
				}
				return true;
			}
		});
	}

//	@Override
//	public boolean isValid() {
//
//		if (((Parameter<?, ?>) option).isOptional() && getValue() == null) {
//			return true;
//		}
//
//		try {
//
//			option.isValid(getValue());
//		} catch (ParameterException e) {
//
//			KDDDialog.showParameterMessage(owner, e.getMessage(), e);
//			return false;
//		}
//
//		return true;
//	}

}
