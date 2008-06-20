package de.lmu.ifi.dbs.utilities.optionhandling;

import de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint;

import java.util.List;

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
        this(optionID);
        addConstraint(constraint);
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
        this(optionID, constraint);
        setOptional(optional);
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
        this(optionID, constraint);
        setDefaultValue(defaultValue);
    }

    /**
     * Constructs an integer parameter with the given name and description.
     *
     * @param name        the parameter name
     * @param description the parameter description
     * @deprecated
     */
    @Deprecated
    public IntParameter(String name, String description) {
        super(name, description);
    }

    /**
     * Constructs an integer parameter with the given name, description, and parameter constraint.
     *
     * @param name        the parameter name
     * @param description the parameter description
     * @param constraint  the constraint for this integer parameter
     * @deprecated
     */
    @Deprecated
    public IntParameter(String name, String description, ParameterConstraint<Number> constraint) {
        this(name, description);
        addConstraint(constraint);
    }

    /**
     * Constructs an integer parameter with the given name, description, parameter constraint,
     * and defualt value.
     *
     * @param name         the parameter name
     * @param description  the parameter description
     * @param constraint   the constraint for this integer parameter
     * @param defaultValue the default value
     * @deprecated
     */
    @Deprecated
    public IntParameter(String name, String description,
                        ParameterConstraint<Number> constraint, Integer defaultValue) {
        this(name, description, constraint);
        setDefaultValue(defaultValue);
    }

    /**
     * Constructs an integer parameter with the given name, description, parameter constraint,
     * and defualt value.
     *
     * @param name         the parameter name
     * @param description  the parameter description
     * @param constraint   the constraint for this integer parameter
     * @param defaultValue the default value
     * @param optional     specifies if this parameter is an optional parameter
     * @deprecated
     */
    @Deprecated
    public IntParameter(String name, String description,
                        ParameterConstraint<Number> constraint, Integer defaultValue, boolean optional) {
        this(name, description, constraint, defaultValue);
        setOptional(optional);
    }

    /**
     * Constructs an integer parameter with the given name, description, and list of parameter constraints.
     *
     * @param name        the parameter name
     * @param description the parameter description
     * @param constraints a list of parameter constraints for this integer parameter
     * @deprecated
     */
    @Deprecated
    public IntParameter(String name, String description, List<ParameterConstraint<Number>> constraints) {
        this(name, description);
        addConstraintList(constraints);
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#setValue(String)
     */
    public void setValue(String value) throws ParameterException {
        if (isValid(value)) {
            this.value = Integer.parseInt(value);
        }
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#isValid(java.lang.String)
     */
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
     * @see Parameter#getParameterType()
     */
    protected String getParameterType() {
        return "<int>";
    }
}
