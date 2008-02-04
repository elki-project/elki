package de.lmu.ifi.dbs.gui;

import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;

import javax.swing.JFrame;

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
