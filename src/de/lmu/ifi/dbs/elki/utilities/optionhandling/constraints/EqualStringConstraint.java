package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

import java.util.Arrays;

/**
 * Represents a parameter constraint for testing if the string value of
 * the string parameter ({@link de.lmu.ifi.dbs.elki.utilities.optionhandling.StringParameter}) to be tested is
 * equal to the specified constraint-strings.
 *
 * @author Steffi Wanka
 */
public class EqualStringConstraint extends AbstractLoggable implements ParameterConstraint<String> {
    /**
     * Constraint-strings.
     */
    private String[] testStrings;

    /**
     * Creates an Equal-To-String parameter constraint.
     * <p/>
     * That is, the string value of
     * the parameter to be tested has to be equal to one of the given constraint-strings.
     *
     * @param testStrings constraint-strings.
     */
    public EqualStringConstraint(String[] testStrings) {
        super(LoggingConfiguration.DEBUG);
        this.testStrings = testStrings;
    }

    private String constraintStrings() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("[");
        for (int i = 0; i < testStrings.length; i++) {
            buffer.append(testStrings[i]);
            if (i != testStrings.length - 1) {
                buffer.append(",");
            }
        }

        buffer.append("]");
        return buffer.toString();
    }

    /**
     * Checks if the given string value of the string parameter is equal to one of the constraint strings.
     * If not, a parameter exception is thrown.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint#test(java.lang.Object)
     */
    public void test(String t) throws ParameterException {
        for (String constraint : testStrings) {
            if (t.equalsIgnoreCase(constraint)) {
                return;
            }
        }

        throw new WrongParameterValueException("Parameter Constraint Error.\n" + "Parameter value must be one of the following values: "
            + constraintStrings());
    }

    /**
     * @see ParameterConstraint#getDescription(String)
     */
    public String getDescription(String parameterName) {
        return parameterName + " in " + Arrays.asList(testStrings).toString();
    }
}
