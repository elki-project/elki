package de.lmu.ifi.dbs.gui;

import java.awt.Color;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.border.Border;

import de.lmu.ifi.dbs.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

public abstract class TextFieldParameterEditor extends ParameterEditor {

	protected JTextField textField;

	public TextFieldParameterEditor(Option<?> option, Window owner) {
		super(option, owner);
	}

	public boolean isValid() {

		if (((Parameter<?, ?>) option).isOptional() || getValue() == null) {
			return true;
		}

		try {

			option.isValid(getValue());
		} catch (ParameterException e) {

			Border border = textField.getBorder();

			textField.setBorder(BorderFactory.createLineBorder(Color.red));
			KDDDialog.showParameterMessage(owner, e.getMessage(), e);
			textField.setBorder(border);
			return false;

		}
		return true;
	}

}
