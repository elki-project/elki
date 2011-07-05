package de.lmu.ifi.dbs.elki.index.tree;

import java.io.Externalizable;

/**
 * Defines the requirements for an entry in an index structure. An entry can
 * represent a node or a data object.
 * 
 * @author Elke Achtert
 */
public interface Entry extends Externalizable {
  /**
   * Returns true if this entry is an entry in a leaf node (i.e. this entry
   * represents a data object), false otherwise.
   * 
   * @return true if this entry is an entry in a leaf node, false otherwise
   */
  public boolean isLeafEntry();
}