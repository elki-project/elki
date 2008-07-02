package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.elki.logging.Loggable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * <p/>
 * Interface for specifying parameter constraints.
 * </p>
 * <p/>
 * Each class specifying a constraint addressing only one parameter should implement this interface.
 * The constraint value for testing the parameter should be defined as private attribute and should be initialized in the
 * respective constructor of the class, i.e. it is a parameter of the constructor. The proper constraint
 * test should be implemented in the method {@link #test(Object) test(T)}.
 * </p>
 *
 * @author Steffi Wanka
 * @param <T> the type of the constraint
 */
public interface ParameterConstraint<T> extends Loggable {
    /**
     * Checks if the value {@code t} of the parameter to be tested fulfills the parameter constraint.
     * If not, a parameter exception is thrown.
     *
     * @param t Value to be checked whether or not it fulfills the underlying
     *          parameter constraint.
     * @throws ParameterException if the parameter to be tested does not
     *                            fulfill the parameter constraint
     */
    public abstract void test(T t) throws ParameterException;

    /**
     * Returns a description of this constraint.
     *
     * @param parameterName the name of the parameter this constraint is used for
     * @return a description of this constraint
     */
    public abstract String getDescription(String parameterName);

}
