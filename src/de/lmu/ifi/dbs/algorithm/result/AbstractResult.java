package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;

/**
 * Abstract super class for a result object. Encapsulates methods common for
 * many result objects.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractResult<O extends DatabaseObject> extends AbstractLoggable implements
                                                               Result<O> {
  /**
   * Extension for txt-files.
   */
  public static final String FILE_EXTENSION = ".txt";

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
	  super(LoggingConfiguration.DEBUG);
    this.db = db;
  }

  /**
   * Writes a header providing information concerning the underlying database
   * and the specified parameter-settings.
   *
   * @param out               the print stream where to write
   * @param settings          the settings to be written into the header
   * @param headerInformation additional information to be printed in the header, each entry
   *                          will be printed in one separate line
   */
  protected void writeHeader(PrintStream out,
                             List<AttributeSettings> settings, List<String> headerInformation) {
    if (settings == null && headerInformation == null)
      return;
    out.println("################################################################################");
    out.println("### db size = " + db.size());
    // noinspection EmptyCatchBlock
    try {
      int dimensionality = db.dimensionality();
      out.println("### db dimensionality = " + dimensionality);
    }
    catch (UnsupportedOperationException e) {
      // dimensionality is unsupported - do nothing
    }
    if (headerInformation != null) {
      for (String s : headerInformation) {
        out.println("### " + s);
      }
    }
    out.println("###");

    for (AttributeSettings setting : settings) {
      if (!setting.getSettings().isEmpty()) {
        out.println(setting.toString("### "));
        out.println("###");
      }
    }

    out.println("################################################################################");
  }

  /**
   * @see Result#output(File, Normalization, List)
   */
  public void output(File out, Normalization<O> normalization,
                     List<AttributeSettings> settings) throws UnableToComplyException {
    PrintStream outStream;
    try
    {
      outStream = new PrintStream(new FileOutputStream(out));
    }
    catch (Exception e)
    {
      outStream = new PrintStream(new FileOutputStream(FileDescriptor.out));
    }
    output(outStream, normalization, settings);
  }

  /**
   * Returns the database to which this clustering result belongs to.
   *
   * @return the database to which this clustering result belongs to
   */
  public Database<O> getDatabase() {
    return db;
  }
}
