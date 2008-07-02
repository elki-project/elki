package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.NumberParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Represents a Less-Equal-Than global parameter constraint. The value of the
 * first number parameter ({@link NumberParameter}) has to be less equal than the value of the second
 * number parameter ({@link NumberParameter}).
 *
 * @author Steffi Wanka
 */
public class LessEqualGlobalConstraint<T extends Number> extends AbstractLoggable implements GlobalParameterConstraint {

    /**
     * First number parameter.
     */
    private NumberParameter<T> first;

    /**
     * Second number parameter.
     */
    private NumberParameter<T> second;

    /**
     * Creates a Less-Equal-Than global parameter constraint.
     * <p/>
     * That is, the value of
     * the first number parameter given has to be less equal than the value of
     * the second number parameter given.
     *
     * @param first  first number parameter
     * @param second second number parameter
     */
    public LessEqualGlobalConstraint(NumberParameter<T> first, NumberParameter<T> second) {
        super(LoggingConfiguration.DEBUG);
        this.first = first;
        this.second = second;
    }

    /**
     * Checks if the value of the first number parameter is less equal than the
     * value of the second number parameter. If not, a parameter exception is
     * thrown.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint#test()
     */
    public void test() throws ParameterException {
        if (first.isSet() && second.isSet()) {
            if (first.getNumberValue().doubleValue() > second.getNumberValue().doubleValue()) {
                throw new WrongParameterValueException("Global Parameter Constraint Error: \n" + "The value of parameter \""
                    + first.getName() + "\" has to be less equal than the value of parameter \"" + second.getName() + " \"." +
                    "(Current values: " + first.getName() + ": " + first.getNumberValue().doubleValue() + ", " + second.getName() + ": " + second.getNumberValue().doubleValue()+ ")\n");
			}
		}
	}

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint#getDescription()
     */
    public String getDescription() {
        return first.getName() + " <= " + second.getName();
    }

}
