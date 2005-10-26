package de.lmu.ifi.dbs.index;

/**
 * Defines the requirements for an identifier in an index. The identifier can represent
 * a node id  or a data object id.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface Identifier {
  /**
   * Returns the value of this identifier.
   * @return the value of this identifier
   */
  Integer value();

  /**
   * Returns true, if this identifier represents a node id, false otherwise.
   * @return true, if this identifier represents a node id, false otherwise
   */
  boolean isNodeID();
}
