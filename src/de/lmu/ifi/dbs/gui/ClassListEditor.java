package de.lmu.ifi.dbs.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.border.Border;

import de.lmu.ifi.dbs.utilities.optionhandling.ClassListParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

public class ClassListEditor extends ParameterEditor {

	public ClassListEditor(Option option, JFrame owner) {
		super(option, owner);
		createInputField();
	}

	@Override
	protected void createInputField() {
		
		inputField = new JPanel();
		
		JTextField textField = new JTextField() {
			public void setText(String t) {
				String text = getText();
				if (text == null || text.equals("")) {
					setText(t);
				} else {
					setText(text.concat("," + t));
				}
			}
		};
		textField.setInputVerifier(new InputVerifier() {

			public boolean verify(JComponent input) {

				try {
					((ClassListParameter) option).isValid(value);
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
					((ClassListParameter) option).isValid(value);
				} catch (ParameterException e) {

					KDDDialog.showParameterMessage(owner, e.getMessage(), e);

				}
			}
		});

		textField.setColumns(30);
		inputField.add(textField);

		JComboBox classSelector = new JComboBox();
		classSelector.setModel(new DefaultComboBoxModel(((ClassListParameter) option)
				.getRestrictionClasses()));

		classSelector.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				JComboBox box = (JComboBox) e.getSource();
				String selClass = (String) box.getSelectedItem();
				((JTextField) inputField).setText(selClass);
				value = ((JTextField) inputField).getText();
			}
		});

		inputField.add(classSelector);
		inputField.add(helpLabel);

	}

	@Override
	public boolean isValid() {
		try {

			option.isValid(value);
		} catch (ParameterException e) {

			Border border = inputField.getBorder();

			inputField.setBorder(BorderFactory.createLineBorder(Color.red));
			KDDDialog.showParameterMessage(owner, e.getMessage(), e);
			inputField.setBorder(border);
			return false;

		}
		return true;
	}

}
