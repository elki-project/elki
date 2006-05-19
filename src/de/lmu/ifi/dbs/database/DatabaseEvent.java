package de.lmu.ifi.dbs.database;

import java.util.EventObject;
import java.util.List;

/**
 * Encapsulates information describing changes, i.e. updates, insertions,
 * or deletions to a database, and used to notify database listeners of the change.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DatabaseEvent extends EventObject {
  /**
   * The ids of the database object that have been changed, i.e. updated, inserted or deleted.
   */
  private List<Integer> objectIDs;

  /**
   * Used to create an event when database objects have been updated, inserted, or
   * removed.
   *
   * @param source  the database responsible for generating the event
   * @param objectIDs the ids of the database objects that have been changed
   */
  public DatabaseEvent(Database source, List<Integer> objectIDs) {
    super(source);
    this.objectIDs = objectIDs;
  }

  /**
   * Returns the database object that have been changed.
   *
   * @return the database object that have been changed
   */
  public List<Integer> getObjectIDs() {
    return objectIDs;
  }
}
