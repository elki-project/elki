package de.lmu.ifi.dbs.database.connection;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Provides a file based database connection based on the parser to be set.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class FileBasedDatabaseConnection<O extends DatabaseObject> extends InputStreamDatabaseConnection<O> {

  /**
   * Label for parameter input.
   */
  public final static String INPUT_P = "in";

  /**
   * Description for parameter input.
   */
  public final static String INPUT_D = "input file to be parsed.";

  /**
   * Provides a file based database connection based on the parser to be set.
   */
  public FileBasedDatabaseConnection() {
    super();
    optionHandler.put(INPUT_P, new Parameter(INPUT_P,INPUT_D,Parameter.Types.FILE));
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingOptions = super.setParameters(args);

    String input = optionHandler.getOptionValue(INPUT_P);
    try {
      in = new FileInputStream(input);
    }
    catch (FileNotFoundException e) {
      throw new WrongParameterValueException(INPUT_P, input, INPUT_D, e);
    }
    setParameters(args, remainingOptions);
    return remainingOptions;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();

    AttributeSettings setting = result.get(0);
    try {
      setting.addSetting(INPUT_P, optionHandler.getOptionValue(INPUT_P));
    }
    catch (ParameterException e) {
      // tested before
      throw new RuntimeException("This should never happen!");
    }

    return result;
  }

}
