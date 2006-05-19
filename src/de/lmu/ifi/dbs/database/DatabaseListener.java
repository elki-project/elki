package de.lmu.ifi.dbs.database;

/**
 * Defines the interface for an object that listens to changes in a Database.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface DatabaseListener {
  /**
   * Invoked after objects of the database have been updated in some way.
   * Use <code>e.getObjects()</code> to get the updated database objects.
   */
  void objectsChanged(DatabaseEvent e);

  /**
   * Invoked after an object has been inserted into the database.
   * Use <code>e.getObjects()</code> to get the newly inserted database objects.
   */
  void objectsInserted(DatabaseEvent e);

  /**
   * Invoked after an object has been deleted from the database.
   * Use <code>e.getObjects()</code> to get the inserted database objects.
   */
  void objectsRemoved(DatabaseEvent e);
}
