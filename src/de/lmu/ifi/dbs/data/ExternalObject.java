package de.lmu.ifi.dbs.data;

/**
 * Provides an id referencing an external data object.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ExternalObject extends AbstractDatabaseObject<ExternalObject> {
  /**
   * Creates a new ExternalObject with the specified id.
   *
   * @param id the external id
   */
  public ExternalObject(Integer id) {
    setID(id);
  }

  /**
   * Provides a deep copy of this object.
   *
   * @return a copy of this object
   */
  public ExternalObject copy() {
    return new ExternalObject(getID());
  }
}
