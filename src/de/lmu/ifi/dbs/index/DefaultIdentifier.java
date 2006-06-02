package de.lmu.ifi.dbs.index;

/**
 * Default implementation of the identifier interface.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DefaultIdentifier implements Identifier {
  /**
   * Holds the value of this identifier.
   */
  private int value;

  /**
   * True, if this identifier represents a node id, false otherwise.
   */
  private boolean isNodeID;

  public DefaultIdentifier(int value, boolean nodeID) {
    this.value = value;
    isNodeID = nodeID;
  }

  /**
   * Returns the value of this identifier.
   *
   * @return the value of this identifier
   */
  public Integer value() {
    return value;
  }

  /**
   * Returns true, if this identifier represents a node id, false otherwise.
   *
   * @return true, if this identifier represents a node id, false otherwise
   */
  public boolean isNodeID() {
    return isNodeID;
  }
}
