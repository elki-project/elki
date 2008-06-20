package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.NumberParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Global parameter constraint specifying that parameters of a list of number
 * parameters ({@link NumberParameter}) are not allowed to have the same value.
 *
 * @author Steffi Wanka
 */
public class NotEqualValueGlobalConstraint<N extends Number> extends AbstractLoggable implements GlobalParameterConstraint {

    /**
     * List of number parameters to be checked.
     */
    private List<NumberParameter<N>> parameters;

    /**
     * Constructs a Not-Equal-Value global parameter constraint.
     * That is, the elements of
     * a list of number parameters are not allowed to have equal values.
     *
     * @param parameters list of number parameters to be tested
     */
    public NotEqualValueGlobalConstraint(List<NumberParameter<N>> parameters) {
        super(LoggingConfiguration.DEBUG);
        this.parameters = parameters;
    }

    /**
     * Checks if the elements of the list of number parameters do have different values.
     * If not, a parameter exception is thrown.
     *
     * @see GlobalParameterConstraint#test()
     */
    public void test() throws ParameterException {
        Set<Number> numbers = new HashSet<Number>();

        for (NumberParameter<N> param : parameters) {
            if (param.isSet()) {
                if (!numbers.add(param.getNumberValue())) {
                    throw new WrongParameterValueException("Global Parameter Constraint Error:\n" +
                        "Parameters " + Util.optionsNamesToString(parameters) +
                        " must have different values. Current values: " +
                        Util.parameterNamesAndValuesToString(parameters) + ".\n");
                }
            }
        }
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalParameterConstraint#getDescription()
     */
    public String getDescription() {
        return "Parameters " + Util.optionsNamesToString(parameters) + " must have different values.";
    }
}
