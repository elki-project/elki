package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

import java.util.List;

/**
 * Parameter class for a parameter specifying a double value.
 *
 * @author Steffi Wanka
 */
public class DoubleParameter extends NumberParameter<Double> {

    /**
     * Constructs a double parameter with the given optionID.
     *
     * @param optionID the unique id of this parameter
     */
    public DoubleParameter(OptionID optionID) {
        super(optionID);
    }

    /**
     * Constructs a double parameter with the given optionID,
     * and parameter constraint.
     *
     * @param optionID   the unique id of this parameter
     * @param constraint the constraint of this parameter
     */
    public DoubleParameter(OptionID optionID, ParameterConstraint<Number> constraint) {
        super(optionID, constraint);
    }

    /**
     * Constructs a double parameter with the given optionID, parameter constraint,
     * and optional flag.
     *
     * @param optionID   the unique id of this parameter
     * @param constraint the constraint of this parameter
     * @param optional   specifies whether this parameter is an optional parameter
     */
    public DoubleParameter(OptionID optionID, ParameterConstraint<Number> constraint, boolean optional) {
        super(optionID, constraint, optional, null);
    }

    /**
     * Constructs a double parameter with the given optionID, parameter constraint,
     * and default value.
     *
     * @param optionID     the unique id of this parameter
     * @param constraint   the constraint of this parameter
     * @param defaultValue the default value for this parameter
     */
    public DoubleParameter(OptionID optionID, ParameterConstraint<Number> constraint, Double defaultValue) {
        super(optionID, constraint, false, defaultValue);
    }

    /**
     * Constructs a double parameter with the given optionID,
     * and parameter constraints.
     *
     * @param optionID    the unique optionID
     * @param constraints a list of parameter constraints for this double parameter
     */
    public DoubleParameter(OptionID optionID, List<ParameterConstraint<Number>> constraints) {
        super(optionID, constraints);
    }

    /**
     * Constructs a double parameter with the given optionID,
     * parameter constraints, and optional flag.
     *
     * @param optionID the unique optionID
     * @param cons     a list of parameter constraints for this double parameter
     * @param optional specifies whether this parameter is an optional parameter
     */
    public DoubleParameter(OptionID optionID, List<ParameterConstraint<Number>> cons, boolean optional) {
        this(optionID, cons);
        setOptional(optional);
    }

    /**
     * Constructs a double parameter with the given optionID,
     * parameter constraints, and default value.
     *
     * @param optionID     the unique optionID
     * @param cons         a list of parameter constraints for this double parameter
     * @param defaultValue the default value for this double parameter
     */
    public DoubleParameter(OptionID optionID, List<ParameterConstraint<Number>> cons, Double defaultValue) {
        this(optionID, cons);
        setDefaultValue(defaultValue);
    }

    /**
     * Constructs a double parameter with the given optionID
     * and default value.
     *
     * @param optionID     the unique optionID
     * @param defaultValue the default value for this double parameter
     */
    public DoubleParameter(OptionID optionID, Double defaultValue) {
        this(optionID);
        setDefaultValue(defaultValue);
    }

    @Override
    public void setValue(String value) throws ParameterException {
        if (isValid(value)) {
            this.value = Double.parseDouble(value);
        }
    }

    @Override
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
        }
        catch (ParameterException ex) {
            throw new WrongParameterValueException("Specified parameter value for parameter \""
                + getName() + "\" breaches parameter constraint!\n" + ex.getMessage());
        }

        return true;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this double parameter has the same
     *         value as the specified object, <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DoubleParameter)) {
            return false;
        }
        return this.value.equals(((DoubleParameter) obj).value);
    }

    /**
     * Returns a string representation of the parameter's type.
     *
     * @return &quot;&lt;double&gt;&quot;
     */
    @Override
    protected String getParameterType() {
        return "<double>";
    }
}
