package de.lmu.ifi.dbs.elki.database.connection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Pseudo database that is empty. Ugly hack.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
@Title("Empty Database")
@Description("Dummy database implementation that cannot not contain any objects.")
public class EmptyDatabaseConnection<O extends DatabaseObject> extends AbstractDatabaseConnection<O> {
  /**
   * Constructor.
   * 
   * @param database the instance of the database
   */
  protected EmptyDatabaseConnection(Database<O> database) {
    super(database, null, null, null);
  }

  /**
   * @param normalization ignored for an empty database.
   */
  @Override
  public Database<O> getDatabase(Normalization<O> normalization) {
    return database;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O extends DatabaseObject> extends AbstractDatabaseConnection.Parameterizer<O> {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configDatabase(config);
    }

    @Override
    protected EmptyDatabaseConnection<O> makeInstance() {
      return new EmptyDatabaseConnection<O>(database);
    }
  }
}