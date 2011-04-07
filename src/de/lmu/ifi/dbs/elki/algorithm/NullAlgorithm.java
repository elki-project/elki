package de.lmu.ifi.dbs.elki.algorithm;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * Null Algorithm, which does nothing. Can be used to e.g. just visualize a data
 * set.
 * 
 * @author Erich Schubert
 */
@Title("Null Algorithm")
@Description("Algorithm which does nothing, just return a null object.")
public class NullAlgorithm extends AbstractAlgorithm<DatabaseObject, Result> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(NullAlgorithm.class);
  
  /**
   * Constructor.
   */
  public NullAlgorithm() {
    super();
  }

  /**
   * Iterates over all points in the database.
   */
  @Override
  protected Result runInTime(@SuppressWarnings("unused") Database<DatabaseObject> database) throws IllegalStateException {
    return null;
  }
  
  @Override
  protected Logging getLogger() {
    return logger;
  }
}