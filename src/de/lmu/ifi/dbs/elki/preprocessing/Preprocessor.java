package de.lmu.ifi.dbs.elki.preprocessing;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Defines the requirements for classes that do some preprocessing steps for
 * objects of a certain database.
 *
 * @author Elke Achtert
 * @param <O> Object type
 */
public interface Preprocessor<O extends DatabaseObject> extends Parameterizable {
  /**
   * This method executes the actual preprocessing step of this Preprocessor
   * for the objects of the specified database.
   *
   * @param database the database for which the preprocessing is performed
   * @param verbose  flag to allow verbose messages while performing the algorithm
   * @param time     flag to request output of performance time
   */
  void run(Database<O> database, boolean verbose, boolean time);
}
