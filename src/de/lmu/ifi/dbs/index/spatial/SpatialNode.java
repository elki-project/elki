package de.lmu.ifi.dbs.index.spatial;

import de.lmu.ifi.dbs.index.Node;

/**
 * Defines the requirements for an object that can be used as a node in a SpatialIndex.
 * A spatial node can be a spatial directory node or a spatial leaf node.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface SpatialNode extends Node {

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
   * Returns the entry at the specified index.
   *
   * @param index the index of the entry to be returned
   * @return the entry at the specified index
   */
  Entry getEntry(int index);

  /**
   * Returns the id of the parent node of this spatial object.
   *
   * @return the id of the parent node of this spatial object
   */
  int getParentID();

  /**
   * Returns the dimensionality of this spatial object.
   *
   * @return the dimensionality of this spatial object
   */
  int getDimensionality();

  /**
   * Computes and returns the MBR of this spatial object.
   *
   * @return the MBR of this spatial object
   */
  MBR mbr();
}
