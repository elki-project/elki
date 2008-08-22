package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.NumberParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Represents a parameter constraint for testing if the value of the
 * number parameter ({@link NumberParameter})
 * tested is greater than the specified constraint value.
 *
 * @author Steffi Wanka
 */
public class GreaterConstraint extends AbstractNumberConstraint<Number> {
    /**
     * Creates a Greater-Than-Number parameter constraint.
     * <p/>
     * That is, the value of the number
     * parameter has to be greater than the given constraint value.
     *
     * @param constraintValue the constraint value
     */
    public GreaterConstraint(Number constraintValue) {
        super(constraintValue);
    }

    /**
     * Checks if the number value given by the number parameter is greater than
     * the constraint value. If not, a parameter exception is thrown.
     *
     */
    public void test(Number t) throws ParameterException {
        if (t.doubleValue() <= constraintValue.doubleValue()) {
            throw new WrongParameterValueException("Parameter Constraint Error:\n"
                + "The parameter value specified has to be greater than "
                + constraintValue.toString() +
                ". (current value: " + t.doubleValue() + ")\n");
        }
    }

    public String getDescription(String parameterName) {
        return parameterName + " > " + constraintValue;
    }
}
