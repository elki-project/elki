package de.lmu.ifi.dbs.parser;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Abstract superclass for all parsers providing the option handler for
 * handling options.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractParser<O extends DatabaseObject> implements Parser<O> {
  /**
   * The comment character.
   */
  public static final String COMMENT = "#";

  /**
   * A sign to separate attributes.
   */
  public static final String ATTRIBUTE_CONCATENATION = " ";

  /**
   * A pattern defining whitespace.
   */
  public static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

  /**
   * Holds the currently set parameter array.
   */
  private String[] currentParameterArray = new String[0];
  
  /**
   * OptionHandler for handling options.
   */
  OptionHandler optionHandler;

  /**
   * Map providing a mapping of parameters to their descriptions.
   */
  Map<String, String> parameterToDescription = new Hashtable<String, String>();

  /**
   * AbstractParser already provides the option handler.
   */
  protected AbstractParser() {
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * Returns a usage string based on the usage of optionHandler.
   *
   * @param message a message string to be included in the usage string
   * @return a usage string based on the usage of optionHandler
   */
  protected String usage(String message) {
    return optionHandler.usage(message, false);
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
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
      currentParameterArray = Util.parameterDifference(complete, part);
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
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = new ArrayList<AttributeSettings>();
    result.add(new AttributeSettings(this));
    return result;
  }

  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
    return getClass().getName();
  }

}
