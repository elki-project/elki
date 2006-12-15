package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;

public class PatternParameter extends Parameter<String, String> {

	private Class patternClass;

	public PatternParameter(String name, String description) {
		super(name, description);

	}

	public PatternParameter(String name, String description, Class patternClass) {
		super(name, description);
		this.patternClass = patternClass;
	}

	@Override
	public void setValue(String value) throws ParameterException {

		if (isValid(value)) {
			this.value = value;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#isValid(java.lang.String)
	 */
	public boolean isValid(String value) throws ParameterException {

		try {
			Pattern.compile(value);
		} catch (PatternSyntaxException e) {
			throw new WrongParameterValueException("Given pattern \"" + value + "\" for parameter \"" + getName()
					+ "\" is no valid regular expression!");
		}
		// test pattern class (if existent)
		if (this.patternClass != null) {
			// create instance
			try {
				DistanceFunction obj = (DistanceFunction) patternClass.newInstance();
				obj.valueOf(value);
			} catch (InstantiationException e) {
				// TODO
				throw new WrongParameterValueException("");
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				throw new WrongParameterValueException("");
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				throw new WrongParameterValueException("");
			}
		}

		return true;
	}

}
