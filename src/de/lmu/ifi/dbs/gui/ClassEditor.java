package de.lmu.ifi.dbs.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import de.lmu.ifi.dbs.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

public class ClassEditor extends ParameterEditor {

	private JComboBox comboField;

	public ClassEditor(Option<String> option, JFrame owner) {
		super(option, owner);
		// System.out.println("class editor, option: "+option.getName());
		createInputField();
	}

	@Override
	protected void createInputField() {

		inputField = new JPanel();
		comboField = new JComboBox();

		comboField.setModel(new DefaultComboBoxModel(((ClassParameter) option)
				.getRestrictionClasses()));

		if (((ClassParameter) option).hasDefaultValue()) {
			comboField.setSelectedItem(((ClassParameter) option)
					.getDefaultValue());
		}
		setValue((String) comboField.getSelectedItem());
		comboField.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				setValue((String) comboField.getSelectedItem());
			}

		});
		inputField.add(comboField);

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
					((ClassParameter) option).isValid(getValue());
				} catch (ParameterException e) {

					KDDDialog.showParameterMessage(owner, e.getMessage(), e);
					return false;
				}
				return true;
			}
		});
	}

	@Override
	public boolean isValid() {

		if (((Parameter<?, ?>) option).isOptional() || getValue() == null) {
			return true;
		}

		try {

			option.isValid(getValue());
		} catch (ParameterException e) {
//TODO
			System.out.println(e.getMessage());
			//			
			return false;
		}

		return true;
	}

}
