package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.List;

/**
 * Represents a Greater-Equal-Than-Number parameter constraint for a list of number values.
 * All values of the list parameter ({@link de.lmu.ifi.dbs.utilities.optionhandling.ListParameter})
 * tested have to be greater than or equal to the specified constraint value.
 *
 * @author Elke Achtert
 */
public class ListGreaterEqualConstraint<N extends Number> extends AbstractNumberConstraint<List<N>> {
    /**
     * Creates a Greater-Equal-Than-Number parameter constraint.
     * <p/>
     * That is, all values of the list parameter
     * tested have to be greater than or equal to the specified constraint value.
     *
     * @param constraintValue parameter constraint value
     */
    public ListGreaterEqualConstraint(N constraintValue) {
        super(constraintValue);
    }

    /**
     * Checks if all number values of the specified list parameter
     * are greater than or equal to the constraint value.
     * If not, a parameter exception is thrown.
     *
     * @see ParameterConstraint#test(Object)
     */
    public void test(List<N> t) throws ParameterException {
        for (Number n : t) {
            if (n.doubleValue() < constraintValue.doubleValue()) {
                throw new WrongParameterValueException("Parameter Constraint Error: \n"
                    + "The parameter values specified have to be greater than or equal to " + constraintValue.toString()
                    + ". (current value: " + t + ")\n");
            }
        }
    }

    /**
     * @see ParameterConstraint#getDescription(String)
     */
    public String getDescription(String parameterName) {
        return "all elements of " + parameterName + " < " + constraintValue;
    }

}
