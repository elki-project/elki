package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.index.spatial.MBR;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * DeLiCluTree is a spatial index structure based on a R-TRee. DeLiCluTree is designed
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
   * Creates a new RTree from an existing persistent file.
   *
   * @param fileName  the name of the file storing the RTree
   * @param cacheSize the size of the cache in bytes
   */
  public DeLiCluTree(String fileName, int cacheSize) {
    super(fileName, cacheSize);
  }

  /**
   * Creates a new RTree with the specified parameters.
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
   * Creates a new RTree with the specified parameters.
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
  public synchronized Integer setHandled(T o) {
    logger.info("setHandled " + o + "\n");

    // find the leaf node containing o
    MBR mbr = new MBR(Util.unbox(o.getValues()), Util.unbox(o.getValues()));
    ParentInfo parentInfo = findLeaf((AbstractNode) getRoot(), mbr, o.getID());

    if (parentInfo == null)
      return null;

    DeLiCluNode node = (DeLiCluNode) parentInfo.leaf;
    int index = parentInfo.index;

    // set o handled
    node.setHandled(index);
    file.writePage(node);

    // propagate to the parents
    while (node.areAllHandled()) {
      index = node.index;
      // get parent and set index handled
      node = (DeLiCluNode) getNode(node.parentID);
      node.setHandled(index);
    }
    return parentInfo.leaf.nodeID;
  }

  /**
   * Marks the nodes with the specified ids as expanded.
   *
   * @param node1 the first node
   * @param node2 the second node
   */
  public void setExpanded(Integer node1, Integer node2) {
    HashSet<Integer> exp1 = expanded.get(node1);
    if (exp1 == null) {
      exp1 = new HashSet<Integer>();
      expanded.put(node1, exp1);
    }
    exp1.add(node2);

    HashSet<Integer> exp2 = expanded.get(node2);
    if (exp2 == null) {
      exp2 = new HashSet<Integer>();
      expanded.put(node2, exp2);
    }
    exp2.add(node1);
  }

  /**
   * Returns the nodes which are already expanded with the specified node.
   *
   * @param node the id of the node for which the expansions should be returned
   */
  public List<Integer> getExpanded(Integer node) {
    HashSet<Integer> exp = expanded.get(node);
    if (exp != null) return new ArrayList<Integer>(exp);
    return new ArrayList<Integer>();
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
}
