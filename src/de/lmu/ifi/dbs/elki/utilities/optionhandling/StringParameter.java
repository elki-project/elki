package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

import java.util.List;

/**
 * Parameter class for a parameter specifying a string value.
 *
 * @author Steffi Wanka
 */
public class StringParameter extends Parameter<String, String> {

    /**
     * Constructs a string parameter with the given name and description
     *
     * @param name        the parameter name
     * @param description the parameter description
     * @deprecated
     */
    @Deprecated
    public StringParameter(String name, String description) {
        super(name, description);
    }

    /**
     * Constructs a string parameter with the given name, description, and a parameter constraint.
     *
     * @param name        the parameter name
     * @param description the parameter description
     * @param con         a parameter constraint for this string parameter
     * @deprecated
     */
    @Deprecated
    public StringParameter(String name, String description, ParameterConstraint<String> con) {
        this(name, description);
        addConstraint(con);
    }

    /**
     * Constructs a string parameter with the given name, description, and a list of parameter constraints.
     *
     * @param name        the parameter name
     * @param description the parameter description
     * @param cons        a list of parameter constraints for this string parameter
     * @deprecated
     */
    @Deprecated
    public StringParameter(String name, String description, List<ParameterConstraint<String>> cons) {
        this(name, description);
        addConstraintList(cons);
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Option#setValue(java.lang.String)
     */
    public void setValue(String value) throws ParameterException {
        if (isValid(value)) {
            this.value = value;
        }
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Option#isValid(java.lang.String)
     */
    public boolean isValid(String value) throws ParameterException {
        try {
            for (ParameterConstraint<String> cons : this.constraints) {
                cons.test(value);
            }
        }
        catch (ParameterException ex) {
            throw new WrongParameterValueException("Specified parameter value for parameter \""
                + getName() + "\" breaches parameter constraint!\n" + ex.getMessage());
        }

        return true;
    }

    /**
     * Returns a string representation of the parameter's type.
     *
     * @return &quot;&lt;string&gt;&quot;
     * @see Parameter#getParameterType()
     */
    protected String getParameterType() {
        return "<string>";
    }
}
