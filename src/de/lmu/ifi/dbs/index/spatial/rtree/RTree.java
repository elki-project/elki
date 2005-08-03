package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.index.spatial.*;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.heap.DefaultHeapNode;
import de.lmu.ifi.dbs.utilities.heap.Heap;
import de.lmu.ifi.dbs.caching.Cache;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RTree is a spatial index structure. Apart from organizing the objects
 * it also provides several methods to search for certain object in the
 * structure and ensures persistence.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class RTree implements SpatialIndex {
  /**
   * Logger object for logging messages.
   */
  protected static Logger logger;

  /**
   * The loggerLevel for logging messages.
   */
  protected static Level loggerLevel = Level.ALL;

  /**
   * The file storing the entries of this RTree.
   */
  private final RTreeFile file;

  /**
   * Contains a boolean for each level of this RTree that indicates
   * if there was already a reinsert operation in this level
   * during the current insert / delete operation
   */
  private final Map<Integer, Boolean> reinsertions;

  /**
   * The height of this RTree.
   */
  private int height;

  /**
   * Creates a new RTree from an existing file
   *
   * @param fileName  the name of the file for storing the entries,
   * @param cacheSize the size of the cache in bytes
   */
  public RTree(String fileName, int cacheSize) {
    initLogger();

    this.reinsertions = new HashMap<Integer, Boolean>();
    this.file = new PersistentRTreeFile(cacheSize, RTreeFile.LRU_CACHE, fileName);

    // compute height
    Node node = file.readNode(0);
    while (!node.isLeaf() && node.getNumEntries() != 0) {
      Entry entry = node.entries[0];
      node = file.readNode(entry.getID());
      height++;
    }
    logger.info("height " + height);
  }

  /**
   * Creates a new RTree with the specified parameters.
   *
   * @param dimensionality the dimensionality of the data objects to be indexed
   * @param fileName       the name of the file for storing the entries,
   *                       if this parameter is null all entries will be hold in
   *                       main memory
   * @param pageSize       the size of a page in bytes
   * @param cacheSize      the size of the cache in bytes
   * @param flatDirectory  if true, this RTree will have a flat directory
   *                       (only one level)
   */
  public RTree(int dimensionality, String fileName, int pageSize,
               int cacheSize, boolean flatDirectory) {

    initLogger();

    this.reinsertions = new HashMap<Integer, Boolean>();

    if (fileName == null) {
      this.file = new MemoryRTreeFile(dimensionality, pageSize, cacheSize,
                                      RTreeFile.LRU_CACHE, flatDirectory);
    }
    else {
      this.file = new PersistentRTreeFile(dimensionality, pageSize, cacheSize,
                                          RTreeFile.LRU_CACHE, flatDirectory, fileName);
    }

    Node rootNode = new LeafNode(file);
    file.writeNode(rootNode);

    height = 0;
    String msg = "Capacity " + (file.getCapacity() - 1) + "\n" +
                 "root: " + rootNode.getID() + "\n" +
                 "height: " + height + "\n";

    logger.info(msg);
  }

  /**
   * Creates a new RTree with the specified parameters.
   *
   * @param objects       the vector objects to be indexed
   * @param ids           the ids of the vector objects
   * @param fileName      the name of the file for storing the entries,
   *                      if this parameter is null all entries will be hold in
   *                      main memory
   * @param pageSize      the size of a page in bytes
   * @param cacheSize     the size of the cache (must be >= 1)
   * @param flatDirectory id true, this RTree will have a flat directory
   *                      (only one level)
   */
  public RTree(final RealVector[] objects, final int[] ids, final String fileName,
               final int pageSize, final int cacheSize, final boolean flatDirectory) {

    if (objects.length != ids.length)
      throw new IllegalArgumentException("objects.length != ids.length!");

    initLogger();
    StringBuffer msg = new StringBuffer();

    this.reinsertions = new HashMap<Integer, Boolean>();

    // create file
    int dimension = objects[0].getValues().length;
    if (fileName == null) {
      this.file = new MemoryRTreeFile(dimension, pageSize, cacheSize,
                                      RTreeFile.LRU_CACHE, flatDirectory);
    }
    else {
      this.file = new PersistentRTreeFile(dimension, pageSize, cacheSize,
                                          RTreeFile.LRU_CACHE, flatDirectory, fileName);
    }

    int maxLoad = file.getMaximum();
    msg.append("\n  maxLoad = " + maxLoad);

    // wrap the vector objects to data objects
    Data[] data = new Data[objects.length];
    for (int i = 0; i < objects.length; i++) {
      RealVector object = objects[i];
      data[i] = new Data(ids[i], object.getValues(), -1);
    }

    // root is leaf node
    if ((double) objects.length / (double) maxLoad <= 1) {
      LeafNode root = new LeafNode(file);
      file.writeNode(root);
      createRoot(root, data);
      height = 1;
      msg.append("\n  numNodes = 1");
    }

    // root is directory node
    else {
      DirectoryNode root = new DirectoryNode(file);
      file.writeNode(root);

      // create leaf nodes
      Node[] nodes = createLeafNodes(data);
      int numNodes = nodes.length;
      msg.append("\n  numLeafNodes = " + numNodes);
      height = 1;

      // create directory nodes
      while (nodes.length > maxLoad) {
        nodes = createDirectoryNodes(nodes);
        numNodes += nodes.length;
        height++;
      }
      // create root
      createRoot(root, nodes);
      numNodes++;
      height++;
      msg.append("\n  numNodes = " + numNodes);
    }
    msg.append("\n  height = " + height);
    logger.info(msg.toString() + "\n");
  }

  /**
   * Inserts the specified reel vector object into this index.
   *
   * @param id the id of the object to be inserted
   * @param o  the vector to be inserted
   */
  public synchronized void insert(int id, RealVector o) {
    Data data = new Data(id, o.getValues(), -1);
    reinsertions.clear();
    insert(data, 0);
  }

  /**
   * Deletes the specified obect from this index.
   *
   * @param id the id of the object to be deleted
   * @param o  the object to be deleted
   * @return true if this index did contain the object with the specified id,
   *         false otherwise
   *         TODO test rausnehmen!
   */
  public synchronized boolean delete(int id, RealVector o) {
    logger.info("delete " + o + "\n");

    // find the leaf node containing o
    MBR mbr = new MBR(o.getValues(), o.getValues());
    Deletion del = findLeaf(file.readNode(0), mbr, id);
    if (del == null) return false;
    Node leaf = del.leaf;
    int index = del.index;

    // delete o
    leaf.deleteEntry(index);
    file.writeNode(leaf);

    // condense the tree
    Stack stack = new Stack();
    condenseTree(leaf, stack);

    // reinsert underflow nodes
    while (!stack.empty()) {
      Node node = (Node) stack.pop();
      if (node.isLeaf()) {
        for (int i = 0; i < node.getNumEntries(); i++) {
          Entry e = node.entries[i];
          Data obj = new Data(e.getID(), e.getMBR().getMin(), -1);
          reinsertions.clear();
          this.insert(obj, 0);
        }
      }
      else {
        for (int i = 0; i < node.getNumEntries(); i++) {
          stack.push(file.readNode(node.entries[i].getID()));
        }
      }
      file.deleteNode(node.getID());
    }

    try {
      Node root = file.readNode(0);
      root.test();
    }
    catch (RuntimeException e) {
      throw e;
    }

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
  public List<QueryResult> rangeQuery(RealVector obj, String epsilon,
                                      SpatialDistanceFunction distanceFunction) {

    Distance range = distanceFunction.valueOf(epsilon);
    final List<QueryResult> result = new ArrayList<QueryResult>();
    final Heap pq = new Heap();

    // push root
    pq.addNode(new DefaultHeapNode(getRoot(), distanceFunction.nullDistance()));

    // search in tree
    while (!pq.isEmpty()) {
      DefaultHeapNode pqNode = (DefaultHeapNode) pq.getMinNode();
      if (pqNode.getKey().compareTo(range) > 0) break;

      Node node = (Node) pqNode.getObject();
      final int numEntries = node.getNumEntries();

      for (int i = 0; i < numEntries; i++) {
        Distance distance = distanceFunction.minDist(node.entries[i].getMBR(), obj);
        if (distance.compareTo(range) <= 0) {
          Entry entry = node.entries[i];
          if (node.isLeaf()) {
            result.add(new QueryResult(entry.getID(), distance));
          }
          else {
            Node childNode = file.readNode(entry.getID());
            pq.addNode(new DefaultHeapNode(childNode, distance));
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
  public List<QueryResult> kNNQuery(RealVector obj, int k,
                                    SpatialDistanceFunction distanceFunction) {
    if (k < 1) {
      throw new IllegalArgumentException("At least one enumeration has to be requested!");
    }

    // variables
    final Heap pq = new Heap();
    final KNNList knnList = new KNNList(k, distanceFunction.infiniteDistance());

    // push root
    pq.addNode(new DefaultHeapNode(file.readNode(0), distanceFunction.nullDistance()));
    Distance maxDist = distanceFunction.infiniteDistance();
    // search in tree

    while (!pq.isEmpty()) {
      DefaultHeapNode pqNode = (DefaultHeapNode) pq.getMinNode();

      if (pqNode.getKey().compareTo(maxDist) > 0) {
        return knnList.toList();
      }

      Node node = (Node) pqNode.getObject();
      // data node
      if (node.isLeaf()) {
        for (int i = 0; i < node.numEntries; i++) {
          Entry entry = node.entries[i];
          Distance distance = distanceFunction.minDist(entry.getMBR(), obj);
          if (distance.compareTo(maxDist) <= 0) {
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
            pq.addNode(new DefaultHeapNode(file.readNode(entry.getID()), distance));
          }

        }
      }
    }

    return knnList.toList();

  }

  /**
   * Returns an iterator over the data objects stored in this RTree.
   *
   * @return an iterator over the data objects stored in this RTree
   */
  public Iterator<SpatialData> dataIterator() {
    return new Iterator<SpatialData>() {
      BreadthFirstEnumeration enumeration = new BreadthFirstEnumeration(getRoot());

      public boolean hasNext() {
        // last element must be a data enumeration
        return enumeration.hasMoreElements();
      }

      public SpatialData next() {
        Object o = enumeration.nextElement();
        while (o instanceof Node && enumeration.hasMoreElements()) {
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
  public int getIOAccess() {
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
  public SpatialNode getNode(int nodeID) {
    return file.readNode(nodeID);
  }

  /**
   * Returns the root node of this RTree.
   *
   * @return the root node of this RTree
   */
  public SpatialNode getRoot() {
    return file.readNode(0);
  }

  /**
   * Tests this RTree for debugging purposes.
   *
   * @return the number of objects stored in this RTree
   */
  public int test() {
    int io = file.getIOAccess();
    Cache cacheClone = (Cache) file.cache.clone();

    StringBuffer result = new StringBuffer();
    int dirNodes = 0;
    int leafNodes = 0;
    int objects = 0;
    int levels = 0;
    final Vector<SpatialNode> v = new Vector<SpatialNode>();
    v.add(getRoot());

    Node node = (Node) getRoot();
    while (!node.isLeaf()) {
      if (node.getNumEntries() > 0) {
        Entry entry = node.entries[0];
        node = file.readNode(entry.getID());
        levels++;
      }
    }

    BreadthFirstEnumeration enumeration = new BreadthFirstEnumeration(getRoot());
    while (enumeration.hasMoreElements()) {
      SpatialObject entry = enumeration.nextElement();
      if (entry instanceof SpatialData)
        objects++;
      else {
        node = (Node) entry;
        if (node.isLeaf())
          leafNodes++;
        else
          dirNodes++;
      }
    }

    file.ioAccess = io;
    file.cache = cacheClone;

    result.append("RTree hat " + (levels + 1) + " Ebenen \n");
    result.append(dirNodes + " Directory Knoten \n");
    result.append(leafNodes + " Daten Knoten (capacity = " + (file.getMaximum()) + ")\n");
    result.append(objects + " " + file.getDimensionality() + "-dim. Punkte im Baum \n");

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
    int io = file.getIOAccess();
    Cache cacheClone = (Cache) file.cache.clone();

    StringBuffer result = new StringBuffer();
    int dirNodes = 0;
    int leafNodes = 0;
    int objects = 0;
    int levels = 0;
    final Vector<SpatialNode> v = new Vector<SpatialNode>();
    v.add(getRoot());

    Node node = (Node) getRoot();
    while (!node.isLeaf()) {
      if (node.getNumEntries() > 0) {
        Entry entry = node.entries[0];
        node = file.readNode(entry.getID());
        levels++;
      }
    }

    BreadthFirstEnumeration enumeration = new BreadthFirstEnumeration(getRoot());
    while (enumeration.hasMoreElements()) {
      SpatialObject entry = enumeration.nextElement();
      if (entry instanceof SpatialData)
        objects++;
      else {
        node = (Node) entry;
        if (node.isLeaf())
          leafNodes++;
        else
          dirNodes++;
      }
    }

    file.ioAccess = io;
    file.cache = cacheClone;

    result.append("RTree hat " + (levels + 1) + " Ebenen \n");
    result.append(dirNodes + " Directory Knoten \n");
    result.append(leafNodes + " Daten Knoten (max = " + (file.getMaximum()) + ", min = " + file.getMinimum() + ")\n");
    result.append(objects + " " + file.getDimensionality() + "-dim. Punkte im Baum \n");
    result.append("IO-Access: " + file.getIOAccess() + "\n");
    result.append("File " + file.getClass() + "\n");

    return result.toString();
  }

  /**
   * Creates and returns a new root node that points to the two specified child nodes.
   *
   * @param oldRoot the old root of this RTree
   * @param newNode the new split node
   * @return a new root node that points to the two specified child nodes
   */
  private DirectoryNode createNewRoot(final Node oldRoot, final Node newNode) {
    logger.info("create new root");
    DirectoryNode root = new DirectoryNode(file);
    file.writeNode(root);

    oldRoot.nodeID = root.getID();
    if (!oldRoot.isLeaf()) {
      for (int i = 0; i < oldRoot.getNumEntries(); i++) {
        Node node = file.readNode(oldRoot.entries[i].getID());
        node.parentID = oldRoot.nodeID;
        file.writeNode(node);
      }
    }

    root.nodeID = 0;
    root.addEntry(oldRoot);
    root.addEntry(newNode);

    file.writeNode(root);
    file.writeNode(oldRoot);
    file.writeNode(newNode);
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
   * @param mbr          the mbr of the node to be inserted
   * @param level        the level at which the node should be inserted
   * @param currentLevel the current level of this method (should be initialized
   *                     with zero at the first call)
   * @return the appropriate subtree to insert the given node
   */
  private Node chooseNode(Node node, MBR mbr, int level, int currentLevel) {
    logger.info("node " + node + ", level " + level);

    if (node.isLeaf()) return node;

    Node childNode = file.readNode(node.entries[0].getID());
    // children are leafs
    if (childNode.isLeaf()) {
      if (currentLevel - 1 == level)
        return getLeastOverlap(node, mbr);
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
  private Node getLeastEnlargement(Node node, MBR mbr) {
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

    return file.readNode(min.nodeID);
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
  private Node getLeastOverlap(Node node, MBR mbr) {
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

    return file.readNode(min.nodeID);
  }

  /**
   * If the node is not the root node and this is the first call of overflowTreatment
   * in the given level during insertion the specified node will be reinserted,
   * otherwise the node will be splitted.
   *
   * @param node  the node where an overflow occured
   * @param level the level of the node in the tree (leaf level = 0)
   */
  private Node overflowTreatment(Node node, int level) {
    Boolean reInsert = reinsertions.get(new Integer(level));

    // there was still no reinsert operation at this level
    if (node.getID() != 0 && (reInsert == null || !reInsert.booleanValue())) {
      reinsertions.put(new Integer(level), new Boolean(true));
      reInsert(node, level);
      return null;
    }

    // there was already a reinsert operation at this level
    else {
      Node split = split(node);
      return split;
    }
  }

  /**
   * Splits the specified node and returns the newly created split node.
   *
   * @param node the node to be splitted
   * @return the newly created split node
   */
  private Node split(Node node) {
    // choose the split dimension and the split point
    SplitDescription split = new SplitDescription();
    split.chooseSplitAxis(node.entries, file.getMinimum());
    split.chooseSplitPoint(node.entries, file.getMinimum());

    // do the split
    Node newNode = null;

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
    file.writeNode(node);
    file.writeNode(newNode);

    return newNode;
  }

  /**
   * Reinserts the specified node at the specified level.
   *
   * @param node  the node to be reinserted
   * @param level the level of the node
   */
  private void reInsert(Node node, int level) {
    MBR mbr = node.mbr();
    SpatialDistanceFunction distFunction = new EuklideanDistanceFunction();
    ReinsertEntry[] reInsertEntries = new ReinsertEntry[node.getNumEntries()];

    // compute the center distances of entries to the node and sort it
    // in decreasing order to their distances
    for (int i = 0; i < node.getNumEntries(); i++) {
      Entry entry = node.entries[i];
      Distance dist = distFunction.centerDistance(mbr, entry.getMBR());
      reInsertEntries[i] = new ReinsertEntry(entry.getID(), entry.getMBR(), dist);
    }
    Arrays.sort(reInsertEntries);

    // define, how many entries will be reinserted
    int start = (int) (0.3 * (double) node.getNumEntries());

    // initialize the reinsertion operation: move the remaining entries forward
    node.initReInsert(start, reInsertEntries);
    file.writeNode(node);
    // and adapt the mbrs
    Node child = node;
    while (child.parentID != -1) {
      Node parent = file.readNode(child.parentID);
      parent.entries[child.index].setMBR(child.mbr());
      file.writeNode(parent);
      child = parent;
    }

    // reinsert the first entries
    for (int i = 0; i < start; i++) {
      ReinsertEntry re = reInsertEntries[i];
      if (node.isLeaf()) {
        Data o = new Data(re.getID(), re.getMBR().getMin(), -1);
        insert(o, level);
      }
      else {
        Node reNode = file.readNode(re.getID());
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
  private boolean hasOverflow(Node node) {
    if (node.isLeaf())
      return node.getNumEntries() == file.getCapacity();
    else
      return !file.isFlatDirectory() && node.getNumEntries() == file.getCapacity();
  }

  /**
   * Inserts the specified data object into this RTree.
   *
   * @param o     the spatial object to be inserted
   * @param level the level at which the spatial object should be inserted
   */
  private synchronized void insert(SpatialObject o, int level) {
    logger.info("insert " + o + "\n");

    // choose leaf node for insertion of o
    MBR mbr = o.mbr();
    Node node = chooseNode(file.readNode(0), mbr, 0, height);
    node.addEntry(o);
    file.writeNode(node);

    // adjust the tree from current level to root level
    while (node != null) {
      // read again from file because of changes during reinsertion
      node = file.readNode(node.getID());

      // todo test raus
      Node fnode = file.readNode(node.getID());
      if (!node.equals(fnode)) {
        System.out.println("node " + node + " != fnode " + fnode);
        System.out.println("node.parent " + node.parentID + " != fnode.parent " + fnode.parentID);
        System.out.println("node.place " + node.index + " != fnode.place " + fnode.index);
        System.out.println("node.entries " + Arrays.asList(node.entries) + " !=  " +
                           "fnode.entries " + Arrays.asList(fnode.entries));
        throw new RuntimeException("!node.equals(fnode)");
      }
      // end todo

      // overflow in node
      if (hasOverflow(node)) {
        // treatment of overflow: reinsertion or split
        Node split = overflowTreatment(node, level);

        // node was splitted
        if (split != null) {
          // if root was split: create a new root that points the two split nodes
          if (node.getID() == 0) {
            node = createNewRoot(node, split);
          }
          // node is not root
          if (node.getID() != 0) {
            // get the parent and add the new split node
            Node parent = file.readNode(node.parentID);
            parent.addEntry(split);

            // adjust the mbrs in the parent node
            Entry entry1 = parent.entries[node.index];
            MBR mbr1 = node.mbr();
            entry1.setMBR(mbr1);
            Entry entry2 = parent.entries[split.index];
            MBR mbr2 = split.mbr();
            entry2.setMBR(mbr2);
            // write changes in parent to file
            file.writeNode(parent);
            node = parent;
          }
          // go to the next level
          level++;
        }
      }
      // no overflow, only adjust mbr of node in parent
      else if (node.getID() != 0) {
        // todo test raus
        fnode = file.readNode(node.getID());
        if (!fnode.equals(node)) throw new RuntimeException();
        // end todo

        Node parent = file.readNode(node.parentID);
        // todo test raus
        if (parent.entries[node.index].getID() != node.getID()) {
          throw new RuntimeException();
        }
        // end todo

        Entry entry = parent.entries[node.index];

        MBR newMbr = node.mbr();
        entry.setMBR(newMbr);
        // write changes in parent to file
        file.writeNode(parent);
        node = parent;
      }
      // no overflow occured or root level is reached
      else
        break;
    }

    // todo test raus
    try {
      Node root = file.readNode(0);
      root.test();
    }
    catch (RuntimeException e) {
      System.out.println("insert " + o + " " + mbr);
      logger.info("\ninsert " + o + " " + mbr);
      logger.info(this.toString());
      throw e;
    }
  }

  /**
   * Returns the leaf node of the specified subtree
   * that contains the data object with the specified mbr and id.
   *
   * @param node the current root of the subtree to be tested
   * @param mbr  the mbr to look for
   * @param id   the id to look for
   * @return the leaf node of the specified subtree
   *         that contains the data object with the specified mbr and id
   */
  private Deletion findLeaf(Node node, MBR mbr, int id) {
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        if (node.entries[i].getID() == id)
          return new Deletion((LeafNode) node, i);
      }
    }
    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        if (node.entries[i].getMBR().intersects(mbr)) {
          Node child = file.readNode(node.entries[i].getID());
          return findLeaf(child, mbr, id);
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
  private void condenseTree(Node node, Stack stack) {
    // node is not root
    if (node.getID() != 0) {
      Node p = file.readNode(node.parentID);
      if (node.getNumEntries() < file.getMinimum()) {
        p.deleteEntry(node.index);
        stack.push(node);
      }
      else {
        p.entries[node.index].setMBR(node.mbr());
      }
      file.writeNode(p);
      condenseTree(p, stack);
    }
    // node is root
    else {
      if (node.getNumEntries() == 1 && !node.isLeaf()) {
        Node child = file.readNode(node.entries[0].getID());
        Node newRoot = null;
        if (child.isLeaf()) {
          newRoot = new LeafNode(this.file);
          newRoot.nodeID = 0;
          for (int i = 0; i < child.getNumEntries(); i++) {
            Entry e = child.entries[i];
            Data o = new Data(e.getID(), e.getMBR().getMin(), 0);
            newRoot.addEntry(o);
          }
        }
        else {
          newRoot = new DirectoryNode(this.file);
          newRoot.nodeID = 0;
          for (int i = 0; i < child.getNumEntries(); i++) {
            Entry e = child.entries[i];
            Node n = file.readNode(e.getID());
            newRoot.addEntry(n);
          }
        }
        file.writeNode(newRoot);
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
  private LeafNode[] createLeafNodes(SpatialData[] objects) {
    int minEntries = file.getMinimum();
    int maxEntries = file.getMaximum();

    ArrayList<LeafNode> result = new ArrayList<LeafNode>();
    while (objects.length > 0) {
      StringBuffer msg = new StringBuffer();

      // get the split axis and split point
      int splitAxis = SplitDescription.chooseBulkSplitAxis(objects);
      int splitPoint = SplitDescription.chooseBulkSplitPoint(objects.length, minEntries, maxEntries);
      msg.append("\nsplitAxis " + splitAxis);
      msg.append("\nsplitPoint " + splitPoint);

      // sort in the right dimension
      final SpatialComparator comp = new SpatialComparator();
      comp.setCompareDimension(splitAxis);
      comp.setComparisonValue(SpatialComparator.MIN);
      Arrays.sort(objects, comp);

      // create node
      LeafNode leafNode = new LeafNode(this.file);
      file.writeNode(leafNode);
      result.add(leafNode);

      // insert data
      for (int i = 0; i < splitPoint; i++) {
        leafNode.addEntry(objects[i]);
      }

      // copy array
      SpatialData[] rest = new SpatialData[objects.length - splitPoint];
      System.arraycopy(objects, splitPoint, rest, 0, objects.length - splitPoint);
      objects = rest;
      msg.append("\nrestl. objects # " + objects.length);

      // write to file
      file.writeNode(leafNode);
      msg.append("\npageNo " + leafNode.getID());
      logger.fine(msg.toString() + "\n");
    }

    logger.fine("numDataPages = " + result.size());
    return result.toArray(new LeafNode[result.size()]);
  }

  /**
   * Creates and returns the directory nodes for bulk load.
   *
   * @param nodes the nodes to be inserted
   * @return the directory nodes containing the nodes
   */
  private DirectoryNode[] createDirectoryNodes(Node[] nodes) {
    int minEntries = file.getMinimum();
    int maxEntries = file.getMaximum();

    ArrayList<DirectoryNode> result = new ArrayList<DirectoryNode>();
    while (nodes.length > 0) {
      StringBuffer msg = new StringBuffer();

      // get the split axis and split point
      int splitAxis = SplitDescription.chooseBulkSplitAxis(nodes);
      int splitPoint = SplitDescription.chooseBulkSplitPoint(nodes.length, minEntries, maxEntries);
      msg.append("\nsplitAxis " + splitAxis);
      msg.append("\nsplitPoint " + splitPoint);

      // sort in the right dimension
      final SpatialComparator comp = new SpatialComparator();
      comp.setCompareDimension(splitAxis);
      Arrays.sort(nodes, comp);

      // create node
      DirectoryNode dirNode = new DirectoryNode(this.file);
      file.writeNode(dirNode);
      result.add(dirNode);

      // insert data
      for (int i = 0; i < splitPoint; i++) {
        dirNode.addEntry(nodes[i]);
      }

      // copy array
      Node[] rest = new Node[nodes.length - splitPoint];
      System.arraycopy(nodes, splitPoint, rest, 0, nodes.length - splitPoint);
      nodes = rest;
      msg.append("\nrestl. nodes # " + nodes.length);

      // write to file
      file.writeNode(dirNode);
      msg.append("\npageNo " + dirNode.getID());
      logger.fine(msg.toString() + "\n");
    }

    logger.info("numDirPages " + result.size());
    return result.toArray(new DirectoryNode[result.size()]);
  }

  /**
   * Returns the root node for bulk load
   *
   * @param root    the new root node
   * @param objects the objects (nodes or data objects) to be inserted
   * @return the root node
   */
  private Node createRoot(Node root, SpatialObject[] objects) {
    StringBuffer msg = new StringBuffer();

    // insert data
    for (int i = 0; i < objects.length; i++) {
      root.addEntry(objects[i]);
    }

    // write to file
    file.writeNode(root);
    msg.append("\npageNo " + root.getID());
    logger.fine(msg.toString() + "\n");

    return root;
  }

  /**
   * Initializes the logger object.
   */
  private void initLogger() {
    logger = Logger.getLogger(RTree.class.toString());
    logger.setLevel(loggerLevel);
  }

  /**
   * Encapsulates the attributes for deletion of leaf nodes.
   */
  private class Deletion {
    /**
     * The leaf node holding the entry to be deleted.
     */
    LeafNode leaf;

    /**
     * The index of the entry to be deleted.
     */
    int index;

    /**
     * Creates a new Deletion object with the specified parameters.
     *
     * @param leaf  the leaf node holding the entry to be deleted
     * @param index the index of the entry to be deleted
     */
    public Deletion(LeafNode leaf, int index) {
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


}
