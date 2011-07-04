package experimentalcode.marisa.index.xtree.common;

import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialPointLeafEntry;
import experimentalcode.marisa.index.xtree.XDirectoryEntry;
import experimentalcode.marisa.index.xtree.XNode;

/**
 * Represents a node in an X-Tree.
 * 
 * @author Marisa Thoma
 */
public class XTreeNode extends XNode<SpatialEntry, XTreeNode> {
  /**
   * Empty constructor for Externalizable interface.
   */
  public XTreeNode() {
    // empty constructor
  }

  /**
   * Creates a new XTreeNode with the specified parameters.
   * 
   * @param capacity the capacity (maximum number of entries plus 1 for
   *        overflow) of this node
   * @param isLeaf indicates whether this node is a leaf node
   */
  public XTreeNode(int capacity, boolean isLeaf, Class<? extends SpatialEntry> eclass) {
    super( capacity, isLeaf, (Class<? super SpatialEntry>) eclass);
  }

  /**
   * Creates a new leaf node with the specified capacity. Subclasses have to
   * overwrite this method.
   * 
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  @Override
  protected XTreeNode createNewLeafNode(int capacity) {
    return new XTreeNode(capacity, true, SpatialPointLeafEntry.class);
  }

  /**
   * Creates a new directory node with the specified capacity. Subclasses have
   * to overwrite this method.
   * 
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  @Override
  protected XTreeNode createNewDirectoryNode(int capacity) {
    return new XTreeNode(capacity, false, XDirectoryEntry.class);
  }
}