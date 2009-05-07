package de.lmu.ifi.dbs.elki.utilities.optionhandling;

/**
 * Option class specifying a flag object.
 * <p/>
 * A flag object is optional parameter which can be set (value
 * &quot;true&quot;) or not (value &quot;false&quot;).
 *
 * @author Steffi Wanka
 */
public class Flag extends Option<Boolean> {

    /**
     * Constant indicating that the flag is set.
     */
    public static final String SET = "true";

    /**
     * Constant indicating that the flag is not set.
     */
    public static final String NOT_SET = "false";

    /**
     * Constructs a flag object with the given optionID.
     * <p/> If the flag is not set its value is &quot;false&quot;.
     *
     * @param optionID the unique id of the option
     */
    public Flag(OptionID optionID) {
        super(optionID);
        this.value = false;
    }

    /**
     * Returns the short description of this flag.
     *
     * @return the short description of this flag
     */
    @Override
    public String getFullDescription() {
        return shortDescription;
    }

    /**
     * Returns true if the flag is set, false otherwise.
     */
    @Override
    public boolean isSet() {
        return value;
    }

    /**
     * Specifies if the flag is set or not. <p/> The given value should be
     * either {@link #SET} or {@link #NOT_SET}.
     */
    @Override
    public void setValue(String value) throws ParameterException {
        if (isValid(value)) {
            this.value = value.equals(SET);
        }
    }

    /**
     * Specifies if the flag is set or not.
     *
     * @param value true, if the flag is set, false otherwise
     */
    public void setValue(boolean value) {
        this.value = value;
    }

    @Override
    public boolean isValid(String value) throws ParameterException {
        if (value.equals(SET) || value.equals(NOT_SET)) {
            return true;
        }
        throw new WrongParameterValueException("Wrong value for flag \"" + getName()
            + "\". Allowed values:\n" + SET + " or " + NOT_SET);
    }

    /**
     * A flag has no syntax, since it doesn't take extra options
     */
    @Override
    public String getSyntax() {
      return "";
    }

}
