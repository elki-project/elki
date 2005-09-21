package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.data.FeatureVector;

import java.util.List;

/**
 * DeLiCluTree is a spatial index structure based on a R-TRee. DeLiCluTree is designed
 * for the DeLiClu algorithm, having in each node
 * a boolean array which indicates wether the child nodes are already handled by the DeLiClu
 * algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DeLiCluTree extends RTree {
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
  public DeLiCluTree(final FeatureVector[] objects, final String fileName, final int pageSize, final int cacheSize) {
    super(objects, fileName, pageSize, cacheSize);
  }

  /**
   * Marks the specified obect as handled and returns the path of node ids from the root to the
   * objects's parent.
   *
   * @param o the object to be marked as handled
   * @return the path of node ids from the root to the objects's parent
   */
  public synchronized List<Integer> setHandled(DoubleVector o) {
    logger.info("setHandled " + o + "\n");

    // find the leaf node containing o
    /*MBR mbr = new MBR(Util.unbox(o.getValues()), Util.unbox(o.getValues()));
    ParentInfo parent = findLeaf((AbstractNode) getRoot(), mbr, o.getID());
    if (parent == null)
      return null;

    DeLiCluNode leaf = (DeLiCluNode) parent.leaf;
    int index = parent.index;

    // set o handled
    leaf.setHandled(index);
    file.writePage(leaf);

    // condense the tree
    Stack<AbstractNode> stack = new Stack<AbstractNode>();
    condenseTree(leaf, stack);

    // reinsert underflow nodes
    while (!stack.empty()) {
      AbstractNode node = stack.pop();
      if (node.isLeaf()) {
        for (int i = 0; i < node.getNumEntries(); i++) {
          Entry e = node.entries[i];
          Data obj = new Data(e.getID(), e.getMBR().getMin(), null);
          reinsertions.clear();
          this.insert(obj, 1);
        }
      }
      else {
        for (int i = 0; i < node.getNumEntries(); i++) {
          stack.push(getNode(node.entries[i].getID()));
        }
      }
      file.deletePage(node.getID());
    }

    // test for debugging
//    Node root = (Node) getRoot();
//    root.test();

    return true;
    */
    return null;
  }

  /**
   * Propgates the handled flag upwards if necessary.
   *
   * @param node the current root of the subtree to be propagte
   */
  private void propagateSetHandled(DeLiCluNode node) {
    /*
    // node is not root
     if (node.getID() != ROOT_NODE_ID) {
       if (! node.allEntriesHandled()) return;

       DeLiCluNode parent = (DeLiCluNode) getNode(node.parentID);
       if ()
       int minimum = node.isLeaf() ? leafMinimum : dirMinimum;
       if (node.getNumEntries() < minimum) {
         parent.deleteEntry(node.index);
         stack.push(node);
       }
       else {
         ((DirectoryEntry) parent.entries[node.index]).setMBR(node.mbr());
       }
       file.writePage(parent);
       condenseTree(parent, stack);
     }

     // node is root
     else {
       if (node.getNumEntries() == 1 && !node.isLeaf()) {
         AbstractNode child = getNode(node.entries[0].getID());
         AbstractNode newRoot;
         if (child.isLeaf()) {
           newRoot = createNewLeafNode(leafCapacity);
           newRoot.nodeID = ROOT_NODE_ID;
           for (int i = 0; i < child.getNumEntries(); i++) {
             Entry e = child.entries[i];
             Data o = new Data(e.getID(), e.getMBR().getMin(), ROOT_NODE_ID);
             newRoot.addEntry(o);
           }
         }
         else {
           newRoot = createNewDirectoryNode(dirCapacity);
           newRoot.nodeID = ROOT_NODE_ID;
           for (int i = 0; i < child.getNumEntries(); i++) {
             Entry e = child.entries[i];
             AbstractNode n = getNode(e.getID());
             newRoot.addEntry(n);
           }
         }
         file.writePage(newRoot);
         height--;
       }
     }
     */
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
