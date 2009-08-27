package experimentalcode.marisa.index.xtree.common;

import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialLeafEntry;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
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
   * @param file the file storing the X-Tree
   * @param capacity the capacity (maximum number of entries plus 1 for
   *        overflow) of this node
   * @param isLeaf indicates whether this node is a leaf node
   */
  public XTreeNode(PageFile<XTreeNode> file, int capacity, boolean isLeaf, Class<? extends SpatialEntry> eclass) {
    super(file, capacity, isLeaf, (Class<? super SpatialEntry>) eclass);
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
    return new XTreeNode(getFile(), capacity, true, SpatialLeafEntry.class);
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
    return new XTreeNode(getFile(), capacity, false, XDirectoryEntry.class);
  }


}
