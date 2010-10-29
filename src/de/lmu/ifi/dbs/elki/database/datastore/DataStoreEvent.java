package de.lmu.ifi.dbs.elki.database.datastore;

import java.util.Collection;
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
   * Serialization ID since Java EventObjects are expected to be serializable.
   */
  private static final long serialVersionUID = 7183716156466324055L;

  /**
   * The IDs of the objects that have been updated in the the {@link DataStore}.
   */
  private DBIDs updateIDs;

  /**
   * The IDs of the objects that have been newly inserted into the
   * {@link DataStore}.
   */
  private DBIDs insertionIDs;

  /**
   * The objects that have been removed from the {@link DataStore}.
   */
  private Collection<T> deletions;

  /**
   * Used to create an event when objects have been updated in, inserted into,
   * or removed from the specified {@link DataStore}.
   * 
   * @param source the object responsible for generating the event
   * @param updateIDs the IDs of the objects that have been updated
   * @param insertionIDs the IDs of the objects that have been newly inserted
   * @param deletions the objects that have been removed
   */
  public DataStoreEvent(Object source, DBIDs updateIDs, DBIDs insertionIDs, Collection<T> deletions) {
    super(source);
    this.updateIDs = updateIDs;
    this.insertionIDs = insertionIDs;
    this.deletions = deletions;
  }

  /**
   * Returns the IDs of the objects that have been updated.
   * 
   * @return the IDs of the objects that have been updated
   */
  public DBIDs getUpdateIDs() {
    return updateIDs;
  }

  /**
   * Returns the IDs of the objects that have been newly inserted.
   * 
   * @return the IDs of the objects that have been newly inserted
   */
  public DBIDs getInsertionsIDs() {
    return insertionIDs;
  }

  /**
   * Returns the objects that have been removed.
   * 
   * @return the objects that have been removed
   */
  public Collection<T> getDeletions() {
    return deletions;
  }

  /**
   * Returns true if this event contains only updates, false otherwise.
   * 
   * @return true if this event contains no insertions or deletions
   */
  public boolean isUpdateEvent() {
    return ((insertionIDs == null || insertionIDs.isEmpty()) && (deletions == null || deletions.isEmpty()));
  }

}
