package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
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
import java.io.ObjectOutput;
import java.io.IOException;
import java.io.ObjectInput;

/**
 * Abstract superclass for RTree like index structures.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
abstract class AbstractRTree<T extends RealVector> implements SpatialIndex<T> {
  /**
   * Logger object for logging messages.
   */
  static Logger logger;

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
  final PageFile<AbstractNode> file;

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
  int dirCapacity;

  /**
   * The capacity of a leaf node (= 1 + maximum number of entries in a leaf node).
   */
  int leafCapacity;

  /**
   * The minimum number of entries in a directory node.
   */
  int dirMinimum;

  /**
   * The minimum number of entries in a leaf node.
   */
  int leafMinimum;

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
    this.file = new PersistentPageFile<AbstractNode>(new RTreeHeader(),
                                                     cacheSize,
                                                     new LRUCache<AbstractNode>(),
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
      this.file = new MemoryPageFile<AbstractNode>(pageSize,
                                                   cacheSize,
                                                   new LRUCache<AbstractNode>());
    }
    else {
      RTreeHeader header = new RTreeHeader(pageSize, dirCapacity, leafCapacity,
                                           dirMinimum, leafMinimum);
      this.file = new PersistentPageFile<AbstractNode>(header,
                                                       cacheSize,
                                                       new LRUCache<AbstractNode>(),
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
  public AbstractRTree(List<T> objects, final String fileName,
                       final int pageSize, final int cacheSize) {
    initLogger();
//    System.out.println("\r init RTree with bulk load...");
    // determine minimum and maximum entries in an node
    int dimensionality = objects.get(0).getValues().length;
    initCapacities(pageSize, dimensionality);

    // init the file
    if (fileName == null) {
      this.file = new MemoryPageFile<AbstractNode>(pageSize,
                                                   cacheSize,
                                                   new LRUCache<AbstractNode>());
    }
    else {
      RTreeHeader header = new RTreeHeader(pageSize, dirCapacity, leafCapacity,
                                           dirMinimum, leafMinimum);
      this.file = new PersistentPageFile<AbstractNode>(header,
                                                       cacheSize,
                                                       new LRUCache<AbstractNode>(),
                                                       fileName);
    }

    // wrap the vector objects to data objects
    Data[] data = new Data[objects.size()];
    for (int i = 0; i < objects.size(); i++) {
      T object = objects.get(i);
      data[i] = new Data(object.getID(), object.getValues(), null);
    }

    String msg = getClass() + "\n" +
                 " file    = " + file.getClass() + "\n" +
                 " maximum number of dir entries = " + (dirCapacity - 1) + "\n" +
                 " minimum number of dir entries = " + dirMinimum + "\n" +
                 " maximum number of leaf entries = " + (leafCapacity - 1) + "\n" +
                 " minimum number of leaf entries = " + leafMinimum + "\n";

//    System.out.println(msg);
    // create the nodes
    bulkLoad(data);

    msg += " height  = " + height + "\n" +
           " root    = " + getRoot();

    logger.info(msg);
  }

  /**
   * Inserts the specified reel vector object into this index.
   *
   * @param o the vector to be inserted
   */
  public synchronized void insert(T o) {
    Data data = new Data(o.getID(), Util.unbox(o.getValues()), null);
    reinsertions.clear();
    insert(data, 1);

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
  public synchronized boolean delete(T o) {
    logger.info("delete " + o + "\n");

    // find the leaf node containing o
    MBR mbr = new MBR(Util.unbox(o.getValues()), Util.unbox(o.getValues()));
    ParentInfo del = findLeaf((AbstractNode) getRoot(), mbr, o.getID());
    if (del == null) return false;
    AbstractNode leaf = del.leaf;
    int index = del.index;

    // delete o
    leaf.deleteEntry(index);
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
  public List<QueryResult> rangeQuery(T obj, String epsilon,
                                      SpatialDistanceFunction<T> distanceFunction) {

    Distance range = distanceFunction.valueOf(epsilon);
    final List<QueryResult> result = new ArrayList<QueryResult>();
    final Heap<Distance, Identifiable> pq = new DefaultHeap<Distance, Identifiable>();

    // push root
    pq.addNode(new PQNode(distanceFunction.nullDistance(), ROOT_NODE_ID));

    // search in tree
    while (!pq.isEmpty()) {
      HeapNode<Distance, Identifiable> pqNode = pq.getMinNode();
      if (pqNode.getKey().compareTo(range) > 0) break;

      AbstractNode node = getNode(pqNode.getValue().getID());
      final int numEntries = node.getNumEntries();

      for (int i = 0; i < numEntries; i++) {
        Distance distance = distanceFunction.minDist(node.entries[i].getMBR(), obj);
        if (distance.compareTo(range) <= 0) {
          Entry entry = node.entries[i];
          if (node.isLeaf()) {
            result.add(new QueryResult(entry.getID(), distance));
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
  public List<QueryResult> kNNQuery(T obj, int k,
                                    SpatialDistanceFunction<T> distanceFunction) {
    if (k < 1) {
      throw new IllegalArgumentException("At least one enumeration has to be requested!");
    }

    // variables
    final Heap<Distance, Identifiable> pq = new DefaultHeap<Distance, Identifiable>();
    final KNNList knnList = new KNNList(k, distanceFunction.infiniteDistance());

    // push root
    pq.addNode(new PQNode(distanceFunction.nullDistance(), ROOT_NODE_ID));
    Distance maxDist = distanceFunction.infiniteDistance();
    // search in tree

    while (!pq.isEmpty()) {
      HeapNode<Distance, Identifiable> pqNode = pq.getMinNode();

      if (pqNode.getKey().compareTo(maxDist) > 0) {
        return knnList.toList();
      }

      AbstractNode node = getNode(pqNode.getValue().getID());
      // data node
      if (node.isLeaf()) {
        for (int i = 0; i < node.numEntries; i++) {
          Entry entry = node.entries[i];
          Distance distance = distanceFunction.minDist(entry.getMBR(), obj);
          if (obj.getID() == 64 && entry.getID() == 80) System.out.println(entry.getID() + " " + distance);
          if (distance.compareTo(maxDist) <= 0) {
            if (obj.getID() == 64 && entry.getID() == 80) {
              System.out.println(knnList.add(new QueryResult(entry.getID(), distance)));
              System.out.println(knnList);
            }
            else
              knnList.add(new QueryResult(entry.getID(), distance));
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
          Distance distance = distanceFunction.minDist(entry.getMBR(), obj);
          if (distance.compareTo(maxDist) <= 0) {
            pq.addNode(new PQNode(distance, entry.getID()));
          }

        }
      }
    }

    return knnList.toList();

  }

  /**
   * Returns a list of the leaf nodes of this spatial index.
   *
   * @return a list of the leaf nodes of this spatial index
   */
  public List<SpatialNode> getLeafNodes() {
    List<SpatialNode> result = new ArrayList<SpatialNode>();

    if (height == 1) {
      AbstractNode root = (AbstractNode) getRoot();
//      result.add(new DirectoryEntry(ROOT_NODE_ID, root.mbr()));
      MBR rootMBR = root.mbr();
      result.add(new NodeWrapper(root, rootMBR));
      return result;
    }

    getLeafNodes((AbstractNode) getRoot(), result, height);
    return result;
  }

  /**
   * Returns an iterator over the data objects stored in this RTree.
   *
   * @return an iterator over the data objects stored in this RTree
   */
  public Iterator<SpatialData> dataIterator() {
    return new Iterator<SpatialData>() {
      AbstractNode root = (AbstractNode) getRoot();
      BreadthFirstEnumeration<AbstractNode> enumeration =
      new BreadthFirstEnumeration<AbstractNode>(file, new DirectoryEntry(root.getID(), root.mbr()));

      public boolean hasNext() {
        // last element must be a data enumeration
        return enumeration.hasMoreElements();
      }

      public SpatialData next() {
        Object o = enumeration.nextElement();
        while (o instanceof AbstractNode && enumeration.hasMoreElements()) {
          o = enumeration.nextElement();
        }
        return (SpatialData) o;
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
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
  public AbstractNode getNode(int nodeID) {
    if (nodeID == ROOT_NODE_ID) return (AbstractNode) getRoot();
    else {
      return file.readPage(nodeID);
    }
  }

  /**
   * Returns the entry that denotes the root.
   *
   * @return the entry that denotes the root
   */
  public Entry getRootEntry() {
    AbstractNode root = (AbstractNode) getRoot();
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

    AbstractNode node = (AbstractNode) getRoot();
    int dim = node.entries[0].getMBR().getDimensionality();

    while (!node.isLeaf()) {
      if (node.getNumEntries() > 0) {
        Entry entry = node.entries[0];
        node = getNode(entry.getID());
        levels++;
      }
    }

    AbstractNode root = (AbstractNode) getRoot();
    BreadthFirstEnumeration<AbstractNode> enumeration =
    new BreadthFirstEnumeration<AbstractNode>(file, new DirectoryEntry(root.getID(), root.mbr()));

    while (enumeration.hasMoreElements()) {
      Entry entry = enumeration.nextElement();
      if (entry.isLeafEntry())
        objects++;
      else {
        node = file.readPage(entry.getID());
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

    AbstractNode node = (AbstractNode) getRoot();
    int dim = node.entries[0].getMBR().getDimensionality();

    while (!node.isLeaf()) {
      if (node.getNumEntries() > 0) {
        Entry entry = node.entries[0];
        node = getNode(entry.getID());
        levels++;
      }
    }

    AbstractNode root = (AbstractNode) getRoot();
    BreadthFirstEnumeration<AbstractNode> enumeration =
    new BreadthFirstEnumeration<AbstractNode>(file, new DirectoryEntry(root.getID(), root.mbr()));

    while (enumeration.hasMoreElements()) {
      Entry entry = enumeration.nextElement();
      if (entry.isLeafEntry())
        objects++;
      else {
        node = file.readPage(entry.getID());
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
   * Creates and returns a new root node that points to the two specified child nodes.
   *
   * @param oldRoot the old root of this RTree
   * @param newNode the new split node
   * @return a new root node that points to the two specified child nodes
   */
  private AbstractNode createNewRoot(final AbstractNode oldRoot, final AbstractNode newNode) {
    logger.info("create new root");
    AbstractNode root = createNewDirectoryNode(dirCapacity);
    file.writePage(root);

    oldRoot.nodeID = root.getID();
    if (!oldRoot.isLeaf()) {
      for (int i = 0; i < oldRoot.getNumEntries(); i++) {
        AbstractNode node = getNode(oldRoot.entries[i].getID());
        node.parentID = oldRoot.nodeID;
        file.writePage(node);
      }
    }

    root.nodeID = ROOT_NODE_ID;
    root.addEntry(oldRoot);
    root.addEntry(newNode);

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
  private AbstractNode chooseNode(AbstractNode node, MBR mbr, int level, int currentLevel) {
    logger.info("node " + node + ", level " + level);

    if (node.isLeaf()) return node;

    AbstractNode childNode = getNode(node.entries[0].getID());
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
  private AbstractNode getLeastEnlargement(AbstractNode node, MBR mbr) {
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
  private AbstractNode getLeastOverlap(AbstractNode node, MBR mbr) {
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
  private AbstractNode overflowTreatment(AbstractNode node, int level) {
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
  private AbstractNode split(AbstractNode node) {
    // choose the split dimension and the split point
    int minimum = node.isLeaf() ? leafMinimum : dirMinimum;
    SplitDescription split = new SplitDescription();
    split.chooseSplitAxis(node.entries, minimum);
    split.chooseSplitPoint(node.entries, minimum);

    // do the split
    AbstractNode newNode;

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
  private void reInsert(AbstractNode node, int level) {
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
    AbstractNode child = node;
    while (child.parentID != null) {
      AbstractNode parent = getNode(child.parentID);
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
        Data o = new Data(entry.getID(), entry.getValues(), null);
        insert(o, level);
      }
      else {
        DirectoryEntry entry = (DirectoryEntry) re.getEntry();
        AbstractNode reNode = getNode(entry.getID());
        insert(reNode, level);
      }
    }
  }

  /**
   * Returns true if in the specified node an overflow occured, false otherwise.
   *
   * @param node the node to be tested for overflow
   * @return true if in the specified node an overflow occured, false otherwise
   */
  abstract boolean hasOverflow(AbstractNode node);

  /**
   * Inserts the specified data object into this RTree.
   *
   * @param o     the spatial object to be inserted
   * @param level the level at which the spatial object should be inserted (1 = leaf level)
   */
  synchronized void insert(SpatialObject o, int level) {
    logger.info("insert " + o + "\n");

    // choose node for insertion of o
    MBR mbr = o.mbr();
    AbstractNode node = chooseNode((AbstractNode) getRoot(), mbr, level, height);
    node.addEntry(o);
    file.writePage(node);

    // adjust the tree from current level to root level
    while (node != null) {
      // read again from file because of changes during reinsertion
      node = getNode(node.getID());

      // overflow in node
      if (hasOverflow(node)) {
        // treatment of overflow: reinsertion or split
        AbstractNode split = overflowTreatment(node, level);

        // node was splitted
        if (split != null) {
          // if root was split: create a new root that points the two split nodes
          if (node.getID() == ROOT_NODE_ID) {
            node = createNewRoot(node, split);
          }
          // node is not root
          if (node.getID() != ROOT_NODE_ID) {
            // get the parent and add the new split node
            AbstractNode parent = getNode(node.parentID);
            parent.addEntry(split);

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
        AbstractNode parent = getNode(node.parentID);
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
   * Returns the leaf node in the specified subtree that contains the data object
   * with the specified mbr and id.
   *
   * @param subtree the current root of the subtree to be tested
   * @param mbr     the mbr to look for
   * @param id      the id to look for
   * @return the leaf node of the specified subtree
   *         that contains the data object with the specified mbr and id
   */
  ParentInfo findLeaf(AbstractNode subtree, MBR mbr, int id) {
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
          AbstractNode child = getNode(subtree.entries[i].getID());
          ParentInfo parentInfo = findLeaf(child, mbr, id);
          if (parentInfo != null) return parentInfo;
        }
      }
    }
    return null;
  }

  /**
   * Condenses the tree after deletion of some nodes.
   *
   * @param node  the current root of the subtree to be condensed
   * @param stack the stack holding the nodes to be reinserted
   */
  private void condenseTree(AbstractNode node, Stack<AbstractNode> stack) {
    // node is not root
    if (node.getID() != ROOT_NODE_ID) {
      AbstractNode parent = getNode(node.parentID);
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
  }

  /**
   * Creates and returns the leaf nodes for bulk load.
   *
   * @param objects the objects to be inserted
   * @return the leaf nodes containing the objects
   */
  AbstractNode[] createLeafNodes(SpatialData[] objects) {
    int minEntries = leafMinimum;
    int maxEntries = leafCapacity - 1;

    ArrayList<AbstractNode> result = new ArrayList<AbstractNode>();
    while (objects.length > 0) {
      StringBuffer msg = new StringBuffer();

      // get the split axis and split point
      int splitAxis = SplitDescription.chooseBulkSplitAxis(objects);
      int splitPoint = SplitDescription.chooseBulkSplitPoint(objects.length, minEntries, maxEntries);
      msg.append("\nsplitAxis ").append(splitAxis);
      msg.append("\nsplitPoint ").append(splitPoint);

      // sort in the right dimension
      final SpatialComparator comp = new SpatialComparator();
      comp.setCompareDimension(splitAxis);
      comp.setComparisonValue(SpatialComparator.MIN);
      Arrays.sort(objects, comp);

      // create leaf node
      AbstractNode leafNode = createNewLeafNode(leafCapacity);
      file.writePage(leafNode);
      result.add(leafNode);

      // insert data
      for (int i = 0; i < splitPoint; i++) {
        leafNode.addEntry(objects[i]);
      }

      // copy array
      SpatialData[] rest = new SpatialData[objects.length - splitPoint];
      System.arraycopy(objects, splitPoint, rest, 0, objects.length - splitPoint);
      objects = rest;
      msg.append("\nremaining objects # ").append(objects.length);

      // write to file
      file.writePage(leafNode);
      msg.append("\npageNo ").append(leafNode.getID());
      logger.fine(msg.toString() + "\n");

//      System.out.print("\r numDataPages = " + result.size());
    }

    logger.fine("numDataPages = " + result.size());
    return result.toArray(new AbstractNode[result.size()]);
  }

  /**
   * Computes the height of this RTree. Is called by the constructor.
   *
   * @return the height of this RTree
   */
  abstract int computeHeight();

  /**
   * Creates an empty root node and writes it to file. Is called by the constructor.
   *
   * @param dimensionality the dimensionality of the data objects to be stored
   */
  abstract void createEmptyRoot(int dimensionality);

  /**
   * Performs a bulk load on this RTree with the specified data.
   * Is called by the constructor.
   *
   * @param data the data objects to be indexed
   */
  abstract void bulkLoad(Data[] data);

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  abstract AbstractNode createNewLeafNode(int capacity);

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  abstract AbstractNode createNewDirectoryNode(int capacity);

  /**
   * Initializes the logger object.
   */
  void initLogger() {
    logger = Logger.getLogger(getClass().toString());
    logger.setLevel(loggerLevel);
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
   * Determines the ids of the leaf nodes of the specified subtree
   *
   * @param node   the subtree
   * @param result the result to store the ids in
   */
  private void getLeafNodes(AbstractNode node, List<SpatialNode>result, int currentLevel) {
    if (currentLevel == 2) {
      for (int i = 0; i < node.numEntries; i++) {
        AbstractNode child = getNode(node.entries[i].getID());
        MBR childMBR = node.entries[i].getMBR();
        result.add(new NodeWrapper(child, childMBR));
      }
    }
    else {
      for (int i = 0; i < node.numEntries; i++) {
        AbstractNode child = file.readPage(node.entries[i].getID());
        getLeafNodes(child, result, (currentLevel - 1));
      }
    }
  }

  /**
   * Encapsulates the attributes for a parent leaf node of a data object.
   */
  class ParentInfo {
    /**
     * The leaf node holding the data object.
     */
    AbstractNode leaf;

    /**
     * The index of the data object.
     */
    int index;

    /**
     * Creates a new ParentInfo object with the specified parameters.
     *
     * @param leaf  the leaf node holding the data object
     * @param index the index of the data object
     */
    public ParentInfo(AbstractNode leaf, int index) {
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

  private class NodeWrapper implements SpatialNode {
    AbstractNode node;
    MBR mbr;

    public NodeWrapper() {
    }

    public NodeWrapper(AbstractNode node, MBR mbr) {
      this.node = node;
      this.mbr = mbr;
    }

    /**
     * Returns the id of this node.
     *
     * @return the id of this node
     */
    public int getNodeID() {
      return node.getID();
    }

    /**
     * Returns the number of entries of this node.
     *
     * @return the number of entries of this node
     */
    public int getNumEntries() {
      return node.getNumEntries();
    }

    /**
     * Returns true if this node is a leaf node, false otherwise.
     *
     * @return true if this node is a leaf node, false otherwise
     */
    public boolean isLeaf() {
      return node.isLeaf();
    }

    /**
     * Returns an enumeration of the children of this node.
     *
     * @return an enumeration of the children of this node
     */
    public Enumeration<Entry> children() {
      return node.children();
    }

    /**
     * Returns the entry at the specified index.
     *
     * @param index the index of the entry to be returned
     * @return the entry at the specified index
     */
    public Entry getEntry(int index) {
      return  node.getEntry(index);
    }

    /**
     * Returns the MBR of this node.
     *
     * @return the MBR of this node
     */
    public MBR mbr() {
      return mbr;
    }

    /**
     * Returns the id of the parent node of this spatial object.
     *
     * @return the id of the parent node of this spatial object
     */
    public int getParentID() {
      return node.getParentID();
    }

    /**
     * Returns the dimensionality of this spatial object.
     *
     * @return the dimensionality of this spatial object
     */
    public int getDimensionality() {
      return node.getDimensionality();
    }

    /**
     * Returns the unique id of this Page.
     *
     * @return the unique id of this Page
     */
    public Integer getID() {
      return node.getID();
    }

    /**
     * Sets the unique id of this Page.
     *
     * @param id the id to be set
     */
    public void setID(int id) {
      node.setID(id);
    }

    /**
     * Sets the page file of this page.
     *
     * @param file the page file to be set
     */
    public void setFile(PageFile file) {
      node.setFile(file);
    }

    /**
     * The object implements the writeExternal method to save its contents
     * by calling the methods of DataOutput for its primitive values or
     * calling the writeObject method of ObjectOutput for objects, strings,
     * and arrays.
     *
     * @param out the stream to write the object to
     * @throws java.io.IOException Includes any I/O exceptions that may occur
     * @serialData Overriding methods should use this tag to describe
     * the data layout of this Externalizable object.
     * List the sequence of element types and, if possible,
     * relate the element to a public/protected field and/or
     * method of this Externalizable class.
     */
    public void writeExternal(ObjectOutput out) throws IOException {
      node.writeExternal(out);
    }

    /**
     * The object implements the readExternal method to restore its
     * contents by calling the methods of DataInput for primitive
     * types and readObject for objects, strings and arrays.  The
     * readExternal method must read the values in the same sequence
     * and with the same types as were written by writeExternal.
     *
     * @param in the stream to read data from in order to restore the object
     * @throws java.io.IOException    if I/O errors occur
     * @throws ClassNotFoundException If the class for an object being
     *                                restored cannot be found.
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
      node.readExternal(in);
    }


  }

}
