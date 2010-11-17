package de.lmu.ifi.dbs.elki.database.query;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;

/**
 * Abstract query bound to a certain database.
 * 
 * @author Erich Schubert
 *
 * @param <O> Database object type
 */
public abstract class AbstractDatabaseQuery<O extends DatabaseObject> implements DatabaseQuery {
  /**
   * Database for this query
   */
  final protected Database<? extends O> database;

  /**
   * Database this query works on.
   * 
   * @param database Database
   */
  public AbstractDatabaseQuery(Database<? extends O> database) {
    super();
    this.database = database;
  }
}
