package de.lmu.ifi.dbs.data;

/**
 * To be a database object is the least requirement for an object to apply
 * distance based approaches. <p/>
 * Any implementing class should ensure to have a proper distance function
 * provided, that can handle the respective class.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface DatabaseObject<O extends DatabaseObject> {
  /**
   * Equality of DatabaseObject should be defined by their values
   * regardless of their id.
   *
   * @param obj another DatabaseObject
   * @return true if all values of both DatabaseObjects are equal, false otherwise
   */
  abstract boolean equals(Object obj);

  /**
   * Returns the unique id of this database object.
   *
   * @return the unique id of this database object
   */
  Integer getID();

  /**
   * Sets the id of this database object. The id must be unique within one
   * database.
   *
   * @param id the id to be set
   */
  void setID(Integer id);

  /**
   * Provides a deep copy of this object.
   *
   * @return a copy of this object
   */
  O copy();
}
