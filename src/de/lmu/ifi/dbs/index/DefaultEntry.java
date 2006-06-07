package de.lmu.ifi.dbs.index;

/**
 * Default implementation of the identifier interface.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DefaultEntry implements Entry {
  /**
   * Holds the value of this identifier.
   */
  private int value;

  /**
   * True, if this identifier represents a node id, false otherwise.
   */
  private boolean isNodeID;

  public DefaultEntry(int value, boolean nodeID) {
    this.value = value;
    isNodeID = nodeID;
  }

  /**
   * Returns the value of this identifier.
   *
   * @return the value of this identifier
   */
  public Integer getID() {
    return value;
  }

  /**
   * Returns true, if this identifier represents a node id, false otherwise.
   *
   * @return true, if this identifier represents a node id, false otherwise
   */
  public boolean representsNode() {
    return isNodeID;
  }
}
