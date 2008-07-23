package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

import java.util.List;

/**
 * Abstract class for defining a number parameter.
 *
 * @author Steffi Wanka
 * @param <T> the type of a possible value (i.e., the type of the option)
 */
public abstract class NumberParameter<T extends Number> extends Parameter<T, Number> {

    /**
     * Constructs a number parameter with the given optionID.
     *
     * @param optionID the unique id of this parameter
     */
    public NumberParameter(OptionID optionID) {
        super(optionID);
    }

    /**
     * Constructs a number parameter with the given optionID, and constraint.
     *
     * @param optionID   the unique id of this parameter
     * @param constraint the constraint of this parameter
     */
    public NumberParameter(OptionID optionID, ParameterConstraint<Number> constraint) {
        super(optionID, constraint);
    }

    /**
     * Constructs a number parameter with the given optionID, and constraint.
     *
     * @param optionID   the unique id of this parameter
     * @param constraints the constraints of this parameter, may be empty if there are no constraints
     */
    public NumberParameter(OptionID optionID, List<ParameterConstraint<Number>> constraints) {
        super(optionID, constraints);
    }

    /**
     * Constructs a number parameter with the given optionID, constraint, and optional flag.
     *
     * @param optionID     the unique id of this parameter
     * @param constraint   the constraint of this parameter
     * @param optional     specifies if this parameter is an optional parameter
     * @param defaultValue the default value for this parameter
     */
    public NumberParameter(OptionID optionID, ParameterConstraint<Number> constraint, boolean optional, T defaultValue) {
        super(optionID, constraint, optional, defaultValue);
    }

    /**
     * Constructs a number parameter with the given name and description.
     *
     * @param name        the parameter name
     * @param description the parameter description
     * @deprecated
     */
    @Deprecated
    public NumberParameter(String name, String description) {
        super(name, description);
    }

    /**
     * Returns the number value of the parameter.
     *
     * @return number value of the parameter.
     */
    public T getNumberValue() {
        return value;
    }
}
