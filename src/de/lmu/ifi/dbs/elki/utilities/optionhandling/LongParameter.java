package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * Parameter class for a parameter specifying a long value.
 *
 * @author Steffi Wanka
 */
public class LongParameter extends NumberParameter<Long> {

    /**
     * Constructs a long parameter with the given optionID.
     *
     * @param optionID the unique OptionID for this parameter
     */
    public LongParameter(OptionID optionID) {
        super(optionID);
    }

    /**
     * Constructs a long parameter with the given optionID,
     * and parameter constraint.
     *
     * @param optionID   the unique OptionID for this parameter
     * @param constraint the parameter constraint for this long parameter
     */
    public LongParameter(OptionID optionID, ParameterConstraint<Number> constraint) {
        super(optionID, constraint);
    }

    /**
     * Constructs a long parameter with the given optionID,
     * parameter constraint and default value.
     *
     * @param optionID     the unique OptionID for this parameter
     * @param constraint   the parameter constraint for this long parameter
     * @param defaultValue the default value
     */
    public LongParameter(OptionID optionID, ParameterConstraint<Number> constraint, long defaultValue) {
        super(optionID, constraint, true, defaultValue);
    }

    /**
     * Constructs a long parameter with the given optionID,
     * parameter constraint and default value.
     *
     * @param optionID     the unique OptionID for this parameter
     * @param optional     specifies if this parameter is an optional parameter
     * @param defaultValue the default value
     */
    public LongParameter(OptionID optionID, boolean optional, long defaultValue) {
        super(optionID, null, optional, defaultValue);
    }

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
        }
        catch (ParameterException ex) {
            throw new WrongParameterValueException("Specified parameter value for parameter \"" + getName()
                + "\" breaches parameter constraint!\n" + ex.getMessage());
        }

        return true;
    }

    @Override
    public void setValue(String value) throws ParameterException {
        super.setValue(value);
        if (isValid(value)) {
            this.value = Long.parseLong(value);
        }
    }

    /**
     * Returns a string representation of the parameter's type.
     *
     * @return &quot;&lt;long&gt;&quot;
     */
    @Override
    public String getSyntax() {
        return "<long>";
    }

}
