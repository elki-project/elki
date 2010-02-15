package de.lmu.ifi.dbs.elki.database.connection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Pseudo database that is empty. Ugly hack.
 * 
 * @author Erich Schubert
 * 
 * @param <O>
 */
public class EmptyDatabaseConnection<O extends DatabaseObject> extends AbstractDatabaseConnection<O> {
  /**
   * Constructor.
   * 
   * @param config Configuration
   */
  protected EmptyDatabaseConnection(Parameterization config) {
    super(config, false);
  }

  /**
   * @param normalization ignored for an empty database.
   */
  @Override
  public Database<O> getDatabase(Normalization<O> normalization) {
    return database;
  }

  @Override
  public String shortDescription() {
    StringBuffer description = new StringBuffer();
    description.append(this.getClass().getName());
    description.append(" provides an empty database.");
    return description.toString();
  }
}
