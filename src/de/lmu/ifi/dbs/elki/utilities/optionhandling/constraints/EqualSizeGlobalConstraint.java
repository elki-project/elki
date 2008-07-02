package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

import java.util.List;

/**
 * Global parameter constraint defining that a number of list parameters ({@link de.lmu.ifi.dbs.elki.utilities.optionhandling.ListParameter})
 * must have equal list sizes.
 *
 * @author Steffi Wanka
 */
public class EqualSizeGlobalConstraint extends AbstractLoggable implements GlobalParameterConstraint {

    /**
     * List parameters to be tested
     */
    private List<ListParameter<?>> parameters;

    /**
     * Creates a global parameter constraint for testing if a number of list
     * parameters have equal list sizes.
     *
     * @param params list parameters to be tested for equal list sizes
     */
    public EqualSizeGlobalConstraint(List<ListParameter<?>> params) {
        super(LoggingConfiguration.DEBUG);
        this.parameters = params;
    }

    /**
     * Checks if the list parameters have equal list sizes. If not, a parameter
     * exception is thrown.
     *
     * @see GlobalParameterConstraint#test()
     */
    public void test() throws ParameterException {
        boolean first = false;
        int constraintSize = -1;

        for (ListParameter<?> listParam : parameters) {
            if (listParam.isSet()) {
                if (!first) {
                    constraintSize = listParam.getListSize();
                    first = true;
                }
                else if (constraintSize != listParam.getListSize()) {
                    throw new WrongParameterValueException("Global constraint errror.\n" +
                        "The list parameters " + Util.optionsNamesToString(parameters) +
                        " must have equal list sizes.");
                }
            }
        }
    }

    /**
     * @see GlobalParameterConstraint#getDescription()
     */
    public String getDescription() {
        return "The list parameters " + Util.optionsNamesToString(parameters) +
            " must have equal list sizes.";
    }

}
