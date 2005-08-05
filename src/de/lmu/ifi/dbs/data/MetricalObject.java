package de.lmu.ifi.dbs.data;

/**
 * To be a metrical object is the least requirement for an object to apply
 * distance based approaches.
 * <p/>
 * So far, this is simply a marker interface. Any implementing class should ensure
 * to have a proper distance function provided, that can handle the respective class.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface MetricalObject {
  /**
   * Returns the unique id of this metrical object.
   *
   * @return the unique id of this metrical object
   */
  int getID();

  /**
   * Sets the id of this metrical object.
   * The id must be unique within one database.
   *
   * @param id the id to be set
   */
  void setID(int id);

  /**
   * Provides a deep copy of this object.
   *
   * @return a copy of this object
   */
  MetricalObject copy();

}
