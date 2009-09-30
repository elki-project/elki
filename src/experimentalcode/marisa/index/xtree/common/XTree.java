package experimentalcode.marisa.index.xtree.common;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialLeafEntry;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import experimentalcode.marisa.index.xtree.XDirectoryEntry;
import experimentalcode.marisa.index.xtree.XTreeBase;

/**
 * The XTree is a spatial index structure extending the R*-Tree. The
 * implementation is based on: <a
 * href="http://www.dbs.ifi.lmu.de/Publikationen/Papers/x-tree.ps">The X-tree:
 * An Index Structure for High-Dimensional Data by Stefan Berchtold, Daniel A.
 * Keim, Hans-Peter Kriegel</a>.
 * 
 * @author Marisa Thoma
 * @param <O> Object type
 */
public class XTree<O extends NumberVector<O, ?>> extends XTreeBase<O, XTreeNode, SpatialEntry> {

  /**
   * Creates a new XStar-Tree with default parameters.
   */
  public XTree() {
    super();
    // this.debug = true;
  }

  public XTree(List<String> parameters) throws ParameterException {
    super();
    setParameters(parameters);
    initializeFromFile();
    // this.debug = true;
  }

  /**
   * Creates an entry representing the root node.
   */
  @Override
  protected SpatialEntry createRootEntry() {
    return new XDirectoryEntry(0, null);
  }

  @Override
  protected SpatialEntry createNewLeafEntry(O o) {
    return new SpatialLeafEntry(o.getID(), getValues(o));
  }

  @Override
  protected SpatialEntry createNewDirectoryEntry(XTreeNode node) {
    return new XDirectoryEntry(node.getID(), node.mbr());
  }

  /**
   * Creates a new leaf node with the specified capacity.
   * 
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  @Override
  protected XTreeNode createNewLeafNode(int capacity) {
    return new XTreeNode(file, capacity, true, SpatialLeafEntry.class);
  }

  /**
   * Creates a new directory node with the specified capacity.
   * 
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  @Override
  protected XTreeNode createNewDirectoryNode(int capacity) {
    return new XTreeNode(file, capacity, false, XDirectoryEntry.class);
  }

  /**
   * Performs necessary operations after deleting the specified object.
   * 
   * @param o the object to be deleted
   */
  @Override
  protected void postDelete(O o) {
    // do nothing
  }

  /**
   * Return the node base class.
   * 
   * @return node base class
   */
  @Override
  protected Class<XTreeNode> getNodeClass() {
    return XTreeNode.class;
  }
}
