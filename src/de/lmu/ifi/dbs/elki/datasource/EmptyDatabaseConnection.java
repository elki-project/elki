package de.lmu.ifi.dbs.elki.datasource;

import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * Pseudo database that is empty.
 * 
 * @author Erich Schubert
 */
@Title("Empty Database")
@Description("Dummy data source that does not provide any objects.")
public class EmptyDatabaseConnection extends AbstractDatabaseConnection {
  /**
   * Static logger
   */
  private static final Logging logger = Logging.getLogger(EmptyDatabaseConnection.class);
  
  /**
   * Constructor.
   */
  protected EmptyDatabaseConnection() {
    super(null);
  }
  
  @Override
  public MultipleObjectsBundle loadData() {
    // Return an empty bundle
    // TODO: add some dummy column, such as DBIDs?
    return new MultipleObjectsBundle();
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}