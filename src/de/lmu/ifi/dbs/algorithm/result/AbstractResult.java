package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.PrintStream;
import java.util.List;

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
   * Creates a new abstract result object.
   *
   * @param db         the database containing the objects of this result
   */
  protected AbstractResult(Database<T> db) {
    this.db = db;
  }

  /**
   * Writes a header with the parameters of the underlying algorithm and the
   * minima and maxima values of the normalization.
   *
   * @param out      the print stream where to write
   * @param settings the settings to be written into the header
   */
  protected void writeHeader(PrintStream out, List<AttributeSettings> settings) throws NonNumericFeaturesException {
    out.println("################################################################################");
    out.println("### db size = " + db.size());
    out.println("### db dimensionality = " + db.dimensionality());
    out.println("###");


    for (AttributeSettings setting : settings) {
      out.println(setting.toString("### "));
      out.println("###");
    }

    out.println("################################################################################");
  }
}
