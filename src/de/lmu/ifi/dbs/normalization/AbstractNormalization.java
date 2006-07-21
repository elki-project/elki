package de.lmu.ifi.dbs.normalization;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

/**
 * Abstract super class for all normalizations. Provides the option handler.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractNormalization<O extends DatabaseObject> extends AbstractParameterizable implements Normalization<O> {

  /**
   * Initializes the option handler and the parameter map.
   */
  protected AbstractNormalization() {
	  super();
  }

  /**
   * Sets the attributes of the class accordingly to the given parameters.
   * Returns a new String array containing those entries of the
   * given array that are neither expected nor used by this
   * Parameterizable.
   *
   * @param args parameters to set the attributes accordingly to
   * @return String[] an array containing the unused parameters
   * @throws IllegalArgumentException in case of wrong parameter-setting
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = optionHandler.grabOptions(args);
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Returns the setting of the attributes of the parameterizable.
   *
   * @return the setting of the attributes of the parameterizable
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = new ArrayList<AttributeSettings>();
    result.add(new AttributeSettings(this));
    return result;
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    return getClass().getName();
  }
}
