package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.NumberParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Represents a Less-Equal-Than-Number parameter constraint. The value of the
 * number parameter ({@link NumberParameter}) tested has to be less equal than the specified constraint
 * value.
 *
 * @author Steffi Wanka
 */
public class LessEqualConstraint extends AbstractNumberConstraint<Number> {
    /**
     * Creates a Less-Equal-Than-Number parameter constraint.
     * <p/>
     * That is, the value of
     * the appropriate number parameter has to be less equal than the given constraint value.
     *
     * @param constraintValue the constraint value
     */
    public LessEqualConstraint(Number constraintValue) {
        super(constraintValue);
    }

    /**
     * Checks if the number value given by the number parameter is less equal than
     * the constraint value. If not, a parameter exception is thrown.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint#test(java.lang.Object)
     */
    public void test(Number t) throws ParameterException {
        if (t.doubleValue() > constraintValue.doubleValue()) {
            throw new WrongParameterValueException("Parameter Constraint Error: \n"
                + "The parameter value specified has to be less equal than "
                + constraintValue.toString() + ". (current value: " + t.doubleValue() + ")\n");
        }
    }

    /**
     * @see ParameterConstraint#getDescription(String)
     */
    public String getDescription(String parameterName) {
        return parameterName + " <= " + constraintValue;
    }
}
