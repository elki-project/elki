package de.lmu.ifi.dbs.data;

/**
 * Provides an id referencing to an external data object.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ExternalObject implements MetricalObject<ExternalObject> {
  /**
   * The unique id within one database.
   */
  private Integer id;

  /**
   * Creates a new ExternalObject with the specified id.
   * @param id the external id
   */
  public ExternalObject(Integer id) {
    this.id = id;
  }

  /**
   * Returns the unique id of this metrical object.
   *
   * @return the unique id of this metrical object
   */
  public Integer getID() {
    return id;
  }

  /**
   * Sets the id of this metrical object. The id must be unique within one
   * database.
   *
   * @param id the id to be set
   */
  public void setID(Integer id) {
    this.id = id;
  }

  /**
   * Provides a deep copy of this object.
   *
   * @return a copy of this object
   */
  public ExternalObject copy() {
    return new ExternalObject(this.id);
  }
}
