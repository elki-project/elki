package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * Parameter class for a parameter specifying an integer value.
 */
public class IntParameter extends NumberParameter<Integer> {

    /**
     * Constructs an integer parameter with the given optionID.
     *
     * @param optionID optionID the unique id of the option
     */
    public IntParameter(OptionID optionID) {
        super(optionID);
    }

    /**
     * Constructs an integer parameter with the given optionID,
     * and parameter constraint.
     *
     * @param optionID   optionID the unique id of the option
     * @param constraint the constraint for this integer parameter
     */
    public IntParameter(OptionID optionID, ParameterConstraint<Number> constraint) {
        super(optionID, constraint);
    }

    /**
     * Constructs an integer parameter with the given optionID,
     * parameter constraint, and optional flag.
     *
     * @param optionID   optionID the unique id of the option
     * @param constraint the constraint for this integer parameter
     * @param optional   specifies if this parameter is an optional parameter
     */
    public IntParameter(OptionID optionID, ParameterConstraint<Number> constraint, boolean optional) {
        super(optionID, constraint, optional, null);
    }

    /**
     * Constructs an integer parameter with the given optionID,
     * parameter constraint, and default value.
     *
     * @param optionID     optionID the unique id of the option
     * @param constraint   the constraint for this integer parameter
     * @param defaultValue the default value
     */
    public IntParameter(OptionID optionID, ParameterConstraint<Number> constraint, Integer defaultValue) {
        super(optionID, constraint, false, defaultValue);
    }

    // FIXME: IntParameter and DoubleParameter have different APIs.
    // DoubleParameter takes (optionID, default), IntParameter takes (optionID, null, default)

    @Override
    public void setValue(String value) throws ParameterException {
        if (isValid(value)) {
            this.value = Integer.parseInt(value);
        }
    }

    @Override
    public boolean isValid(String value) throws ParameterException {
        try {
            Integer.parseInt(value);
        }
        catch (NumberFormatException e) {
            throw new WrongParameterValueException("Wrong parameter format. Parameter \""
                + getName() + "\" requires an integer value. (given value: " + value + ")\n");
        }

        try {
            for (ParameterConstraint<Number> cons : this.constraints) {
                cons.test(Integer.parseInt(value));
            }
        }
        catch (ParameterException e) {
            throw new WrongParameterValueException("Specified parameter value for parameter \""
                + getName() + "\" breaches parameter constraint.\n" + e.getMessage());
        }
        return true;
    }

    /**
     * Returns a string representation of the parameter's type.
     *
     * @return &quot;&lt;int&gt;&quot;
     */
    @Override
    protected String getParameterType() {
        return "<int>";
    }
}
