package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.util.List;

/**
 * Specifies the requirements to a printable result of some algorithm.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface Result<T extends MetricalObject> {
  /**
   * String to separate different entries while printing.
   */
  public static final String SEPARATOR = " ";

  /**
   * Writes the clustering result to the given file.
   * Clustering result implementations, which are likely to
   * provide several clusters are supposed to use the filename
   * as prefix for every file to create and to append a proper suffix.
   * In case of occuring IOExceptions the output is expected
   * to be given at the standard-out. Therefore this behaviour
   * should be also achievable by giving a null-Object as parameter.
   *
   * @param out           file, which designates the location to write the results,
   *                      or which's name designates the prefix of any locations to write the results,
   *                      or which could remain null to designate the standard-out as location for output.
   * @param normalization Normalization to restore original values according to, if this action is supported
   *                      - may remain null.
   * @param settings      the settings to be written into the header
   * @throws de.lmu.ifi.dbs.utilities.UnableToComplyException
   *          if any feature vector is not compatible with values initialized during normalization
   */
  public void output(File out, Normalization<T> normalization,
                     List<AttributeSettings> settings) throws UnableToComplyException;
}
