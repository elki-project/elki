package de.lmu.ifi.dbs.index.tree.spatial.rstarvariants.deliclu;

import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.index.tree.BreadthFirstEnumeration;
import de.lmu.ifi.dbs.index.tree.Entry;
import de.lmu.ifi.dbs.index.tree.TreeIndexPath;
import de.lmu.ifi.dbs.index.tree.TreeIndexPathComponent;
import de.lmu.ifi.dbs.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.index.tree.spatial.rstarvariants.NonFlatRStarTree;
import de.lmu.ifi.dbs.utilities.HyperBoundingBox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DeLiCluTree is a spatial index structure based on an R-TRee. DeLiCluTree is
 * designed for the DeLiClu algorithm, having in each node a boolean array which
 * indicates whether the child nodes are already handled by the DeLiClu
 * algorithm.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DeLiCluTree<O extends NumberVector> extends NonFlatRStarTree<O, DeLiCluNode, DeLiCluEntry> {

  /**
   * Holds the ids of the expanded nodes.
   */
  private HashMap<Integer, HashSet<Integer>> expanded = new HashMap<Integer, HashSet<Integer>>();

  /**
   * Creates a new DeLiClu-Tree.
   */
  public DeLiCluTree() {
    super();
    this.debug = true;
  }

  /**
   * Marks the specified object as handled and returns the path of node ids
   * from the root to the objects's parent.
   *
   * @param o the object to be marked as handled
   * @return the path of node ids from the root to the objects's parent
   */
  public synchronized List<TreeIndexPathComponent<DeLiCluEntry>> setHandled(O o) {
    if (this.debug) {
      debugFine("setHandled " + o + "\n");
    }

    // find the leaf node containing o
    double[] values = getValues(o);
    HyperBoundingBox mbr = new HyperBoundingBox(values, values);
    TreeIndexPath<DeLiCluEntry> pathToObject = findPathToObject(getRootPath(), mbr, o.getID());

    if (pathToObject == null)
      return null;

    // set o handled
    DeLiCluEntry entry = pathToObject.getLastPathComponent().getEntry();
    entry.setHasHandled(true);
    entry.setHasUnhandled(false);

    for (TreeIndexPath<DeLiCluEntry> path = pathToObject; path.getParentPath() != null; path = path.getParentPath()) {
      DeLiCluEntry parentEntry = path.getParentPath().getLastPathComponent().getEntry();
      DeLiCluNode node = getNode(parentEntry);
      boolean allHandled = true;
      boolean allUnhandled = true;
      for (int i = 0; i < node.getNumEntries(); i++) {
        allHandled = allHandled && node.getEntry(i).hasHandled();
        allUnhandled = allUnhandled && node.getEntry(i).hasUnhandled();
      }
      parentEntry.setHasUnhandled(!allHandled);
      parentEntry.setHasHandled(!allUnhandled);

    }

    return pathToObject.getPath();
  }

  /**
   * Marks the nodes with the specified ids as expanded.
   *
   * @param entry1 the first node
   * @param entry2 the second node
   */
  public void setExpanded(SpatialEntry entry1, SpatialEntry entry2) {
    HashSet<Integer> exp1 = expanded.get(entry1.getID());
    if (exp1 == null) {
      exp1 = new HashSet<Integer>();
      expanded.put(entry1.getID(), exp1);
    }
    exp1.add(entry2.getID());
  }

  /**
   * Returns the nodes which are already expanded with the specified node.
   *
   * @param entry the id of the node for which the expansions should be returned
   * @return the nodes which are already expanded with the specified node
   */
  public Set<Integer> getExpanded(SpatialEntry entry) {
    HashSet<Integer> exp = expanded.get(entry.getID());
    if (exp != null)
      return exp;
    return new HashSet<Integer>();
  }

  /**
   * Returns the nodes which are already expanded with the specified node.
   *
   * @param entry the id of the node for which the expansions should be returned
   * @return the nodes which are already expanded with the specified node
   */
  public Set<Integer> getExpanded(DeLiCluNode entry) {
    HashSet<Integer> exp = expanded.get(entry.getID());
    if (exp != null)
      return exp;
    return new HashSet<Integer>();
  }

  /**
   * Determines and returns the number of nodes in this index.
   *
   * @return the number of nodes in this index
   */
  public int numNodes() {
    int numNodes = 0;

    BreadthFirstEnumeration<O, DeLiCluNode, DeLiCluEntry> bfs = new BreadthFirstEnumeration<O, DeLiCluNode, DeLiCluEntry>(this, getRootPath());
    while (bfs.hasMoreElements()) {
      Entry entry = bfs.nextElement().getLastPathComponent().getEntry();
      if (!entry.isLeafEntry()) {
        numNodes++;
      }
    }

    return numNodes;
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected DeLiCluNode createNewLeafNode(int capacity) {
    return new DeLiCluNode(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected DeLiCluNode createNewDirectoryNode(int capacity) {
    return new DeLiCluNode(file, capacity, false);
  }

  /**
   * Creates a new leaf entry representing the specified data object.
   *
   * @param object the data object to be represented by the new entry
   */
  protected DeLiCluEntry createNewLeafEntry(O object) {
    return new DeLiCluLeafEntry(object.getID(), getValues(object));
  }

  /**
   * Creates a new directory entry representing the specified node.
   *
   * @param node the node to be represented by the new entry
   */
  protected DeLiCluEntry createNewDirectoryEntry(DeLiCluNode node) {
    return new DeLiCluDirectoryEntry(node.getID(), node.mbr(), node.hasHandled(), node.hasUnhandled());
  }

  /**
   * Creates an entry representing the root node.
   *
   * @return an entry representing the root node
   */
  protected DeLiCluEntry createRootEntry() {
    return new DeLiCluDirectoryEntry(0, null, false, true);
  }

  /**
   * Performs necessary operations before inserting the specified entry.
   *
   * @param entry the entry to be inserted
   */
  protected void preInsert(DeLiCluEntry entry) {
    // do nothing
  }

  /**
   * Performs necessary operations after deleting the specified object.
   *
   * @param o the object to be deleted
   */
  protected void postDelete(O o) {
    // do nothing
  }
}
