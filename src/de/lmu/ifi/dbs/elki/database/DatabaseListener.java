package de.lmu.ifi.dbs.elki.database;

import java.util.EventListener;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;

/**
 * Defines the interface for an object that listens to changes in a Database.
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject as element of the database
 */
public interface DatabaseListener<O extends DatabaseObject> extends EventListener {
  /**
   * Invoked after objects of the database have been updated in some way. Use
   * <code>e.getObjectIDs()</code> to get the ids of the updated database
   * objects.
   * 
   * @param e the update event
   */
  void objectsChanged(DatabaseEvent<O> e);

  /**
   * Invoked after objects have been inserted into the database. Use
   * <code>e.getObjectIDs()</code> to get the ids of the newly inserted database
   * objects.
   * 
   * @param e the insertion event
   */
  void objectsInserted(DatabaseEvent<O> e);

  /**
   * Invoked after objects have been deleted from the database. Use
   * <code>e.getObjectIDs()</code> to get the ids of the removed database
   * objects.
   * 
   * @param e the removal event
   */
  void objectsRemoved(DatabaseEvent<O> e);
}
