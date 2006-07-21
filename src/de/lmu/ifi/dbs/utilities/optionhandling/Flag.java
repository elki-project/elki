package de.lmu.ifi.dbs.utilities.optionhandling;

public class Flag extends Option {

	private boolean value;

	public static final String SET = "true";

	public static final String NOT_SET = "false";

	public Flag(String name, String description) {
		super(name, description);
		this.value = false;
	}

	public boolean isSet() {
		return value;
	}

	public void setValue(String value) {

		if (value.equals(SET)) {
			this.value = true;
		}
	}

	public String getValue() {
		if (value) {
			return SET;
		}
		return NOT_SET;

	}
}
