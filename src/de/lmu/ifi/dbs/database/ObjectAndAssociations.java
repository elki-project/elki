package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.DatabaseObject;

import java.util.Map;

/**
 * Provides a single database objects and a map of associations associated with this object.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ObjectAndAssociations<O extends DatabaseObject> {
  /**
   * The database object.
   */
  private final O object;

  /**
   * The map of associations associated with the database objects.
   */
  private final Map<AssociationID, Object> associations;

  /**
   * Provides a single database objects and a map of associations associated with this object.
   *
   * @param object       the database object
   * @param associations the map of associations associated with the database objects
   */
  public ObjectAndAssociations(O object, Map<AssociationID, Object> associations) {
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
   * Returns the list of string labels associated with the database object.
   *
   * @return the list of string labels associated with the database object
   */
  public Map<AssociationID, Object> getAssociations() {
    return associations;
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    return object.toString() + " " + associations.toString();
  }
}
