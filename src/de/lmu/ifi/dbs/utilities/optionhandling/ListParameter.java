package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.List;
import java.util.regex.Pattern;

public abstract class ListParameter<T> extends Parameter<List<T>> {

	/**
	 * A pattern defining a comma.
	 */
	public static final Pattern SPLIT = Pattern.compile(",");

	/**
	 * A pattern defining a :.
	 */
	public static final Pattern VECTOR_SPLIT = Pattern.compile(":");

	public ListParameter(String name, String description) {
		super(name, description);
	}

	public abstract int getListSize();
}
