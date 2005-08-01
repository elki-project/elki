package de.lmu.ifi.dbs.parser;

import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.Hashtable;
import java.util.Map;
import java.util.Arrays;

/**
 * AbstractParser already provides the setting of the database according to parameters.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractParser implements Parser {
  /**
   * Option string for parameter database.
   */
  public static final String DATABASE_CLASS_P = "database";

  /**
   * Description for parameter database.
   */
  public static final String DATABASE_CLASS_D = "<classname>a class name specifying the database to be provided by the parse method (must implement " + Database.class.getName() + ")";

  /**
   * The database class.
   */
  private Class database;

  /**
   * OptionHandler for handling options.
   */
  private OptionHandler optionHandler;

  /**
   * AbstractParser already provides the setting of the database according to parameters.
   */
  protected AbstractParser() {
    Map<String, String> parameterToDescription = new Hashtable<String, String>();
    parameterToDescription.put(DATABASE_CLASS_P + OptionHandler.EXPECTS_VALUE, DATABASE_CLASS_D);
    optionHandler = new OptionHandler(parameterToDescription, "");
  }

  /**
   * Provides an instance of the specified database.
   *
   * @return an instance of the specified database
   */
  protected Database databaseInstance() {
    try {
      return (Database) database.newInstance();
    }
    catch (InstantiationException e) {
      return null;
    }
    catch (IllegalAccessException e) {
      return null;
    }
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
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingOptions = optionHandler.grabOptions(args);
    if (optionHandler.isSet(DATABASE_CLASS_P)) {
      try {
        Class<?> databaseClass = Class.forName(optionHandler.getOptionValue(DATABASE_CLASS_P));
        database = ((Database) databaseClass.newInstance()).getClass();
      }
      catch (UnusedParameterException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (NoParameterValueException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (ClassNotFoundException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (InstantiationException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (IllegalAccessException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }
    else {
      throw new IllegalArgumentException("Parser: Database is not specified.");
    }
    return remainingOptions;
  }

}
