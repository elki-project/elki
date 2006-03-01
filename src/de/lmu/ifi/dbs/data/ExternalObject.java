package de.lmu.ifi.dbs.data;

/**
 * Provides an id referencing an external data object.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ExternalObject extends AbstractDatabaseObject {
  /**
   * Creates a new ExternalObject with the specified id.
   *
   * @param externalID the external id
   */
  public ExternalObject(Integer externalID) {
    setID(externalID);
  }
}
