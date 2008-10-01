package de.lmu.ifi.dbs.elki.gui;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameter;

import java.text.ChoiceFormat;
import java.text.NumberFormat;

import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.text.NumberFormatter;

public class IntegerTestEditor extends ParameterEditor{

	
	protected JFormattedTextField textfield;
	
	public IntegerTestEditor(Option<?> option, JFrame owner, ParameterChangeListener l){
		super(option, owner,l);
	}
	
	@Override
	protected void createInputField() {
		inputField = new JPanel();


		textfield = new JFormattedTextField(createFormatter());
		textfield.setColumns(10);

		inputField.add(textfield);

		inputField.add(helpLabel);
	}

	@Override
  public boolean isOptional(){
		return ((Parameter<?, ?>)this.option).isOptional();
	}

	public NumberFormatter createFormatter(){
		NumberFormatter f = new NumberFormatter();
		double[] limits = {0,1};
		String[] formats = {"untere Schranke","obere Schranke"};
		NumberFormat nf = new ChoiceFormat(limits,formats);
		f.setFormat(nf);
		
		return f;
	}
	
//	@Override
//	public boolean isValid() {
//	return true;
//	}

}
