package de.lmu.ifi.dbs.gui;

import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.Border;

import de.lmu.ifi.dbs.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

public class FlagEditor extends ParameterEditor {

	public FlagEditor(Option option, JFrame owner) {
		super(option, owner);
		createInputField();
	}

	@Override
	protected void createInputField() {
		
		inputField = new JPanel();
		JCheckBox check  = new JCheckBox();
		check.setSelected(((Flag) option).isSet());
		check.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {

				if (e.getStateChange() == ItemEvent.SELECTED) {
					value = Flag.SET;

				} else {
					value = Flag.NOT_SET;
				}
			}
		});
		inputField.add(check);
		inputField.add(helpLabel);
	}

	@Override
	public boolean isValid() {
		try {

			option.setValue(value);
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
