package de.lmu.ifi.dbs.elki.database.connection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.Normalization;

/**
 * Pseudo database that is empty. Ugly hack.
 * 
 * @author Erich Schubert
 *
 * @param <O>
 */
public class EmptyDatabaseConnection<O extends DatabaseObject> extends AbstractDatabaseConnection<O> {
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
