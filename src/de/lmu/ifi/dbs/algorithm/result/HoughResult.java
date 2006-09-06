package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.ParameterizationFunction;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.algorithm.result.AbstractResult;

import java.io.PrintStream;
import java.util.List;

/**
 * TODO: comment
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class HoughResult extends AbstractResult<ParameterizationFunction> {
  /**
   * todo
   *
   * @param db
   */
  public HoughResult(Database<ParameterizationFunction> db) {
    super(db);
  }

  /**
   * todo
   * Writes the clustering result to the given stream.
   *
   * @param outStream     the stream to write to
   * @param normalization Normalization to restore original values according to, if this action is supported
   *                      - may remain null.
   * @param settings      the settings to be written into the header, if this parameter is <code>null</code>,
   *                      no header will be written
   * @throws de.lmu.ifi.dbs.utilities.UnableToComplyException
   *          if any feature vector is not compatible with values initialized during normalization
   */
  public void output(PrintStream outStream, Normalization<ParameterizationFunction> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}
