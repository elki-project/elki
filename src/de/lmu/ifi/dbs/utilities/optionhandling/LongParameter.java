package de.lmu.ifi.dbs.utilities.optionhandling;

import de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint;

import java.util.List;

/**
 * Parameter class for a parameter specifying a long value.
 * 
 * @author Steffi Wanka
 *
 */
public class LongParameter extends NumberParameter<Long> {

	/**
	 * Constructs a long parameter with the given name and description.
	 * 
	 * @param name the parameter name
	 * @param description the parameter description
   * @deprecated
	 */
	@Deprecated
  public LongParameter(String name, String description) {
		super(name, description);
	}
  
  /**
   * Constructs a long parameter with the given OptionID.
   * 
   * @param optionID the unique OptionID for this parameter
   */
  public LongParameter(OptionID optionID) {
    super(optionID);
  }

	/**
	 * Constructs a long parameter with the given name, description, and parameter constraint.
	 * 
	 * @param name the parameter name
	 * @param description the parameter description
	 * @param cons the parameter constraint for this long parameter
   * @deprecated
	 */
	@Deprecated
  public LongParameter(String name, String description, ParameterConstraint<Number> cons) {
		this(name, description);
		addConstraint(cons);
	}
  
  /**
   * Constructs a long parameter with the given OptionID.
   * 
   * @param optionID the unique OptionID for this parameter
   * @param cons the parameter constraint for this long parameter
   */
  public LongParameter(OptionID optionID, ParameterConstraint<Number> cons) {
    this(optionID);
    addConstraint(cons);
  }

	/**
	 * Constructs a long parameter with the given name, description, and a list of parameter constraints.
	 * 
	 * @param name the parameter name
	 * @param description the parameter description
	 * @param cons a list of parameter constraints for this long parameter
   * @deprecated
	 */
	@Deprecated
  public LongParameter(String name, String description, List<ParameterConstraint<Number>> cons) {
		this(name, description);
		addConstraintList(cons);
	}
  
  /**
   * Constructs a long parameter with the given OptionID.
   * 
   * @param optionID the unique OptionID for this parameter
   * @param cons a list of parameter constraints for this long parameter
   */
  public LongParameter(OptionID optionID, List<ParameterConstraint<Number>> cons) {
    this(optionID);
    addConstraintList(cons);
  }


	/* (non-Javadoc)
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#isValid(java.lang.String)
	 */
	@Override
	public boolean isValid(String value) throws ParameterException {
		
		try {
			Long.parseLong(value);
		}

		catch (NumberFormatException e) {
			throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a long value, read: "
					+ value + "!\n");
		}

		try {
			for (ParameterConstraint<Number> cons : this.constraints) {

				cons.test(Long.parseLong(value));
			}
		} catch (ParameterException ex) {
			throw new WrongParameterValueException("Specified parameter value for parameter \"" + getName()
					+ "\" breaches parameter constraint!\n" + ex.getMessage());
		}

		return true;
	}

	/* (non-Javadoc)
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#setValue(java.lang.String)
	 */
	@Override
  public void setValue(String value) throws ParameterException {
		if (isValid(value)) {
			this.value = Long.parseLong(value);
		}
	}

}
