package de.lmu.ifi.dbs.gui;

import de.lmu.ifi.dbs.utilities.optionhandling.StringParameter;

import javax.swing.JFrame;

public class StringEditor extends TextFieldParameterEditor {

	public static final int COLUMN_NUMBER = 30;

	public StringEditor(StringParameter option, JFrame owner, ParameterChangeListener l) {
		super(option, owner, l);
	}

	@Override
	protected int getColumnNumber() {
		return COLUMN_NUMBER;
	}

}
