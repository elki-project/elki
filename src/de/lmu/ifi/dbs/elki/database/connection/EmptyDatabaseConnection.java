package de.lmu.ifi.dbs.elki.database.connection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
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
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return EmptyDatabaseConnection
   */
  public static <O extends DatabaseObject> AbstractDatabaseConnection<O> parameterize(Parameterization config) {
    AbstractDatabaseConnection.Parameters<O> p = AbstractDatabaseConnection.getParameters(config);
    return new EmptyDatabaseConnection<O>(p.database);
  }

}