package de.lmu.ifi.dbs.gui;

import javax.swing.JFrame;

import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;

public class DoubleEditor extends TextFieldParameterEditor {

	public static final int COLUMN_NUMBER = 10;
	
	public DoubleEditor(DoubleParameter option, JFrame owner, ParameterChangeListener l) {
		super(option, owner,l);
	}

	@Override
	protected int getColumnNumber() {
		return COLUMN_NUMBER;
	}

}
