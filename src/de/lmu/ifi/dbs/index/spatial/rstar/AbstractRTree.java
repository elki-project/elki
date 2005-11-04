package de.lmu.ifi.dbs.index.spatial.rstar;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.index.BreadthFirstEnumeration;
import de.lmu.ifi.dbs.index.Identifier;
import de.lmu.ifi.dbs.index.spatial.*;
import de.lmu.ifi.dbs.persistent.LRUCache;
import de.lmu.ifi.dbs.persistent.MemoryPageFile;
import de.lmu.ifi.dbs.persistent.PageFile;
import de.lmu.ifi.dbs.persistent.PersistentPageFile;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.heap.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract superclass for index structures based on a R*-Tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractRTree<O extends RealVector> implements SpatialIndex<O> {
  /**
   * Logger object for logging messages.
   */
  protected static Logger logger;

  /**
   * The loggerLevel for logging messages.
   */
  static Level loggerLevel = Level.OFF;

  /**
   * The id of the root node.
   */
  static int ROOT_NODE_ID = 0;

  /**
   * The file storing the entries of this RTree.
   */
  protected final PageFile<RTreeNode> file;

  /**
   * Contains a boolean for each level of this RTree that indicates
   * if there was already a reinsert operation in this level
   * during the current insert / delete operation
   */
  private final Map<Integer, Boolean> reinsertions = new HashMap<Integer, Boolean>();

  /**
   * The height of this RTree.
   */
  int height;

  /**
   * The capacity of a directory node (= 1 + maximum number of entries in a directory node).
   */
  protected int dirCapacity;

  /**
   * The capacity of a leaf node (= 1 + maximum number of entries in a leaf node).
   */
  protected int leafCapacity;

  /**
   * The minimum number of entries in a directory node.
   */
  protected int dirMinimum;

  /**
   * The minimum number of entries in a leaf node.
   */
  protected int leafMinimum;

  /**
   * Creates a new RTree from an existing persistent file.
   *
   * @param fileName  the name of the file storing the RTree
   * @param cacheSize the size of the cache in bytes
   */
  public AbstractRTree(String fileName, int cacheSize) {
    initLogger();

    // init the file
    RTreeHeader header = new RTreeHeader();
    this.file = new PersistentPageFile<RTreeNode>(header,
                                                  cacheSize,
                                                  new LRUCache<RTreeNode>(),
                                                  fileName);
    this.dirCapacity = header.getDirCapacity();
    this.leafCapacity = header.getLeafCapacity();
    this.dirMinimum = header.getDirMinimum();
    this.leafMinimum = header.getLeafMinimum();

    // compute height
    this.height = computeHeight();

    StringBuffer msg = new StringBuffer();
    msg.append(getClass());
    msg.append("\n height = ").append(height);
    msg.append("\n file = ").append(file.getClass());
    logger.info(msg.toString());
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
  public AbstractRTree(int dimensionality, String fileName, int pageSize,
                       int cacheSize) {
    initLogger();

    // determine minimum and maximum entries in an node
    initCapacities(pageSize, dimensionality);

    // init the file
    if (fileName == null) {
      this.file = new MemoryPageFile<RTreeNode>(pageSize,
                                                cacheSize,
                                                new LRUCache<RTreeNode>());
    }
    else {
      RTreeHeader header = new RTreeHeader(pageSize, dirCapacity, leafCapacity,
                                           dirMinimum, leafMinimum);
      this.file = new PersistentPageFile<RTreeNode>(header,
                                                    cacheSize,
                                                    new LRUCache<RTreeNode>(),
                                                    fileName);
    }

    String msg = getClass() + "\n" +
                 " file    = " + file.getClass() + "\n" +
                 " maximum number of dir entries = " + (dirCapacity - 1) + "\n" +
                 " minimum number of dir entries = " + dirMinimum + "\n" +
                 " maximum number of leaf entries = " + (leafCapacity - 1) + "\n" +
                 " minimum number of leaf entries = " + leafMinimum + "\n";

    // create empty root
    createEmptyRoot(dimensionality);

    msg += " height  = " + height + "\n" +
           " root    = " + getRoot();

    logger.info(msg);
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
  public AbstractRTree(List<O> objects, final String fileName,
                       final int pageSize, final int cacheSize) {
    initLogger();
//    System.out.println("\r init RTree with bulk load...");
    // determine minimum and maximum entries in an node
    int dimensionality = objects.get(0).getValues().length;
    initCapacities(pageSize, dimensionality);

    // init the file
    if (fileName == null) {
      this.file = new MemoryPageFile<RTreeNode>(pageSize,
                                                cacheSize,
                                                new LRUCache<RTreeNode>());
    }
    else {
      RTreeHeader header = new RTreeHeader(pageSize, dirCapacity, leafCapacity,
                                           dirMinimum, leafMinimum);
      this.file = new PersistentPageFile<RTreeNode>(header,
                                                    cacheSize,
                                                    new LRUCache<RTreeNode>(),
                                                    fileName);
    }

    String msg = getClass() + "\n" +
                 " file    = " + file.getClass() + "\n" +
                 " maximum number of dir entries = " + (dirCapacity - 1) + "\n" +
                 " minimum number of dir entries = " + dirMinimum + "\n" +
                 " maximum number of leaf entries = " + (leafCapacity - 1) + "\n" +
                 " minimum number of leaf entries = " + leafMinimum + "\n";

//    System.out.println(msg);
    // create the nodes
    bulkLoad(objects);

    msg += " height  = " + height + "\n" +
           " root    = " + getRoot();

    logger.info(msg);
  }

  /**
   * Inserts the specified reel vector object into this index.
   *
   * @param o the vector to be inserted
   */
  public synchronized void insert(O o) {
    logger.info("insert " + o + "\n");

    reinsertions.clear();

    double[] values = Util.unbox(o.getValues());
    LeafEntry entry = new LeafEntry(o.getID(), values);
    insert(entry);

    // test for debugging
//    Node root = (Node) getRoot();
//    root.test();
  }

  /**
   * Deletes the specified obect from this index.
   *
   * @param o the object to be deleted
   * @return true if this index did contain the object with the specified id,
   *         false otherwise
   */
  public synchronized boolean delete(O o) {
    logger.info("delete " + o + "\n");

    // find the leaf node containing o
    MBR mbr = new MBR(Util.unbox(o.getValues()), Util.unbox(o.getValues()));
    ParentInfo del = findLeaf(getRoot(), mbr, o.getID());
    if (del == null) return false;
    RTreeNode leaf = del.leaf;
    int index = del.index;

    // delete o
    leaf.deleteEntry(index);
    file.writePage(leaf);

    // condense the tree
    Stack<RTreeNode> stack = new Stack<RTreeNode>();
    condenseTree(leaf, stack);

    // reinsert underflow nodes
    while (!stack.empty()) {
      RTreeNode node = stack.pop();
      if (node.isLeaf()) {
        for (int i = 0; i < node.getNumEntries(); i++) {
          LeafEntry e = (LeafEntry) node.entries[i];
          reinsertions.clear();
          this.insert(e);
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
  }

  /**
   * Performs a range query for the given spatial objec with the given
   * epsilon range and the according distance function.
   * The query result is in ascending order to the distance to the
   * query object.
   *
   * @param obj              the query object
   * @param epsilon          the string representation of the query range
   * @param distanceFunction the distance function that computes the distances beween the objects
   * @return a List of the query results
   */
  public <D extends Distance> List<QueryResult<D>> rangeQuery(O obj, String epsilon,
                                                              DistanceFunction<O, D> distanceFunction) {

    if (!(distanceFunction instanceof SpatialDistanceFunction))
      throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");
    SpatialDistanceFunction<O, D> df = (SpatialDistanceFunction<O, D>) distanceFunction;

    D range = distanceFunction.valueOf(epsilon);
    final List<QueryResult<D>> result = new ArrayList<QueryResult<D>>();
    final Heap<Distance, Identifiable> pq = new DefaultHeap<Distance, Identifiable>();

    // push root
    pq.addNode(new PQNode(distanceFunction.nullDistance(), ROOT_NODE_ID));

    // search in tree
    while (!pq.isEmpty()) {
      HeapNode<Distance, Identifiable> pqNode = pq.getMinNode();
      if (pqNode.getKey().compareTo(range) > 0) break;

      RTreeNode node = getNode(pqNode.getValue().getID());
      final int numEntries = node.getNumEntries();

      for (int i = 0; i < numEntries; i++) {
        D distance = df.minDist(node.entries[i].getMBR(), obj);
        if (distance.compareTo(range) <= 0) {
          Entry entry = node.entries[i];
          if (node.isLeaf()) {
            result.add(new QueryResult<D>(entry.getID(), distance));
          }
          else {
            pq.addNode(new PQNode(distance, entry.getID()));
          }
        }
      }
    }

    // sort the result according to the distances
    Collections.sort(result);
    return result;
  }

  /**
   * Performs a k-nearest neighbor query for the given RealVector with the given
   * parameter k and the according distance function.
   * The query result is in ascending order to the distance to the
   * query object.
   *
   * @param obj              the query object
   * @param k                the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances beween the objects
   * @return a List of the query results
   */
  public <D extends Distance> List<QueryResult<D>> kNNQuery(O obj, int k,
                                                            DistanceFunction<O, D> distanceFunction) {

    if (!(distanceFunction instanceof SpatialDistanceFunction))
      throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");
    SpatialDistanceFunction<O, D> df = (SpatialDistanceFunction<O, D>) distanceFunction;

    if (k < 1) {
      throw new IllegalArgumentException("At least one enumeration has to be requested!");
    }

    // variables
    final Heap<Distance, Identifiable> pq = new DefaultHeap<Distance, Identifiable>();
    final KNNList<D> knnList = new KNNList<D>(k, distanceFunction.infiniteDistance());

    // push root
    pq.addNode(new PQNode(distanceFunction.nullDistance(), ROOT_NODE_ID));
    Distance maxDist = distanceFunction.infiniteDistance();
    // search in tree

    while (!pq.isEmpty()) {
      HeapNode<Distance, Identifiable> pqNode = pq.getMinNode();

      if (pqNode.getKey().compareTo(maxDist) > 0) {
        return knnList.toList();
      }

      RTreeNode node = getNode(pqNode.getValue().getID());
      // data node
      if (node.isLeaf()) {
        for (int i = 0; i < node.numEntries; i++) {
          Entry entry = node.entries[i];
          D distance = df.minDist(entry.getMBR(), obj);
          if (distance.compareTo(maxDist) <= 0) {
            knnList.add(new QueryResult<D>(entry.getID(), distance));
            if (knnList.size() == k) {
              maxDist = knnList.getMaximumDistance();
            }
          }
        }
      }
      // directory node
      else {
        for (int i = 0; i < node.numEntries; i++) {
          Entry entry = node.entries[i];
          Distance distance = df.minDist(entry.getMBR(), obj);
          if (distance.compareTo(maxDist) <= 0) {
            pq.addNode(new PQNode(distance, entry.getID()));
          }

        }
      }
    }

    return knnList.toList();

  }

  /**
   * Returns a list of entries pointing to the leaf nodes of this spatial index.
   *
   * @return a list of entries pointing to the leaf nodes of this spatial index
   */
  public List<DirectoryEntry> getLeaves() {
    List<DirectoryEntry> result = new ArrayList<DirectoryEntry>();

    if (height == 1) {
      RTreeNode root = getRoot();
      result.add(new DirectoryEntry(ROOT_NODE_ID, root.mbr()));
      return result;
    }

    getLeafNodes(getRoot(), result, height);
    return result;
  }

  /**
   * Returns the I/O-access of this RTree.
   *
   * @return the I/O-access of this RTree
   */
  public long getIOAccess() {
    return file.getIOAccess();
  }

  /**
   * Resets the I/O-access of this RTree.
   */
  public void resetIOAccess() {
    file.resetIOAccess();
  }

  /**
   * Returns the node with the specified id.
   *
   * @param nodeID the page id of the node to be returned
   * @return the node with the specified id
   */
  public RTreeNode getNode(int nodeID) {
    if (nodeID == ROOT_NODE_ID) return getRoot();
    else {
      return file.readPage(nodeID);
    }
  }

  /**
   * Returns the entry that denotes the root.
   *
   * @return the entry that denotes the root
   */
  public DirectoryEntry getRootEntry() {
    RTreeNode root = getRoot();
    return new DirectoryEntry(root.getID(), root.mbr());
  }

  /**
   * Tests this RTree for debugging purposes.
   *
   * @return the number of objects stored in this RTree
   */
  public int test() {
    StringBuffer result = new StringBuffer();
    int dirNodes = 0;
    int leafNodes = 0;
    int objects = 0;
    int levels = 0;

    RTreeNode node = getRoot();
    int dim = node.entries[0].getMBR().getDimensionality();

    while (!node.isLeaf()) {
      if (node.getNumEntries() > 0) {
        Entry entry = node.entries[0];
        node = getNode(entry.getID());
        levels++;
      }
    }

    RTreeNode root = getRoot();
    BreadthFirstEnumeration<RTreeNode> enumeration =
    new BreadthFirstEnumeration<RTreeNode>(file, new DirectoryEntry(root.getID(), root.mbr()));

    while (enumeration.hasMoreElements()) {
      Identifier id = enumeration.nextElement();
      if (! id.isNodeID())
        objects++;
      else {
        node = file.readPage(id.value());
        if (node.isLeaf())
          leafNodes++;
        else
          dirNodes++;
      }
    }

    result.append("RTree hat ").append((levels + 1)).append(" Ebenen \n");
    result.append(dirNodes).append(" Directory Knoten (maximum = ").append(dirCapacity - 1).append(")\n");
    result.append(leafNodes).append(" Daten Knoten (maximum = ").append(leafCapacity - 1).append(")\n");
    result.append(objects).append(" ").append(dim).append("-dim. Punkte im Baum \n");

    return objects;
  }

  /**
   * Closes this RTree and the underlying file.
   * If this RTree has a oersistent file, all entries are written to disk.
   */
  public void close() {
    file.close();
  }

  /**
   * Returns a string representation of this RTree.
   *
   * @return a string representation of this RTree
   */
  public String toString() {
    StringBuffer result = new StringBuffer();
    int dirNodes = 0;
    int leafNodes = 0;
    int objects = 0;
    int levels = 0;

    RTreeNode node = getRoot();
    int dim = node.entries[0].getMBR().getDimensionality();

    while (!node.isLeaf()) {
      if (node.getNumEntries() > 0) {
        Entry entry = node.entries[0];
        node = getNode(entry.getID());
        levels++;
      }
    }

    RTreeNode root = getRoot();
    BreadthFirstEnumeration<RTreeNode> enumeration =
    new BreadthFirstEnumeration<RTreeNode>(file, new DirectoryEntry(root.getID(), root.mbr()));

    while (enumeration.hasMoreElements()) {
      Identifier id = enumeration.nextElement();
      if (! id.isNodeID())
        objects++;
      else {
        node = file.readPage(id.value());
//        System.out.println(node + " " + node.numEntries);
        if (node.isLeaf())
          leafNodes++;
        else {
          dirNodes++;
        }
      }
    }

    result.append(getClass().getName()).append(" hat ").append((levels + 1)).append(" Ebenen \n");
    result.append(dirNodes).append(" Directory Knoten (max = ").append(dirCapacity - 1).append(", min = ").append(dirMinimum).append(")\n");
    result.append(leafNodes).append(" Daten Knoten (max = ").append(leafCapacity - 1).append(", min = ").append(leafMinimum).append(")\n");
    result.append(objects).append(" ").append(dim).append("-dim. Punkte im Baum \n");
    result.append("IO-Access: ").append(file.getIOAccess()).append("\n");
    result.append("File ").append(file.getClass()).append("\n");

    return result.toString();
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

    // dirCapacity = (pageSize - overhead) / (childID + childMBR) + 1
    dirCapacity = (int) (pageSize - overhead) / (4 + 16 * dimensionality) + 1;

    if (dirCapacity <= 1)
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");

    if (dirCapacity < 10)
      logger.severe("Page size is choosen too small! Maximum number of entries " +
                    "in a directory node = " + (dirCapacity - 1));

    // minimum entries per directory node
    dirMinimum = (int) Math.round((dirCapacity - 1) * 0.5);
    if (dirMinimum < 2)
      dirMinimum = 2;

    // leafCapacity = (pageSize - overhead) / (childID + childValues) + 1
    leafCapacity = (int) (pageSize - overhead) / (4 + 8 * dimensionality) + 1;

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
   * Returns the root of this index.
   *
   * @return the root of this index
   */
  abstract protected RTreeNode getRoot();

  /**
   * Returns true if in the specified node an overflow occured, false otherwise.
   *
   * @param node the node to be tested for overflow
   * @return true if in the specified node an overflow occured, false otherwise
   */
  abstract protected boolean hasOverflow(RTreeNode node);

  /**
   * Computes the height of this RTree. Is called by the constructor.
   *
   * @return the height of this RTree
   */
  abstract protected int computeHeight();

  /**
   * Creates an empty root node and writes it to file. Is called by the constructor.
   *
   * @param dimensionality the dimensionality of the data objects to be stored
   */
  abstract protected void createEmptyRoot(int dimensionality);

  /**
   * Performs a bulk load on this RTree with the specified data.
   * Is called by the constructor.
   *
   * @param objects the data objects to be indexed
   */
  abstract protected void bulkLoad(List<O> objects);

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  abstract protected RTreeNode createNewLeafNode(int capacity);

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  abstract protected RTreeNode createNewDirectoryNode(int capacity);

  /**
   * Inserts the specified data object into this RTree.
   *
   * @param entry the leaf entry to be inserted
   */
  synchronized void insert(LeafEntry entry) {
    logger.info("insert " + entry + "\n");

    // choose node for insertion of o
    MBR mbr = entry.getMBR();
    RTreeNode parent = chooseNode(getRoot(), mbr, 1, height);
    parent.addLeafEntry(entry);
    file.writePage(parent);

    // adjust the tree from current level to root level
    adjustTree(parent, 1);
  }

  /**
   * Inserts the specified data object into this RTree.
   *
   * @param node  the spatial node to be inserted
   * @param level the level at which the spatial object should be inserted (1 = leaf level)
   */
  synchronized void insert(RTreeNode node, int level) {
    logger.info("insert " + node + "\n");

    // choose node for insertion of o
    MBR mbr = node.mbr();
    RTreeNode parent = chooseNode(getRoot(), mbr, level, height);
    parent.addNode(node);
    file.writePage(parent);

    // adjust the tree from current level to root level
    adjustTree(parent, level);
  }

  /**
   * Returns the leaf node in the specified subtree that contains the data object
   * with the specified mbr and id.
   *
   * @param subtree the current root of the subtree to be tested
   * @param mbr     the mbr to look for
   * @param id      the id to look for
   * @return the leaf node of the specified subtree
   *         that contains the data object with the specified mbr and id
   */
  ParentInfo findLeaf(RTreeNode subtree, MBR mbr, int id) {
    if (subtree.isLeaf()) {
      for (int i = 0; i < subtree.getNumEntries(); i++) {
        if (subtree.entries[i].getID() == id) {
          return new ParentInfo(subtree, i);
        }
      }
    }
    else {
      for (int i = 0; i < subtree.getNumEntries(); i++) {
        if (subtree.entries[i].getMBR().intersects(mbr)) {
          RTreeNode child = getNode(subtree.entries[i].getID());
          ParentInfo parentInfo = findLeaf(child, mbr, id);
          if (parentInfo != null) return parentInfo;
        }
      }
    }
    return null;
  }

  /**
   * Creates and returns the leaf nodes for bulk load.
   *
   * @param objects the objects to be inserted
   * @return the leaf nodes containing the objects
   */
  RTreeNode[] createLeafNodes(List<O> objects) {
    int minEntries = leafMinimum;
    int maxEntries = leafCapacity - 1;

    ArrayList<RTreeNode> result = new ArrayList<RTreeNode>();
    while (objects.size() > 0) {
      StringBuffer msg = new StringBuffer();

      // get the split axis and split point
      BulkSplit<O> split = new BulkSplit<O>(objects, minEntries, maxEntries);
      int splitAxis = split.splitAxis;
      int splitPoint = split.splitPoint;
      msg.append("\nsplitAxis ").append(splitAxis);
      msg.append("\nsplitPoint ").append(splitPoint);

      // sort in the right dimension
      final SpatialComparator comp = new SpatialComparator();
      comp.setCompareDimension(splitAxis);
      comp.setComparisonValue(SpatialComparator.MIN);
      Collections.sort(objects, comp);

      // create leaf node
      RTreeNode leafNode = createNewLeafNode(leafCapacity);
      file.writePage(leafNode);
      result.add(leafNode);

      // insert data
      for (int i = 0; i < splitPoint; i++) {
        O o = objects.remove(0);
        LeafEntry entry = new LeafEntry(o.getID(), Util.unbox(o.getValues()));
        leafNode.addLeafEntry(entry);
      }

      // copy array
      msg.append("\nremaining objects # ").append(objects.size());

      // write to file
      file.writePage(leafNode);
      msg.append("\npageNo ").append(leafNode.getID());
      logger.fine(msg.toString() + "\n");

//      System.out.print("\r numDataPages = " + result.size());
    }

    logger.fine("numDataPages = " + result.size());
    return result.toArray(new RTreeNode[result.size()]);
  }

  /**
   * Initializes the logger object.
   */
  void initLogger() {
    logger = Logger.getLogger(getClass().toString());
    logger.setLevel(loggerLevel);
  }

  /**
   * Creates and returns a new root node that points to the two specified child nodes.
   *
   * @param oldRoot the old root of this RTree
   * @param newNode the new split node
   * @return a new root node that points to the two specified child nodes
   */
  private RTreeNode createNewRoot(final RTreeNode oldRoot, final RTreeNode newNode) {
    logger.info("create new root");
    RTreeNode root = createNewDirectoryNode(dirCapacity);
    file.writePage(root);

    oldRoot.nodeID = root.getID();
    if (!oldRoot.isLeaf()) {
      for (int i = 0; i < oldRoot.getNumEntries(); i++) {
        RTreeNode node = getNode(oldRoot.entries[i].getID());
        node.parentID = oldRoot.nodeID;
        file.writePage(node);
      }
    }

    root.nodeID = ROOT_NODE_ID;
    root.addNode(oldRoot);
    root.addNode(newNode);

    file.writePage(root);
    file.writePage(oldRoot);
    file.writePage(newNode);
    String msg = "New Root-ID " + root.nodeID + "\n";
    logger.info(msg);

    height++;
    return root;
  }

  /**
   * Chooses the best node of the specified subtree for insertion of
   * the given mbr at the specified level.
   *
   * @param node         the root of the subtree to be tested for insertion
   * @param mbr          the mbr to be inserted
   * @param level        the level at which the mbr should be inserted
   * @param currentLevel the current level of this method (should be initialized
   *                     with 1 at the first call)
   * @return the appropriate subtree to insert the given node
   */
  private RTreeNode chooseNode(RTreeNode node, MBR mbr, int level, int currentLevel) {
    logger.info("node " + node + ", level " + level);

    if (node.isLeaf()) return node;

    RTreeNode childNode = getNode(node.entries[0].getID());
    // children are leafs
    if (childNode.isLeaf()) {
      if (currentLevel - 1 == level) {
        return getLeastOverlap(node, mbr);
      }
      else
        throw new IllegalArgumentException("childNode is leaf, but currentLevel != level: " +
                                           currentLevel + " != " + level);
    }
    // children are directory nodes
    else {
      if (currentLevel - 1 == level)
        return getLeastEnlargement(node, mbr);
      else
        return chooseNode(getLeastEnlargement(node, mbr), mbr, level, currentLevel - 1);
    }
  }

  /**
   * Returns the children of the specified node with the least enlargement
   * if the given mbr would be inserted into.
   *
   * @param node the node which children have to be tested
   * @param mbr  the mbr of the node to be inserted
   * @return the node with the least enlargement if the given mbr would be inserted into
   */
  private RTreeNode getLeastEnlargement(RTreeNode node, MBR mbr) {
    Enlargement min = null;

    for (int i = 0; i < node.getNumEntries(); i++) {
      Entry entry = node.entries[i];
      double volume = entry.getMBR().volume();
      MBR newMBR = entry.getMBR().union(mbr);
      double inc = newMBR.volume() - volume;
      Enlargement enlargement = new Enlargement(entry.getID(), volume, inc, 0);

      if (min == null || min.compareTo(enlargement) > 0)
        min = enlargement;
    }

    assert min != null;
    return getNode(min.nodeID);
  }

  /**
   * Returns the children of the specified node which needs least overlap
   * enlargement if the given mbr would be inserted into.
   *
   * @param node the node of which the children should be tested
   * @param mbr  the mbr to be inserted into the children
   * @return the children of the specified node which needs least overlap
   *         enlargement if the given mbr would be inserted into
   */
  private RTreeNode getLeastOverlap(RTreeNode node, MBR mbr) {
    Enlargement min = null;

    for (int i = 0; i < node.getNumEntries(); i++) {
      Entry entry_i = node.entries[i];
      MBR newMBR = entry_i.getMBR().union(mbr);

      double currOverlap = 0;
      double newOverlap = 0;
      for (int k = 0; k < node.getNumEntries(); k++) {
        if (i != k) {
          Entry entry_k = node.entries[k];
          currOverlap += entry_i.getMBR().overlap(entry_k.getMBR());
          newOverlap += newMBR.overlap(entry_k.getMBR());
        }
      }

      double volume = entry_i.getMBR().volume();
      double inc_volume = newMBR.volume() - volume;
      double inc_overlap = newOverlap - currOverlap;
      Enlargement enlargement = new Enlargement(entry_i.getID(),
                                                volume, inc_volume,
                                                inc_overlap);

      if (min == null || min.compareTo(enlargement) > 0)
        min = enlargement;
    }

    assert min != null;
    return getNode(min.nodeID);
  }

  /**
   * If the node is not the root node and this is the first call of overflowTreatment
   * in the given level during insertion the specified node will be reinserted,
   * otherwise the node will be splitted.
   *
   * @param node  the node where an overflow occured
   * @param level the level of the node in the tree (leaf level = 0)
   */
  private RTreeNode overflowTreatment(RTreeNode node, int level) {
    Boolean reInsert = reinsertions.get(level);

    // there was still no reinsert operation at this level
    if (node.getID() != 0 && (reInsert == null || ! reInsert)) {
      reinsertions.put(level, true);
      reInsert(node, level);
      return null;
    }

    // there was already a reinsert operation at this level
    else {
      return split(node);
    }
  }

  /**
   * Splits the specified node and returns the newly created split node.
   *
   * @param node the node to be splitted
   * @return the newly created split node
   */
  private RTreeNode split(RTreeNode node) {
    // choose the split dimension and the split point
    int minimum = node.isLeaf() ? leafMinimum : dirMinimum;
    Split split = new Split(node.entries, minimum);

    // do the split
    RTreeNode newNode;

    if (split.bestSort == SpatialComparator.MIN) {
      newNode = node.splitEntries(split.minSorting, split.splitPoint);
    }
    else if (split.bestSort == SpatialComparator.MAX) {
      newNode = node.splitEntries(split.maxSorting, split.splitPoint);
    }
    else
      throw new IllegalStateException("split.bestSort is undefined!");

    String msg = "Split Node " + node.getID() + " (" + this.getClass() + ")\n" +
                 "      splitAxis " + split.splitAxis + "\n" +
                 "      splitPoint " + split.splitPoint + "\n" +
                 "      newNode " + newNode.getID() + "\n";

    logger.info(msg);

    // write changes to file
    file.writePage(node);
    file.writePage(newNode);

    return newNode;
  }

  /**
   * Reinserts the specified node at the specified level.
   *
   * @param node  the node to be reinserted
   * @param level the level of the node
   */
  private void reInsert(RTreeNode node, int level) {
    MBR mbr = node.mbr();
    SpatialDistanceFunction distFunction = new EuklideanDistanceFunction();
    ReinsertEntry[] reInsertEntries = new ReinsertEntry[node.getNumEntries()];

    // compute the center distances of entries to the node and sort it
    // in decreasing order to their distances
    for (int i = 0; i < node.getNumEntries(); i++) {
      Entry entry = node.entries[i];
      Distance dist = distFunction.centerDistance(mbr, entry.getMBR());
      reInsertEntries[i] = new ReinsertEntry(entry, dist);
    }

    Arrays.sort(reInsertEntries);

    // define, how many entries will be reinserted
    int start = (int) (0.3 * (double) node.getNumEntries());

    // initialize the reinsertion operation: move the remaining entries forward
    node.initReInsert(start, reInsertEntries);
    file.writePage(node);
    // and adapt the mbrs
    RTreeNode child = node;
    while (child.parentID != null) {
      RTreeNode parent = getNode(child.parentID);
      ((DirectoryEntry) parent.entries[child.index]).setMBR(child.mbr());
      file.writePage(parent);
      child = parent;
    }

    // reinsert the first entries
    for (
    int i = 0;
    i < start; i++)

    {
      ReinsertEntry re = reInsertEntries[i];
      if (node.isLeaf()) {
        LeafEntry entry = (LeafEntry) re.getEntry();
        insert(entry);
      }
      else {
        DirectoryEntry entry = (DirectoryEntry) re.getEntry();
        RTreeNode reNode = getNode(entry.getID());
        insert(reNode, level);
      }
    }
  }

  /**
   * Adjusts the tree after insertion of some nodes.
   *
   * @param node  the root of the subtree to be adjusted
   * @param level the level of the node
   */
  private void adjustTree(RTreeNode node, int level) {
    // adjust the tree from current level to root level
    while (node != null) {
      // read again from file because of changes during reinsertion
      node = getNode(node.getID());

      // overflow in node
      if (hasOverflow(node)) {
        // treatment of overflow: reinsertion or split
        RTreeNode split = overflowTreatment(node, level);

        // node was splitted
        if (split != null) {
          // if root was split: create a new root that points the two split nodes
          if (node.getID() == ROOT_NODE_ID) {
            node = createNewRoot(node, split);
          }
          // node is not root
          if (node.getID() != ROOT_NODE_ID) {
            // get the parent and add the new split node
            RTreeNode parent = getNode(node.parentID);
            parent.addNode(split);

            // adjust the mbrs in the parent node
            DirectoryEntry entry1 = (DirectoryEntry) parent.entries[node.index];
            MBR mbr1 = node.mbr();
            entry1.setMBR(mbr1);
            DirectoryEntry entry2 = (DirectoryEntry) parent.entries[split.index];
            MBR mbr2 = split.mbr();
            entry2.setMBR(mbr2);

            // write changes in parent to file
            file.writePage(parent);
            node = parent;
          }
          // go to the next level
          level++;
        }
      }
      // no overflow, only adjust mbr of node in parent
      else if (node.getID() != ROOT_NODE_ID) {
        RTreeNode parent = getNode(node.parentID);
        DirectoryEntry entry = (DirectoryEntry) parent.entries[node.index];
        MBR newMbr = node.mbr();
        entry.setMBR(newMbr);
        // write changes in parent to file
        file.writePage(parent);
        node = parent;
      }
      // no overflow occured or root level is reached
      else
        break;
    }
  }

  /**
   * Condenses the tree after deletion of some nodes.
   *
   * @param node  the current root of the subtree to be condensed
   * @param stack the stack holding the nodes to be reinserted
   */
  private void condenseTree(RTreeNode node, Stack<RTreeNode> stack) {
    // node is not root
    if (node.getID() != ROOT_NODE_ID) {
      RTreeNode parent = getNode(node.parentID);
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
        RTreeNode child = getNode(node.entries[0].getID());
        RTreeNode newRoot;
        if (child.isLeaf()) {
          newRoot = createNewLeafNode(leafCapacity);
          newRoot.nodeID = ROOT_NODE_ID;
          for (int i = 0; i < child.getNumEntries(); i++) {
            LeafEntry e = (LeafEntry) child.entries[i];
            newRoot.addLeafEntry(e);
          }
        }
        else {
          newRoot = createNewDirectoryNode(dirCapacity);
          newRoot.nodeID = ROOT_NODE_ID;
          for (int i = 0; i < child.getNumEntries(); i++) {
            Entry e = child.entries[i];
            RTreeNode n = getNode(e.getID());
            newRoot.addNode(n);
          }
        }
        file.writePage(newRoot);
        height--;
      }
    }
  }

  /**
   * Determines the entries pointing to the leaf nodes of the specified subtree
   *
   * @param node   the subtree
   * @param result the result to store the ids in
   */
  private void getLeafNodes(RTreeNode node, List<DirectoryEntry>result, int currentLevel) {
    if (currentLevel == 2) {
      for (int i = 0; i < node.numEntries; i++) {
        result.add((DirectoryEntry) node.entries[i]);
      }
    }
    else {
      for (int i = 0; i < node.numEntries; i++) {
        RTreeNode child = file.readPage(node.entries[i].getID());
        getLeafNodes(child, result, (currentLevel - 1));
      }
    }
  }

  /**
   * Encapsulates the attributes for a parent leaf node of a data object.
   */
  protected class ParentInfo {
    /**
     * The leaf node holding the data object.
     */
    public RTreeNode leaf;

    /**
     * The index of the data object.
     */
    public int index;

    /**
     * Creates a new ParentInfo object with the specified parameters.
     *
     * @param leaf  the leaf node holding the data object
     * @param index the index of the data object
     */
    public ParentInfo(RTreeNode leaf, int index) {
      this.leaf = leaf;
      this.index = index;
    }
  }

  /**
   * Encapsulates the paramaters for enlargement of nodes.
   */
  private class Enlargement implements Comparable<Enlargement> {
    /**
     * The id of the node.
     */
    int nodeID;

    /**
     * The volume of the node's MBR.
     */
    double volume;

    /**
     * The increasement of the volume.
     */
    double volInc;

    /**
     * The increasement of the overlap.
     */
    double overlapInc;

    /**
     * Creates an new Enlaregemnt object with the specified parameters.
     *
     * @param nodeID     the id of the node
     * @param volume     the volume of the node's MBR
     * @param volInc     the increasement of the volume
     * @param overlapInc the increasement of the overlap
     */
    public Enlargement(int nodeID, double volume, double volInc, double overlapInc) {
      this.nodeID = nodeID;
      this.volume = volume;
      this.volInc = volInc;
      this.overlapInc = overlapInc;
    }

    /**
     * Compares this Enlargement with the specified Enlargement.
     * First the increasement of the overlap will be compared. If both are equal
     * the increasement of the volume will be compared. If also both are equal
     * the volumes of both nodes will be compared. If both are equal the ids of
     * the nodes will be compared.
     *
     * @param other the Enlargement to be compared.
     * @return a negative integer, zero, or a positive integer as this Enlargement
     *         is less than, equal to, or greater than the specified Enlargement.
     */
    public int compareTo(Enlargement other) {
      if (this.overlapInc < other.overlapInc) return -1;
      if (this.overlapInc > other.overlapInc) return +1;

      if (this.volInc < other.volInc) return -1;
      if (this.volInc > other.volInc) return +1;

      if (this.volume < other.volume) return -1;
      if (this.volume > other.volume) return +1;

      return this.nodeID - other.nodeID;
    }
  }

  private class PQNode extends DefaultHeapNode<Distance, Identifiable> {
    /**
     * Empty constructor for serialization purposes.
     */
    public PQNode() {
      super();
    }

    /**
     * Creates a new heap node with the specified parameters.
     *
     * @param key   the key of this heap node
     * @param value the value of this heap node
     */
    public PQNode(Distance key, final Integer value) {
      super(key, new Identifiable() {
        public Integer getID() {
          return value;
        }

        public int compareTo(Identifiable o) {
          return value - o.getID();
        }
      });
    }

  }

}
