package de.lmu.ifi.dbs.elki.database.datastore;

import java.util.Collection;
import java.util.EventObject;
import java.util.Map;

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
    DELETE,
    
    INSERT_AND_UPDATE,
    DELETE_AND_UPDATE
    
  }

  /**
   * The event type.
   */
  private final Type type;

  /**
   * The objects that were changed in the {@link DataStore}.
   */
  private final Map<Type, Collection<T>> objects;

  /**
   * Used to create an event when objects have been updated in, inserted into,
   * or removed from the specified {@link DataStore}.
   * 
   * @param source the object responsible for generating the event
   * @param objects the objects that have been changed
   * @param type the event type: {@link Type#UPDATE}, {@link Type#INSERT} or
   *        {@link Type#DELETE}
   */
  public DataStoreEvent(Object source, Map<Type, Collection<T>> objects, Type type) {
    super(source);
    this.objects = objects;
    this.type = type;
  }

  /**
   * Returns the event type, i.e. {@link Type#UPDATE}, {@link Type#INSERT} or
   * {@link Type#DELETE}
   * 
   * @return the type of this event
   */
  public Type getType() {
    return type;
  }

  /**
   * Returns the objects that have been changed
   * and the type of change.
   * 
   * @return the objects that have been changed
   */
  public Map<Type, Collection<T>> getObjects() {
    return objects;
  }

 

}
