package de.lmu.ifi.dbs.gui;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

import de.lmu.ifi.dbs.utilities.optionhandling.Flag;

public class FlagEditor extends ParameterEditor {

	public FlagEditor(Flag option, JFrame owner, ParameterChangeListener l) {
		super(option, owner,l);
	}

	@Override
	protected void createInputField() {

		inputField = new JPanel();
		JCheckBox check = new JCheckBox();
		check.setSelected(((Flag) option).isSet());
		check.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {

				if (e.getStateChange() == ItemEvent.SELECTED) {
					setValue(Flag.SET);

				} else {
					setValue(Flag.NOT_SET);
				}
			}
		});
		if (check.isSelected()) {
			setValue(Flag.SET);
		} else {
			setValue(Flag.NOT_SET);
		}
		inputField.add(check);
		inputField.add(helpLabel);
	}
	
	public boolean isOptional(){
		return false;
	}

//	@Override
//	public boolean isValid() {
//		try {
//
//			option.isValid(getValue());
//		} catch (ParameterException e) {
//
//			Border border = inputField.getBorder();
//			inputField.setBorder(BorderFactory.createLineBorder(Color.red));
//			KDDDialog.showParameterMessage(owner, e.getMessage(), e);
//			inputField.setBorder(border);
//			return false;
//
//		}
//		return true;
//	}

	public String getDisplayableValue() {
		if (getValue().equals(Flag.SET)) {
			return "-" + option.getName();
		}
		return "";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.lmu.ifi.dbs.gui.ParameterEditor#parameterToValue()
	 */
	public String[] parameterToValue() {
		if (getValue().equals(Flag.SET)) {
			return new String[] { "-" + option.getName() };
		}
		return new String[] {};
	}
}
