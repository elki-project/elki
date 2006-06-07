package de.lmu.ifi.dbs.index;

import java.io.Externalizable;

/**
 * Defines the requirements for an entry in an index structure. An entry can
 * represent a node or a data object.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface Entry extends Externalizable {
  /**
   * Returns the id of the node or data object that is represented by this entry.
   *
   * @return the id of the node or data object that is represented by this entry
   */
  Integer getID();

  /**
   * Returns true if this entry is an entry in a leaf node
   * (i.e. this entry represents a data object),  false otherwise.
   *
   * @return true if this entry is an entry in a leaf node, false otherwise
   */
  public boolean isLeafEntry();
}
