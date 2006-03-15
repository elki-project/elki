package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

/**
 * Defines the requirements for classes that do some preprocessing steps
 * for objects of a certain database.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface Preprocessor extends Parameterizable {
  /**
   * This method executes the actual preprocessing step of this Preprocessor
   * for the objects of the specified database.
   *
   * @param database the database for which the preprocessing is performed
   * @param verbose  flag to allow verbose messages while performing the algorithm
   * @param time     flag to request output of performance time
   */
  void run(Database<RealVector> database, boolean verbose, boolean time);
}
