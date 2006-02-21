package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.PrintStream;
import java.util.List;

/**
 * Abstract super class for a result object. Encapsulates methods common for many result objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractResult<O extends DatabaseObject> implements Result<O> {
  /**
   * The database containing the objects of this result.
   */
  protected Database<O> db;

  /**
   * Creates a new abstract result object.
   *
   * @param db the database containing the objects of this result
   */
  protected AbstractResult(Database<O> db) {
    this.db = db;
  }

  /**
   * Writes a header providing information concerning the underlying database
   * and the specified parameter-settings.
   *
   * @param out      the print stream where to write
   * @param settings the settings to be written into the header
   */
  protected void writeHeader(PrintStream out, List<AttributeSettings> settings) {
    out.println("################################################################################");
    out.println("### db size = " + db.size());
    //noinspection EmptyCatchBlock
    try {
        int dimensionality = db.dimensionality();
      out.println("### db dimensionality = " + dimensionality);
    }
    catch (UnsupportedOperationException e) {
        // dimensionality is unsupported - do nothing
    }
    out.println("###");


    for (AttributeSettings setting : settings) {
      if (! setting.getSettings().isEmpty()) {
        out.println(setting.toString("### "));
        out.println("###");
      }
    }

    out.println("################################################################################");
  }
}
