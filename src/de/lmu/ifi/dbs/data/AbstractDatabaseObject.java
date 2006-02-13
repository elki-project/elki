package de.lmu.ifi.dbs.data;

/**
 * Abstract super class for all database objects.
 * Provides the required access methods for the unique object id.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractDatabaseObject<O extends DatabaseObject> implements DatabaseObject<O> {
  /**
   * The unique id of this object.
   */
  private Integer id;

  /**
   * @see DatabaseObject#getID()
   */
  public final Integer getID() {
    return id;
  }

  /**
   * @see DatabaseObject#setID(Integer)
   */
  public void setID(Integer id) {
    this.id = id;
  }


}
