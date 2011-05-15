package de.lmu.ifi.dbs.elki.datasource;

import de.lmu.ifi.dbs.elki.database.UpdatableDatabase;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Pseudo database that is empty.
 * 
 * @author Erich Schubert
 */
@Title("Empty Database")
@Description("Dummy database implementation that cannot not contain any objects.")
public class EmptyDatabaseConnection extends AbstractDatabaseConnection {
  /**
   * Static logger
   */
  private static final Logging logger = Logging.getLogger(EmptyDatabaseConnection.class);
  
  /**
   * Constructor.
   * 
   * @param database the instance of the database
   */
  protected EmptyDatabaseConnection(UpdatableDatabase database) {
    super(database, null);
  }

  @Override
  public UpdatableDatabase getDatabase() {
    return database;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractDatabaseConnection.Parameterizer {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configDatabase(config);
    }

    @Override
    protected EmptyDatabaseConnection makeInstance() {
      return new EmptyDatabaseConnection(database);
    }
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}