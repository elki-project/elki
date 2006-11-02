package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

/**
 * Interface Measurement describes the requirements of any measurement
 * function (e.g. distance or similarity function), that provides a measurement
 * for comparing database objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface MeasurementFunction<O extends DatabaseObject, D extends Distance> extends Parameterizable {
  /**
   * Returns a String as description of the required input format.
   *
   * @return a String as description of the required input format
   */
  String requiredInputPattern();

  /**
   * Set the database that holds the associations for the DatabaseObject for
   * which the measurements should be computed.
   *
   * @param database the database to be set
   * @param verbose  flag to allow verbose messages while performing the method
   * @param time     flag to request output of performance time
   */
  void setDatabase(Database<O> database, boolean verbose, boolean time);

  /**
   * Provides a measurement suitable to this measurement function based on the given
   * pattern.
   *
   * @param pattern a pattern defining a similarity suitable to this
   *                measurement function
   * @return a measurement suitable to this measurement function based on the given
   *         pattern
   * @throws IllegalArgumentException if the given pattern is not compatible with the requirements
   *                                  of this measurement function
   */
  D valueOf(String pattern) throws IllegalArgumentException;
}
