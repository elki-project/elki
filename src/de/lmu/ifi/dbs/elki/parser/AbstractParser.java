package de.lmu.ifi.dbs.elki.parser;

import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;

/**
 * Abstract superclass for all parsers providing the option handler for
 * handling options.
 *
 * @author Arthur Zimek
 * @param <O> object type
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
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  @Override
  public String toString() {
    return getClass().getName();
  }
}
