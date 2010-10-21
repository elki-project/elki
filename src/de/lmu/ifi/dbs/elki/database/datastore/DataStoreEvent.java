package de.lmu.ifi.dbs.elki.database.datastore;

import java.util.EventObject;

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Encapsulates information describing changes, i.e. updates, insertions, or
 * deletions in a {@link DataStore}, and used to notify all subscribed
 * {@link DataStoreListener} of the change.
 * 
 * @author Elke Achtert
 * @param <T> the data type as element of the {@link DataStore}
 * @see DataStore
 * @see DataStoreListener
 */
public class DataStoreEvent<T> extends EventObject {

  /**
   * Serialization ID
   */
  private static final long serialVersionUID = 7183716156466324055L;

  /**
   * The ids of the objects that have been updated in the the {@link DataStore}.
   */
  private DBIDs updates;

  /**
   * The ids of the objects that have been newly inserted into the
   * {@link DataStore}.
   */
  private DBIDs insertions;

  /**
   * The ids of the objects that have been removed from the {@link DataStore}.
   */
  private DBIDs deletions;

  /**
   * Used to create an event when objects have been updated in, inserted into,
   * or removed from the specified {@link DataStore}.
   * 
   * @param source the datastore responsible for generating the event
   * @param updates the ids of the objects that have been updated
   * @param insertions the ids of the objects that have been newly inserted
   * @param deletions the ids of the objects that have been removed
   */
  public DataStoreEvent(DataStore<T> source, DBIDs updates, DBIDs insertions, DBIDs deletions) {
    super(source);
    this.updates = updates;
    this.insertions = insertions;
    this.deletions = deletions;
  }

  /**
   * Returns the ids of the objects that have been updated.
   * 
   * @return the ids of the objects that have been updated
   */
  public DBIDs getUpdateIDs() {
    return updates;
  }
  
  /**
   * Returns the ids of the objects that have been newly inserted.
   * 
   * @return the ids of the objects that have been newly inserted
   */
  public DBIDs getInsertionsIDs() {
    return insertions;
  }
  
  /**
   * Returns the ids of the objects that have been removed.
   * 
   * @return the ids of the objects that have been removed
   */
  public DBIDs getDeletionsIDs() {
    return deletions;
  }

  /**
   * Returns the datastore on which the changes have been occurred.
   * 
   * @return the datastore
   * @see #getSource()
   */
  @SuppressWarnings("unchecked")
  public DataStore<T> getDataStore() {
    return (DataStore<T>) super.getSource();
  }

}
