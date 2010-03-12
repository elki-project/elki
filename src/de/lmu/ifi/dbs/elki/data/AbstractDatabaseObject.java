package de.lmu.ifi.dbs.elki.data;

/**
 * Abstract super class for all database objects. Provides the required access
 * methods for the unique object id.
 * 
 * @author Elke Achtert
 */
public abstract class AbstractDatabaseObject implements DatabaseObject {
  /**
   * The unique id of this object.
   */
  private Integer id;

  public final Integer getID() {
    return id;
  }

  public void setID(Integer id) {
    this.id = id;
  }
}
