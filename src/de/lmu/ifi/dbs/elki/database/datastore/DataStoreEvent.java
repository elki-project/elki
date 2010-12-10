package de.lmu.ifi.dbs.elki.database.datastore;

import java.util.Collection;
import java.util.EventObject;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates information describing changes, i.e. updates, insertions, and /
 * or deletions in a {@link DataStore}, and used to notify all subscribed
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
   * Available event types.
   * 
   * @apiviz.exclude
   */
  public enum Type {
    /**
     * Identifies a change on existing objects.
     */
    UPDATE,

    /**
     * Identifies the insertion of new objects.
     */
    INSERT,

    /**
     * Identifies the removal of objects.
     */
    DELETE
  }

  /**
   * The objects that were changed in the {@link DataStore} mapped by the type
   * of change.
   */
  private final Map<Type, Collection<T>> objects;

  /**
   * Used to create an event when objects have been updated in, inserted into,
   * and / or removed from the specified {@link DataStore}.
   * 
   * @param source the object responsible for generating the event
   * @param objects the objects that have been changed mapped by the type of
   *        change
   * @see {@link Type#INSERT}, {@link Type#DELETE}, {@link Type#UPDATE}
   */
  public DataStoreEvent(Object source, Map<Type, Collection<T>> objects) {
    super(source);
    this.objects = objects;
  }

  /**
   * Returns the types of change this event consists of.
   * 
   * @see {@link Type#INSERT}, {@link Type#DELETE}, {@link Type#UPDATE}
   * 
   * @return the types of this event
   */
  public Set<Type> getTypes() {
    return objects.keySet();
  }

  /**
   * Returns the objects that have been changed and the type of change.
   * 
   * @return the objects that have been changed
   */
  public Map<Type, Collection<T>> getObjects() {
    return objects;
  }

}
