package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.List;
import java.util.ArrayList;

/**
 * FileBasedDatabaseConnectionWrapper is an abstract super class for all wrapper
 * classes running algorithms in a kdd task using a file based database connection.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class FileBasedDatabaseConnectionWrapper extends KDDTaskWrapper {
  /**
   * Sets the parameter database connection in the parameter map additionally to the
   * parameters provided by super-classes.
   */
  public FileBasedDatabaseConnectionWrapper() {
    super();
    parameterToDescription.put(FileBasedDatabaseConnection.INPUT_P + OptionHandler.EXPECTS_VALUE, FileBasedDatabaseConnection.INPUT_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * @see KDDTaskWrapper#getParameters()
   */
  public List<String> getParameters() {
    List<String> parameters = new ArrayList<String>();
    // input
    parameters.add(OptionHandler.OPTION_PREFIX + FileBasedDatabaseConnection.INPUT_P);
    parameters.add(optionHandler.getOptionValue(FileBasedDatabaseConnection.INPUT_P));

    return parameters;
  }
}


