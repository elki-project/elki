package de.lmu.ifi.dbs.utilities.optionhandling;

import de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Paramter class for a parameter specifying a list of double values.
 * 
 * @author Steffi Wanka
 *
 */
public class DoubleListParameter extends ListParameter<Double> {

	/**
	 * Constructs a double list parameter with the given name and description.
	 * 
	 * @param name the parameter name
	 * @param description the parameter description
	 */
	public DoubleListParameter(String name, String description) {
		super(name, description);

	}

	/**
	 * Constructs a double list parameter with the given name, description, and parameter constraint.
	 * 
	 * @param name the parameter name
	 * @param description the parameter description
	 * @param con the parameter constraint of this double list parameter
	 */
	public DoubleListParameter(String name, String description, ParameterConstraint<List<Double>> con) {
		this(name, description);
		addConstraint(con);
	}

	@Override
	public void setValue(String value) throws ParameterException {

		if (isValid(value)) {
			String[] values = SPLIT.split(value);
			Vector<Double> doubleValue = new Vector<Double>();
			for (String val : values) {
				doubleValue.add(Double.parseDouble(val));
			}
			this.value = doubleValue;
		}
	}

	/* (non-Javadoc)
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#isValid(java.lang.String)
	 */
	public boolean isValid(String value) throws ParameterException {

		String[] values = SPLIT.split(value);
		if (values.length == 0) {

			throw new WrongParameterValueException("Wrong parameter format! Given list of double values for parameter \"" + getName()
					+ "\" is either empty or has the wrong format!\nParameter value required:\n" + getDescription());
		}

		// list for checking the parameter constraints
		List<Double> doubleList = new ArrayList<Double>();
		for (String val : values) {

			try {
				Double.parseDouble(val);
				doubleList.add(Double.parseDouble(val));
			} catch (NumberFormatException e) {
				throw new WrongParameterValueException("Wrong parameter format for parameter \"" + getName() + "\". Given parameter " + val
						+ " is no double!\n");
			}

		}

		for (ParameterConstraint<List<Double>> cons : this.constraints) {

			cons.test(doubleList);
		}

		return true;
	}

  /**
   * Sets the default value of this parameter.
   *
   * @param allListDefaultValue default value for all list elements of this parameter
   */
  public void setDefaultValue(double allListDefaultValue){
    for (int i = 0; i < defaultValue.size(); i++) {
      defaultValue.set(i, allListDefaultValue);
    }
  }

}
