package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

/**
 * Represents a global parameter constraint for testing if the size of
 * the list parameter ({@link ListParameter}) given is equal to the constraint size
 * specified by the integer parameter ({@link IntParameter}) given.
 *
 * @author Steffi Wanka
 */
public class GlobalListSizeConstraint extends AbstractLoggable implements GlobalParameterConstraint {
    /**
     * List parameter to be tested.
     */
    private ListParameter<?> list;

    /**
     * Integer parameter specifying the constraint list size.
     */
    private IntParameter length;

    /**
     * Creates a List-Size global parameter constraint.
     * <p/>
     * That is, the size of the given
     * list parameter hat to be equal to the constraint list size specified by the
     * integer parameter given.
     *
     * @param v the list parameter to be tested.
     * @param i integer parameter specifying the constraint list size.
     */
    public GlobalListSizeConstraint(ListParameter<?> v, IntParameter i) {
        super(LoggingConfiguration.DEBUG);
        this.list = v;
        this.length = i;
    }

    /**
     * Checks is the size of the list parameter is equal to the constraint list
     * size specified. If not, a parameter exception is thrown.
     *
     * @see GlobalParameterConstraint#test()
     */
    public void test() throws ParameterException {
        if (!list.isSet() || !length.isSet()) {
            return;
        }

        if (list.getListSize() != length.getValue()) {
            throw new WrongParameterValueException("Global Parameter Constraint Error." +
                "\nThe size of the list parameter \"" +
                list.getName() +
                "\" must be " +
                length.getValue() +
                ", current size is " + list.getListSize() +
                ". The value is defined by the integer parameter " +
                length.getName() + ".\n");
        }
    }

    /**
     * @see GlobalParameterConstraint#getDescription()
     */
    public String getDescription() {
        try {
            return "size(" + list.getName()+") == " + length.getValue();
        }
        catch (UnusedParameterException e) {
            throw new IllegalStateException(e);
        }
    }
}
