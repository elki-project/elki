package de.lmu.ifi.dbs.index.spatial.rstarvariants.deliclu;

import de.lmu.ifi.dbs.index.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.persistent.PageFile;

/**
 * Represents a node in a DeLiClu-Tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DeLiCluNode extends AbstractRStarTreeNode<DeLiCluNode, DeLiCluEntry> {
  /**
   * Empty constructor for Externalizable interface.
   */
  public DeLiCluNode() {
  }

  /**
   * Creates a new DeLiCluNode with the specified parameters.
   *
   * @param file     the file storing the DeLiClu-Tree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow)
   *                 of this node
   * @param isLeaf   indicates wether this node is a leaf node
   */
  public DeLiCluNode(PageFile<DeLiCluNode> file, int capacity, boolean isLeaf) {
    super(file, capacity, isLeaf);
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected DeLiCluNode createNewLeafNode(int capacity) {
    return new DeLiCluNode(getFile(), capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected DeLiCluNode createNewDirectoryNode(int capacity) {
    return new DeLiCluNode(getFile(), capacity, false);  }
}
