package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.persistent.PageFile;

/**
 * Default class for a node in a RTree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class Node extends AbstractNode {
  /**
   * Empty constructor for Externalizable interface.
   */
  public Node() {
  }

  /**
   * Creates a new Node object.
   *
   * @param file     the file storing the RTree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow) of this node
   * @param isLeaf   indicates wether this node is a leaf node
   */
  public Node(PageFile<AbstractNode> file, int capacity, boolean isLeaf) {
    super(file, capacity, isLeaf);
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected AbstractNode createNewLeafNode(int capacity) {
    return new Node(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected AbstractNode createNewDirectoryNode(int capacity) {
    return new Node(file, capacity, false);
  }
}