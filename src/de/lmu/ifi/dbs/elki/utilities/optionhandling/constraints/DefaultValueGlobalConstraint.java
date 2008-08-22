package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Global parameter constraint for specifying the default value of a parameter dependent on
 * the parameter value of another parameter.
 *
 * @author Steffi Wanka
 */
public class DefaultValueGlobalConstraint<T extends Comparable<T>> extends AbstractLoggable implements GlobalParameterConstraint {

    /**
     * Parameter to be set.
     */
    private Parameter<T, ?> needsValue;

    /**
     * Parameter providing the value.
     */
    private Parameter<T, ?> hasValue;

    /**
     * Creates a global parameter constraint for specifying the default value of a parameter
     * dependent on the value of an another paramter.
     *
     * @param needsValue the parameter whose default value is to be set
     * @param hasValue   the parameter providing the value
     */
    public DefaultValueGlobalConstraint(Parameter<T, ?> needsValue, Parameter<T, ?> hasValue) {
        super(LoggingConfiguration.DEBUG);
        this.needsValue = needsValue;
        this.hasValue = hasValue;
    }

    /**
     * Checks if the parameter providing the default value is already set,
     * and if the two parameters have the same parameter type. If so, the
     * default value of one parameter is set as the default value of the other
     * parameter.
     * If not so, a parameter exception is thrown.
     *
     */
    public void test() throws ParameterException {
        if (!hasValue.isSet()) {
            throw new WrongParameterValueException("Parameter " + hasValue.getName() +
                " is currently not set but must be set!");
        }

        if (!hasValue.getClass().equals(needsValue.getClass())) {
            throw new WrongParameterValueException("Global Parameter Constraint Error!\n" +
                "Parameters " + hasValue.getName() + " and " + needsValue.getName() + "" +
                " must be of the same parameter type!");
        }

        if (!needsValue.isSet()) {
            needsValue.setDefaultValue(hasValue.getValue());
            needsValue.setDefaultValueToValue();
        }
    }

    public String getDescription() {
        return "If parameter " + needsValue.getName() + " is not specified, " +
            " its value will be set to the value of parameter " + hasValue.getName();
    }

}
