package de.lmu.ifi.dbs.elki.parser;

import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;

/**
 * Abstract superclass for all parsers providing the option handler for handling
 * options.
 * 
 * @author Arthur Zimek
 * @param <O> object type
 */
public abstract class AbstractParser<O extends DatabaseObject> implements Parser<O> {
  /**
   * A pattern defining whitespace.
   */
  public static final String WHITESPACE_PATTERN = "\\s+";

  /**
   * OptionID for {@link #COLUMN_SEPARATOR_PARAM}
   */
  private static final OptionID COLUMN_SEPARATOR_ID = OptionID.getOrCreateOptionID("parser.colsep", "Column separator pattern. The default assumes whitespace separated data.");

  /**
   * Stores the column separator pattern
   */
  protected Pattern colSep = null;

  /**
   * The comment character.
   */
  public static final String COMMENT = "#";

  /**
   * A sign to separate attributes.
   */
  public static final String ATTRIBUTE_CONCATENATION = " ";

  /**
   * AbstractParser already provides the option handler.
   * 
   * @param config Parameterization
   */
  protected AbstractParser(Parameterization config) {
    super();
    config = config.descend(this);
    PatternParameter COLUMN_SEPARATOR_PARAM = new PatternParameter(COLUMN_SEPARATOR_ID, WHITESPACE_PATTERN);
    if(config.grab(COLUMN_SEPARATOR_PARAM)) {
      colSep = COLUMN_SEPARATOR_PARAM.getValue();
    }
  }

  /**
   * Returns a string representation of the object.
   * 
   * @return a string representation of the object.
   */
  @Override
  public String toString() {
    return getClass().getName();
  }
}
