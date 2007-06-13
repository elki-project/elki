package de.lmu.ifi.dbs.utilities.optionhandling;

import de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint;

import java.util.List;

/**
 * Parameter class for a parameter specifying a double value.
 * 
 * @author Steffi Wanka
 *
 */
public class DoubleParameter extends NumberParameter<Double,Number> {

	/**
	 * Constructs a double parameter with the given name and description
	 * 
	 * @param name the parameter name
	 * @param description the parameter description
	 */
	public DoubleParameter(String name, String description) {
		super(name, description);

	}

	/**
	 * Constructs a double parameter with the given name, description, and parameter constraint.
	 * 
	 * @param name the parameter name
	 * @param description the parameter description
	 * @param cons the constraint for this double parameter
	 */
	public DoubleParameter(String name, String description, ParameterConstraint<Number> cons) {
		this(name, description);
		addConstraint(cons);
	}

	/**
	 * Constructs a double parameter with the given name, description, and list of parameter constraints.
	 * 
	 * @param name the parameter name
	 * @param description the parameter description
	 * @param cons a list of parameter constraints for this double parameter
	 */
	public DoubleParameter(String name, String description, List<ParameterConstraint<Number>> cons) {
		this(name, description);
		addConstraintList(cons);
	}

	@Override
	public void setValue(String value) throws ParameterException {

		if (isValid(value)) {
			this.value = Double.parseDouble(value);
		}
	}

	/* (non-Javadoc)
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#isValid(java.lang.String)
	 */
	public boolean isValid(String value) throws ParameterException {
		try {
			Double.parseDouble(value);
		}

		catch (NumberFormatException e) {
			throw new WrongParameterValueException("Wrong parameter format! Parameter \""
					+ getName() + "\" requires a double value, read: " + value + "!\n");
		}

		try {
			for (ParameterConstraint<Number> cons : this.constraints) {

				cons.test(Double.parseDouble(value));
			}
		} catch (ParameterException ex) {
			throw new WrongParameterValueException("Specified parameter value for parameter \""
					+ getName() + "\" breaches parameter constraint!\n" + ex.getMessage());
		}

		return true;
	}
}
