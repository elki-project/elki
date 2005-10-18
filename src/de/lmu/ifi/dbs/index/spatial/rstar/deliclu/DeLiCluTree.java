package de.lmu.ifi.dbs.index.spatial.rstar.deliclu;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.index.spatial.BreadthFirstEnumeration;
import de.lmu.ifi.dbs.index.spatial.DirectoryEntry;
import de.lmu.ifi.dbs.index.spatial.Entry;
import de.lmu.ifi.dbs.index.spatial.MBR;
import de.lmu.ifi.dbs.index.spatial.rstar.RTree;
import de.lmu.ifi.dbs.index.spatial.rstar.AbstractNode;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.*;

/**
 * DeLiCluTree is a spatial index structure based on an R-TRee. DeLiCluTree is designed
 * for the DeLiClu algorithm, having in each node a boolean array which indicates whether
 * the child nodes are already handled by the DeLiClu algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DeLiCluTree<T extends RealVector> extends RTree<T> {

  /**
   * Holds the ids of the expanded nodes.
   */
  private HashMap<Integer, HashSet<Integer>> expanded = new HashMap<Integer, HashSet<Integer>>();

  /**
   * Creates a new DeLiClu-Tree from an existing persistent file.
   *
   * @param fileName  the name of the file storing the RTree
   * @param cacheSize the size of the cache in bytes
   */
  public DeLiCluTree(String fileName, int cacheSize) {
    super(fileName, cacheSize);
  }

  /**
   * Creates a new DeLiClu-Tree with the specified parameters.
   *
   * @param dimensionality the dimensionality of the data objects to be indexed
   * @param fileName       the name of the file for storing the entries,
   *                       if this parameter is null all entries will be hold in
   *                       main memory
   * @param pageSize       the size of a page in Bytes
   * @param cacheSize      the size of the cache in Bytes
   */
  public DeLiCluTree(int dimensionality, String fileName, int pageSize, int cacheSize) {
    super(dimensionality, fileName, pageSize, cacheSize);
  }

  /**
   * Creates a new DeLiClu-Tree with the specified parameters.
   *
   * @param objects   the vector objects to be indexed
   * @param fileName  the name of the file for storing the entries,
   *                  if this parameter is null all entries will be hold in
   *                  main memory
   * @param pageSize  the size of a page in bytes
   * @param cacheSize the size of the cache (must be >= 1)
   */
  public DeLiCluTree(List<T> objects, final String fileName, final int pageSize, final int cacheSize) {
    super(objects, fileName, pageSize, cacheSize);
  }

  /**
   * Marks the specified object as handled and returns the path of node ids from the root to the
   * objects's parent.
   *
   * @param o the object to be marked as handled
   * @return the path of node ids from the root to the objects's parent
   */
  public synchronized List<Entry> setHandled(T o) {
    logger.info("setHandled " + o + "\n");

    // find the leaf node containing o
    MBR mbr = new MBR(Util.unbox(o.getValues()), Util.unbox(o.getValues()));
    List<Entry> path = new ArrayList<Entry>();
    ParentInfo parentInfo = findLeaf(getRootEntry(), mbr, o.getID(), path);

    if (parentInfo == null)
      return null;

    DeLiCluNode node = (DeLiCluNode) parentInfo.leaf;
    int index = parentInfo.index;

    // set o handled in leaf
    node.setHasHandled(index);
    node.resetHasUnhandled(index);
    file.writePage(node);

    // propagate to the parents
    while (node.parentID != null) {
      index = node.getIndex();
      // get parent and set index handled
      node = (DeLiCluNode) getNode(node.parentID);
      boolean allHandled = node.setHasHandled(index);
      if (allHandled) node.resetHasUnhandled(index);
    }
    return path;
  }

  /**
   * Marks the nodes with the specified ids as expanded.
   *
   * @param entry1 the first node
   * @param entry2 the second node
   */
  public void setExpanded(Entry entry1, Entry entry2) {
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
   */
  public Set<Integer> getExpanded(Entry entry) {
    HashSet<Integer> exp = expanded.get(entry.getID());
    if (exp != null) return exp;
    return new HashSet<Integer>();
  }

    /**
   * Returns the nodes which are already expanded with the specified node.
   *
   * @param entry the id of the node for which the expansions should be returned
   */
  public Set<Integer> getExpanded(DeLiCluNode entry) {
    HashSet<Integer> exp = expanded.get(entry.getID());
    if (exp != null) return exp;
    return new HashSet<Integer>();
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  AbstractNode createNewLeafNode(int capacity) {
    return new DeLiCluNode(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  AbstractNode createNewDirectoryNode(int capacity) {
    return new DeLiCluNode(file, capacity, false);
  }

  /**
   * Determines the maximum and minimum number of entries in a node.
   *
   * @param pageSize       the size of a page in Bytes
   * @param dimensionality the dimensionality of the data to be indexed
   */
  protected void initCapacities(int pageSize, int dimensionality) {
    // overhead = index(4), numEntries(4), parentID(4), id(4), isLeaf(0.125)
    double overhead = 16.125;
    if (pageSize - overhead < 0)
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");

    // dirCapacity = (pageSize - overhead) / (childID + childMBR + handledBit) + 1
    dirCapacity = (int) ((pageSize - overhead) / (4 + 16 * dimensionality + 0.125)) + 1;

    if (dirCapacity <= 1)
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");

    if (dirCapacity < 10)
      logger.severe("Page size is choosen too small! Maximum number of entries " +
                    "in a directory node = " + (dirCapacity - 1));

    // minimum entries per directory node
    dirMinimum = (int) Math.round((dirCapacity - 1) * 0.5);
    if (dirMinimum < 2)
      dirMinimum = 2;

    // leafCapacity = (pageSize - overhead) / (childID + childValues + handledBit) + 1
    leafCapacity = (int) ((pageSize - overhead) / (4 + 8 * dimensionality + 0.125)) + 1;

    if (leafCapacity <= 1)
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");

    if (leafCapacity < 10)
      logger.severe("Page size is choosen too small! Maximum number of entries " +
                    "in a leaf node = " + (leafCapacity - 1));

    // minimum entries per leaf node
    leafMinimum = (int) Math.round((leafCapacity - 1) * 0.5);
    if (leafMinimum < 2)
      leafMinimum = 2;
  }

  /**
   * Returns the leaf node in the specified subtree that contains the data object
   * with the specified mbr and id.
   *
   * @param entry the current root of the subtree to be tested
   * @param mbr   the mbr to look for
   * @param id    the id to look for
   * @return the leaf node of the specified subtree
   *         that contains the data object with the specified mbr and id
   */
  ParentInfo findLeaf(Entry entry, MBR mbr, int id, List<Entry> path) {
    DeLiCluNode subtree = (DeLiCluNode) getNode(entry.getID());
    if (subtree.isLeaf()) {
      for (int i = 0; i < subtree.getNumEntries(); i++) {
        if (subtree.getEntry(i).getID() == id) {
          path.add(subtree.getEntry(i));
          path.add(entry);
          return new ParentInfo(subtree, i);
        }
      }
    }
    else {
      for (int i = 0; i < subtree.getNumEntries(); i++) {
        if (subtree.getEntry(i).getMBR().intersects(mbr)) {
          ParentInfo parentInfo = findLeaf(subtree.getEntry(i), mbr, id, path);
          if (parentInfo != null) {
            path.add(entry);
            return parentInfo;
          }
        }
      }
    }
    return null;
  }

  /**
   * Determines and returns the number of nodes in this index.
   *
   * @return the number of nodes in this index
   */
  public int numNodes() {
    int numNodes = 0;

    AbstractNode root = (AbstractNode) getRoot();
    BreadthFirstEnumeration<AbstractNode> bfs =
    new BreadthFirstEnumeration<AbstractNode>(file, new DirectoryEntry(root.getID(), root.mbr()));

    while (bfs.hasMoreElements()) {
      Entry entry = bfs.nextElement();
      if (! entry.isLeafEntry()) {
        numNodes++;
      }
    }

    return numNodes;
  }

}
