package de.lmu.ifi.dbs.elki.database.connection;

import java.io.FileInputStream;
import java.io.IOException;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.FileUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;

/**
 * Provides a file based database connection based on the parser to be set.
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObject to be provided by the implementing
 *        class as element of the supplied database
 */
public class FileBasedDatabaseConnection<O extends DatabaseObject> extends InputStreamDatabaseConnection<O> {
  /**
   * OptionID for {@link #INPUT_PARAM}
   */
  public static final OptionID INPUT_ID = OptionID.getOrCreateOptionID("dbc.in", "The name of the input file to be parsed.");

  /**
   * Parameter that specifies the name of the input file to be parsed.
   * <p>
   * Key: {@code -dbc.in}
   * </p>
   */
  private final FileParameter INPUT_PARAM = new FileParameter(INPUT_ID, FileParameter.FileType.INPUT_FILE);

  /**
   * Provides a file based database connection based on the parser to be set,
   * adding parameter {@link #INPUT_PARAM} to the option handler additionally to
   * parameters of super class.
   */
  public FileBasedDatabaseConnection(Parameterization config) {
    super(config);
    if (config.grab(this, INPUT_PARAM)) {
      try {
        in = new FileInputStream(INPUT_PARAM.getValue());
        in = FileUtil.tryGzipInput(in);
      }
      catch(IOException e) {
        config.reportError(new WrongParameterValueException(INPUT_PARAM, INPUT_PARAM.getValue().getPath(), e));
      }
    }
  }
}