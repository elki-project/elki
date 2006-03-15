package de.lmu.ifi.dbs.normalization;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Abstract super class for all normalizations. Provides the option handler.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractNormalization<O extends DatabaseObject> implements Normalization<O>, Parameterizable {
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
   * Holds the currently set parameter array.
   */
  private String[] currentParameterArray = new String[0];

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
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = optionHandler.grabOptions(args);
    setParameters(args, remainingParameters);
    return remainingParameters;
  }
  /**
   * Sets the difference of the first array minus the second array
   * as the currently set parameter array.
   * 
   * 
   * @param complete the complete array
   * @param part an array that contains only elements of the first array
   */
  protected void setParameters(String[] complete, String[] part)
  {
      currentParameterArray = Util.difference(complete, part);
  }
  
  /**
   * 
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getParameters()
   */
  public String[] getParameters()
  {
      String[] param = new String[currentParameterArray.length];
      System.arraycopy(currentParameterArray, 0, param, 0, currentParameterArray.length);
      return param;
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
