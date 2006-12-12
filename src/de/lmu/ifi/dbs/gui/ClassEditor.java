package de.lmu.ifi.dbs.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.border.Border;

import de.lmu.ifi.dbs.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

public class ClassEditor extends ParameterEditor {

	private JComboBox comboField;

	public ClassEditor(Option option, JFrame owner) {
		super(option, owner);
		createInputField();
	}

	@Override
	protected void createInputField() {
		// check if possible Classes are Parameterizable
		boolean parameterizable = false;
		for (String cl : ((ClassParameter) option).getRestrictionClasses()) {

			try {
				if (Class.forName(cl).newInstance() instanceof Parameterizable) {
					parameterizable = true;
					break;
				}
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (parameterizable) {
			inputField = new ObjectEditor(((ClassParameter) option).getRestrictionClass(), owner);

			inputField.setInputVerifier(new InputVerifier() {

				public boolean verify(JComponent input) {
					System.out.println("verify input");
					return true;
				}

				public boolean shouldYieldFocus(JComponent input) {
					boolean inputOK = verify(input);
					checkInput();
					return inputOK;
				}

				public void checkInput() {

					// String text = textField.getText();
					// if (text.equals("")) {
					// return;
					// }
					//
					// try {
					// ((DoubleParameter) option).isValid(text);
					// } catch (ParameterException e) {
					//
					// Border border = textField.getBorder();
					// textField.setBorder(BorderFactory.createLineBorder(Color.red));
					// ErrorDialog.showParameterMessage(owner, e.getMessage(),
					// e);
					// textField.setBorder(border);
					// textField.setText(null);
					// return;
					// }
					// value = text;
				}

			});
		}

		else {

			inputField = new JPanel();
			comboField = new JComboBox();

			comboField.setModel(new DefaultComboBoxModel(((ClassParameter) option)
					.getRestrictionClasses()));

			setValue((String) comboField.getSelectedItem());
			comboField.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					setValue((String) comboField.getSelectedItem());
				}

			});
			inputField.add(comboField);

		}
		inputField.add(helpLabel);
		inputField.setInputVerifier(new InputVerifier() {

			public boolean verify(JComponent input) {

				try {
					((ClassParameter) option).isValid(getValue());
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

				try {
					((ClassParameter) option).isValid(getValue());
				} catch (ParameterException e) {

					KDDDialog.showParameterMessage(owner, e.getMessage(), e);
				}
			}
		});
	}

	@Override
	public boolean isValid() {

		if (inputField instanceof ObjectEditor) {

			setValue(((ObjectEditor) inputField).getEditObjectAsString());
			return true;
		}

		try {

			option.isValid(getValue());
		} catch (ParameterException e) {

			if (!(inputField instanceof ObjectEditor)) {

				Border border = inputField.getBorder();

				inputField.setBorder(BorderFactory.createLineBorder(Color.red));
				KDDDialog.showParameterMessage(owner, e.getMessage(), e);
				inputField.setBorder(border);
			} else {
				System.out.println(e.getMessage());

			}
			return false;

		}

		return true;
	}

}
