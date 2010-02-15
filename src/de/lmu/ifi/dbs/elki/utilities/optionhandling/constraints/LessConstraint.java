package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.NumberParameter;

/**
 * Represents a Less-Than-Number parameter constraint. The value of the
 * number parameter ({@link NumberParameter}) tested has to be less than the specified constraint value.
 *
 * @author Steffi Wanka
 */
public class LessConstraint extends AbstractNumberConstraint<Number> {
    /**
     * Creates a Less-Than-Number parameter constraint.
     * <p/>
     * That is, the value of the number
     * parameter tested has to be less than the constraint value given.
     *
     * @param constraintValue the constraint value
     */
    public LessConstraint(Number constraintValue) {
        super(constraintValue);
    }

    /**
     * Checks if the number value given by the number parameter is less than the constraint value.
     * If not, a parameter exception is thrown.
     *
     */
    public void test(Number t) throws ParameterException {
        if (t.doubleValue() >= constraintValue.doubleValue()) {
            throw new WrongParameterValueException("Parameter Constraint Error: \n"
                + "The parameter value specified has to be less than " + constraintValue.toString()
                + ". (current value: " + t.doubleValue() + ")\n");
        }
    }

    public String getDescription(String parameterName) {
        return parameterName + " < " + constraintValue;
    }

}
