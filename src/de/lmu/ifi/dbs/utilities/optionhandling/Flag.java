package de.lmu.ifi.dbs.utilities.optionhandling;

/**
 * Holds a flag object, i.e. an optional parameter which can be set (value
 * &quot;true&quot;) or not (value &quot;false&quot;).
 * 
 * @author Steffi Wanka
 * 
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
	 * Constructs a flag object with the given name and description. <p/> The
	 * flag is not set, i.e. its value is &quot;false&quot;.
	 * 
	 * @param name
	 *            the name of the flag.
	 * @param description
	 *            the description of the flag.
	 */
	public Flag(String name, String description) {
		super(name, description);
		this.value = false;
	}

	/**
	 * Returns true if the flag is set, false otherwise.
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#isSet()
	 */
	public boolean isSet() {
		return value;
	}

	/**
	 * Specifies if the flag is set or not. <p/> The given value should be
	 * either {@link #SET} or {@link #NOT_SET}.
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#setValue(java.lang.String)
	 */
	public void setValue(String value) throws ParameterException {

		if (isValid(value)) {

			if (value.equals(SET)) {
				this.value = true;
			} else {
				this.value = false;
			}
		}
	}

	/**
	 * Returns &quot;true&quot; if the flag is set, &quot;false&quot; otherwise.
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#getValue()
	 */
	public String getValue() {
		if (value) {
			return SET;
		}
		return NOT_SET;
	}

	public boolean isValid(String value) throws ParameterException {
		if (value.equals(SET) || value.equals(NOT_SET)) {
			return true;
		}
		throw new WrongParameterValueException("Wrong value for flag \"" + getName()
				+ "\". Allowed values:\n" + SET + " or " + NOT_SET);
	}

}
