package de.lmu.ifi.dbs.normalization;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.util.Map;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;

/**
 * Abstract super class for all normalizations. Provides the option handler.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractNormalization<M extends MetricalObject> implements Normalization<M>, Parameterizable {
  /**
   * Map providing a mapping of parameters to their descriptions.
   */
  Map<String, String> parameterToDescription;

  /**
   * OptionHandler to handler options. optionHandler should be initialized
   * using parameterToDescription in any non-abstract class extending this
   * class.
   */
  OptionHandler optionHandler;

  /**
   * Initializes the option handler and the parameter map.
   */
  protected AbstractNormalization() {
    parameterToDescription = new Hashtable<String, String>();
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
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
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    return  optionHandler.grabOptions(args);
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
