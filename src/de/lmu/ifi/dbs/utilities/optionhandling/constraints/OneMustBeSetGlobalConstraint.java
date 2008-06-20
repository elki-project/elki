package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.List;

/**
 * Represents a global parameter constraint specifying that at least one
 * parameter value of a given list of parameters ({@link Parameter}) has to be
 * set.
 *
 * @author Steffi Wanka
 */
public class OneMustBeSetGlobalConstraint extends AbstractLoggable implements GlobalParameterConstraint {

    /**
     * List of parameters to be checked.
     */
    private List<Parameter<?, ?>> parameters;

    /**
     * Creates a One-Must-Be-Set global parameter constraint. That is, at least
     * one parameter value of the given list of parameters has to be set.
     *
     * @param params list of parameters
     */
    public OneMustBeSetGlobalConstraint(List<Parameter<?, ?>> params) {
        super(LoggingConfiguration.DEBUG);
        parameters = params;
    }

    /**
     * Checks if at least one parameter value of the list of parameters
     * specified is set. If not, a parameter exception is thrown.
     *
     * @see de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalParameterConstraint#test()
     */
    public void test() throws ParameterException {
        for (Parameter<?, ?> p : parameters) {
            if (p.isSet()) {
                return;
            }
        }
        throw new WrongParameterValueException("Global Parameter Constraint Error.\n" +
            "At least one of the parameters " + Util.optionsNamesToString(parameters) + " has to be set.");

	}

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalParameterConstraint#getDescription()
     */
    public String getDescription() {
        return "At least one of the parameters " + Util.optionsNamesToString(parameters) + " has to be set.";
    }
}
