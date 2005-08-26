package de.lmu.ifi.dbs.index.spatial;

import java.util.Enumeration;


/**
 * Defines the requirements for an object that can be used as a node in a SpatialIndex.
 * A spatial node can be a spatial directory node or a spatial leaf node.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface SpatialNode extends SpatialObject {

  /**
   * Returns the id of this node.
   *
   * @return the id of this node
   */
  int getNodeID();

  /**
   * Returns the number of entries of this node.
   *
   * @return the number of entries of this node
   */
  int getNumEntries();

  /**
   * Returns true if this node is a leaf node, false otherwise.
   *
   * @return true if this node is a leaf node, false otherwise
   */
  boolean isLeaf();

  /**
   * Returns an enumeration of the children of this node.
   *
   * @return an enumeration of the children of this node
   */
  Enumeration<Entry> children();

  /**
   * Returns the entry at the specified index.
   *
   * @param index the index of the entry to be returned
   * @return the entry at the specified index
   */
  Entry getEntry(int index);

}
