package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.Util;

/**
 * Abstract superclass for classes parameterizable. Provides the option handler
 * and the parameter array.
 *
 * @author Elke Achtert
 */
public abstract class AbstractParameterizable extends AbstractLoggable
		implements Parameterizable {

	/**
	 * OptionHandler for handling options.
	 */
	protected OptionHandler optionHandler;

	/**
	 * Holds the currently set parameter array.
	 */
	private String[] currentParameterArray = new String[0];

	
	
	/**
	 * Creates a new AbstractParameterizable that provides the option handler
	 * and the parameter array.
	 */
	public AbstractParameterizable() {
		super(LoggingConfiguration.DEBUG);
		optionHandler = new OptionHandler(new TreeMap<String, Option<?>>(),this.getClass().getName());
	}

	/**
	 * @see Parameterizable#setParameters(String[])
	 */
	public String[] setParameters(String[] args) throws ParameterException {
		String[] remainingParameters = optionHandler.grabOptions(args);
		setParameters(args, remainingParameters);
		return remainingParameters;
	}

	/**
	 * Sets the difference of the first array minus the second array as the
	 * currently set parameter array.
	 *
	 * @param complete
	 *            the complete array
	 * @param part
	 *            an array that contains only elements of the first array
	 */
	protected final void setParameters(String[] complete, String[] part) {
		currentParameterArray = Util.parameterDifference(complete, part);
	}

	/**
	 * @see Parameterizable#getParameters()
	 */
	public final String[] getParameters() {
		String[] param = new String[currentParameterArray.length];
		System.arraycopy(currentParameterArray, 0, param, 0,
				currentParameterArray.length);
		return param;
	}

	/**
	 * @see Parameterizable#getAttributeSettings()
	 */
	public List<AttributeSettings> getAttributeSettings() {
    try {
      List<AttributeSettings> settings = new ArrayList<AttributeSettings>();
      AttributeSettings mySettings = new AttributeSettings(this);
      optionHandler.addOptionSettings(mySettings);
      settings.add(mySettings);
      return settings;
    }
    catch (UnusedParameterException e) {
      throw new RuntimeException("This should never happen! " + e);
    }
	}

	/**
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
	 */
	public String description() {
		return optionHandler.usage("");
	}

	/**
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getPossibleOptions()
	 */
	public Option<?>[] getPossibleOptions(){
		return optionHandler.getOptions();
	}
	
	/**
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#checkGlobalParameterConstraints()
	 */
	public void checkGlobalParameterConstraints() throws ParameterException{
		this.optionHandler.checkGlobalParameterConstraints();
	}
}
