package de.lmu.ifi.dbs.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

/**
 * Abstract superclass for all parsers providing the option handler for
 * handling options.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractParser<O extends DatabaseObject> extends AbstractParameterizable implements Parser<O> {
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
   * AbstractParser already provides the option handler.
   */
  protected AbstractParser() {
	  super();
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
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
    return getClass().getName();
  }

}
