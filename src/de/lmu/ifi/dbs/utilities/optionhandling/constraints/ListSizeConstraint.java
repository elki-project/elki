package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.optionhandling.ListParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.List;

/**
 * Represents a list-size parameter constraint. The size of the list parameter ({@link ListParameter}) to be tested
 * has to be equal to the specified list size constraint.
 *
 * @author Steffi Wanka
 */
public class ListSizeConstraint<T> extends AbstractLoggable implements ParameterConstraint<List<T>> {

    /**
     * The list size constraint.
     */
    private int sizeConstraint;

    /**
     * Constructs a list size constraint with the given constraint size.
     *
     * @param size the size constraint for the list parameter
     */
    public ListSizeConstraint(int size) {
        super(LoggingConfiguration.DEBUG);
        sizeConstraint = size;

    }

    /**
     * Checks if the list parameter fulfills the size constraint. If not, a parameter
     * exception is thrown.
     *
     * @throws ParameterException, if the size of the list parameter given is not
     *                             equal to the list size constraint specified.
     * @see de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint#test(java.lang.Object)
     */
    public void test(List<T> t) throws ParameterException {
        if (t.size() != sizeConstraint) {
            throw new WrongParameterValueException("Parameter Constraint Error.\n" +
                "List parameter has not the required size. (Requested size: " +
                +sizeConstraint + ", current size: " + t.size() + ").\n");
        }
    }

    /**
     * @see ParameterConstraint#getDescription(String)
     */
    public String getDescription(String parameterName) {
        return "size(" + parameterName + ") = " + sizeConstraint;
    }
}

