package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Represents an interval parameter constraint testing if a given value lies within
 * the specified interval.
 * The value of the number parameter ({@link de.lmu.ifi.dbs.elki.utilities.optionhandling.NumberParameter})
 * tested has to be greater than (or equal to, if specified) than the specified low constraint value
 * and less than (or equal to, if specified) than the specified high constraint value.
 *
 * @author Steffi Wanka
 */
public class IntervalConstraint extends AbstractLoggable implements ParameterConstraint<Number> {
    /**
     * Available interval boundary types types:
     * {@link IntervalConstraint.IntervalBoundary#OPEN} denotes an opend interval,
     * i.e. less than (or greater than) comparison
     * {@link IntervalConstraint.IntervalBoundary#CLOSE} denotes a closed interval,
     * i.e. an equal to or less than (or equal to or greater than) comparison
     */
    public enum IntervalBoundary {
        OPEN,
        CLOSE
    }

    /**
     * The low constraint value (left interval boundary).
     */
    private final Number lowConstraintValue;

    /**
     * The interval boundary for the low constraint value (@see IntervalBoundary)
     */
    private final IntervalBoundary lowBoundary;

    /**
     * The high constraint value (right interval boundary).
     */
    private final Number highConstraintValue;

    /**
     * The interval boundary for the high constraint value (@see IntervalBoundary)
     */
    private final IntervalBoundary highBoundary;

    /**
     * Creates an IntervalConstraint parameter constraint.
     * <p/>
     * That is, the value of the number parameter given
     * has to be greater than (or equal to, if specified) than the specified low constraint value
     * and less than (or equal to, if specified) than the specified high constraint value.
     *
     * @param lowConstraintValue  the low constraint value (left interval boundary)
     * @param lowBoundary         the interval boundary for the low constraint value (@see IntervalBoundary)
     * @param highConstraintValue the high constraint value (right interval boundary)
     * @param highBoundary        the interval boundary for the high constraint value (@see IntervalBoundary)
     */
    public IntervalConstraint(Number lowConstraintValue, IntervalBoundary lowBoundary,
                              Number highConstraintValue, IntervalBoundary highBoundary) {
        super(LoggingConfiguration.DEBUG);

        if (lowConstraintValue.doubleValue() >= highConstraintValue.doubleValue()) {
            throw new IllegalArgumentException("Left interval boundary is greater than " +
                "or equal to right interval boundary!");
        }

        this.lowConstraintValue = lowConstraintValue;
        this.lowBoundary = lowBoundary;
        this.highConstraintValue = highConstraintValue;
        this.highBoundary = highBoundary;
    }

    /**
     * Checks if the number value given by the number parameter is
     * greater equal than the constraint
     * value. If not, a parameter exception is thrown.
     *
     */
    public void test(Number t) throws ParameterException {
        // lower value
        if (lowBoundary.equals(IntervalBoundary.CLOSE)) {
            if (t.doubleValue() < lowConstraintValue.doubleValue()) {
                throw new WrongParameterValueException("Parameter Constraint Error: \n" +
                    "The parameter value specified has to be " +
                    "equal to or greater than " +
                    lowConstraintValue.toString() +
                    ". (current value: " + t.doubleValue() + ")\n");
            }
        }
        else if (lowBoundary.equals(IntervalBoundary.OPEN)) {
            if (t.doubleValue() <= lowConstraintValue.doubleValue()) {
                throw new WrongParameterValueException("Parameter Constraint Error: \n" +
                    "The parameter value specified has to be " +
                    "greater than " +
                    lowConstraintValue.toString() +
                    ". (current value: " + t.doubleValue() + ")\n");
            }
        }

        // higher value
        if (highBoundary.equals(IntervalBoundary.CLOSE)) {
            if (t.doubleValue() > highConstraintValue.doubleValue()) {
                throw new WrongParameterValueException("Parameter Constraint Error: \n" +
                    "The parameter value specified has to be " +
                    "equal to or less than " +
                    highConstraintValue.toString() +
                    ". (current value: " + t.doubleValue() + ")\n");
            }
        }
        else if (highBoundary.equals(IntervalBoundary.OPEN)) {
            if (t.doubleValue() >= highConstraintValue.doubleValue()) {
                throw new WrongParameterValueException("Parameter Constraint Error: \n" +
                    "The parameter value specified has to be " +
                    "less than " +
                    highConstraintValue.toString() +
                    ". (current value: " + t.doubleValue() + ")\n");
            }
        }
    }

    public String getDescription(String parameterName) {
        String description = parameterName + " in ";
        if (lowBoundary.equals(IntervalBoundary.CLOSE)) {
            description += "[";
        }
        else if (lowBoundary.equals(IntervalBoundary.OPEN)) {
            description += "(";
        }

        description += lowConstraintValue.toString() + ", " + highConstraintValue;

        if (highBoundary.equals(IntervalBoundary.CLOSE)) {
            description += "]";
        }
        if (highBoundary.equals(IntervalBoundary.OPEN)) {
            description += ")";
        }
        return description;
    }

}
