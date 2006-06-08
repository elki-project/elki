package de.lmu.ifi.dbs.index.spatial.rstar.rdnn;

import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.spatial.rstar.RTreeNode;
import de.lmu.ifi.dbs.persistent.PageFile;

/**
 * Represents a node in a RDkNN-Tree.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class RdKNNTreeNode<D extends Distance> extends RTreeNode {
  /**
   * Empty constructor for Externalizable interface.
   */
  public RdKNNTreeNode() {
  }

  /**
   * Creates a new Node object.
   *
   * @param file     the file storing the RTree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow)
   *                 of this node
   * @param isLeaf   indicates wether this node is a leaf node
   */
  public RdKNNTreeNode(PageFile<RTreeNode> file, int capacity, boolean isLeaf) {
    super(file, capacity, isLeaf);
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected RTreeNode createNewLeafNode(int capacity) {
    return new RdKNNTreeNode<D>(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected RTreeNode createNewDirectoryNode(int capacity) {
    return new RdKNNTreeNode<D>(file, capacity, false);
  }

}
