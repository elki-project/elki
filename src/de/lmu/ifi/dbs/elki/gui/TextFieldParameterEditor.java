package de.lmu.ifi.dbs.elki.gui;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

import java.awt.Color;
import java.awt.Window;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

// todo steffi comment all
public abstract class TextFieldParameterEditor extends ParameterEditor {

	protected JTextField textField;


	public TextFieldParameterEditor(Option<?> option, Window owner, ParameterChangeListener l) {
		super(option, owner,l);
	}

	@Override
  public boolean isOptional(){
		return ((Parameter<?,?>)this.option).isOptional();
	}
	
//	public boolean isValid() {
//
//		if (getValue() == null) {
//			if (((Parameter<?, ?>) option).isOptional()) {
//				return true;
//			}
//			return false;
//		}
//
//		return true;
//	}

	@Override
  public String getValue() {
		if (((Parameter<?, ?>) this.option).isOptional() && this.value == null) {
			return "";
		}
		return value;
	}

	protected void addInputVerifier() {
		this.textField.setInputVerifier(new InputVerifier() {
			@Override
      public boolean verify(@SuppressWarnings("unused") JComponent input) {
				return checkInput();
			}

			@Override
      public boolean shouldYieldFocus(JComponent input) {
				return verify(input);
			}

			private boolean checkInput() {

				String text = textField.getText();
				if (text.equals("")) {
					setValue(null);
					return true;
				}
				try {
					((Parameter<?, ?>) option).isValid(text);
				} catch (ParameterException e) {

					showErrorMessage(e);
					return false;
				}
				setValue(text);
				return true;
			}
		});
	}

	protected void showErrorMessage(ParameterException e) {
		Border border = this.textField.getBorder();
		this.textField.setBorder(BorderFactory.createLineBorder(Color.red));
		KDDDialog.showParameterMessage(owner, e.getMessage(), e);
		this.textField.setBorder(border);
		this.textField.setText(null);
	}

	@Override
	protected void createInputField() {
		inputField = new JPanel();

		textField = new JTextField();

		if (((Parameter<?, ?>) option).hasDefaultValue()) {
			textField.setText(((Parameter<?, ?>) option).getDefaultValue().toString());
			setValue(textField.getText());
		}

		addInputVerifier();

		textField.setColumns(this.getColumnNumber());

		inputField.add(textField);

		inputField.add(helpLabel);
	}

	protected int getColumnNumber() { return 30; }

	public String[] parameterToValue() {

		if (((Parameter<?, ?>) option).isOptional() && this.value == null) {
			return new String[] {};
		}

		String[] paramToValue = new String[2];
		paramToValue[0] = "-" + option.getName();
		paramToValue[1] = getValue();

		return paramToValue;
	}

	@Override
  public void setValue(String value) {

		if (value == null) {
			((Parameter<?, ?>) option).reset();
//			this.firePropertyChangeEvent(new PropertyChangeEvent(this,option.getName(),"",value));
			this.fireParameterChangeEvent(new ParameterChangeEvent(this,option.getName(),"",""));
			return;
		}
		super.setValue(value);
	}

	public void addPropertyChangeListener(String option, PropertyChangeListener l) {
		this.textField.addPropertyChangeListener(option, l);
	}

//	public void firePropertyChangeEvent(PropertyChangeEvent e) {
//		for (PropertyChangeListener l : this.textField.getPropertyChangeListeners()) {
//			// l.propertyChange(e);
//		}
//	}
	
}
