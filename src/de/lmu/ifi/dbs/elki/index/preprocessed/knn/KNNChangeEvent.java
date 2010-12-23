package de.lmu.ifi.dbs.elki.index.preprocessed.knn;

import java.util.EventObject;

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Encapsulates information describing changes of the k nearest neighbors (kNNs)
 * of some objects due to insertion or removal of objects. Used to notify all
 * subscribed {@link KNNListener} of the change.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has DBIDs
 * 
 * @see KNNListener
 */
public class KNNChangeEvent extends EventObject {
  /**
   * Serialization ID since Java EventObjects are expected to be serializable.
   */
  private static final long serialVersionUID = 513913140334355886L;

  /**
   * Available event types.
   * 
   * @apiviz.exclude
   */
  public enum Type {
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
   * Holds the type of this change event.
   * 
   * @see Type
   */
  private final Type type;

  /**
   * The ids of the kNNs that were inserted or deleted due to the insertion or
   * removals of objects.
   */
  private final DBIDs objects;

  /**
   * The ids of the kNNs that were updated due to the insertion or removals of
   * objects.
   */
  private final DBIDs updates;

  /**
   * Used to create an event when kNNs of some objects have been changed.
   * 
   * @param source the object responsible for generating the event
   * @param type the type of change
   * @param objects the ids of the removed or inserted kNNs (according to the
   *        type of this event)
   * @param updates the ids of kNNs which have been changed due to the removals
   *        or insertions
   * @see Type#INSERT
   * @see Type#DELETE
   */
  public KNNChangeEvent(Object source, Type type, DBIDs objects, DBIDs updates) {
    super(source);
    this.type = type;
    this.objects = objects;
    this.updates = updates;
  }

  /**
   * Returns the type of this change event.
   * 
   * @return {@link Type#INSERT} or {@link Type#DELETE}
   */
  public Type getType() {
    return type;
  }

  /**
   * Returns the ids of the removed or inserted kNNs (according to the type of
   * this event).
   * 
   * @return the ids of the removed or inserted kNNs
   */
  public DBIDs getObjects() {
    return objects;
  }

  /**
   * Returns the ids of kNNs which have been changed due to the removals or
   * insertions.
   * 
   * @return the ids of kNNs which have been changed
   */
  public DBIDs getUpdates() {
    return updates;
  }
}