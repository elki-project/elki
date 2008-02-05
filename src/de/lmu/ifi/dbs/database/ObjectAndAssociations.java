package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.DatabaseObject;

/**
 * Provides a single database objects and a map of associations associated with
 * this object.
 *
 * @author Elke Achtert 
 */
public class ObjectAndAssociations<O extends DatabaseObject> {
  /**
   * The database object.
   */
  private final O object;

  /**
   * The map of associations associated with the database objects.
   */
  private final Associations associations;

  /**
   * Provides a single database objects and a map of associations associated
   * with this object.
   *
   * @param object       the database object
   * @param associations the map of associations associated with the database objects
   */
  public ObjectAndAssociations(O object, Associations associations) {
    this.object = object;
    this.associations = associations;
  }

  /**
   * Returns the database object.
   *
   * @return the database object
   */
  public O getObject() {
    return object;
  }

  /**
   * Returns the associations associated with the database object.
   *
   * @return the associations associated with the database object
   */
  public Associations getAssociations() {
    return associations;
  }

  /**
   * Adds the given association with the specified association ID.
   *
   * @param associationID the association ID
   * @param association   the association to be added
   */
  public <T> void addAssociation(AssociationID<T> associationID, T association) {
    associations.put(associationID, association);
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  @Override
public String toString() {
    return object.toString() + " " + associations.toString();
  }
}
