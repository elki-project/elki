package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;

import java.io.PrintStream;

/**
 * Abstract super class for a result object. Encapsulates methods common for many result objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractResult<T extends MetricalObject> implements Result<T> {
  /**
   * The database containing the objects of this result.
   */
  protected Database<T> db;

  /**
   * The parameter setting of the algorithm to which this result belongs to.
   */
  protected String[] parameters;

  /**
   * Creates a new abstract result object.
   *
   * @param db the database containing the objects of this result
   * @param parameters the parameter setting of the algorithm to which this result belongs to
   */
  protected AbstractResult(Database<T> db, String[] parameters) {
    this.db = db;
    this.parameters = parameters;
  }

  /**
   * Writes a header with the parameters of the underlying algorithm and the
   * minima and maxima values of the normalization.
   *
   * @param out           the print stream where to write
   * @param normalization a Normalization to restore original values for output - may
   *                      remain null
   */
  protected void writeHeader(PrintStream out, Normalization<T> normalization) throws NonNumericFeaturesException {
    out.println("###  db size = " + db.size());
    out.println("###  db dimensionality = " + db.dimensionality());
    for (String param: parameters) {
      out.println("### " + param);
    }

    if (normalization != null) {
      out.println("###");
      out.println(normalization.toString("### "));
      out.println("###");
    }
  }
}
